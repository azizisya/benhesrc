package Labrador::Crawler::Document;

use strict;
use Digest::MD5 qw(md5_base64);
use constant CONTENT_TYPE_MAP => 
	{
		'application/pdf' => 'PDF',
		'application/postscript' => 'PS',
		'text/html' => 'HTML',
		'application/xhtml+xml' => 'HTML' ,
		#'application/vnd.ms-powerpoint' => 'PPT',
		#'applicatin/msword' => 'DOC'
		'application/xhtml+xml' => 'RSSAtom',
		'text/xml' => 'RSSAtom',
		'application/rdf+xml' => 'RSSAtom',
		'application/xml' => 'RSSAtom',
		'application/atom+xml'  => 'RSSAtom'
	};

my $support_HTTP_compression = 0;
my $support_URI_extraction = 0;

BEGIN
{
	eval{
		$support_URI_extraction = 1 if (require 'URI::Find');
	};
}


#content types to index
#ContentTypeWhitelist text/html text/plain application/xhtml+xml application/postscript application/pdf text/xml
#application/vnd.ms-powerpoint

#content types to process for links
#HTMLContentTypeWhitelist text/html application/xhtml+xml text/xml


=head1 NAME

Labrador::Crawler::Document

=head1 SYNOPSIS

	my $doc = new Labrador::Crawler::Document($HTTPresponse);
	my @links = $doc->links;
	my $content_type = $doc->content_type;
	print $doc->content;

=head1 DESCRIPTION

This module is a wrapper around HTTP::Response, such that generic methods
can be called on a Document object, and the correct child class will provide
the data required. Child classes will be created for common types of document,
including HTML, PDF, PS.

=cut

=head1 METHODS

=over 4

=item new($response)

Constructs a new Document object from HTTP::Response object $response.
If a specific child implementation exists for the given type of document,
it will be used. Eg PDF, Postscript.

=cut

sub new
{
	my ($me, $response, $ref_data) = @_;

	my $class = $me.'::'._determine_child($response);
	_load_module($class);
	my $self = bless {data => $ref_data, response => $response}, $class;
	$self->init();
	return $self;
}

sub _determine_child
{
	my $response = shift;
	#a static hash will do nicely here, I think.
	
	return CONTENT_TYPE_MAP->{$response->content_type} 
		if exists CONTENT_TYPE_MAP->{$response->content_type};

	#now, we should examine the filename

	#else return a default handler
	return 'Generic';
}

=item init

Initialiase the handler. This should be called from child handlers to ensure any
common functionality is initiliased.

=cut

sub init
{
	#my $self = shift;
	return 1;
}

=item content_type

Returns the original content type of the response.

=cut

sub content_type
{
	my $self = shift;
	return $self->{content_type} || $self->{response}->content_type;	
}

=item header($name, [$value])

Passes through to HTTP::Headers->header

=cut

sub header
{
	return shift()->{response}->header(@_);
}


=item response

Returns the original HTTP::Response object returned by the request.
NB: the content() method of HTTP::Response should not be used, as the
data may have been compressed.

=cut

sub response
{
	return $_[0]->{response};
}

=item url

Return the URI object used during this request.

=cut

sub url
{
	return $_[0]->{response}->request->uri;
}

=item links

Returns an array of links found in this document.

=cut

sub links
{
	return () unless $support_URI_extraction;
	#TODO use URI::Find to find URLs included in any document
}

=item content

=item contents

Returns the downloaded content for this Document. Note this content may have been transformed
or converted by the document handler. NB: To limit stack size, this returns
a reference to the data.

=cut

sub content { return contents(@_); }

sub contents
{
    return $_[0]->{data};
}

=item fingerprint

Returns an MD5 sum (base 64) of the content of this data. This can be used for 
ignoring content across identical hosts.

=cut

sub fingerprint
{
	#TODO if content transformation occurs in data() then use that
	return md5_base64(${$_[0]->{data}});
}

=back

=head1 PRIVATE METHODS

=over 4

=item _load_module($name)

Load the module named $name.

=cut

sub _load_module {
    eval "require $_[0]";
    die $@ if $@;
    #$_[0]->import(@_[1 .. $#_]);
}


=head1 TODO

=over 4

=item Look at file extensions to match file types

=item Peruse modules in BEGIN{} to generate CONTENT_TYPE_MAP etc

=item More Child classes, eg for RTF, RSS/RDF?, MS Word, Powerpoint

=item Generic link abstraction using URI::Find, if available

=back

=head1 REVISION

	$Revision: 1.6 $

=cut

1;

