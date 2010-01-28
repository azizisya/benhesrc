package Labrador::Crawler::CrawlerHandler;

use strict;
use lib '../../';
use WWW::RobotRules;
use Labrador::Crawler::Agent;
use Labrador::Crawler::Manager;
use Labrador::Common::RobotsCache;
use Labrador::Common::URLFilter;
use Labrador::Crawler::ContentFilter;
use Labrador::Crawler::ContentHandler;
use Labrador::Common::Data;

use constant MANAGER_NAMESPACE => 'Labrador::Crawler::Manager';
my $filebase;

=head1 NAME

Labrador::Crawler::CrawlerHandler

=head1 SYPNOPSIS

	use Labrador::Crawler::CrawlerHandler
	my $manager = new Labrador::Crawler::CrawlerHandler(
		config => $config,
		dispatcher => 'sibu.dcs.gla.ac.uk',
		port => 2680);
	$manager->run();
	exit;

=head1 DESCRIPTION

Communicates with the dispatcher, generally managing this crawler. 

=head1 CONFIGURATION

This module is affected by the following configuration file directives:

=over 4

=item SpiderSyncStats

=item SpiderName

=item FollowTagLinks

=item AllowedProtocols

=item ContentHandler

=back

=head1 METHODS

=over 4

=item new(%params)

Pure instantiation.

=cut

sub new
{
	my ($class, @params) = @_;
	my $self = bless {}, $class;
	$self->init(@params);	
	return $self;
}

=item init(%params)

Automatically called from the constructor.

=over 6

=item Instantiates Labrador::Crawler::Agent

=item Instantiates WWW::RobotRules

=item Instantiates Labrador::Common::RobotsCache

=item Loads file name extensions blacklist into a regex

=item Loads content type whitelists into regexs

=item Loads follow tag whitelist into a regex

=item Loads Protocol scheme whitelist into a regex

=item Instantiates all ContentHandlers

=item Sets default for retries.

=back

=cut

sub init
{
	my ($self, $config, $dispatcher, $port) = @_;
	$self->{client} = new Labrador::Crawler::Dispatcher_Client($dispatcher, $port);
	$self->{client}->connect('CRAWLER' =>1 );
	$self->{config} = $config;
	$self->{data} = 
	my $data = new Labrador::Common::Data($config);
	$self->{data} = $data;
	
	$self->{statssyncdelay} = $config->get_scalar("SpiderSyncStats", 1) || 3600;
	$self->{statslastsync} = time;
	$self->{stats} = {};
	$self->{hostnames} = {};
	
	$self->{agent} = new Labrador::Crawler::Agent($config, $self);	
	
	#load Manager module
	my ($manager_name, @params0) = $config->get_scalar('Manager', 1);
	$manager_name = ($manager_name || 'Simple');
	$self->{manager} = new Labrador::Crawler::Manager(
		$manager_name, $data, $self->{client}, $config, @params0);

	
	#WWW::RobotRules performs robots.txt matches
	$self->{robots} = new WWW::RobotRules($config->get_scalar('SpiderName'));
	#disk cache implementation for robots.txt files
	$self->{robotscache} = new Labrador::Common::RobotsCache($config);
	
	$filebase = $config->get_scalar('Base', 1) || '../';

	#load in some handy debugging config values	
	$self->{sleeppostrequest} = $config->get_scalar('SleepPostRequest', 1) || 0;
	$self->{maxcrawlerrequests} = $config->get_scalar('MaxCrawlerRequests', 1) || 0;
	
	#load in the blacklists and whitelists for crawling	
	#$self->{file_extensions_blacklist} = 
	#	join "|", $config->get_scalar('ExtensionsBlacklist');
	
	$self->{link_types} = '^(?:' . 
		join('|', ($config->get_scalar('FollowTagLinks',1)||'aaaNONEXISTANTTAGbbb')).')$';

	#load in each of the url filters
	$self->{url_filters} = [];
	foreach my $filter_entry (@{$config->get_array('URLFilter')})
	{
		my $filter_name = shift @{$filter_entry};
		push @{$self->{url_filters}},
			Labrador::Common::URLFilter->new($filter_name, $config, @{$filter_entry});
	}	

	#load in each content handlers
	$self->{content_handlers} = [];
	foreach my $handler_entry (@{$config->get_array('ContentHandler')})
	{
		my $handler_name = shift @{$handler_entry};
		push @{$self->{content_handlers}}, 
			Labrador::Crawler::ContentHandler->new(
				$handler_name, 
				$handler_entry,
				$config, $self->{client});
	}

	#load in each of the content filters
	$self->{content_filters} = [];
	foreach my $filter_entry (@{$config->get_array('ContentFilter')})
    {
		my $filter_name = shift @{$filter_entry};
		push @{$self->{content_filters}}, 
			Labrador::Crawler::ContentFilter->new($filter_name, $config, $self->{client});
	}

	$self->{work_retries} = 100;
	
	$self->{http_retries} = {500=>2, 502=>2, 503=>2, 504=>4};
	$self->{retries} = {};
	$self->{retry} = {};
	$self->{retry_delay} = 60;
	return 1;	
}


