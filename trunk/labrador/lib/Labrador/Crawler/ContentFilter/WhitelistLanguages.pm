package Labrador::Crawler::ContentFilter::WhitelistLanguages;

use base qw(Labrador::Crawler::ContentFilter);
use strict;

=head1 NAME

Labrador::Crawler::ContentFilter::WhitelistLanguages

=head1 DESCRIPTION

This content filter responsible for filtering out documents that can
categorically be said not to be of the given list of stopwords for a 
particular langage.

=head1 METHODS

=over 4

=item init

=cut

sub init
{
	my $self = shift;
	#@_ should contain a list of filenames

	my $base = $self->{config}->get_scalar("Base", 1) || '../';
	$self->{words} = [];
	foreach (@_)
	{
		my $filename = $base.$_;
		if (-e $filename)
		{
			open(FILEI, $filename);
			my @data = <FILEI>;
			close FILEI;
			s/\r\n//g for @data;
			chomp @data;
			push @{$self->{words}}, @data;
		}else{
			warn "Skipping $_ as it could not be opened for use in language tests (ContentFilter)";
		}
	}
	return 1;
}

=item filter($document, $privs)

=cut

sub filter
{
	my ($self, $document, $privs) = @_;

	my $count = 0;

	my $content = $document->contents;
	foreach my $word (@{$self->{words}})
	{
		if (index $content, $word)
		{
			$count =1;
			last;
		}
	}
	
	#if we didn't match any words from our given languages, then
	#we can fairly accurately say it's not those languages, hence
	#we probably don't want the document
	unless ($count)
	{
		$privs->{'follow'} = 0;
		$privs->{'index'} = 0;
	}
}

=back

=head1 REVISION

	$Revision: 1.2 $

=cut

1;
