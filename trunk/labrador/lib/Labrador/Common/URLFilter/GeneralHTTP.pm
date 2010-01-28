#!/usr/bin/perl -w

package Labrador::Common::URLFilter::GeneralHTTP;

#inherit from Labrador::Common::URLFilter
use base qw(Labrador::Common::URLFilter);
use strict;

=head1 NAME

Labrador::Common::URLFilter::GeneralHTTP

=head1 DESCRIPTION

Checks the URL is generally valid HTTP

=head1 EXAMPLE

    URLFilter GeneralHTTP

=cut


sub init
{
	return 1;
}

sub filter
{
	my ($self, $uri, $url) = @_;

	#check it's actually HTTP
	return unless $uri->scheme =~ 'http(?:s?)';

	#check that it does actually have a hostname
	#eg <a href="http://">Bla</a>
	my $hostname = $uri->host || '';
	return 0 unless length $hostname;
	$hostname =~ s/[^A-Za-z\-]//g; #TODO should we just do a negative match instead
	return 0 unless length $hostname;
	return 1;
}

=head1 REVISION

	$Revision: 1.3 $

=cut

1;