=item run

Commence crawling. Doesn't return until crawling ends.
Fetches next URL from $manager->next_url(), checks it for robots.txt
compliance, and crawls it.
Syncs stats with the dispatcher if enough time has expired.
Also involves a retry mechanism for retries - backs off for
5 seconds each time it finds no work to do, upto work_retries
times.

=cut

sub run
{
	my $self = shift;
	my ($agent, $client) = ($self->{agent}, $self->{client});
	my $run = $self->{work_retries}; my $default_sleep = 5;
	my $retries = $run;
	
	my $maxrequests = $self->{maxcrawlerrequests};
	my $requests = 0;

	#this is the main crawler loop. This is exectuted ONCE for each page fetch
	while ($run)
	{
		#1. get a page
		my ($url, $referer, $when) = $self->{manager}->next_url($client);
		#1.5 sleep then try again if no pages ready
		unless (defined $url)
		{
			$run--;
			my $sleep = $self->{manager}->when_next_available();
			$sleep = $sleep < 0 ? $default_sleep : $sleep;
			warn "No work found($run) for $0 - backing off for $sleep seconds\n";
			warn "\t".$self->{manager}->queue_status()."\n";
			#warn "Last code was ".$client->last_result_code()."\n";
			$self->{stats}->{slept}+=$sleep;
			sleep $sleep;
			next;
		}
	
		if ($run < $retries)
		{
			warn "$0 got work this time round\n";
			$run = $retries;
		}
		
		#2. check the robots.txt file, and crawl.	
		if ($self->url_robots_allowed($url))
		{		
			$agent->crawl($url, $referer, $when);
			$requests++;
			warn "Finished $url\n";

			#sleep after the request if we're in development mode.
			sleep $self->{sleeppostrequest} if $self->{sleeppostrequest};
		}
		
	
		#3. Sync stats with dispatcher if it's that time	
		if ( time - $self->{statslastsync} >= $self->{statssyncdelay})
		{
			#time to resync our stats with the dispatcher
			$self->{statslastsync} = time;
			$self->{stats} = {} if $client->STATS(%{$self->{stats}});
			#as our stats are added to the dispatcher, we should
			#clear our own (i think)
		}

		#4. Check to see if we've reached our max pages
		$run = 0 if ($maxrequests and $requests >= $maxrequests);
	}
	warn "Subcrawler $0 finished. Exiting...\n";
	$client->disconnect();
}


=item agent_success($url, $document)

Probably the most important subroutine in the entire crawler
Event called by agent, when it has successfully retrieved
$url. All sorts of useful information wrapped up in $document.
For more information see, the documentation for L<Labrador::Crawler::Document>

=cut

