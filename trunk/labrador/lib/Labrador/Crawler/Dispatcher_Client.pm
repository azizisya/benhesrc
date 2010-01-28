package Labrador::Crawler::Dispatcher_Client;

use strict;
use IO::Socket;
use Sys::Hostname;

=head1 NAME

Labrador::Crawler::Dispatcher_Client

=head1 SYNOPSIS

	my $client = new Labrador::Crawler::Dispatcher_Client('localhost' 2460);
	$client->connect;
	my $forkcount = $client->WORK;
	$client->disconnect;

=head1 DESCRIPTION

This object provides an easy way for each sub crawler to talk to the dispatcher.
It encapsulates all possible VERBs (questions). 

For more information about how this class talks to the dispatcher, please refer to
docs/protocol.txt

=head1 METHODS

Methods are documented below. Most methods are actually VERB invocations and are
described under VERBS.

=over 4

=item new($dispatcher_hostname, $dispatcher_port, $name)

Instantiates the object, but does NOT open a connection to the dispatcher. This
is done separately using connect(). This is a two-state object (connection open,
and connection closed).

=cut

sub new
{
	my $package = shift;
	die "Dispatcher_Client requires more parameters\n" unless (scalar @_ == 2);
	my ($disp_host, $disp_port, $name) = @_;
	
	#my $var = {disp_host => $disp_host,
	#			disp_port => $disp_port,
	#			last_code => 0
	#			};
	
	my $self = bless {disp_host => $disp_host,
				disp_port => $disp_port,
				last_code => 0,
				name => $name},
				$package;
	$self->init();
	return $self;
}


sub init
{
	
}

=item connect

Opens a (TCP) connection to the dispatcher. Returns 1 if successfully connected
and protocol handshake succeeded, 0 otherwise.

=cut

sub connect
{
	my ($self, %capabilities) = @_;
	eval
	{
		$self->{'socket'} = new IO::Socket::INET(
			PeerAddr => $self->{disp_host},
			PeerPort => $self->{disp_port},
			Proto => 'tcp',
			Type => SOCK_STREAM
		) or die "Couldn't connect to ".$self->{disp_host}.':'.$self->{disp_port}.": $@\n";
	}; if ($@){ warn $@; return 0; }

	$self->{connected} = 1; #KLUDGE No1: we need to fake connected for _command() to work 
	my @args = map {"$_ ".$capabilities{$_}} keys %capabilities;		
	my ($status, $return) = $self->_command('HELO', $self->name, @args);

	if ($status == 200)
	{
		$self->{connected} = 1;
		return 1;
	}
	$self->{connected}=0; 
	return 0;
}

=item disconnect

Sends QUIT, and closes the (TCP) connection with the dispatcher.

=cut

sub disconnect
{
	my $self = shift;
	$self->_command('QUIT');
	my $socket = $self->{'socket'};
	close $socket;
	$self->{connected} = 0;

}

sub QUIT
{
	disconnect(@_);
}	

=back

=head1 VERBS

This object supports the following verbs, and they may be called as
described. For more protocol information refer to docs/protocol.txt

=over 4

=item WORK

Queries the dispatcher, to see how many clients a host fork

=cut

sub WORK
{
	my $self = shift;

	return 0 unless $self->{connected};
	my ($status, $return, $work) = $self->_command('WORK');

	if ($status == 205)
	{
		#print no work available, don't bother forking	
		return 0;
	}
	elsif ($status == 206)
	{
		#$work contains how many subs the dispatcher thinks
		#this crawler should fork
		return $work;
	}
	#invalid response
	return 0;

}

=item CONF

Obtain the configuration file from the dispatcher.
Returns an array of the configuration text file lines, or
an empty array if failure.

=cut

sub CONF
{
	my ($self) = @_;
	
	return () unless $self->{connected};
	my ($status, $return, @text) = $self->_command('CONF');

	if ($status == 201)
	{	#success
		return @text;
	}
	return ();
}

=item NEXT($n)

Obtain the next $n URLs to process from the dispatcher.

=cut

sub NEXT
{
	my ($self, $requested) = @_;
	$requested = '' unless $requested;
	return () unless $self->{connected};
	my ($status, $return, @URLs) = $self->_command('NEXT', $requested);

	if ($status == 205)
	{#no work to be done
		return ();
	}
	elsif ($status == 202)
	{
		#warn "We got ".scalar @URLs."URLs\n";
		return wantarray ? @URLs : $URLs[0];
	}
	else
	{
		#warn "Got ".$return."\n";
		return ();
	}

}

=item FINISHED($url, @links)

Ask the dispatcher to mark $url as finished, and add @links to the
master queue.

=cut

