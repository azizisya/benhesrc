package Labrador::Crawler::ContentHandler::Save;

use strict;
use lib '../../..';
use Digest::MD5 qw(md5_hex);
use base qw(Labrador::Crawler::ContentHandler);

sub init
{
	my ($self, @args) = @_;
	$self->{filebase} = shift @args;

}

sub process
{
	my ($self, $url, $ref_outlinks, $ref_followlinks, 
		$response, $ref_content)
		= @_;

	#TODO use Base if not absolute
	my $filebase = $self->{filebase};

	my $UID = md5_hex($url);

	open(FILEO, ">$filebase$UID.content") 
		or die "Couldn't write to $filebase$UID.content : $!\n";
	print FILEO $$ref_content;
	close FILEO;
	
	open(FILEO, ">$filebase$UID.meta");
	print FILEO "$UID\n";
	print FILEO "$url\n";
	print FILEO time()."\n";
	print FILEO $response->headers->as_string();
	print FILEO "\n";
	print FILEO "Follow Links:\n";
	print FILEO join "\n", @{$ref_followlinks};
	print FILEO "\n";
	print FILEO "Out Links:\n";
	print FILEO join "\n", map{$_->[2]} @{$ref_outlinks};
	print FILEO "\n";
	close FILEO;
}

1;

=head1 NAME

	Labrador::Crawler::ContentHandler::Save

=head1 DESCRIPTION

A simple ContentHandler, that saves the content to .data
file, and meta-data to a .meta file. Hence two new files
are created for each URL retrieved. The first part of the
filename is the MD5 encoding of the URL.

=head1 REVISION

	$Revision: 1.2 $

=cut

