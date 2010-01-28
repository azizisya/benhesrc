package Labrador::Common::URLAlloc::BFS;

use lib '../../../';
use base qw(Labrador::Common::URLAlloc);
use strict;

=head1 NAME

Labrador::Common::URLAlloc::BFS

=head1 SYNOPSIS

	use Labrador::Common::URLAlloc;
	use Labrador::Common::URLAlloc::BFS;
	my $urlalloc = Labrador::Common::URLAlloc::new(
		'Labrador::Common::URLAlloc::BFS',
		$data, $urlstates, $config);
	$urlalloc->new_url('http://www.gla.ac.uk/#');
	print $urlalloc->get_url();


=head1 DESCRIPTION

A simple queueing implemetation. New urls are entered at the back of the queue,
retreived from front of queue. This follows a breadth-first fashion.

=head1 METHODS

=over 4

=item new_url($url)

Enqueue $url

=cut

sub new_url
{
	my ($self, $url) = @_;

	#TODO move urlstate check elsewhere?
	#return if the URL is in a queue somewhere
	my $url_state = $self->{urlstates}->url($url);
	if (defined $url_state and $url_state < 0)
	{
		warn "URL $url  already queued, skipping\n";
	}
	
	warn "Adding URL $url to masterqueue\n";
	push @{$self->{masterqueue}}, $url;
}

=back

=head1 REVISION

	$Revision: 1.7 $

=cut

1;
