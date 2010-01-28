package Labrador::Crawler::Document::HTML;

use base qw(Labrador::Crawler::Document);
use vars qw($linkextractor $linkextor);
use strict;

BEGIN{
	$linkextor = 0;
	eval {
		require HTML::LinkExtor;
	};
	$linkextor = 1 unless $@;
	
	#we'd rather use HTML::LinkExtractor if it's available
	#as it provides anchor text as well
	$linkextractor = 0;
	eval {
		require HTML::LinkExtractor;
	};
	$linkextractor = 1 unless $@;
}


=head1 NAME

Labrador::Crawler:Document::HTML

=head1 SYNOPSIS

	use Labrador::Crawler::Document;
	my $doc = new Document(\$data, $req);
	#$doc will be instantiated with appopriate subclass if one exists

=head1 DESCRIPTION

This is the custom subclass of Labrador::Crawler::Document for HTML classes. 
It provides two ways to extract links from an HTML document - using HTML::LinkExtor
(which is part of the standard HTML::Parser distribution), or if it's available
HTML::LinkExtractor. HTML::LinkExtractor is preferred as this also extracts Anchors
texts of links.

=head1 METHODS

=over 4

=item init

Initialise the class.

=cut

sub init
{
	my $self = shift;
	return 0 unless $self->SUPER::init(@_);
	
	return 1;
}

=item links

Extract a list of links from the page.

=cut

sub links
{
	my $self = shift;
	return @{$self->{links}} if @{$self->{links}||[]};

	if ($linkextractor)
	{
		$self->{links} = $self->_linkextractor;
	}
	elsif ($linkextor)
	{
		$self->{links} = $self->_linkextor;
	}
	else
	{
		$self->{links} = [];
	}
	return @{$self->{links}};
}


=back 

=head1 PRIVATE METHODS

=over 4

=item _linkextor

Extract links from HTML document using HTML::LinkExtor

=cut

sub _linkextor
{
	my $self = shift;
	# ( [$tag, $attr => $url1, $attr2 => $url2,...], )

	warn "Parsing for ".$self->url()."\n";

	my @return = ();
	my $parser = HTML::LinkExtor->new(undef, $self->{response}->base);
	$parser->parse(${$self->{data}});
	my @tags = $parser->links;
	foreach my $line (@tags)
	{
		my $tag = shift @{$line};
		my %attr_urls = @{$line};
		foreach my $attr (keys %attr_urls)
		{
			push @return, {tag => $tag, attribute => $attr,
				text => '', url => $attr_urls{$attr}};
		}
	}
	undef $parser;
	return \@return;
}

=item _linkextractor

Extract links using HTML::LinkExtractor

=cut

sub _linkextractor
{	#TODO test
	my $self = shift;
	#[ { type => 'img', src => 'image.png' }, ]

	my @return;
	my $parser = HTML::LinkExtractor->new(undef, $self->{response}->base, 1);
	$parser->parse($self->{data});
	foreach my $line ( @{ $parser->links() } )
	{
		my %attr_urls = %{$line};
		my $tag = $attr_urls{'type'};
		my $text = $attr_urls{'_TEXT'} || '';
		delete $attr_urls{'type'}; delete $attr_urls{'_TEXT'};
		
		foreach my $attr (keys %attr_urls)
		{
			push @return, {tag => $tag, attribute => $attr,
				text => $text, url => $attr_urls{$attr}};
		}
	}
	undef $parser;
	return \@return;
}

=back

=head1 REVISION

	$Revision: 1.4 $

=cut

1;

