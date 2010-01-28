package Labrador::Common::URLAlloc::Delay;

use lib '../../../';
use base qw(Labrador::Common::URLAlloc);
use Labrador::Common::DNSLookup;
use DelayLine;
use strict;

=head1 NAME 

Labrador::Common::URLAlloc::Delay

=head1 SYNOPSIS

	use Labrador::Common::URLAlloc;
	use Labrador::Common::URLAlloc::Delay;
	my $urlalloc = Labrador::Common::URLAlloc::new(
		'Labrador::Common::URLAlloc::Delay',
		$data, $urlstates, $config);
	$urlalloc->new_url('http://www.gla.ac.uk/#');
	print $urlalloc->get_url();

=head1 DESCRIPTION

This is a URLAlloc class implementing Host Delay support. Queuing
is done in a breadth-first fashion, but without respect only for
URLs in hosts. Queues between hosts are ignored.

NB: Not all methods are described here, some are inherited from
URLAlloc parent class.

=head1 METHODS

=over 4

=item init()

Initialisatoin. Called automatically by new()

=cut

sub init
{
	my ($self, %options) = @_;
	$self->{delayline} = new DelayLine();
	$self->{hostdelay} = $self->{config}->get_scalar('HostDelay', 1) || 5;
	$self->{queues} = {}; #a hash of arrays #TODO some form of Data comppatability
	
	$self->{lasthostfetch} = {}; #TODO data obj?
	$self->{hostqueued} = {}; #TODO data obj?
	#$self->{lasthostdelay} = {}; #TODO data obj?
	return 1;
}

=item next()

How long to wait until the next URL will be ready to fetch

=cut

sub next
{
	my $self = shift;
	$self->{delayline}->dump;
	return $self->{delayline}->next();
}

=item new_url($url)

Enqueue $url

=cut

sub new_url
{
	shift->new_urls(@_);
}

=item new_urls(@urls)

Enqueue all the @urls.

=cut

sub new_urls #assumes that all of @_ are either URI objects, or string URLs
{
	my $self = shift;
	my $time = time;

	#check if we've got URI objects, or just URL strings
	my $URI_objs = 0;
	if (ref($_[0]) =~ /^URI/)
	{
		$URI_objs = 1;
	}
	
	my @delays;
	foreach my $url(@_) #for every url
	{
		#get a URL object for each URL
		my $uri;
		if ($URI_objs)
		{
			$uri = $url;
			$url = "$uri";
		}
		else
		{
			$uri = URI->new($url);
		}

	
		my $hostname = $self->hash( $uri );
		#my $hostname = $uri->host;
		
		if (! exists $self->{hostqueued}->{$hostname})
		{
			#we don't currently have any urls for this host
			my $lastfetch = $self->{lasthostfetch}->{$hostname};
			my $delay;
			if (defined $lastfetch)
			{
				#but it is a host we have seen before
				
					#if hostdelay has expired since last fetch
				$delay = ($time - $lastfetch >= $self->{hostdelay}) 
					? 0
					: $self->{hostdelay} - ($time - $lastfetch);
			}
			else
			{
				#host never seen before
				$delay = 0;
			}
			warn "Queued $hostname for $delay\n";
			$self->{hostqueued}->{$hostname} = 1;
			push @delays, $hostname, $delay;
		}
		#enqueue this URL onto that hosts queue
		$self->{queues}->{$hostname} ||= [];
		push @{$self->{queues}->{$hostname}}, $uri;
		
	}
	$self->{delayline}->ins(@delays);
}

=item get_urls([$count])

Retrieves as many URLs upto $count as are allowed based
on the delay line.

=cut

sub get_urls
{
	my ($self, $count) = @_;
	$count ||=1;
	
	my @urls; my $url; my $time = time; my $hostname;
	my @delays;
	for (;$count>0 and defined($hostname = $self->{delayline}->out()); $count--)
	{
		
		push @urls, "".shift @{$self->{queues}->{$hostname}}; #TODO can we use URI object as much as possibe
		
		#mark the last time we fetched a url from this hostname
		$self->{lasthostfetch}->{$hostname} = $time;
		
		#requeue this host in the delayline if it's still got work to do
		if (@{$self->{queues}->{$hostname}})
		{
			warn "Requeued $hostname for ".$self->{hostdelay}."\n";
			push @delays, $hostname, $self->{hostdelay};
		}
		else
		{
			delete $self->{hostqueued}->{$hostname};
		}
	}
	$self->{delayline}->ins(@delays);
	return @urls;
}

=item queue_size()

Returns the size of all the host queues, and if called in array
context, how many hosts are queued in the delay line.

=cut

sub queue_size
{
	my $self = shift;
	my $sum = 0;
	$sum += scalar @{$self->{queues}->{$_}} foreach (keys %{$self->{queues}});
	return wantarray ? ($sum, $self->{delayline}->size) : $sum;
}

=item empty_queues

Empties all the queues

=cut

sub empty_queues
{
	my $self = shift;
	$self->{delayline}->{_LINE} = [];
	$self->{lasthostfetch} = {};
	$self->{queues} = {};
}

=item hash

Returns the hash of the uri such that it can be partitioned for throttling purposes.

=cut

sub hash
{
	my $self = shift;
	my $uri = shift;
	#return $uri->host;
	return Labrador::Common::DNSLookup::lookup($uri->host);
}

=back

=head1 REVISION

	$Revision: 1.7 $

=cut

1;
