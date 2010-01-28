package Labrador::Dispatcher::Connections;
use POSIX;
use IO::Socket;
use IO::Select;
use Socket;
use Fcntl;
use Tie::RefHash;
use IO::Handle; #easier autoflushing on filehandles

use lib '../..';
use Labrador::Dispatcher::Events;
use strict;


=head1 NAME

Labrador::Dispatcher::Connections

=head1 SYNOPSIS

	Labrador::Dispatcher::Connections::run(2680, $event_handler);
	warn "Server has been shutdown\n";
	exit;

=head1 DESCRIPTION

This class performs the non-blocking network IO with each subcrawler. It passes on
command events to the event handler object, whose job it is to pass the command on
to the appropriate command handler. Other events are handled by the events handler, 
such as unexpected disconection of a client.

This class is a singleton - ie it cannot be instantiated as an object. The non-blocking
network code is heavily based on Recipe 17.17 in the Perl Cookbook, with some alterations
where I disagree with its handling of partial sends and EWOULDBLOCK. Further reference
should be made to Richard W. Stevens book on Unix Network Progamming.

=head1 EVENTS

This class calls several methods of $event_handler to represent events occurring.
The event handling methods are expected to exist in the provided $event_handler.. 
They are listed below:

NB: Event handling code is not permitted to write to $client at any time.

=over 4

=item event_connect($client)

Fired when a network connection is accepted. $client contains the socket. 

=item event_disconnect($client)

Fired when a network connection is dropped unexpectedly. This module assumes
it is it's own responsibilty to disconnect clients when they ask. Clients
disconnecting of their own accord is an error condition. $client contains
the socket.

=item event_command($invocation)

Fired when an entire command sequence is received on a network connection.
$invocation is a reference to a hash. This hash contains details of the
invocation - view the code of _handle for more infomation.

=back

=head1 FUNCTIONS

=over 4

=cut

#module level variables

my $select;

# begin with empty buffers
my %inbuffer  = ();
my %outbuffer = ();
my %ready     = ();

my @to_disconnect = ();

my $event_handler;
my $logging = 0;
my $logger_fileh;
$logging = 0;
my ($bytes_in, $bytes_out, $accepts, $clients) = (0,0,0,0);
my $run = 1;

my %alarms = ();
my %alarm_times = ();

=item run($port, $event_handler)

Starts up all the network gubbins. Does not return unless a shutdown is 
signalled by calling shutdown().

=cut


sub run
{
	my $port = shift;
	$event_handler = shift;


	# Listen to port. ReuseAddr ensures the port is available
	# should the program fail to shutfown properly
	my $server = IO::Socket::INET->new(	LocalPort => $port,
										ReuseAddr => 1,
										Listen    => 10 )
		or die "Can't make server socket: $@\n";

	#special hash for keying on references
	tie %ready, 'Tie::RefHash';
	
	#make the server socket non-blocking
	_nonblock($server);
	$select = IO::Select->new($server);

	# Main loop: check reads/accepts, check writes, check ready to process
	while ($run)
	{
		my $client;
		my $rv;
		my $data;

		# check for new information on the connections we have

		# anything to read or accept?
		foreach $client ($select->can_read(1)) {

			if ($client == $server) {
				# accept a new connection

				$client = $server->accept();
				$accepts++; $clients++;
				$select->add($client);
				_nonblock($client);
				#warn "accepted socket from ".$client->peerhost()."\n";
				$event_handler->event_connect($client);

			} else {
				# read data
				$data = '';
				$rv  = $client->recv($data, POSIX::BUFSIZ, 0);

				unless (defined($rv) && length $data) {
	
					# This would be the end of file, so close the client
			
					$event_handler->event_disconnect($client);
					_disconnect($client);
					next;
				}

				#this makes the server telnet compatable, because
				#telnet sends \r\n instead of \n.
				$data =~ s/\r\n/\n/g;

				$inbuffer{$client} .= $data;
				$bytes_in += length $data;

				# test whether the data in the buffer or the data we
				# just read means there is a complete request waiting
				# to be fulfilled.  If there is, set $ready{$client}
				# to the requests waiting to be fulfilled.

				#this used to be regex, but perl 5.8.(0,2) has
				#problems with big commands (c6000 lines)
				while ((my $i = index($inbuffer{$client}, "\n.\n")) > -1)
				{
					#remove everything upto the 2nd last \n
					push @{$ready{$client}}, 
						substr $inbuffer{$client}, 0, $i+1, '';
					#remove the .\n
					substr $inbuffer{$client}, 0, 2, '';
				}
			}
		}

		# Any complete requests to process?	
		if (scalar keys %ready)
		{
			foreach $client (keys %ready) {
				_handle($client);
			}
		}
		else
		{
			my $ran_something = 0;
			if (keys %alarms)
			{
				foreach my $alarmname (keys %alarms)
				{
					if ((time - $alarm_times{$alarmname}) 
						>= $alarms{$alarmname}->[0])
					{
						warn "Running idler $alarmname\n";
						$ran_something = 1;
						my $coderef = $alarms{$alarmname}->[1];			
						&$coderef();
						$alarm_times{$alarmname} = time;						
					}
				}
			}
			
			unless($ran_something)
			{
				#nothing to do, give up some of our idling
				#time to do something else
				$event_handler->event_idle();
			}
		}

		# Buffers to flush?
		foreach $client ($select->can_write(1))
		{
			# Skip this client if we have nothing to say
			next unless exists $outbuffer{$client};

			$rv = send($client, $outbuffer{$client}, 0);

			if ($! == POSIX::EWOULDBLOCK)
			{
				#caught a EWOULDBLOCK. Ignore.
			}
			elsif (! defined $rv)
			{
				# Whine, but move on.
				warn "I was told I could write, but I can't : $!\n";
				next;
			}
			elsif ($rv) 
			{
				substr($outbuffer{$client}, 0, $rv) = '';
				warn "successfully wrote $rv bytes\n" if length $outbuffer{$client};
				delete $outbuffer{$client} unless length $outbuffer{$client};
			} 
			else
			{
				# Couldn't write all the data, and it wasn't because
				# it would have blocked.  Shutdown and move on.
				warn "Failed to write all data, \$rv was $rv, \$! was ".($!+0).", length was ".(length $outbuffer{$client})."\n";
				$event_handler->event_disconnect($client);
				_disconnect($client);
				next;
			}
		}

		# Out of band data?
		foreach $client ($select->has_exception(0)) {  # arg is timeout
			# Deal with out-of-band data here, if you want to.
		}

		foreach $client (@to_disconnect)
		{
			_disconnect($client);
		}
		@to_disconnect = ();
	}
}


