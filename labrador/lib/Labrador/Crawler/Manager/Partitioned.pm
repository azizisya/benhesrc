package Labrador::Crawler::Manager::Partitioned;

use strict;
use lib '../../..';
use base 'Labrador::Crawler::Manager';
use Labrador::Common::URLAlloc;
use Labrador::Common::Partition;
use Labrador::Common::URLState;
use Labrador::Common::URLFilter;

=head1 NAME

Labrador::Crawler::Manager::Partitioned

=head1 DESCRIPTION

Provides the crawler with all queuing operations. This manager supports
URL partitioning.

=head1 METHODS

=over 4

=cut


use constant NAMESPACE_URLALLOC => 'Labrador::Common::URLAlloc';
use constant NAMESPACE_URLFILTER => 'Labrador::Common::URLFilter';
use constant NAMESPACE_URLSTATE => 'Labrador::Common::URLState';
use constant NAMESPACE_PARTITION => 'Labrador::Common::Partition';

=item init()

Called automagically by the constructor
Loads URLState, URLAlloc, and Partitioning modules

=cut

sub init
{
	my $self = shift;
	my $config = $self->{config};
	my $data = $self->{data};

	#load URLState module
	my ($url_states, @params0) = $config->get_scalar('URLState', 1);
	$url_states = NAMESPACE_URLSTATE.'::'.($url_states || 'Normal');
	_load_module($url_states);
	$self->{urlstates} = Labrador::Common::URLState::new(
		$url_states, $data, @params0);
		
	#load URLAlloc module
	my ($url_allocator, @params) = $config->get_scalar('PartitionedAlloc', 1);
	$url_allocator = NAMESPACE_URLALLOC.'::'.($url_allocator || 'BFS');
	_load_module($url_allocator);
	#TODO can this be a string?
	$self->{urlalloc} = Labrador::Common::URLAlloc::new(
		$url_allocator, $data, $self->{urlstates}, $config, @params);
	
	
	#load Partition module
	my ($partition, @params1) = $config->get_scalar('Partition', 1);
	$partition = NAMESPACE_PARTITION.'::'.($partition || 'SeenHost');
	_load_module($partition);
	$self->{partition} = Labrador::Common::Partition::new(
		$partition, $data, @params1);
	
	$self->{partition_name} = $self->{client}->name;

	#TODO - make process safe, but persistent (currently not persistent)
	$data->register_variable("Referers", 'HASH', 0);
	$self->{referers} = $data->obtain_variable("Referers");

	#careful when setting a default, and allowed value is 0
	$self->{perhostdelay} = $config->get_scalar("HostDelay", 0);
	$self->{perhostdelay} = 60 unless defined $self->{perhostdelay};

	
	#$self->{http_retries} = {500=>2, 502=>2, 503=>2, 504=>4};
	$self->{lastqueuefetch} = 0;
	$self->{lasthostfetch} = {};
	$self->{lasthostdelay} = {};

	#this is a spider trap prevention mechanism
	$self->{hostfilename_count} = {};
	$self->{hostfilename_maxreq} = $config->get_scalar("MaxFilenameRequests",0) ||0;
}

=item next_url

Provides the next_url to be fetched

=cut

sub next_url
{
	my ($self) = @_;
	my $time = time;
	
	my $urlalloc = $self->{urlalloc};
	my $urlstates = $self->{urlstates};
	my $MAX_QUEUEFETCH=60; my $MAXMOVE = 100;
	my $MIN_QUEUEFETCH=10;

	#ask the dispatcher if we dont have anything or its time we asked it anyway		
	if (! $urlalloc->queue_size or $time - $self->{lastqueuefetch} > $MAX_QUEUEFETCH)
	{
		$self->_update_queue();
	}

	return undef unless $urlalloc->queue_size;


	#give it $i attempts
	my $out_url; my $i=20;
	while (! $out_url and $i and $urlalloc->queue_size)
	{
		$i--;
		$out_url = $urlalloc->get_url();

		#spider trapping
		if ($self->{hostfilename_maxreq} and #if enabled
			($self->{hostfilename_count}->{_stripped(URI->new($out_url))} || 0) 
			> $self->{hostfilename_maxreq})
		{	# this checks if more than Config:MaxFilenameRequests 
			# to the same filename and filters url out if so
			warn "Skipped - spider trap? $out_url\n";
			$out_url = '';
		
			#TODO should we mark this url as something else?
		}
				
		#well if we didn't get a url first time round, maybe we will if we get
		#more urls from the dispatcher
		$self->_update_queue() if (! $out_url and time - $self->{lastqueuefetch} > $MIN_QUEUEFETCH);
	}
	return undef unless $out_url;

	return ($out_url, 
		$self->{referers}->{$out_url}, 
		$self->{urlstates}->url($out_url));
}

=item when_next_available

Number of seconds until the next url is ready to be fetched

=cut

sub when_next_available
{
	my $self = shift;
	return $self->{urlalloc}->next() || 5;
}



=item finished_url($url)

Mark $url as finished.

=cut

sub finished_url
{
	my ($self, $url) = @_;
	#tell dispatcher we've finished this URL
	$self->{client}->FINISHED($url);	

	#and do other common stuff for finishing	
	$self->_done_url(URI->new($url), $url);
}

=item failed_url($url, $HTTPresponse)

Mark $url as finished. $HTTPresponse contains the HTTP::Response object
which can be used to examine failure reasons and requeue appropraitely.

=cut

