package Labrador::Crawler::ContentHandler::PreTerrier;



use strict;
use lib '../../..';
use Digest::MD5 qw(md5_hex);
use base qw(Labrador::Crawler::ContentHandler);
use File::Temp qw(tmpnam);
use Labrador::Common::FileWriter;

my $HOSTNAME;

BEGIN{
	$HOSTNAME = "unknown.host";
	eval { require Sys::Hostname; };
	$HOSTNAME = Sys::Hostname::hostname() unless $@;
}

sub init
{
	my ($self, @args) = @_;
	$self->{filebase} = shift @args;

	#TODO only use filebase if not absolute
	my $data_filename = $self->{filebase}."data-$HOSTNAME-$$-%%";
	my $links_filename =  $self->{filebase}."links-$HOSTNAME-$$-%%";
	my $redirects_filename =  $self->{filebase}."redirects-$HOSTNAME-$$-%%";


	#TODO put 100, 1000 into config file	
	$self->{file_data} = new Labrador::Common::FileWriter($data_filename, 100, 1);
	$self->{file_links} = new Labrador::Common::FileWriter($links_filename, 1000, 1);
	$self->{file_redirects} = new
	Labrador::Common::FileWriter($redirects_filename, 1000, 1);
	
}

sub process_success
{
	my ($self, $url, $ref_outlinks, $ref_followlinks, 
		$document)
		= @_;

	#TODO some times $url is URI reference. The stringify prevents this
	my $UID = md5_hex("$url");

	my $headers = $document->response->headers->as_string();
	my $filetime = _longdate();
	my $content = ${$document->contents};
	
	$content =~ s/\<((?:\/)?DOC(NO|OLDNO|HDR)?)\>/\<__$1__\>/gi;
	
	my $size = length($headers) + length($content); 
	my $fileh = $self->{file_data}->write();
	print $fileh <<FORMAT;
<DOC>
<DOCNO>$UID</DOCNO>
<DOCOLDNO>$UID></DOCOLDNO>
<DOCHDR>
$url 0.0.0.0 $filetime $size
$headers
</DOCHDR>
$content
</DOC>
FORMAT


	$fileh = $self->{file_links}->write();

	foreach my $entry (@{$ref_outlinks})
	{
		my @fields = ($url);
		push @fields, $entry->{url}, $entry->{tag}, $entry->{attribute},
			$entry->{text}, exists $ref_followlinks->{$entry->{url}};

		s/\f|\t|\n|\r/ /g for @fields; #remove bad forms of whitespace
		print $fileh join("\t", @fields)."\n";
	}

}


sub process_redirect
{
	my ($self, $old_url, $new_url, $response) = @_;
	my $fileh = $self->{file_redirects}->write();
	print $fileh "$old_url $new_url ".$response->code()."\n";
}


sub _longdate
{
	my @timeparts = (localtime)[5,4,3,3,1,0];
	$timeparts[0]+= 1900; $timeparts[1]++;
	return join '', @timeparts;
}


=head1 REVISION

	$Revision: 1.11 $

=cut

1;