=item shutdown()

Close all sockets, causes a return from run()

=cut

sub shutdown
{
	foreach my $client ($select->handles)
	{
		#TODO should this really be done this way.
		#How about using disconnect_client instead?
		_disconnect($client);
		
	}
	$run = 0;
	#TODO finish shutdown?
}

=item disconnect_client($client)

$client will be disconnected a the end of the current iteration
of the mail while(1) loop.
By delaying the disconnection until the end of the iteration, this 
means the final message reply to the client can be written.

=cut

sub disconnect_client
{
	my $client = shift;
	push @to_disconnect, $client;
}

=item ip($client)

Returns the IP address of $client

=cut

sub ip
{
	my $client = shift;
	return $client->peerhost();
}

=item idle_alarm($every, $name, $coderef)

Run this $coderef every $every seconds, called $name.

=cut

sub idle_alarm
{
	my ($seconds, $name, $coderef) = @_;
	$alarms{$name} = [$seconds, $coderef];
	$alarm_times{$name} = time;
}



=item get_stats()

Returns an array of network level stats

=cut

sub get_stats
{
	return ($bytes_in, $bytes_out, $accepts, $clients, 
	$event_handler->{'command_invocations'});
	#TODO shouldn't there be a method for querying the event
	#handler's stats?
}

=item log_to_file($filename)

Enables connection logging to $filename.

=cut

sub log_to_file
{
	my $filename = shift;
	if (open($logger_fileh, ">$filename"))
	{
		$logger_fileh->autoflush(1);
		$logging = 1;	
	}
	else
	{
		warn "Couldn't log to file";
	}
	
}

=item log_off()

Disabled connection logging.

=cut

sub log_off
{
	$logging = 0;	
}


=back

=head1 PRIVATE METHODS

NB: Should not be called by client code, but documented here for completeness.

=over 4

=item _disconnect($client)

Handle the disconnection of $client. Called by disconnect_client() (indirectly), and also 
when an error occurs when reading or writing to a socket, and the connection should be 
dropped. Handles removal of all lowlevel mapping - ie per-client buffers.

=cut

sub _disconnect
{
	my $client = shift;
	$clients--;
	delete $inbuffer{$client};
	delete $outbuffer{$client};
	delete $ready{$client};
	$select->remove($client);
	close $client;
}

=item _handle($client)

Deals with all pending requests from $client, by taking exactly one
command from the start of the client's ready buffer, processing the request
(by calling forward to the Event handler), and the supplying the returned
data to the client by appending it to the client's outbuffer.

=cut

sub _handle {
	# requests are in $ready{$client}
	# send output to $outbuffer{$client}
	my $client = shift;
	my $request;

	foreach $request (@{$ready{$client}}) {
		# $request is the text of the request
		# put text of reply into $outbuffer{$client}
	
		#put in the .\n at end, as it's not matched into
		#the ready buffer, but debugging is less confusing
		#with it put back in	
		_log('IN', $client, $request.".\n");

		#think this is less harsh on the Perl regex engine
		$request =~ s/^(\w*)\s+//;
		my $verb = $1;
		#my ($verb, $args) = $request =~ m/^(\w*)\s+(.*)/s;

		my @args = split /\n/, $request;
		chomp @args; #remove line endings 
		
		#build up the details of each invocation of a protocol VERB
		my $invocation = { verb => uc $verb, args => \@args, client => $client};
		
		my $return = $event_handler->event_command($invocation);
		$return .= ".\n";

		_log('OUT', $client, $return);
		
		$outbuffer{$client} .= $return;
		$bytes_out += length $outbuffer{$client};
				
	}
	delete $ready{$client};
}

=item _nonblock($socket)

Puts the given $socket into non-blocking mode.

=cut

sub _nonblock 
{
	my $socket = shift;
	my $flags;
	
	$flags = fcntl($socket, F_GETFL, 0)
		or die "Can't get flags for socket: $!\n";
	fcntl($socket, F_SETFL, $flags | O_NONBLOCK)
		or die "Can't make socket nonblocking: $!\n";
}

=item _log($state, $client, $data)

=cut

sub _log
{
	return unless $logging;
	my ($state, $client, $data) = @_;
	foreach my $line (split /\n/, $data)
	{
		next if $line =~ /^\s+$/;
		print($logger_fileh "$state [".ip($client)."]: $line\n")
			or $logging = 0;
	}
}

=back

=head1 REVISION

	$Revision: 1.18 $

=cut

1;
