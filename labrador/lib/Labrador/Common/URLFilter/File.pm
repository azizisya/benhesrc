#!/usr/bin/perl -w

package Labrador::Common::URLFilter::File;

#inherit from Labrador::Common::URLFilter;
use lib '../..';
use base qw(Labrador::Common::URLFilter);
use File::Spec::Functions qw(rel2abs);
use constant ALLOWED_STATES => {'blacklist' => 1, 'whitelist' => 1};
use constant ALLOWED_TYPES => {
	'url' => 1, 'filename' => 1,
	'extension' => 1, 'hostname' => 1, 'host'=>1, 'querystring' => 1
	};

use strict;

=head1 NAME

Labrador::Common::URLFilter::File

=head1 DESCRIPTION

This URL Filter is used to blacklist or whitelist all URLs of parts
of URLs

=head1 EXAMPLES

	URLFilter File Whitelist Host data/whitelists/hosts_gla.txt
	URLFilter File Blacklist Host data/blacklists/hosts_gla.txt
	URLFilter File Blacklist Extension data/blacklists/extensions_gla.txt
	URLFilter File Blacklist URL data/blacklists/urls_gla.txt

=cut

sub init
{
	my $self = shift;
	
	#check the way the data file has to be used
	my ($state, $type) = ($self->{params}->[0], $self->{params}->[1]);
	$state = lc $state; $type = lc $type;
	
	#check state is allowed
	die "URLFilter::File State $state is unknown\n" unless
		exists ALLOWED_STATES->{$state};
	$self->{state} = $state;

	#check type we're checking against is allowed
	die "URLFilter::File Matching against $type is unknown\n" unless
		exists ALLOWED_TYPES->{$type};
	$self->{type} = $type;

	#check we can open the data file
	my $filename = rel2abs($self->{params}->[2],$self->{config}->get_scalar("Base"));
	die "Could not find URLFilter::File datafile $filename\n" unless (-e $filename);

	#parse the data file into a hash for easy use!	
	$self->{data} = {};
	
	open FILEI, "<$filename" or die "URLFilter::File Could not open $filename - $!\n";
	foreach (<FILEI>)
	{
		chomp $_;
		$self->{data}->{lc $_} = 1;
	}
	close FILEI;

}

sub filter
{
	my ($self, $uri, $url) = @_;
	
	my $matching = lc $self->_get_part($uri, $self->{type});

	#be safe - if the field doesn't exist, passonto next filter
	return 1 unless (defined $matching and length $matching);

	#check for exact matches
	return $self->{state} eq 'blacklist' ? 0 : 1
		if exists $self->{data}->{$matching};

	#TODO any others we need to check here?
	#need to play safe here
	return 1 if $self->{type} eq 'extension';
	
	my $matched = $self->{state} eq 'blacklist' ? 1 : 0;
	foreach my $black_entry (keys %{$self->{data}})
	{
		#black_entry was found at the start of $url
		if (index($matching, $black_entry) == 0)
		{
			$matched = $self->{state} eq 'blacklist' ? 0 : 1;
		}
	}

	return $matched;

}

=head1 REVISION

	$Revision: 1.13 $

=cut

1;