sub FINISHED
{
	my ($self, $url, @links) = @_;
	my ($status, $return) = $self->_command('FINISHED', $url, @links);
	if ($status == 203)
	{
		return 1;
	}
	warn "$status  $return\n";
	return 0;
}

=item FOUND($url, @links)

Ask dispatcher to add @links to the master queue, all having been found
on $url. Does NOT mark $url as finished.

=cut

sub FOUND
{
	my ($self, $url, @links) = @_;
	my ($status, $return) = $self->_command('FOUND', $url, @links);
	if ($status == 203)
	{
		return 1;
	}
	warn "$status  $return\n";
	return 0;
}


=item ALLOWED($url)

Returns the result of the URL filters of the dispatcher on this URL.
Implements local caching on the result of filtering $url. NB: this cache
could grow extremely large over pro-longed usage.

=cut

sub ALLOWED
{
    my ($self, $url) = @_;
	return $self->{ALLLOWED_CACHE}->{$url} if exists $self->{ALLLOWED_CACHE}->{$url};
    my ($status, $return) = $self->_command('ALLOWED', $url);
    if ($status == 203)
    {
		$self->{ALLLOWED_CACHE}->{$url} = 1;
        return 1;
    }
	$self->{ALLLOWED_CACHE}->{$url} = 0;
    return 0;
}

=item ROBOTS($hostnameport)

Asks the dispatcher for the robots.txt file for the hostname and port given.
(Joined by :). Will return ('#') for an empty file, or blank for not cached
by the dispatcher.

=cut

sub ROBOTS
{
	my ($self, $hostname) = @_;
	my ($status, $return, @file) = $self->_command('ROBOTS', $hostname);
	if ($status == 201)
	{
		return @file;
	}
	return ();
}

=item ROBOTSFILE($hostnameport, @file)

Submit a robots.txt (@file) for server running on specified $hostnameport
to the dispatcher, so that other crawlers can access it.

=cut

sub ROBOTSFILE
{
	my ($self, $hostname, @file) = @_;
	my ($status, $return) = $self->_command('ROBOTSFILE', $hostname, @file);
	if ($status == 203)
	{
		return 1;
	}
	return 0;
}

=item STATS(%stats)

Submit the stats of this subcrawler to the dispatcher where they can be
aggregated.

=cut

sub STATS
{
	my ($self, %stats) = @_;
	
	my ($status, $return) = $self->_command('STATS', '', map {"$_: ".$stats{$_}}keys %stats);
	if ($status == 203)
	{
		return 1;
	}
	return 0;
}

=item FAILED($url, $reason)

Inform the dispatcher that the retrieval of $url failed
because of $reason.

=cut

sub FAILED
{
	my ($self, $url, $reason) = @_;
	my ($status, $return) = $self->_command('FAILED', $url, $reason);
	if ($status == 203)
	{
		return 1;
	}
	return 0;
}

=item NOOP

Just checks a reply can be obtained from the dispatcher.
Useful for checking connectivity with a client.

=cut

sub NOOP
{
	my $self = shift;
	my ($status) = $self->_command('NOOP');
	if ($status == 203)
	{
		return 1;
	}
	return 0;
}

=item MONITOR

Obtain the stats hash from the dispatcher. Mainly used for
monitoring the progress of a crawl.

=cut

sub MONITOR
{
	my $self = shift;
    return () unless $self->{connected};
    my ($status, $return, @args) = $self->_command('MONITOR');

    if ($status != 210)
    {
		#TODO can this happen?
        return ();
    }
	my %hash;
	foreach (@args)
	{
		my ($key, $value) = split /\s*:\s*/;
		$hash{$key} = $value;
	}
	return %hash;
}

=item FINGERPRINT($md5, $url)

Checks the fingerprint with the dispatcher, and returns 
the url which it was seen at already, otherwise returns
0, having noted the fingerprint for future reference.

=cut

sub FINGERPRINT
{
	my ($self, $fingerprint, $url) = @_;
	return () unless $self->{connected};
	my ($status, undef, $where) = $self->_command('FINGERPRINT', $fingerprint, $url);
	
	return $where if $status == 201;
	return 0 if $status == 209;
}

=item IS_MODIFIED($url, $md5)

=cut

sub IS_MODIFIED
{
	my ($self, $url, $fingerprint) = @_;
	return () unless $self->{connected};
	my ($status) = $self->_command('IS_MODIFIED', $url, $fingerprint);
	return 1 if $status == 209;
	return 0;
}

=item SETDOCID($url, $docid)

=cut

sub SETDOCID
{
	my ($self, $url, $docid) = @_;
	return () unless $self->{connected};
	my ($status) = $self->_command('SETDOCID', $url, $docid);
	return 1 if $status == 201;
	return 0;
}

