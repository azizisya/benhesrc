package Labrador::Common::RobotsCache;
use strict;

=head1 NAME

Labrador::Common::RobotsCache

=head1 SYPNOPSIS

	use Labrador::Common::RobotsCache;
	my $robotscache = new Labrador::Common::RobotsCache($config);
	my @file;
	if (! @file = $robotscache->get_file('www.gla.ac.uk');
	{
		#fetch http://www.gla.ac.uk/robots.txt using HTTP 
		#..
		#save to cache
		$robotscache->set_file('www.gla.ac.uk', @file);
	}

=head1 DESCRIPTION

Implements a disk-cache of robots.txt for hosts. Files are expired after
a default of 25 days.

=head1 CONFIGURATION

Behaviour can be altered by the following configuration file options:

=over 4

=item RobotsTxtExpiry

Number of days to keep a robots.txt file in cache. Defaults to 25 days.

=item RobotsTxtCache

Absolute location to use as the disk cache for robots.txt cache. Will
be created if it does not exist. Defaults to 'data/robots.txt' relative
to configuration directive Base

=item Base

Used as the file base for default robots.txt file cache directory. Defaults to '../'

=back

=head1 METHODS

=over 4

=item new($configuration)

Constructor. Calls init() automatically;

=cut



sub new
{
	my ($class, $config) = @_;
	my $self = bless { config => $config }, $class;
	$self->init();
	return $self;

}

=item init

Initialises class, loading appropriate directives from configuration file.

=cut

sub init
{
	my $self = shift;
	my $config = $self->{config};
	my $filebase = $config->get_scalar('Base', 1) || '../';
	my $cache = $config->get_scalar('RobotsTxtCache', 1) || $filebase.'data/robots.txt/';
	mkdir $cache unless -e $cache;
	$self->{cache} = $cache;
	$self->{cacheexpiry} = 24*60*60* ($config->get_scalar('RobotsTxtExpiry',0));
	$self->{cacheexpiry} ||= (25*24*60*60);
}

=item cached($hostname)

Returns a boolean determining whether the cache contains the robots.txt
file for the given $hostname;

=cut

sub cached
{
	my ($self, $hostname) = @_;
	my $filename =  $self->{cache}.$hostname;
	return 0 unless -e $filename;
	if (time - (stat($filename))[9] > $self->{cacheexpiry})
	{
		#robots.txt file was a bit old, should probably be deleted and refetched
		unlink $filename;
		return 0;
	}
	return 1;
}

=item get_file($hostname)

Retrieve the robots.txt file for $hostname. Note that an empty array
signifies that the file was not found in the cache, and a single comment ('#')
implies that the given host has no robots.txt file.

=cut

sub get_file
{
	my ($self, $hostname) = @_;

	#return some shortcut cases, where robots.txt file are known not to exist
	return '#' if ($hostname =~ /\.blogspot\.com$/);
	return '#' if ($hostname =~ /\.canalblog\.com$/);
		
	my $filename = $self->{cache}.$hostname;
	return () unless -e $filename;
	return "#" unless -s $filename;
	if (time - (stat($filename))[9] > $self->{cacheexpiry})
	{
		#robots.txt file was a bit old, should probably be deleted and refetched
		unlink $filename;
		return ();
	}

	open(FILE_ROBOTS, "<$filename") or die "Couldn't open robots.txt cache for $hostname ($filename) : $!\n";
	my @data = <FILE_ROBOTS>;	
	close FILE_ROBOTS;
	chomp @data;
	return @data;
}

=item set_file($hostname, @contents)

Update the cache with the robots.txt file for $hostname.

=cut

sub set_file
{
	my ($self, $hostname) = (shift, shift);
	my $filename = $self->{cache}.$hostname;
	open(FILE_ROBOTS, ">$filename") or die "Couldn't write to robots.txt cache for $hostname ($filename) : $!\n";
	print FILE_ROBOTS join "\n", @_ if scalar @_;
	close FILE_ROBOTS;
}

=back

=head1 REVISION

	$Revision: 1.8 $

=cut

1;
