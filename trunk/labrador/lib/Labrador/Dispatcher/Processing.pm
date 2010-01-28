package Labrador::Dispatcher::Processing;

=head1 NAME

Labrador::Dispatcher::Processing

=head1 SYPNOPSIS

	use Labrador::Dispatcher::Processing;
	Labrador::Dispatcher::Processing::init($config);
	Labrador::Dispatcher::Processing::get_urls('crawler1', 5);	

=head1 DESCRIPTION

Main interface between commands and data processing. If running a crawler
was a business, then this module would contain all the business logic.

=head1 FUNCTIONS

=over 4

=cut

use strict;
my @filters = ();
my ($urlalloc, $crawleralloc, $urlstates);
my $data;
my $paused;
my ($filebase, $config); 
my ($stats, $referers, $starttime, $md5s);
my $maxurls; my $last_av = 0; my $last_done = 0;
my $MEMOIZE; my $LRUCACHE;

use lib '../..';
use constant NAMESPACE_URLALLOC => 	'Labrador::Common::URLAlloc';
use constant NAMESPACE_URLSTATE => 'Labrador::Common::URLState';
use constant NAMESPACE_CRAWLERALLOC =>  'Labrador::Dispatcher::CrawlerAlloc';


use Labrador::Common::Data;
use Labrador::Common::URLAlloc;
use Labrador::Common::URLState;
use Labrador::Common::URLFilter;
use Labrador::Dispatcher::CrawlerAlloc;

BEGIN
{
	#this block optimises as much as it can given
	#the modules available

	$MEMOIZE = 0;
	eval {require Memoize; import Memoize;};
	$MEMOIZE = 1 unless $@;

	$@ = '';
	
	$LRUCACHE = 0;
	eval {require Tie::Cache::LRU;};
	$LRUCACHE = 1 unless $@;
}

=item init($config)

Imports and loads the URL Allocator, the Crawler Allocator, and
all the URLFilters.

=cut

sub init
{
	$config = shift;
	$starttime = time();

	$filebase = $config->get_scalar('Base', 1) || '../';	

	$maxurls = $config->get_scalar('MaxCrawlURLs', 1) || 0;

	$data = new Labrador::Common::Data($config);	

	#initialise the URLState module
	my ($url_states, @params0) = $config->get_scalar('URLState', 1);
	$url_states = NAMESPACE_URLSTATE.'::'.($url_states || 'Normal');
	_load_module($url_states);
	$urlstates = Labrador::Common::URLState::new
		($url_states, $data, @params0);


	#initialise the URLAlloc module
	my ($url_allocator, @params) = $config->get_scalar('URLAlloc', 1);
	$url_allocator = NAMESPACE_URLALLOC.'::'.($url_allocator || 'BFS');
	_load_module($url_allocator);
	#TODO can this be a string?
	$urlalloc = Labrador::Common::URLAlloc::new
		($url_allocator, $data, $urlstates, $config, @params);

	#initialiase CrawlerAlloc module
	my ($crawler_allocator, @params2) = $config->get_scalar('CrawlerAlloc', 1);
	$crawler_allocator = NAMESPACE_CRAWLERALLOC.'::'.($crawler_allocator || 'PerHost');	
	_load_module($crawler_allocator);
	$crawleralloc = Labrador::Dispatcher::CrawlerAlloc::new
		($crawler_allocator, $config, $data, $urlalloc, $urlstates, @params2);

	#initialise URLFilter modules
	my @filters_details = @{ $config->get_array('URLFilter') };
	foreach my $filter_details (@filters_details)
	{
		my $filter_name = shift @{$filter_details};
		push @filters,
			new Labrador::Common::URLFilter($filter_name, $config, @{$filter_details});
	}

	$data->register_variable("Statistics", 'HASH', 0);
	$data->register_variable("Referers", 'HASH', 1);
	$referers = $data->obtain_variable("Referers");
	$stats = $data->obtain_variable("Statistics");
	
	#initialise some statistics
	$stats->{urls_failed} = 0; $stats->{urls_filtered} = 0;
	$stats->{urls_finished} = 0; $stats->{urls_enqueued} = 0;
	$stats->{urls_dequeued} = 0;

	if ( $config->get_scalar('MD5Fingerprints', 1) )
	{
		$data->register_variable("MD5Fingerprints", 'HASH', 1);
		$md5s = $data->obtain_variable("MD5Fingerprints");
	}	

	

	#activate logging in Connections.pm if necessary
	my $log_to = $config->get_scalar('DispatcherCommsLog', 1);
	Labrador::Dispatcher::Connections::log_to_file($filebase.$log_to)
		if $log_to;

	if ($MEMOIZE)
	{
		#this caches results of _filter function, if caching is avaiable
		#is available. Also uses an LRU cache if also available
		if ($LRUCACHE)
		{
			my %filter_cache; my $cache_size = 10000;
			#TODO put cache size in config file?
			tie %filter_cache, 'Tie::Cache::LRU', $cache_size;
			memoize('_filter', SCALAR_CACHE => ['HASH', \%filter_cache]);
		}
		else
		{
			memoize('_filter');
		}
	}


	#load up URLs from a .txt file
	my @obtains = @{ $config->get_array('ObtainURLs', 1)||[] };
	foreach my $line (@obtains)
	{
		my $filename = $line->[0];
		eval {
			open(FILEI, $filebase.$filename) || die "Could not open the root set $filebase$filename : $!\n";
			my @lines = <FILEI>;
			close FILEI;
			chomp @lines;
			my $i = scalar @lines;
			#@lines = grep { filter($_) } @lines;
			#$urlalloc->new_urls(@lines);
			my $count = 0;
			foreach (@lines)
			{
				my $uri = URI->new($_);
				$uri->fragment('');
				$uri = $uri->canonical;
				$count++ if new_url("$uri", '');
			}
			
			warn "Added $count - out of $i from rootset\n";
		};
		warn "Problem obtaining URLS from $filename : $@\n" if $@;
	}	

}

