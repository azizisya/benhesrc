#!/usr/bin/perl -w

package Labrador::Common::URLFilter::Regexp;

#inherit from Labrador::Common::URLFilter
use base qw(Labrador::Common::URLFilter);
use strict;

=head1 NAME

Labrador::Common::URLFilter::Regexp

=head1 DESCRIPTION

Allows matching or discaring of URLs, based on exact or regular expression
string matching, and on different parts of the URL, or the entire URL.

Matchings allowed are == != =~ !~

=head1 EXAMPLES

	URLFilter Regexp Scheme =~ http(?:s?)
	URLFilter Regexp URL =~ ^http(?:s?)://(?:[A-Za-z0-9\-.]+)\.gla\.ac\.uk
	URLFilter Regexp Host =~ (?:[A-Za-z0-9\-.]+)\.gla\.ac\.uk
	#dont follow links into any calendar type web pages
	URLFilter Regexp Querystring !~ calendar

=cut


sub init
{
	#TODO check that $self->{params}->[1] is one of == != =~ !~

}

sub filter
{
	my ($self, $uri, $url) = @_;
	
	my ($field, $type, $regexp) = @{$self->{params}};
	
	$field = lc $field;
	my $match = $field eq 'url' ? $url : $self->_get_part($uri, $field);

	my $return = 1;
	if (!defined $match or ! length $match)
	{
		#field requested wasn't present, move on
		$return  = 1; 	
	}
	elsif ($type eq '==')
	{
		$return = lc $match eq lc $regexp ? 1 : 0;
	}
	elsif ($type eq '!=')
	{
		$return = $match ne $regexp ? 1 : 0;
	}
	elsif ($type eq '=~')
	{
		$return = $match =~ /$regexp/i ? 1 : 0;	
	}
	elsif ($type eq '!~')
	{
		$return = $match !~ /$regexp/i ? 1 : 0;
	}
	
	return $return;
}

=head1 REVISION

	$Revision: 1.11 $

=cut

1;
