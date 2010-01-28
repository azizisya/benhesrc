package Labrador::Dispatcher::CrawlerAlloc;

use strict;
#TODO put this Constants.pm methinks
#as URLAlloc will need access to it
use constant CRAWLERS => 'Crawlers';
use constant CRAWLER_PREFIX => 'crawler_';

=head1 NAME

Labrador::Dispatcher::CrawlerAlloc

=head1 SYPNOPSIS

	use Labrador::Dispatcher::CrawlerAlloc;

=head1 DESCRIPTION

Call this module to get URLs for a crawler to process.
NB: This module is abstract.

=head1 METHODS

=over 4

=item new($data, $urlalloc, $urlstates, @params)

Construct a new CrawlerAlloc module. Calls init() automatically.

=cut

sub new
{
	my ($class, $config, $data, $urlalloc, $urlstates, @params) = @_;
	my $self = bless {config => $config, data => $data, 
		urlalloc=>$urlalloc, urlstates => $urlstates},
		$class;

	$self->init();
	return $self;
}

=item init()

=cut

sub init
{
	my $self = shift;

	#shouldn't be persistent, as its values are references to crawler queues
	$self->{data}->register_variable(CRAWLERS, 'HASH', 0);

	$self->{crawlers} = $self->{data}->obtain_variable(CRAWLERS);
}

=item register_crawler($hostname)

A crawler with from $hostname just connected. Register it

=cut

sub register_crawler
{
	my ($self, $crawler_hostname) = @_;

	unless (exists $self->{crawlers}->{$crawler_hostname})
	{
		$self->{data}->register_variable(CRAWLER_PREFIX.$crawler_hostname, 'ARRAY', 1);
		$self->{crawlers}->{$crawler_hostname} = 
			$self->{data}->obtain_variable(CRAWLER_PREFIX.$crawler_hostname);
	}
	
	
}

=item unregister_crawler($crawler_hostname)

Crawler $crawler_hostname just disconnected.

=cut

sub unregister_crawler
{
	my ($self, $crawler_hostname) = @_;

}

=item get_urls($crawler_hostname, $count)

Obtains upto $count URLs for $crawler_hostname

=cut

sub get_urls;

=item update_queues()

Dispatcher thinks we have some time free, so we should use it to build up the
crawler queues

=cut

sub update_queues;

=item empty_queues()

Remove all items in all queues,

=cut

sub empty_queues;

=item queue_sizes()

Return the total size of all queues

=cut

sub queue_sizes;

=item get_stats

Return any statistics provided

=cut

sub get_stats;


=back

=head1 REVISION

	$Revision: 1.8 $

=cut

1;
