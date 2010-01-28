package Labrador::Dispatcher::CrawlerAlloc::AnyHost;

use strict;
use base qw(Labrador::Dispatcher::CrawlerAlloc);

#if we're serving more than MAXMOVE crawlers then update_queue will become problematic
use constant MAXMOVE => 40;
use constant HOSTNAMES => 'Hostnames';
use URI;

=head1 NAME

Labrador::Dispatcher::CrawlerAlloc::AnyHost

=head1 DESCRIPTION

Simple CrawlerAlloc that assigns any request to any crawler. NOT suitable
for using with Delay modules on crawlers.

For more information, see Labrador::Dispatcher::CrawlerAlloc

=head1 METHODS

=over 4

=item init()

=cut
sub init
{
	my $self = shift;
	$self->{data}->register_variable(HOSTNAMES, 'HASH', 1);
	$self->{data}->register_variable("NextQueue", 'ARRAY', 2);
	$self->{hostnames} = $self->{data}->obtain_variable(HOSTNAMES);
	$self->{nextqueue} = $self->{data}->obtain_variable("NextQueue");

	#careful when setting a default, and allowed value is 0
	$self->{perhostdelay} = $self->{config}->get_scalar("HostDelay",0 );
	$self->{perhostdelay} = 60 unless defined $self->{perhostdelay};

	return $self->SUPER::init();
}

=item get_urls($crawler_hostname, $count)

Get $count urls for crawler $crawler_hostname

=cut

sub get_urls
{
	my ($self, $crawler_hostname, $count) = @_;

	my @urls; my $time = time;
	if (scalar @{$self->{nextqueue}} < $count)
	{
		$self->update_queues($count);
	}

	@urls = splice @{$self->{nextqueue}}, 0, $count;

	#TODO - what if @urls is empty?
	foreach my $host (map {URI->new($_)->host}@urls)
	{
		$self->{hostnames}->{$host} = $time;
	}
	$self->{urlstates}->url($_, -3) foreach (@urls);	
	return @urls;
}

=item update_queues()

Does nothing for this allocation method

=cut

sub update_queues
{
	my ($self, $count) = @_;
	$count ||=0;
	my $time = time;
	my @urls = $self->{urlalloc}->get_urls(MAXMOVE + $count);

	if ($self->{perhostdelay})
	{
		my @run_urls; my %delay_urls;
		my %delay_hostnames = ();
		#we have to delay URLs that are being visited too soon
		foreach my $url (@urls)
		{
			my $host= URI->new($url)->host;

			my $time = time;
			my $last_fetch_delay = $time - ($self->{hostnames}->{$host}||0);
			if ($last_fetch_delay < $self->{perhostdelay})
			{
				$delay_hostnames{$host} ||= 0;
				#delay this url, by at least enough time to ensure
				#it'll be hostdelay before it's next fetch
				$delay_urls{$url} = $delay_hostnames{$host} =
					$delay_hostnames{$host}  #time of predicted previous request for this host
					+ $self->{perhostdelay}  #add enough delay 
					- $last_fetch_delay; #subtract time of last request (TODO not sure)
			}
			else
			{
				#crawl this url
				push @run_urls, $url;
				$self->{hostnames}->{$host} = $time;
			}
		}
		
		#do something with %delay_urls;
		$self->{urlalloc}->delay_urls(%delay_urls) if scalar keys %delay_urls;		
		@urls = @run_urls;
		warn "Used ".scalar  @run_urls.", sent back ".(scalar keys %delay_urls)."\n"; 
		undef @run_urls;
	}
	push @{$self->{nextqueue}}, @urls;
}

=item empty_queues()

Does nothing for this allocation method

=cut

sub empty_queues
{
}

=item queue_sizes

Does nothing for this module

=cut


sub queue_sizes
{
	my $self = shift;
	return 0; 
}

=item get_stats

No stats in this module

=cut

sub get_stats
{

}

=head1 REVISION

	$Revision: 1.5 $

=cut

1;

