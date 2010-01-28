package Labrador::Dispatcher::CrawlerAlloc::PerHost;

use strict;
use base qw(Labrador::Dispatcher::CrawlerAlloc);

#if we're serving more than MAXMOVE crawlers then update_queue will become problematic
use constant MAXMOVE => 100;
use constant HOSTNAMES => 'Hostnames';
use URI;

=head1 NAME

Labrador::Dispatcher::CrawlerAlloc::PerHost

=head1 DESCRIPTION

THIS MODULE IS DEPRECATED. Do not use.

=head1 METHODS

=over 4

=cut

sub init
{
	my $self = shift;
	$self->{data}->register_variable(HOSTNAMES, 'HASH', 1);
	$self->{hostnames} = $self->{data}->obtain_variable(HOSTNAMES);
	return $self->SUPER::init();
}

sub get_urls
{
	my ($self, $crawler_hostname, $count) = @_;
	
	my @urls = splice @{$self->{crawlers}->{$crawler_hostname}}, 0, $count;
	if ( ! scalar @urls)
	{
		warn "My crawler didn't have any URLs enqueued - forcing update_queues\n";
		if (! $self->update_queues)
		{
			return ();
		}
	}

	@urls = splice @{$self->{crawlers}->{$crawler_hostname}}, 0, $count;
	$self->{urlstates}->url($_, -3) foreach (@urls);
	return @urls;
}

=item update_queues()

move upto MAXMOVE urls into crawler queues

=cut

sub update_queues
{
	my $self = shift;
	#OK, let's move upto MAXMOVE urls into crawler queues

	#take MAXMOVE urls off the master queue
	my @to_allocate = $self->{urlalloc}->get_urls(MAXMOVE);

	my $URLsmoved = scalar @to_allocate;
	return undef unless $URLsmoved;

	#plan - round robin @to_allocate urls round each crawler
	#unless we've seen the hostname before, in which case
	#it goes to the same crawler it went to before IF that crawler 
	#still exists
	my @crawlers = keys %{$self->{crawlers}};

	my $last_queue = 0;
	for (my $i=0; $i<$URLsmoved; )
	{
		#obtain the hostname
		my $hostname = URI->new($to_allocate[$i])->host;

		#if the hostname has been seen before
		if (exists $self->{hostnames}->{"$hostname|crawler"})
		{
			my $crawler = $self->{hostnames}->{"$hostname|crawler"};
			#this hostname had a crawler, but it no longer exists
			if (! exists $self->{crawlers}->{$crawler})
			{
				delete $self->{hostnames}->{"$hostname|crawler"};
				next;	
			}
			warn "Allocating $to_allocate[$i] to $crawler cos last time it was\n";
			#allocate the url to the same crawler as last time
			push @{$self->{crawlers}->{$crawler}}, $to_allocate[$i];
			#change the recorded state of the URL		
			$self->{urlstates}->url($to_allocate[$i], -2);	
			#and move to the next url
			$i++;
		}
		else
		{
			#allocate the next url to the next crawler
			warn "Allocating $to_allocate[$i] to $crawlers[$last_queue]\n";
			push @{$self->{crawlers}->{$crawlers[$last_queue]}}, $to_allocate[$i];
			#change the recorded state of the URL
			$self->{urlstates}->url($to_allocate[$i], -2);
			#record which crawler we gave the hostname to
			$self->{hostnames}->{"$hostname|crawler"} = $crawlers[$last_queue];
			#next URL
			$i++;
			#round robin to next crawler
			$last_queue = ($last_queue+1) % scalar @crawlers;
		}
	}
	return $URLsmoved; 	
}

sub empty_queues
{
	my $self = shift;
	foreach my $crawler (keys %{$self->{crawlers}})
	{
		#TODO what happens to the state of URLs that we've just dumped?
		$self->{crawlers}->{$crawler} = {};
	}
}

sub queue_sizes{}
sub get_stats{}

1;
