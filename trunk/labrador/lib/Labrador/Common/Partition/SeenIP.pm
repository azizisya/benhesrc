package Labrador::Common::Partition::SeenIP;

use base qw(Labrador::Common::Partition);
use Labrador::Common::DNSLookup;
use URI;

=head1 NAME

Labrador::Common::Partition::SeenIP

=head1 DESCRIPTION

A simple partitioning implementation, which remembers which hosts were assigned to
which named partitions, by the IP address of the hostname of the URL

=head1 METHODS

=over 4


=item hash($url)

Returns the IP address associated with the hostname

=cut

sub hash
{
	my ($self, $url) = @_;
	my $hostname = '';
	eval{
		$hostname = URI->new($url)->host;
	};
	if ($@) { 
		warn "$url didnt turn out as a valid url : $@";
		$hostname = '';
	}
	return Labrador::Common::DNSLookup::lookup($hostname);
}

=back

=head1 REVISION

	$Revision: 1.1 $

=cut

1;
