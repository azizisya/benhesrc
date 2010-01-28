package Labrador::Crawler::Agent;

use lib '../..';
use LWP::UserAgent;
use HTTP::Headers;
use HTTP::Request;
use Labrador::Crawler::AccessLog;
use Labrador::Crawler::Document;
my $HIRES;

BEGIN
{
    $HIRES = 0;
    eval {
		require Time::HiRes;
		import Time::HiRes 'time';
	};
    $HIRES = 1 unless $@;
}

=head1 NAME

=head1 DESCRIPTION

=head1 PRE-REQUISITES

	lib-www-perl (LWP, HTTP::Headers, HTTP::Request)
	Time::HiRes (optional)
	Compress::Zlib (optional)

=head1 METHODS

=over 4 

=item new()

Constructs a new agent object.

=cut

sub new
{
	my $class = shift;
	my $self = bless {}, $class;
	$self->init(@_);
	return $self;
}

=item init($config, $manager)

Initialiases class

=cut

sub init
{
	my ($self, $config, $manager) = @_;
	
	$self->{manager} = $manager;


	my $ua = new LWP::UserAgent(
		parse_head => 0 #no please don't bother with HTML::HeadParser
						#a regex is only 2643% faster at extracting meta-robots
	);


	#TODO - compare this with config file
	$ua->protocols_allowed(['http', 'https']);
	
	$ua->requests_redirectable([]);

	#set agent and from HTTP headers
	$ua->agent(
		$config->get_scalar('SpiderName').'/'.
		$config->get_scalar('SpiderVersion'). '; '.
		$config->get_scalar('SpiderWebsite'). '; '.
		$config->get_scalar('SpiderEmail'));
	$ua->from($config->get_scalar('SpiderEmail'));
	
	#set the decent LWP timeout if it's specified in the config file
	my $timeout = $config->get_scalar('Timeout', 1);
	$ua->timeout($timeout) if $timeout;

	#setup proxy configuration
	if (my $proxy = $config->get_scalar('SpiderProxy', 1))
	{
		$ua->proxy(['http', 'https'], $proxy);
		if (my ($proxyuser, $proxypass) = 
			($config->get_scalar('SpiderProxyUsername', 1), 
			$config->get_scalar('SpiderProxyPassword',1 ))
		){
			$self->{proxyuser} = $proxyuser;
			$self->{proxypass} = $proxypass;
		}
	}	
	
	$self->{access_logger} = new Labrador::Crawler::AccessLog(data => $config);

	$self->{ua} = $ua;
	$self->{supportcompression} = 0;
	eval{
		$self->{supportcompression} = 1 if (require 'Compress::Zlib');
	};

	$self->{deaduntil} = {};
	$self->{badattempts} = {};
	
	
}

=item crawl($url, $refering_url, $when)

Crawls $url, setting the HTTP referer field to be
$refering_url. Calls back event handlers (agent_unmodified,
agent_redirect, agent_success, agent_failure).
If $when is set, then the HTTP header If-Last-Modified-Since
is used, to avoid downloading the page if it not already changed.

=cut

sub crawl
{
	my ($self, $url, $referer, $when) = @_;
	my $starttime = time; my $fetchtime;

	my ($response, $ref_data) = $self->get($url, $referer, $when);
	$fetchtime = time - $starttime;

	#check for unmodified content	
	if ($response->code eq 304)
	{
		eval{
			$self->{manager}->agent_unmodified($url, $response);
		}; 
		if ($@)
		{
			warn "Problem on agent_unmodified event: $@\n";
		}		
	}
	#check for a redirect
	elsif ($response->is_redirect)
	{
		eval{
			$self->{manager}->agent_redirect($url, $response);
		};
		if ($@)
		{
			warn "Problem on agent_redirect event: $@\n";
		}
	}
	#got content
	elsif ($response->is_success)
	{
		eval{
			$self->{manager}->agent_success($url, new Labrador::Crawler::Document($response, $ref_data));
		};
		if ($@)
		{
			warn "Problem on agent_success event: $@\n";
		}
	}
	#otherwise it must be an error
	else
	{
		eval{
			$self->{manager}->agent_failure($url, $response);
		};
		if ($@)
		{
			warn "Problem on agent_failure event: $@\n";
		}
	}

	#now log the request
	$self->{access_logger}->log($response->request, $response, $ref_data, 
		$fetchtime, time-$fetchtime-$starttime);

	undef $response; undef $$ref_data; undef $ref_data;
}

=item get($url, $referer, $when)

Fetches $url, setting the HTTP referer field to be
$refering_url. Returns the HTTP::Response object and
a reference to the retrieved data.

A conditional fetch is made if $when is defined and > 0

=cut

sub get
{
	my ($self, $url, $referer, $when) = @_;
	warn "$0 fetch - $url\n";
	my $ua = $self->{ua};
	my $headers = new HTTP::Headers();
	#use string form of $referer, in case it's a URI object
	$headers->referer("$referer") if $referer;
	#tell remote server we accept compressed http content
	$headers->header('Accept-Encoding' => 'gzip; deflate') if ($self->{supportcompression});

	my $request = new HTTP::Request('GET',$url,$headers);

	#setup proxy authentication details if needed
	if ($self->{proxyuser})
	{
		$request->proxy_authorization_basic($self->{proxyuser}, $self->{proxypass})
	}

	#if (defined $when and $when >0) #check $when is a time, ie >0
	#{
	#	$request->if_modified_since($when);
	#}

	#make the request
	my $response = $ua->request($request);

	if ($response->code == 408)
	{
		#timeout
		my $host = $response->request->host;
		$self->{badattempts}->{$host}++;
		#todo 5, 100 into config
		if ($self->{badattempts} > 5)
		{
			$self->{deaduntil}->{$host}	= 100+int(time);
		}
	}

	#we enabled compression, so we should decompress in case it has been compressed
	my $data = $response->content;
	if ($self->{supportcompression} and my $encoding = $response->content_encoding)
	{
		$data = Compress::Zlib::memGunzip($data) if $encoding =~ /gzip/i;
		$data = Compress::Zlib::uncompress($data) if $encoding =~ /deflate/i;
	}
	
	return ($response, \$data)
}

=back

=head1 REVISION

	$Revision: 1.12 $

=cut

1;
