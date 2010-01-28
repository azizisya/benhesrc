package Labrador::Common::URLAlloc::DFS;

use lib '../../../';
use base qw(Labrador::Common::URLAlloc);
use strict;

=head1 NAME

Labrador::Common::URLAlloc::DFS

=head1 SYNOPSIS

    use Labrador::Common::URLAlloc;
    use Labrador::Common::URLAlloc::DFS;
    my $urlalloc = Labrador::Common::URLAlloc::new(
        'Labrador::Common::URLAlloc::DFS',
        $data, $urlstates, $config);
    $urlalloc->new_url('http://www.gla.ac.uk/#');
    print $urlalloc->get_url();


=head1 DESCRIPTION

A simple queueing implemetation. New urls are entered at the fronth of the queue,
retreived from front of queue. This follows a depth-first fashion.

NB: Depth-first is not a recommended crawling strategy.

=head1 METHODS

=over 4

=item new_url($url)

Enqueue $url

=cut

sub new_url
{
	my ($self, $url) = @_;
	unshift @{$self->{masterqueue}}, $url;
}

=back

=head1 REVISION

	$Revision: 1.7 $ 
	
=cut

