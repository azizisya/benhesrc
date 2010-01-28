package Labrador::Dispatcher::Commands;
use strict;
use lib '../../';
use Labrador::Common::RobotsCache;
use Digest::MD5 qw(md5_base64);

=head1 NAME

Labrador::Dispatcher::Commands

=head1 DESCRIPTION

This module contains all the event handlers for all verbs.

=head1 METHODS

=over 4

=item new($config)

Constructor. Called init()

=cut

sub new
{
	my ($class, $config) = @_;
	my $self = bless {config => $config}, $class;
	return $self;

}

=item init($event_handler)

Registers all commands with the event handler, at their appropraite privielege level.

=cut

sub init
{
	my ($self, $event_handler) = @_;
	$self->{'event_handler'} = $event_handler;

	$event_handler->register_command('HELO', sub {$self->helo(@_)}, 0);	
	$event_handler->register_command('QUIT', sub {$self->quit(@_)}, 0);
	
	#not sure what the privilege level of NOOP should be
	$event_handler->register_command('NOOP', sub {$self->noop(@_)}, 0);

	#unpriveleged, but logged on commands
	$event_handler->register_command('MONITOR', sub {$self->monitor(@_)} ,1);
	$event_handler->register_command('WORK', sub {$self->work(@_)}, 1);
	#todo should intial crawler master processes have a differnet priv level
	$event_handler->register_command('CONF', sub {$self->conf(@_)}, 1);

	#crawler commands
	$event_handler->register_command('NEXT', sub {$self->next(@_)}, 3);
	$event_handler->register_command('FINISHED', sub {$self->finished(@_)}, 3);
	$event_handler->register_command('FOUND', sub {$self->found(@_)}, 3);
	$event_handler->register_command('FAILED', sub {$self->failed(@_)}, 3);
	$event_handler->register_command('ROBOTS', sub {$self->robots(@_)}, 3);
	$event_handler->register_command('ROBOTSFILE', sub {$self->robotsfile(@_)}, 3);
	$event_handler->register_command('STATS', sub {$self->stats(@_)}, 3);
	$event_handler->register_command('FINGERPRINT', sub {$self->fingerprint(@_)}, 3);

	#admin commands
	$event_handler->register_command('SHUTDOWN', sub {$self->shutdown(@_)}, 5);
	$event_handler->register_command('ALLOWED', sub {$self->allowed(@_)}, 5);
	$event_handler->register_command('CHECKPOINT', sub {$self->checkpoint(@_)}, 5);
	$event_handler->register_command('PAUSE', sub {$self->pause(@_)}, 5);
	$event_handler->register_command('START', sub {$self->start(@_)}, 5);
	$event_handler->register_command('EVAL', sub {$self->eval(@_)}, 5);
	#TODO implement STOP?


	#other initialisation
	$self->{robotscache} = new Labrador::Common::RobotsCache($self->{config});
	$self->{adminpassword} = $self->{config}->get_scalar('AdminPassword', 0);
}

=item helo($invocation)

Logon a client.

=cut

sub helo
{
	my ($self, $invocation) = @_;
	my $clientname = shift @{$invocation->{args}} || '';
	
	if ($clientname !~ /.+:\d+/)
	{
		return "304 Bad Syntax\n";
	}

	my %priv_requests;
	foreach (@{$invocation->{args}})
	{
		my @line = split /\s+/;
		$priv_requests{lc($line[0])} = $line[1] || 1;
	}
		
	my $privs = 1;
	if ($priv_requests{'crawler'})
	{
		$privs |= 2;
	}
	if ($priv_requests{'admin'})
	{
		if (md5_base64($priv_requests{'password'}) eq $self->{adminpassword})
		{
			$privs |= 4;
		}
		else
		{
			return "304 Bad Password\n";
		}
	}
	($privs & 2) and Labrador::Dispatcher::Processing::register_crawler($clientname);
	$self->{'event_handler'}->client_authorise($invocation->{'client'}, $clientname, $privs);
	
	warn "$clientname($privs) connected\n";	
	#TODO put some a version string here?
	return "200 Hello $clientname\n";
}

