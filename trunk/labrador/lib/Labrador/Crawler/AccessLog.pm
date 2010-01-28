package Labrador::Crawler::AccessLog;

use strict;

use File::Basename;
use IO::Handle; #easier autoflushing on filehandles
use Digest::MD5 qw(md5_base64); #for the fingerprinting of data


our @filehandles;
my $HOSTNAME;



BEGIN {
	# Check if we've got Time::HiRes. If not, don't make a big fuss,
	# just set a flag so we know later on that we can't have fine-grained
	# time stamps
#	$TIME_HIRES_AVAILABLE = 0;
#	eval { require Time::HiRes; };
#	if($@) {
#		$PROGRAM_START_TIME = time();
#	} else {
#		$TIME_HIRES_AVAILABLE = 1;
#		$PROGRAM_START_TIME = [Time::HiRes::gettimeofday()];
#	}

	# Check if we've got Sys::Hostname. If not, just punt.
	$HOSTNAME = "unknown.host";
	eval { require Sys::Hostname; };
	$HOSTNAME = Sys::Hostname::hostname() unless $@;
}

#close all our filehandles
END{
	close $_ foreach @filehandles;
}

sub new
{
	my ($class, %options) = @_;
	my $self = bless {data => $options{data}}, $class;
	$self->init();
	return $self;
}

sub init
{
	my $self = shift;
	
	my $filename = $self->{data}->get_scalar("SpiderAccessLog", 0);
	my $dirname = dirname($filename);
	mkdir $dirname unless -d $dirname;

	my $fileh;
	$self->{DIRECTIVES} = 'AaBbCcdfHMmPpqrSsTtUu%';

	$self->{stack} = [];
	$self->{operations} = {};

	my $format = $self->{data}->get_scalar("SpiderAccessLogFormat", 1) ||
		'"%t %s %U"';
	
	#remove starting and trailing "s, 
	#TODO but not if escaped
	#$format =~ s/^\"(.*)[^\]?\"/$1/g if $format =~ /^\"(.*)[^\]?\"/
	#$format =~ s///g;
	$format =~ s/^\"//g;
	$format =~ s/\"$//g;

	#replace tabs etc
	$format =~ s/\\t/\t/g;
	$format =~ s/\\n/\n/g;
	
	#replace directives with positional directives to sprintf
	$format =~ s/%([$self->{DIRECTIVES}])/_parse($self, $1);/egx;

	$self->{format} = $format;
	
	#setup the static (non-changing directives)
	$self->{info}->{H} = $HOSTNAME;
	$self->{info}->{P} = $$;

	$filename =~ s/\%H/$HOSTNAME/;
	$filename =~ s/\%P/$$/;
	my $time = time;
	$filename =~ s/\%t/$time/;	

	open($fileh, ">>$filename") or die "Couldn't open $filename : $!\n";
	$fileh->autoflush(1);
	$self->{filehandle} = $fileh;
	push @filehandles, $fileh;

}

