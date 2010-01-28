package Labrador::Common::Partition::SingleHostDirectory;

use base qw(Labrador::Common::Partition);
use URI;

=head1 NAME

Labrador::Common::Partition::SingleHostDirectory

=head1 DESCRIPTION

A simple partitioning implementation, for use when crawling a single host.
It hashes on the directory path, but not filename of the URL.

NB: This can mean that large dynamic applications where the page is changed
using the querystring, all fall in the same partition as their hash value is
the same.

=head1 METHODS

=over 4

=item hash($url)

Returns the path of $url, minus the filename should there be one.

=cut

sub hash
{
	my ($self, $url) = @_;
	my @uri_segs = URI->new($url)->path_segments();
	pop @uri_segs;
	return (join '/',@uri_segs).'/';
}

=back

=head1 REVISION

	$Revision: 1.3 $

=cut

1;