=item register_crawler($crawler)

Register a crawler running on hostname $crawler to be allocated
URLs.

=cut

sub register_crawler
{
	my $crawler = shift;
	$crawleralloc->register_crawler($crawler);
	return 1;
}

=item crawler_disconnect($crawler)

Note that crawler named $crawler has disconnected.

=cut

sub crawler_disconnect
{
	my $crawler = shift;
	$crawleralloc->unregister_crawler($crawler);
	return 1;
}

=item masterqueue_size()

Returns the size of the master queue.

=cut

sub masterqueue_size
{
	return $urlalloc->queue_size;
}

=item crawlerqueue_size 

Returns the size of the crawler queues.

=cut

sub crawlerqueue_size
{
	return $crawleralloc->queue_sizes;
}

=item new_url($url, $linking_url)

This URL $url  has been found on $linking_url. Add it to the
queue if it passes the filters.

=cut

sub new_url
{
	my ($url, $linking_url) = @_;
	
	#Don't add the URL if it fails filtering
	if (! _filter($url) )
	{
		$stats->{urls_filtered}++;
		return 0;
	}

	#run through hasSeen
	return 0 if ($urlstates->url_exists($url));

	#enqueue the url	
	$urlalloc->new_url($url);

	#add to hasSeen
	$urlstates->url($url, -1);
	
	#mark it's referer, if any
	link_url($url, $linking_url||'');
	
	#stats housekeeping
	$stats->{urls_enqueued}++;

	return 1;	
}

=item get_urls($crawler, $count)

Please find $count number of URLs for a subcrawler on $crawler to 
process.

=cut

sub get_urls
{
	my ($crawler, $count) = @_;
	$count ||= 1;
	
	return () if $paused;

	if ($maxurls and ($stats->{urls_finished}||0) >= $maxurls)
	{
		#we've reached the limit of the crawl.
		#empty the queues		
		$crawleralloc->empty_queues();
		$urlalloc->empty_queues();
		return ();
	}	

	my @urls = (); my $attempts = 0; my $max_attempts = $urlalloc->queue_size || 2;
	while (!( scalar @urls) and $attempts < $max_attempts)
	{
		@urls = $crawleralloc->get_urls($crawler, $count);
		$attempts++;
	}

	$stats->{urls_dequeued}+= scalar @urls;
	warn "Dequeued $_\n" foreach (@urls);
	return @urls;
}

=item finished_url($url)

Note that the crawler has finished crawling $url.

=cut

sub finished_url
{
	my $url = shift;
	$stats->{urls_finished}++;
	#mark $url as finished
	$urlstates->url($url, time);
	#we don't need the referrer of this URL any more
	delete $referers->{$url};
}

=item failed_url($url)

Note that the crawler has failed to crawl the url $url.

=cut

sub failed_url
{
	my $url = shift;
	$stats->{urls_failed}++;
	$urlstates->url($url, -4);
	#we don't need the referrer of this URL any more
	delete $referers->{$url};
}

=item linking_url($url)

What is the URL that linked to $url?

=cut

sub linking_url
{
	my $url = shift;
	return $referers->{$url}|| '';
}

=item link_url($url, $from)

=cut

