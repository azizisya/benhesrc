#!/usr/bin/perl -w

package Labrador::Common::URLFilter::Length;

#inherit from Labrador::Dispatcher::URLFilter
use base qw(Labrador::Common::URLFilter);
use strict;

=head1 NAME

Labrador::Common::URLFilter::Length

=head1 DESCRIPTION

This URL Filter is used to discard overly long URLs. URLs longer than a
certain length are generally spider traps, or of little interest to crawlers
anyway.

=head1 EXAMPLE

	URLFilter Length 1024

=cut

sub filter
{
	my ($self, $uri, $url) = @_;
	
	if (length $url > $self->{params}->[0])
	{
		return 0;
	}
	return 1;
}

=head1 REVISION

	$Revision: 1.8 $

=cut

1;