sub failed_url
{
	my ($self, $url, $HTTPresponse) = @_;

	#TODO only if we're not retrying
	$self->_done_url(URI->new($url), $url);

	#TODO put retry material here instead of in CrawlerHandler
	$self->{client}->FAILED($url);
	#TODO pass reason: $HTTPresponse->message;
	
}

=item found_urls($url, @urls)

Enqueue @urls, that were all found in the page $url.

=cut

sub found_urls
{
	my ($self, $url, @urls) = @_;
	my $MAX_CHUNK = 100;
	my @urls_chunk = ();

	my $skipped = 0;
	
	my $partitioner = $self->{partition};
	my $partition_name = $self->{partition_name};
	
	foreach my $follow (@urls)
	{	

		#check to see if $follow is in our partition
		if ($partitioner->in_partition($partition_name, $follow))
		{
			#skip if seen before by this crawler
			if ($self->{urlstates}->url_exists($follow))
			{
				$skipped++;
				next;
			}
			
			#add to the queue
			$self->{urlalloc}->new_url($follow);
			#save the referring url
			$self->{referers}->{$follow} = $url;

			#note that we've got it queued
			$self->{urlstates}->url($follow, -1);

			#warn "Queued $follow to myself\n";
		}
		else
		{
			#not in our partition, so send back to the dispatcher
			push @urls_chunk, $follow;

			#warn "Queued $follow to dispatcher\n";

			if (scalar @urls > $MAX_CHUNK)
			{
				$self->{client}->FOUND($url, @urls_chunk);
				@urls_chunk = ();
			}
		}
	}
	
	#send any remaining URLs, or if we've not sent anything yet
	#this ensures all urls are sent
	$self->{client}->FOUND($url, @urls_chunk) if scalar(@urls_chunk); 

	#warn "Skipped $skipped urls\n";
}

=item queue_status

Returns a string depicting the status of the queues. Useful for
displaying when the crawler has no work.

=cut

sub queue_status
{
	my $self = shift;	

	return "Queues: ".scalar $self->{urlalloc}->queue_size	;
	#TODO warning occuring here, no idea why
	#presumably bug in URLAlloc::Delay
	#my @arr = $self->{urlalloc}->queue_size;
	#return "Queues: Master: ".
	#	$arr[0].
	#	", Delay: ".
	#	$arr[1];
}

=item get_stats

Returns a hash of statistics. Keys are master, delay and partition_size

=cut

sub get_stats
{
	my $self = shift;
	my @arr = $self->{urlalloc}->queue_size;
	return (master => $arr[0],
			#delay => $arr[1],
			partition_size=>($self->{partitioner}->size($self->{client}->name)));
}

=back

=head1 PRIVATE METHODS

Not intended to be called from outside the class. Only documented for completeness.

=over 4

=item _load_module($name)

Load the module called $name

=cut

sub _load_module {
	eval "require $_[0]";
	die $@ if $@;
	#$_[0]->import(@_[1 .. $#_]);
}

=item =stripped($uri)

Used for spider trap detection - removes a URL with the fragment and querystring
removed.

=cut

sub _stripped
{
	my $stripped = shift()->clone;
    $stripped->query(''); $stripped->fragment('');
	return "$stripped";
}

=item _update_queue()

Fetches any available URLs from the dispatcher

=cut

sub _update_queue
{
	my $self = shift;
	my $count =0;
	warn "$0 performing an update of URLs from the dispatcher\n";
	$self->{lastqueuefetch} = time;
	my $MAXMOVE = 100;

	#fetch upto 100 URLs from the dispatcher
	my @returns = $self->{client}->NEXT($MAXMOVE);

	if (@returns)
	{
		my %urls;
		foreach my $line (@returns)
		{
			my ($url, $referer, $when) = split /\s/, $line;
			$urls{$url} = [$referer ||'', $when || -1];
		}

		my @urls;
		foreach (keys %urls)
		{
			#remove urls we know about already
			#next if $self->{urlstates}->url_exists($_);
			#warn "".$self->{urlstates}->url($_)." > ".$urls{$_}->[1];
			next if $self->{urlstates}->url_exists($_);
			#TODO - come back to for recrawling support
			#next if $self->{urlstates}->url($_) > $urls{$_}->[1];

			#save the url's referer
			$self->{referers}->{$_} = $urls{$_}->[0] if (length $urls{$_}->[0]);
		
			#mark the url as known about
			$self->{urlstates}->url($_,  $urls{$_}->[1]);

			push @urls, $_;
		}
		#save the URLs
		$self->{urlalloc}->new_urls(@urls);

		#notify partitioner that the partition is ours
		$self->{partition}->new_urls($self->{partition_name}, @urls);
		$count += scalar @urls;
	}
	warn "$0 got $count URLs from dispatcher\n";
}

=item _done_url($uri, $url)

Mark $url as finished. Contains common code extracted from finished,
failure and failure events.

=cut

sub _done_url
{
	my ($self, $uri, $url) = @_;
	my $time = time;
	
	#mark the url as done
	$self->{urlstates}->url($url, $time);

	#update lasthostfetch
	$self->{lasthostfetch}->{$uri->host} = $time;

	#delete the url's referer
	delete $self->{referers}->{$url};

	#mark the filename as having one more request marked against it
	#this is a CGI spider trap prevention sheme
	#idea is that most spider traps are based on their querystring
	#so we limit the number of requests to each filename
	$self->{hostfilename_count}->{_stripped($uri)}++;
	#NB: this doesn't work if the site generates infinitely new paths/filenames
}

=head1 REVISION

	$Revision: 1.18 $

=cut

1;