sub link_url
{
	my ($url, $from) = @_;
	
	$referers->{$url} = $from unless exists $referers->{$url};
	#if (! exists $referers->{$from})
	#{
		#must be an initiating URL of the crawl
	#	$referers->{$from} = '';
	#}
}


=item get_stats()

Obtain a hash of all the statistics.

=cut

sub get_stats
{
	my @connection_stats = Labrador::Dispatcher::Connections::get_stats();
	my $time = time;
	my $runningtime = $time - $starttime;
	my @times = times();
	$stats->{'time'} = $time;
	$stats->{dispatcher_bytes_in} = $connection_stats[0];
	$stats->{dispatcher_bytes_out} = $connection_stats[1];
	$stats->{dispatcher_user_time} = $times[0];
	$stats->{dispatcher_sys_time} = $times[1];
	$stats->{dispatcher_bytes_sec_in} = $connection_stats[0] / $runningtime;
	$stats->{dispatcher_bytes_sec_out} = $connection_stats[1] / $runningtime;
	$stats->{total_connections} = $connection_stats[2];
	$stats->{current_connections} = $connection_stats[3];
	$stats->{command_invocations} = $connection_stats[4];
	$stats->{running_time} = $runningtime;
	$stats->{urls_queued} = masterqueue_size();
	#$stats->{urls_sec_last} = $urlalloc->last_average();
	
	my $done = ($stats->{urls_failed} + $stats->{urls_finished})||0;
	$stats->{urls_sec_global_av} = $done / ($runningtime || 1);
	
	if ($time - $last_av > 5 )
	{
		$last_av = $time;
		$stats->{urls_sec_5_av} = ($done - $last_done) / 5;
		$last_done = $done;
	}
		

	my %tmpstats = $crawleralloc->get_stats();
	$stats->{$_} = $tmpstats{$_} foreach (keys %tmpstats);

	return $stats;
}

=item submit_stats($clientname, %stats)

Add the stats %stats submitted by $clientname to the running totals

=cut

sub submit_stats
{
	my ($clientname, %stats) = @_;
	foreach (keys %stats)
	{
		$stats->{$_}+= $stats{$_};
	}
}

=item add_fingerprint($md5)

Add the fingerprint denoted by $md5 to a hash of seen fingerprints.

=cut

sub add_fingerprint
{
	my ($md5, $url) = @_;
	$md5s->{$md5} = $url;
}

=item have_fingerprint($md5)

Returns true if the page denoted by $md5 has been seen before.

=cut

sub have_fingerprint
{
	my $md5 = shift;
	return $md5s->{$md5} || 0;
}

=item filter($url)

Run $url through all registered URL filters

=cut

sub filter
{
	return _filter(@_);
}


=item checkpoint

Save all checkpoint supporting data structures to disk.

=cut

sub checkpoint
{
	$data->checkpoint;
	return 1;
}

=item pause

Pause allocating URLs to clients.
Also forces a checkpoint.

=cut

sub pause
{
	return 0 if $paused;
	$paused = 1;
	#use this opertunity to force a checkpoint
	#TODO remove, and only checkpoint when asked?
	$data->checkpoint;
	return 1;
}


=item start

Restart after a pause

=cut

sub start
{
	return 0 unless $paused;
	$paused = 0;
	return 1;
}

=item eval($code)

Allow an administrator connection to execute arbitrary code.
The following variables are made avaiable to the code being
executed.
=over 6

=item $crawleralloc

=item $urlalloc

=item $urlstates

=item $data

=item $md5

=item $stats

=item $referers

=back

$return can be used to give data back to the calling client.
$@ is also returned.

=cut

sub eval
{
	my $code = shift;
	my $return = '';	
	eval ($code);
	
	$return = "OUT::\n$return\n";
	$return .= "ERR::\n$@\n" if $@;
	return $return;
}

=back

=head1 PRIVATE FUNCTIONS

=over 4

=item _filter($url)

Run $url through all registered filters 

=cut

sub _filter
{
	my $url = shift;
	my $uri;
	if (ref $url)
	{
		$uri = $url;
		$url = "$url";
	}else{
		$uri = new URI($url);
	}
	
	my $result;
	foreach my $filter (@filters)
	{
		$result = $filter->filter($uri, $url);
		unless ($result)
		{
			warn "$url failed on ".$filter->name()."\n";
			last;
		}
	}
	return $result;
}

=item _load_module($name)

Load the module called $name.

=cut

sub _load_module {
    eval "require $_[0]";
    die $@ if $@;
    #$_[0]->import(@_[1 .. $#_]);
}

=back

=head1 REVISION

	$Revision: 1.34 $

=cut

1;

