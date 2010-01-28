package Labrador::Common::Partition::SingleHostTopDirectory;

use base qw(Labrador::Common::Partition);
use URI;

=head1 NAME

Labrador::Common::Partition::SingleHostTopDirectory

=head1 DESCRIPTION

A simple partitioning implementation, for use when crawling a single host.
It hashes on the top folder in the directory path.

=head1 METHODS

=over 4


=item hash($url)

Returns the top-level directectory name of $url.

=cut

sub hash
{
	my ($self, $url) = @_;
	my @uri_segs = URI->new($url)->path_segments();
	return $uri_segs[1] .'/' if (scalar @uri_segs > 2);
	return '/';
}

=back

=head1 REVISION

	$Revision: 1.1 $

=cut

1;
