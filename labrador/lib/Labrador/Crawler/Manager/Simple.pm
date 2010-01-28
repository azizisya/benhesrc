package Labrador::Crawler::Manager::Simple;

use strict;
use base 'Labrador::Crawler::Manager';

=head1 NAME

Labrador::Crawler::Manager::Simple

=head1 DESCRIPTION

This Manager handles simple operations with the dispatcher, in relation
to enqueuing and dequeueing. No attempt is made to queue any URLs locally.
Using this manager is not as fast as using a partitioned manager.

=head1 METHODS

=over 4

=item init()

Initialise the module. Called automatically by new()

=cut


sub init
{
	my $self = shift;
	#TODO - make process safe
	#TODO needed?
	$self->{data}->register_variable("Referers", 'ARRAY', 1);
	$self->{referers} = $self->{data}->obtain_variable("Referers");
	
	$self->{data}->register_variable("Hostnames", 'HASH', 1);
	$self->{hostnames} = $self->{data}->obtain_variable("Hostnames");
	$self->{retry} = {};
}

=item next_url()

Fetch the next url to crawl from the dispatcher

=cut

sub next_url
{
	my ($self) = @_;
	my $time = time;
	if (scalar %{ $self->{retry} })
	{
		#select the next URL from the list to be retried
		my $next_time = 0;
		my $next_url;
		foreach (keys %{ $self->{retry} })
		{
			my $this_time = $self->{retry}->{$_};
			if ($this_time > $time
				and ($next_time == 0 or $this_time < $next_time)
				)
			{
				$next_time = $this_time;
				$next_url = $_;
			}
		}
		delete $self->{retry}->{$next_url};
		
		return $next_url if $next_time > 0;
	}
	#TODO implement support for connection pipelining?
	my ($url, $referer, $when) = split /\s/, $self->{client}->NEXT();
	
	#check for host delay?
	return ($url, $referer, $when);
}

=item finished_url($url)

Mark $url as finished.

=cut

sub finished_url
{
	my ($self, $url) = @_;
	$self->{hostnames}->{URI->new($url)->host} = time;
	$self->{client}->FINISHED($url);
}

=item failed_url($url)

Mark $url as failed.

=cut

sub failed_url
{
	my ($self, $url) = @_;
	#TODO put retry material here instead of in CrawlerHandler
	$self->{client}->FAILED($url);
	#TODO pass reason: $HTTPresponse->message;
}

=item found_urls($url, @urls)

Enqeueue @urls that were found on $url

=cut

sub found_urls
{
	my ($self, $url, @urls) = @_;
	my $MAX_CHUNK = 100;
	my @urls_chunk;
	foreach my $follow (@urls)
	{
		push @urls_chunk, $follow;
		if (scalar @urls > $MAX_CHUNK)
		{
			$self->{client}->FOUND($url, @urls_chunk);
			@urls_chunk = ();
		}
	}
	#send any remaining URLs, or if we've not sent anything yet
	#this ensures all urls are sent
	$self->{client}->FOUND($url, @urls_chunk) if scalar(@urls_chunk);
}

=item when_next_available()

If no work available, when to try next.

=cut

sub when_next_available
{
	return 5;
}

=item queue_status

A simple message for waiting.

=cut

sub queue_status
{
	return "Dispatcher didn't return anything";
}

=item get_stats

Returns an empty array, this implementation does not support any statistics

=cut

sub get_stats
{
	return ();
}

=head1 REVISION

	$Revision: 1.6 $

=cut

1;
