package Labrador::Common::Partition::SeenHost;

use base qw(Labrador::Common::Partition);
use URI;

=head1 NAME

Labrador::Common::Partition::SeenHost

=head1 DESCRIPTION

A simple partitioning implementation, which remembers which hosts were assigned to
which named partitions, using the hostname of the URL

=head1 METHODS

=over 4


=item hash($url)

=cut

sub hash
{
	my ($self, $url) = @_;
	return URI->new($url)->host;
}

=back

=head1 REVISION

	$Revision: 1.3 $

=cut

1;
