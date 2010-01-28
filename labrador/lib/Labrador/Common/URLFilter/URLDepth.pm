#!/usr/bin/perl -w

package Labrador::Common::URLFilter::URLDepth;

#inherit from Labrador::Common::URLFilter
use base qw(Labrador::Common::URLFilter);
use strict;

=head1 NAME

Labrador::Common::URLFilter::URLDepth

=head1 DESCRIPTION

This URL Filter is used to discard URLs with too many slashes in them. This
may occur to an incorrect symlink causing a recursive directory structure.

=head1 EXAMPLE

	URLFilter URLDepth 10

=cut

sub filter
{
	my ($self, $uri, $url) = @_;
	
	#TODO this will also include the filename, but out by one
	#isn't a huge issue!	
	my @parts = $uri->path_segments;	
	if (scalar @parts > $self->{params}->[0])
	{
		return 0;
	}
	return 1;
}

=head1 REVISION

	$Revision: 1.6 $

=cut


1;