sub log
{
	my ($self, $HTTPrequest, $HTTPresponse, $data, $fetchtime, $processingtime) = @_;
	my $fileh = $self->{filehandle};

	my @op_results = ();
	foreach my $op (@{$self->{stack}})
	{
		#NB: this was a switch statement, but it complicated debugging
		#(Switch.pm is a source filter)
		
		my $value='';
		if ($op eq 'A')
		{	#Remote hostname
			$value = $HTTPrequest->uri->host
		}
		elsif ($op eq 'a')
		{	#Remote IP address
			#TODO				
		}
		elsif ($op eq 'b')
		{	#$fetch time
			$value = $fetchtime;
		}
		elsif ($op eq 'B')
		{	#processing time
			$value = $processingtime;
		}
		elsif ($op eq 'C')
		{	#Size of content downloaded compressed (excluding headers)
			$value = length $HTTPresponse->content
		}
		elsif ($op eq 'c')
		{	#Size of content downloaded uncompressed (excluding headers)
			$value = length $$data
		}
		elsif ($op eq 'd')
		{	#printable date/time
			$value = scalar localtime();
		}
		elsif ($op eq 'f')
		{	#Filename
			my @url_segs = $HTTPrequest->uri->path_segments();
            $value = $url_segs[$#url_segs];
		}
		elsif ($op eq 'M')
		{	#MD5 of $$data
			$value = md5_base64($$data);
		}
		elsif ($op eq 'm')
		{	#HTTP method (GET,HEAD etc)
			$value = $HTTPrequest->method
		}
		elsif ($op eq 'p')
		{	#Remote port
			$value = $HTTPrequest->uri->port
		}
		elsif ($op eq 'q')
		{	#Querystring
			$value = $HTTPresponse->request->uri->query() || ''
		}
		elsif ($op eq 'r')
		{	#Referring URL
			$value = $HTTPrequest->referer() || ''
		}
		elsif ($op eq 'S')
		{	#Protocol scheme (eg http, https)
			$value = $HTTPrequest->uri->scheme
		}
		elsif ($op eq 's')
		{	#HTTP response code
			$value = $HTTPresponse->code
		}
		elsif ($op eq 'T')
		{	#time taken to get and process the file
			$value = $fetchtime + $processingtime;
		}
		elsif ($op eq 't')
		{	#Current Epoch Time
			$value = time;
		}
		elsif ($op eq 'U')
		{	#entire URL requested
			$value = "".$HTTPrequest->uri;
		}
		elsif ($op eq 'u')
		{	#URI of request
			$value = $HTTPrequest->uri->path
		}
		else
		{	#perhaps it's been defined at startup
			#else ignore it
			$value = $self->{info}->{$op} || '';
		}
		push @op_results, $value;
	}

	print $fileh sprintf $self->{format}."\n", @op_results;
}

sub _parse
{
	my ($self, $op) = @_;
	return "%%" if $op eq "%";
	die "$op is an unknown Accesslog directive\n" unless index($self->{DIRECTIVES}, $op) > -1;

	push @{$self->{stack}}, $op;
	$self->{operations}->{$op}++;
	return '%'.scalar @{$self->{stack}}.'s';
}

1;

__END__

=head1 NAME

Labrador::Crawler::AccessLog

=head1 SYPNOPSIS

	use Labrador::Crawler::AccessLog;
	my $logger = new Labrador::Crawler::AccessLog(data => $data);
	$logger->log();

=head1 DESCRIPTION

Customisable access logger, which when allows custom formats for log entries to be created, similar to the way Apache allows.

=head1 CONFIGURATION

=over 4

=item SpiderAccessLog

The filename to save the Access log file to. Must be aboslute at the moment. (TODO)

=item SpiderAccessLogFormat

The format with which to save the log file entry.

=back

=head1 FORMAT

=over 4 

=item C<%a>

Remote IP address

=item C<%A>

Remote hostname

=item C<%d>

L<localtime> standard date/time scalar format

=item C<%M>

MD5 base64 fingerprint of the uncompressed data

=item C<%f>

Filename

=item C<%p>

Remote port

=item C<%T>

Time taken to make request

=item C<%t>

Current epoch time from L<time>

=item C<%m>

Request method

=item C<%q>

Querystring

=item C<%U>

Entire URL requested

=item C<%u>

URI of request

=item C<%s>

HTTP status code

=item C<%S>

Protocol scheme (eg http, https)

=item C<%r>

Referring URL

=item C<%c>

Size of content downloaded uncompressed (excluding headers)

=item C<%C>

Size of content downloaded compressed (excluding headers)

=item C<%P>

PID of requesting crawler

=item C<%H>

Hostname of the requesting crawler

=back


=head1 METHODS

=over 4

=item new(data=> $data)

Constuct new object

=item init()

Initialises this module (called by new()).

=item log($HTTPrequest, $HTTPresponse, \$data, $deltatime)

Log a request and response for the crawler to the log file with the configured format.

=back


=head1 PRIVATE METHODS

=over 4

=item parse()

Converts a charatcter from the log formatting into a L<sprintf>
compatable number, such that it can be found in the stack of
formatting clauses values passed to L<sprintf>.

=back

=head1 REVISION

	$Revision: 1.11 $

=cut