sub agent_success
{
	my ($self, $url, $document) = @_;

	my $followlinks = $self->{link_types};	
	
	#run through the contentfilters
	my $privs = {'follow' =>1, 'index' =>1};
	$self->run_content_filters($document, $privs);	

	#only extract links if we need them	
	my @urls_extracted;	
	@urls_extracted = $document->links() 
		if ($privs->{'follow'} || $privs->{'index'});

	my %urls_to_follow = ();	
		
	#only if the page allows its links to be followed
	if ($privs->{'follow'})
	{
		foreach my $link (@urls_extracted)
		{
			#check it's a tag we're allowed to follow 
			#assuming we got a tag
			next unless $link->{tag} =~ /$followlinks/i;
	
			#TODO $link->{url} maybe a URI already	
			my $uri = URI->new($link->{url});

			#remove fragment (ie bookmark)
			$uri->fragment('');
			
			#remove /index.html etc
			my $path = $uri->path;
			$path =~ s/\/(?:default|index).(?:(?:s)?htm(?:l)?|asp(?:x)?|jsp|php|cfm)$/\//i;
			$uri->path($path);	
			
			#throw to the URLFilters
			next unless $self->run_url_filters($uri);
			
			$urls_to_follow{$link->{url}} = $uri->canonical;
		}
	}
	
	if ($privs->{'index'})
	{
		#we're allowed to save the document
		foreach my $handler (@{$self->{content_handlers}})
		{
			$handler->process_success(
				$url, \@urls_extracted, \%urls_to_follow,
				$document
			);
			
		}
	}
	
	#update the statistics	
	$self->{stats}->{'HTTP_'.$document->response->code}++;
	$self->{stats}->{finished}++;
	$self->{stats}->{content_filtered}++ unless ($privs->{'follow'} or $privs->{'index'});

	#now send the URLs
	$self->{manager}->found_urls($url, values %urls_to_follow);
	#mark $url as finished
	$self->{manager}->finished_url($url);

	undef $url; undef $document;
	undef %urls_to_follow; undef @urls_extracted;
	
}

=item agent_redirect($url, $HTTPresponse)

Event method called when the Agent finds that $url redirects
to another URL. More information can be found in $HTTPresponse.

The Manager treats a redirection as a page with only one URL
on it.

=cut

sub agent_redirect
{
	my ($self, $url, $HTTPresponse) = @_;
	warn "Redirect on $url\n";
		
	# Make a copy of the request and initialize it with the new URI
	my $referral = $HTTPresponse->request->clone;

	# And then we update the URL based on the Location:-header.
	my($referral_uri) = $HTTPresponse->header('Location');

	{#anonymous block for local

		# Some servers erroneously return a relative URL for redirects,
		# so make it absolute if it not already is.
		local $URI::ABS_ALLOW_RELATIVE_SCHEME = 1;
		my $base = $HTTPresponse->base;
		$referral_uri = 
			$HTTP::URI_CLASS->new($referral_uri, $base)->abs($base);
		$referral_uri->fragment('');
		$referral_uri = $referral_uri->canonical;
	}

	foreach my $handler (@{$self->{content_handlers}})
	{
		$handler->process_redirect(
			$url, "$referral_uri", $HTTPresponse
			);
	}	

	$self->{stats}->{'HTTP_'.$HTTPresponse->code}++;
	$self->{stats}->{finished}++;
	$self->{stats}->{redirects}++;
	
	
	#now send the URLs
	$self->{manager}->found_urls($url, $referral_uri);
	#mark $url as finished
	$self->{manager}->finished_url($url);	
}

=item agent_failure($url, $HTTPresponse)

Event method called when the Agent failed to retrieve $url.
More information can be found in $HTTPresponse.

=cut

sub agent_failure
{
	my ($self, $url, $HTTPresponse) = @_;

	my $code = $HTTPresponse->code;
	$self->{stats}->{"HTTP_$code"}++;
	my $max_retries = 0;#DISABLED: $self->{http_retries}->{$code} || 0;
	$self->{retries}->{$url}++;
	
	if ($max_retries and $self->{retries}->{$url} < $max_retries)
	{
		#retry this url
		$self->{retry}->{$url} = time + $self->{retry_delay};
	}
	else
	{
		#enough attempts
		delete $self->{retries}->{$url};

		foreach my $handler (@{$self->{content_handlers}})
		{
			$handler->process_failure(
				$url, $HTTPresponse
				);
		}	
		
		$self->{client}->FAILED($url, $HTTPresponse->message);
		$self->{stats}->{failed}++;
	}
}


=item agent_unmodified($url, $HTTPResponse)

=cut
sub agent_unmodified
{
	my ($self, $url, $HTTPResponse) = @_;
	#update the statistics
	$self->{stats}->{'HTTP_'.$HTTPResponse->code}++;
	$self->{stats}->{finished}++;
	$self->{stats}->{unmodified}++;

	#mark $url as finished
    $self->{manager}->finished_url($url);
}


