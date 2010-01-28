package Labrador::Common::Partition::MultipleHostTopDirectory;

use base qw(Labrador::Common::Partition);
use URI;

=head1 NAME

Labrador::Common::Partition::MultipleHostTopDirectory

=head1 DESCRIPTION

A simple partitioning implementation, for use when crawling a few host.
It hashes on the top folder in the directory path in a given host.

=head1 METHODS

=over 4


=item hash($url)

Returns the top-level directectory name of $url.

=cut

sub hash
{
	my ($self, $url) = @_;
	my $uri = URI->new($url);
	my @uri_segs = $uri->path_segments();
	return $uri->host_port().$uri_segs[1] .'/' if (scalar @uri_segs > 2);
	return $uri->host_port().'/';
}

=back

=head1 REVISION

	$Revision: 1.1 $

=cut

1;