=item quit($invocation)

Disconnect a client.

=cut

sub quit
{
	my ($self, $invocation) = @_;
	Labrador::Dispatcher::Processing::crawler_disconnect($invocation->{clientname});
	$self->{'event_handler'}->client_disconnect($invocation->{'client'});
	return "208 Bye!\n";
}

=item work($invocation)

Any work to be done?

=cut

sub work
{
	my ($self, $invocation) = @_;
	#TODO I still think this is a prototype - how about some intelligence
	#about how many clients a crawler should fork
	my $queued = (Labrador::Dispatcher::Processing::masterqueue_size() > 0
		or Labrador::Dispatcher::Processing::crawlerqueue_size() > 0);
	#use a safe default that doesn't fork too much
	my $forks = $self->{config}->get_scalar('ForksPerCrawler', 1) || 1;
	return $queued
		? "206 Work to be done\n$forks\n" 
		: "205 No work\n";
}

=item noop

Do nothing - a ping essentially.

=cut

sub noop
{
	my ($self, $invocation) = @_;
	return "203 OK\n";
}

=item conf($invocation)

Sends the configuration file to the client.

=cut

sub conf
{
	my ($self, $invocation) = @_;
	my $return = "201 Config follows\n";
	my @config = $self->{config}->get_file_text();
	
	#don't let anyone have the admin password
	@config = grep {!/adminpassword/} @config;

	$return	.= join "\n", @config;
	$return .= "\n";
	return $return;
}

=item next($invocation)

Obtain the next URLs to crawl

=cut

sub next
{
	my ($self, $invocation) = @_;
	my $count = $invocation->{args}->[0] || 1;

	#my ($crawler) = split /:/, $invocation->{clientname};

	my @new_urls = Labrador::Dispatcher::Processing::get_urls(
		$invocation->{clientname}, 
		$count);

	if (! scalar @new_urls)
	{
		return "205 No Work\n";
	}

	my $return = "202 URLs follow\n";
	foreach my $url (@new_urls)
	{
		$return .= "$url ".Labrador::Dispatcher::Processing::linking_url($url);
		$return .= "\n";
	}
	return $return;	
}

=item finished($invocation)

Mark this URL as finished.

=cut

sub finished
{
	my ($self, $invocation) = @_;

	my $done_url = shift @{$invocation->{args}};
	$done_url =~ s/\s//g;
	Labrador::Dispatcher::Processing::finished_url($done_url);

	#add all new urls to queue	
	foreach my $new_url (@{$invocation->{args}})
	{
		$new_url =~ s/\s//g;
		Labrador::Dispatcher::Processing::new_url($new_url, $done_url);
	}
	return "203 Thanks\n";
}

=item found($invocation)

=cut

sub found
{
	my ($self, $invocation) = @_;
	my $done_url = shift @{$invocation->{args}};
	$done_url =~ s/\s//g;
	#add all new urls to queue
    foreach my $new_url (@{$invocation->{args}})
	{
		$new_url =~ s/\s//g;
		Labrador::Dispatcher::Processing::new_url($new_url, $done_url);
	}
	return "203 Thanks\n";
}

=item failed($invocation)

Mark this URL as failed.

=cut

sub failed
{
	my ($self, $invocation) = @_;
	my $url = shift @{$invocation->{args}};
	$url =~ s/\s//g;
	Labrador::Dispatcher::Processing::failed_url($url);
	return "203 Thanks\n";
}

=item allowed($invocation)

Returns whether a URL is OK to crawl or not. Not used by any client code currently.

=cut

