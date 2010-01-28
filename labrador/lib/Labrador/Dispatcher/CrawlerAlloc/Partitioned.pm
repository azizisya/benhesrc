package Labrador::Dispatcher::CrawlerAlloc::Partitioned;

#NB: we can assume that crawlers have their own queue, hence not worry about the host delay

use constant MAXMOVE => 100;
use constant NAMESPACE_PARTITION => 'Labrador::Common::Partition';
use base qw(Labrador::Dispatcher::CrawlerAlloc);
use strict;

=head1 NAME

Labrador::Dispatcher::CrawlerAlloc::Partitioned

=head1 DESCRIPTION

A partitioning Crawler Allocator. URLs are allocated to crawlers
based on the partition they fall in. This is done using Labrador::Common::Partition::*
modules.

=head1 METHODS

=over 4

=item init()

=cut

sub init
{
	my $self = shift;

    #load Partition module
	my ($partition, @params1) = $self->{config}->get_scalar('Partition', 1);
	$partition = NAMESPACE_PARTITION.'::'.($partition || 'SeenHost');
	_load_module($partition);
	$self->{partitioner} = Labrador::Common::Partition::new(
		$partition, $self->{data}, @params1);

	$self->{data}->register_variable("Crawlers", "HASH");
	$self->{crawlers} = $self->{data}->obtain_variable("Crawlers");

}

=item get_urls($self, $crawler_name, $count)

Returns an array of $count URLs that are in the partition named $crawler_name.

=cut

sub get_urls
{
	my ($self, $crawler_name, $count) = @_;

	#if the queue is empty, fill it up a bit
	if (! @{$self->{crawlers}->{$crawler_name}})
	{
		$self->update_queues(MAXMOVE);
	}

	return splice @{$self->{crawlers}->{$crawler_name}}	, 0, $count;

}

=item update_queues

Move URLs from URLAlloc to here.

=cut

sub update_queues
{
	my ($self, $mincount, $newcrawler) = @_;
	$mincount ||= 5;
	
	my %crawlers; my %emptycrawlers;
	$crawlers{$_} = scalar @{$self->{crawlers}->{$_}} foreach (keys %{$self->{crawlers}});
	%emptycrawlers = map {$_ => 1} grep{! $crawlers{$_}} keys %crawlers;
	
	#iterate 5 times max - this prevents a deadlock if the urlalloc is empty
	my $i = 5;	

	while ( $i and (%emptycrawlers or _below($mincount, %crawlers)))
	{
		$i--;

		#get a new block of data from the allocator
		my @data = $self->{urlalloc}->get_urls(MAXMOVE);	
		foreach my $url (@data)
		{				
			my $rtr = $self->{partitioner}->assigned($url);
			if ($rtr) #$url already assigned to crawler $rtr
			{
				push @{$self->{crawlers}->{$rtr}}, $url;
				$crawlers{$rtr}++;
				delete $emptycrawlers{$rtr};
			}
			else
			{
				#find an empty crawler and assign it to it
				#there should only be 0 or 1 crawler with no partitions:
				if (defined $newcrawler)
				{
					$self->{partitioner}->new_urls($newcrawler, $url);
					push @{$self->{crawlers}->{$newcrawler}}, $url;
					$crawlers{$newcrawler}++;
					delete $emptycrawlers{$newcrawler};
				}
				else
				{
					my $crawler;
					#pick one of %emptycrawlers, or another crawler at random
					#TODO here we pick randomly, we could also pick the one with least partitions
					if (%emptycrawlers)
					{
						$crawler = (keys %emptycrawlers)[rand() * scalar keys %emptycrawlers];
					}
					else
					{
						#TODO what if there aren't any crawlers?
						$crawler = (keys %crawlers)[rand() * scalar keys %crawlers];
					}
					push @{$self->{crawlers}->{$crawler}}, $url;
					$self->{partitioner}->new_urls($crawler, $url);
					$crawlers{$crawler}++;
					delete $emptycrawlers{$crawler};
				} 
			}
		}
	}
}

=item register_crawler($crawler)

This crawler may now have partitions assigned to it.

=cut

sub register_crawler
{
	my ($self, $crawler) = @_;
	$self->{crawlers}->{$crawler}=[];
	$self->update_queues(MAXMOVE, $crawler);
}

=item unregister_crawler($crawler)

De-assign partitions for $crawler

=cut

sub unregister_crawler
{
    my ($self, $crawler) = @_;
	my %returns;
	%returns = map{$_ => 0} @{$self->{crawlers}->{$crawler}};
	$self->{urlalloc}->delay_urls(%returns);
	warn "Unregistering crawler $crawler, delayed ".keys(%returns)." URLs\n";
	delete $self->{crawlers}->{$crawler};
	my $removed_partitions = $self->{partitioner}->delete_partition($crawler);
	warn "Unregistering crawler $crawler, delayed ".keys(%returns)." URLs, removed $removed_partitions partitions\n";
}

=item queue_sizes

Returns the total size of the crawler queues 

=cut

sub queue_sizes
{
	my $self = shift;
	my $sum = 0;
	foreach (values %{$self->{crawlers}})
	{
		$sum += scalar @{$_};
	}
	return $sum;
}

=item get_stats()

Returns some statistics about partitions.

=cut

sub get_stats
{
	my $self = shift;
	my %stats;
	$stats{crawler_alloc_queued} = $self->queue_sizes;
	$stats{crawler_alloc_queued_av} = 
		$stats{crawler_alloc_queued}/ scalar keys %{$self->{crawlers}} if scalar keys %{$self->{crawlers}};
	$stats{crawler_alloc_crawlers} = scalar keys %{$self->{crawlers}};
	
	my %tmpDetails = $self->{partitioner}->sizes();
	my $sum = 0;
    foreach (values %tmpDetails)
	{
		$sum += $_;
	}

	$stats{partition_size_av} = $sum/ scalar values %tmpDetails if scalar values %tmpDetails;
	$stats{partition_number} = scalar values %tmpDetails;
	return %stats;
}


sub _below
{
	my ($value, %hash) = @_;
	foreach (values %hash)
	{
		return 1 if $_ < $value;
	}
	return 0;
}

sub _load_module {
	eval "require $_[0]";
	die $@ if $@;
	#$_[0]->import(@_[1 .. $#_]);
}


=back

=head1 REVISION

	$Revision: 1.6 $

=cut

1;
