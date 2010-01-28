package Labrador::Crawler::ContentFilter::Binary;

use base qw(Labrador::Crawler::ContentFilter);
use strict;

=head1 NAME

Labrador::Crawler::ContentFilter::Binary

=head1 DESCRIPTION

This content filter responsible for filtering out documents that contain
binary data.

=head1 METHODS

=over 4

=item filter($document, $privs)

Check the contents of the document for the presence of \0 (null
character), which is a pretty good indicator that the type of data
is binary in content, allowing us to ignore the document.

=cut

sub filter
{
	my ($self, $document, $privs) = @_;
	
	my $ref_data = $document->content();	
	
	#if( $$ref_data =~ /[^\\w\\s\\d\\p{IsP}]/ )
	if( $$ref_data =~ /\0/ )
	{
		$privs->{'follow'} = 0;
		$privs->{'index'} = 0;
		warn "Ignoring binary data ".$document->url()."\n";
	}

}

=back

=head1 REVISION

	$Revision: 1.2 $

=cut

1;