sub allowed
{
	my ($self, $invocation) = @_;
    my $url = shift @{$invocation->{args}};
	return Labrador::Dispatcher::Processing::filter($url)
			? "203 OK to crawl\n" 
			: "205 Denied by filters\n";
}

=item robots($invocation)

Sends back the robots.txt file for the mentioned host if available.

=cut

sub robots
{
	my ($self, $invocation) = @_;
	my $hostname = shift @{$invocation->{args}};
	return "304 Bad Syntax\n" unless $hostname;
	$hostname =~s/\s//g;
	my @file = $self->{robotscache}->get_file($hostname);
	if (scalar @file)
	{
		return "201 Robots.txt follows\n".join("\n", @file)."\n";	
	}
	else
	{
		return "209 Not yet\n";
	}
}

=item fingerprint($invocation)

Marks the provided fingerprint as seen.

=cut

sub fingerprint
{
	my ($self, $invocation) = @_;
	my $md5 = shift @{$invocation->{args}};
	my $url = shift @{$invocation->{args}};
	my $rtr;
	my $where = Labrador::Dispatcher::Processing::have_fingerprint($md5);
	if ($where)
	{
		$rtr = "201 Seen already\n$where\n";
	}
	else
	{	
		Labrador::Dispatcher::Processing::add_fingerprint($md5, $url);
		$rtr = "209 First seen\n";
	}
	return $rtr;
}


=item robotsfile($invocation)

Adds the provided robots.txt file to the cache.

=cut

sub robotsfile
{
	my ($self, $invocation) = @_;
	my $hostname = shift @{$invocation->{args}};
	$hostname =~s/\s//g;
	return "304 Bad Syntax\n" unless $hostname;
	$self->{robotscache}->set_file($hostname, @{$invocation->{args}});
	return "203 Thanks\n";
}

=item stats($invocation)

Adds submitted stats to running totals.

=cut

sub stats
{
	my ($self, $invocation) = @_;
	
	my %stats;
	foreach (@{$invocation->{args}})
	{
		my ($key, $value) = split /\s*:\s*/, $_;
		$stats{$key} = $value;
	}
	
	#use this instead?
	#my ($crawler) = split /:/, $invocation->{clientname};
	Labrador::Dispatcher::Processing::submit_stats($invocation->{clientname}, %stats);
	return "203 Thanks\n";
}

=item monitor($invocation)

Obtains the dispatchers running totals.

=cut

sub monitor
{
	my ($self, $invocation) = @_;
	my $stats = Labrador::Dispatcher::Processing::get_stats();
	return "210 Statistics Follow\n" .join("\n", map{"$_: ".$stats->{$_}} keys %{$stats})."\n";
}

=item checkpoint($invocation)

Force a checkpoint of all data structures to disk.

=cut

sub checkpoint
{
	my ($self, $invocation) = @_;
	Labrador::Dispatcher::Processing::checkpoint();
	return "203 OK\n";
}

=item shutdown($invocation)

Shutdown the dispatcher

=cut

sub shutdown
{
	my ($self, $invocation) = @_;
	Labrador::Dispatcher::Connections::shutdown();
	return "203 Ok\n";
}

=item pause($invocation)

Temporarily pause the dispatcher from allocating more URLs.

=cut

sub pause
{
	my ($self, $invocation) = @_;
	return Labrador::Dispatcher::Processing::pause() ? "203 Ok\n" : "209 Already paused!\n";
}

=item start($invocation)

Restart the dispacher after a pause.

=cut

sub start
{
	my ($self, $invocation) = @_;
	return Labrador::Dispatcher::Processing::start() ? "203 Ok\n" : "209 Not paused!\n";
}

=item eval($invocation)

Eval this code. EXPERIMENTAL

=cut

sub eval
{
	my ($self, $invocation) = @_;
	return "203 Done\n".Labrador::Dispatcher::Processing::eval(join "\n", @{$invocation->{args}});
}

=back

=head1 REVISION

	$Revision: 1.28 $

=cut

1;
