package Labrador::Common::URLAlloc;

use strict;
use constant MASTERQUEUE => "MasterQueue";
use lib '../../';

=head1 NAME 

Labrador::Common::URLAlloc

=head1 SYNOPSIS

    use Labrador::Common::URLAlloc;
    use Labrador::Common::URLAlloc::Delay;
    my $urlalloc = Labrador::Common::URLAlloc::new(
        'Labrador::Common::URLAlloc::Delay',
        $data, $urlstates, $config);
    $urlalloc->new_url('http://www.gla.ac.uk/#');
    print $urlalloc->get_url();

=head1 DESCRIPTION

Central URL queueing class, though it must be implemented. Examples of
child classes are Delay, BFS, DFS.

NB: This class is abstract, and must be implemented by a child class.

=head1 METHODS

=over 4

=item new($data, $urlstates, $config, @params)

=cut

sub new
{
	my ($package, $data, $urlstates, $config, @params) = @_;
	my $self = bless {config => $config, data => $data}, $package;
	$self->{urlstates} = $urlstates; #TODO useless now?
	$self->init(@params);	
	return $self;
}

=item init()

=cut

sub init
{
	my ($self) = @_; 
	$self->{data}->register_variable(MASTERQUEUE, "ARRAY", 2);	
	$self->{masterqueue} = $self->{data}->obtain_variable(MASTERQUEUE);
	
	$self->{av_time} = time;
	$self->{av_urls} = 0;
	$self->{last_av} = 0;
	return 1;
}

=item new_urls(@urls)

Maps to new_url for each @url.

=cut

sub new_urls
{
	my $self = shift;
	foreach (@_)
	{
		$self->new_url($_);
	}
}

=item new_url($url)

ABSTRACT: Must be implemented by a child class.

=cut

sub new_url;

=item update_average()

=cut

sub update_average
{
	my $self = shift;
	my $time = time;
	my $diff = $time - $self->{av_time};
	if ($diff > 60 or $self->{last_av} == 0)
	{
		$self->{last_av} = $self->{av_urls} / ($diff||1);
		$self->{av_time} = time;
	}
	return $self->{last_av};
}

=item last_average()

=cut

sub last_average
{
	return $_[0]->{last_av} || $_[0]->update_average;
}

=item get_url()

Dequeue one URL

=cut

sub get_url
{
	return ($_[0]->get_urls(1))[0] || ();
}

=item get_urls($count)

Dequeue upto $count URLs.

=cut

sub get_urls
{
	my ($self, $count) = @_;

	my @urls = splice @{$self->{masterqueue}}, 0, $count;
	$self->{av_urls} += scalar @urls;
	$self->update_average();
	return @urls;
}

=item empty_queues

Empties all the queues.

=cut

sub empty_queues
{
	my $self = shift;
	#TODO what happens to all the URLs saved in URLState?
	splice @{$self->{masterqueue}};
}

=item next()

In Delay URLAlloc modules, returns number of seconds until next URL is ready,

=cut

sub next
{
	my $self = shift;
	return 0 if $self->queue_size;
	return 5;
}

=item queue_size()

Returns the number of URLs queued.

=cut

sub queue_size
{
	my $self = shift;
	return scalar @{$self->{masterqueue}};
}

=item delay_urls(%urls)

Delay the URLs in the keys of %urls by the amount of seconds
in values of %urls. Only URLAlloc implementations that implement
delay handling will actually delay the URLs. Other implemetation
will just enqueue the URLs to the back of the queues.

=cut

sub delay_urls
{
	my ($self, %urls) = @_;
	unshift @{$self->{masterqueue}}, keys %urls;
}

=back

=head1 REVISION

	$Revision: 1.13 $

=cut

1;

