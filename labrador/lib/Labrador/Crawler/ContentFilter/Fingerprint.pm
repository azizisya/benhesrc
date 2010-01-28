package Labrador::Crawler::ContentFilter::Fingerprint;

use base qw(Labrador::Crawler::ContentFilter);
use strict;

=head1 NAME

Labrador::Crawler::ContentFilter::Fingerprint

=head1 DESCRIPTION

This content filter responsible for filtering out documents that have been
seen before, according to a lookup on the dispatcher.

=head1 METHODS

=over 4

=item filter($document, $privs)

Looks up the MD5 fingerprint of $document in the hashtable the dispatcher
maintains. Note this method may be a bottleneck in performance.

=cut

sub filter
{
	my ($self, $document, $privs) = @_;
	
	my $where = $self->{client}->FINGERPRINT( $document->fingerprint(), "".($document->url) );	
	if( $where )
	{
		$privs->{'follow'} = 0;
		$privs->{'index'} = 0;
		warn "Ignoring duplicate ".$document->url()." as duplicate with $where\n";
	}

}

=back

=head1 REVISION

	$Revision: 1.1 $

=cut

1;