=item url_robots_allowed($url)

Checks whether $url is allowed by the robots.txt file
on that host (if any). Will check it's own disk cache,
dispatcher's disk cache, and finally retrieve the
robots.txt if necessary.

Returns true if the URL is allowed, or false if the 
URL is explicitly disallowed, or the URL is invalid.

=cut

sub url_robots_allowed
{
	my ($self, $url) = @_;
	my $uri = URI->new($url);
	my $hostname;
	eval{
		$hostname = $uri->host_port();
	};
	if ($@)
	{
		warn "$url is invalid : $@";
		return 0;
	}
	if (! exists $self->{hostnames}->{$hostname})
	{
		my @cached;
		#first try local disk cache
		@cached = $self->{robotscache}->get_file($hostname);
		if (! scalar @cached)
		{	
			#now try dispatcher's cache
			@cached = $self->{client}->ROBOTS($hostname);
			#none found in central cache, fetch using HTTP
			if (! scalar @cached)
			{
				
				#recontructs the original URL into a robots.txt request
				my $robots_request = $uri->clone;
				$robots_request->path_query('/robots.txt');
				$robots_request->fragment('');
				my ($response, $ref_data) = $self->{agent}->get($robots_request->canonical());
			
				#check for a successful retrieval - also reject unless the type is
				#text/plain
				if ($response->is_success and $response->content_type eq 'text/plain')
				{
					@cached = split /\n+/, $$ref_data;
				}
				else
				{
					@cached = '#'; #use a safe default
				}
				#inform dispacther of new robots.txt file
				$self->{client}->ROBOTSFILE($hostname, @cached);
			}
			#save to disk cache
			$self->{robotscache}->set_file($hostname, @cached);
		}

		#TODO perhaps move out in above if, once we delete robots.txt cache
		#remove any prefixed whitespace, such as http://www.maths.gla.ac.uk/robots.txt
		s/^\s*//g for (@cached);	
	
		$self->{robots}->parse("http://$hostname/robots.txt", join "\n", @cached); #TODO some expiry support?
		$self->{hostnames}->{$hostname} =1;	
	}
	return $self->{robots}->allowed($url);
}

=item run_url_filters($url)

Runs $url through each of the loaded URLFilters. Returns 1 if the URL
is allowed, 0 otherwise.

=cut

sub run_url_filters
{
	my ($self, $url) = @_;
	my $result = 1;
	my $i = 0;


	#make a URI object of $url
	my $uri;
	if (ref $url)
	{
		$uri = $url;
		$url = "$url";
	}else{
		$uri = new URI($url);
	}

	foreach my $filter (@{$self->{url_filters}})
	{
		$i++;
		$result = $filter->filter($uri, $url);
		unless ($result)
		{
			# warn "$url failed on filter number $i (".$filter->name().")\n";
			last;
		}
	}
	return $result;
}

=item run_content_filters($document, $privs)

Runs the document through all loaded content filters, altering
values of $privs as it progresses. Notice that this shortcuts -
it will terminate if all privilege values are zero.

=cut

sub run_content_filters
{
	my ($self, $document, $privs) = @_;
	foreach my $filter (@{$self->{content_filters}})
	{
		$filter->filter($document, $privs);
		if (_all_zero(values %{$privs}))
		{
			$self->{stats}->{'filtered_'.$filter->name()}++;
			last;
		}
	}
}


=item shutdown()

Shutdown the crawler. Simplisticly undefs each of the objects fields.

=cut

sub shutdown
{
	my $self = shift;
	while (my ($key, $value) = each %{$self})
	{
		delete $self->{$key};   # This is safe as it's an each
		undef $value; undef $key;
	}
}

=back

=head1 PRIVATE METHODS

As usual, these are only documented for completeness, and should not
be directly called.

=over 4

=item _all_zero(@array)

Returns 1 if all items of @array are false. (Remember false
means 0 or "" or undef).

=cut

sub _all_zero
{
    foreach (@_)
    {
        return 0 if $_;
    }
    return 1;
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

	$Revision: 1.14 $

=cut

1;

