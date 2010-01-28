package Labrador::Crawler::Manager;
use strict;
use lib '../..';

my $filebase;

=head1 NAME

=head1 SYNOPSIS

	use Labrador::Crawler::Manager;
	my $manager = new Labrador::Crawler::Manager(
		'Partitioned', $data, $disp_client,
		$config, @params);

=head1 DESCRIPTION

Base class for crawler Manager classes. Known implementors are
Simple and Partitioned.

=head1 METHODS

=over 4

=item new($manager_name, $data, $disp_client, $config, @params)

Constructs a new manager module inherited from this one.

=cut

sub new
{
	my ($me, $manager_name, $data, $client, $config, @params) = @_;
	my $class = $me.'::'.$manager_name;
	_load_module($class);
	my $self = bless {data => $data, client => $client, config => $config}, $class;
	$self->init(@params);	
	return $self;
}

=item init()

=cut

sub init
{

}

=item next_url

Provide the next URL to fetch.

=cut

sub next_url;

=item finished_url($url)

Mark $url as finished.

=cut

sub finished_url;

=item failed_url($url)

Mark $url as failed.

=cut

sub failed_url;

=item found_urls($url, @urls_found)

Process the @urls_found that were found in $url.

=cut

sub found_urls;

=item queue_status()

=cut

sub queue_status;

=item get_stats()

=cut

sub get_stats;

=back

=head1 PRIVATE METHODS

=over 4

=item _load_module($name)

Load the module called $name.

=cut

sub _load_module {
	eval "require $_[0]";
	die $@ if $@;
	#$_[0]->import(@_[1 .. $#_]);
}


=cut

=back

=head1 REVISION

	$Revision: 1.14 $

=cut


1;
