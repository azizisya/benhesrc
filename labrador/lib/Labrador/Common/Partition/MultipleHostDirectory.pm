package Labrador::Common::Partition::MultipleHostDirectory;

use base qw(Labrador::Common::Partition);
use URI;

=head1 NAME

Labrador::Common::Partition::MultipleHostDirectory

=head1 DESCRIPTION

A simple partitioning implementation, for use when crawling a few hosts.
It hashes on the hostname andn port, plus the directory path, but not filename of the URL.

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
	my $uri = URI->new($url);
	my @uri_segs = $uri->path_segments();
	pop @uri_segs;
	return $uri->host_port().(join '/',@uri_segs).'/';
}

=back

=head1 REVISION

	$Revision: 1.1 $

=cut

1;