sub GETDOCDIR
{
	my ($self) = @_;
	return () unless $self->{connected};
	my ($status, undef, $newdir) = $self->_command('GETDOCDIR');
	return undef unless $status == 201;
	return $newdir; 
}


=item GETDOCID($url)

=cut

sub GETDOCID
{
	my ($self, $url) = @_;
	return () unless $self->{connected};
	my ($status, $docid) = $self->_command('GETDOCID', $url);
	return $docid if $status == 201;
	return 0;
}

=back

=head1 Private methods

These are documented for completeness, but should only be used internally.

=over 4

=item _command($command, $arg1, @args)

Sends a command with verb $command to the dispatcher.
$arg1 is appended to the verb (following a space).
@args are appended as separate lines to the request.
returns ($status_code, @all_returned_lines);

=cut

sub _command
{
	my $self = shift;
	my ($command, $arg1, @args) = @_;
	
    unless ($self->{connected})
	{
		$self->{last_code} = 300;	
		return 300, "300 Not Connected";
	}

	#build up the parameters of the command
	$command = uc $command;
	
	my @send;
	my $strSend = uc $command;
	$strSend .= " $arg1" if (defined $arg1 and length $arg1);
	push @send, $strSend;
	push @send, @args if (scalar @args);
	#send the lines
	$self->_sendlines(@send, '.');

	my $socket = $self->{'socket'};	
	
	my @answers = ();
	#now read until we hit \n.\n, or we hit a disconnect
	do
	{
		$_ = $self->_getline if $socket->connected;
		if (defined $_)
		{
			chomp;
			push(@answers, $_);
		}
	} until (! $socket->connected or (scalar @answers && $answers[$#answers] eq '.'));
	if (! $socket->connected)
	{
		$self->{last_code} = 300;
		return 300, "300 Not Connected";
	}	

	pop @answers; #don't need . on the array

	#obtain the status code
	my ($status) = $answers[0] =~ m/^(\d+)/;
	$self->{last_code} = $status;
	return ($status, @answers);
}

=item _sendlines(@lines)

Code copied from Net::Cmd. Uses syswrites to write to the secoket.

=cut

sub _sendlines
{
	my $self = shift; #lines in @_
	return unless @_;
	local $SIG{PIPE} = 'IGNORE' unless $^O eq 'MacOS';
	my $str = join "\n", @_;
	$str .= "\n";
	my $len = length $str;
	my $offset = 0; my $socket = $self->{'socket'};
	while($len) #handle partial writes
	{
		my $written = syswrite($socket, $str, $len, $offset);
		die "System error writing to dispatcher: $!\n"
			unless defined $written;
		$offset += $written;
		$len -= $written;
	}
}

=item _getline()

Reads lines using sysread. Handles partial reads! Code shamelessly stolen
from Net::Cmd, which is the basis for Net::FTP among other modules.

=cut

sub _getline
{
	my $self = shift;
	my $socket = $self->{socket};
 
	$self->{'net_lines'} ||= [];

	return shift @{$self->{'net_lines'}}
		if scalar(@{$self->{'net_lines'}});

	my $partial = defined($self->{'net_partial'})
		? $self->{'net_partial'} : "";

	my $fd = fileno($socket);

	return undef unless defined $fd;

	my $rin = "";
	vec($rin,$fd,1) = 1;

	my $buf;

	until(scalar(@{$self->{'net_lines'}}))
	{
		my $timeout = $self->{timeout} || undef; #TODO set a default for timeout
		my $rout;
		if (select($rout=$rin, undef, undef, $timeout))
		{
			unless (sysread($socket, $buf="", 1024))
			{
				warn("Unexpected EOF on TCP connection to dispatcher. Socket closed");
					#if $cmd->debug; #TODO
				$socket->close;
				return undef;
			} 

			substr($buf,0,0) = $partial; # prepend from last sysread

			my @buf = split(/\015?\012/, $buf, -1); # break into lines
			$partial = pop @buf;

			push(@{$self->{'net_lines'}}, map { "$_\n" } @buf);

		}
		else
		{
			warn("Dispatcher Client: Timeout"); #if($cmd->debug); #TODO
			return undef;
		}
	}

	$self->{'net_partial'} = $partial;

	shift @{$self->{'net_cmd_lines'}};
}

=item name

Returns the name of this crawler. Currently set to hostname:processid.

=cut

sub name
{
	return $_[0]->{name} || (hostname().':'.$$);
}

=item last_result_code()

Returns the last status code from the last command executed.
Returns 0 if no command has yet been executed.

=cut

sub last_result_code
{
	return $_[0]->{last_code};
}

=back

=head1 REVISION

	$Revision: 1.24 $

=cut

1;
