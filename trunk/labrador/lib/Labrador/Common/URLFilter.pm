#!/usr/bin/perl -w

package Labrador::Common::URLFilter;
use URI;
use strict;

sub new
{
	my ($me, $filter_name, $config, @params) = @_;
	my $class = $me.'::'.$filter_name; 
	_load_module($class);
	my $self = bless {config => $config, params => \@params}, $class;
	my @classparts = split /::/, $class;
	$self->{name} = $classparts[$#classparts];
	$self->init;
	return $self;
}


sub filter;


sub init
{
}

sub name
{
	return $_[0]->{name};
}

sub _load_module {
    eval "require $_[0]";
    die $@ if $@;
    #$_[0]->import(@_[1 .. $#_]);
}


sub _get_part
{
	my ($self, $uri, $field) = @_;
	my $match ='';

	eval
	{
		if ($field eq 'url')
		{
			$match = "$uri";
		}
		elsif ($field eq 'scheme')
		{
			$match = $uri->scheme;
		}
		elsif ($field eq 'hostname' or $field eq 'host')
		{
			$match = $uri->host;
		}
		elsif ($field eq 'path')
		{
			$match = $uri->path;
		}
		elsif ($field eq 'filename')
		{
			my @url_segs = $uri->path_segments();
			$match = $url_segs[$#url_segs];
		}
		elsif ($field eq 'extension')
		{
			my @url_segs = $uri->path_segments();
			my $filename = $url_segs[$#url_segs];
			if ($filename)
			{
				my (@rubbish) = split /\./, $filename;
				$match = $rubbish[$#rubbish];
			}
		}
		elsif ($field eq 'querystring')
		{
			$match = $uri->query;
		}
	}; 
	if ($@) { warn "Problem working with $uri : $@";}
	
	return $match;
}

1;

__END__

=head1 NAME

Labrador::Dispatcher::URLFilter

=head1 SYPNOPSIS

	use Labrador::Dispatcher::URLFilter::Regexp;
	my $filter = Labrador::Dispatcher::URLFilter::new(
		'Labrador::Dispatcher::URLFilter::Regexp', $config, @param);
	my $url = 'http://....';
	print $filter->filter($url) ? "Good" : "Bad";

=head1 DESCRIPTION

This is an abstract base class for URLFilters. Note it is hoped that URLFilters should be proper (repeatable) functions - ie they should return the same result given the same parameters. Hence, a filter() implementation shouldn't have side-effects. 
Labrador::Dispatcher::Processing loads Memoize, and uses it to cache the result of a URL being filtered. Hence after a URL has been seen once, it may not be seen again.

=head1 METHODS

=over 4

=item new($CLASSNAME, $config, @params)

Constructor. It should be passed the name of the class it should instantiate. A reference to an instance of Labrador::Common::Config is also recommended. This allows children modules to pull additional configuration parameters from the configuration file.

=item init()

Is called by new() to initialise a filter. 

=item filter($url)

Abstract method, must be implemented by a filter. Returns true for
a good URL, or 0 for a bad one.

=item name()

Returns the name of this filter, so can be used for debugging purposes.

=back

=head1 REVISION

	$Revision: 1.11 $

=cut

