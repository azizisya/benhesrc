package Labrador::Common::URLState::Normal;

use base qw(Labrador::Common::URLState);

=head1 NAME

Labrador::Common::URLState

=head1 SYNOPSIS

	use Labrador::Common::URLState::Normal;
	my $states = new Labrador::Common::URLState::Normal($data);
	$states->url('http://www.gla.ac.uk/#', time);
	print "Seen http://www.gla.ac.uk/# before" if $states->url_exists(http://www.gla.ac.uk/#');

=head1 DESCRIPTION

Used for recording seen urls. Concrete class. This class does not use any digests
for URLs to save space, or a Bloom filter.

=head1 STATES

-1 is in the master queue
-2 is in the crawler queue
-3 is with the crawler
-4 failed
>0 is the time we were informed by the crawler it finished crawling the URL

=head1 METHODS

=over 4

=item new($data)

Constructs a new URLState object

=item init()

Initialises this module. Automagically called by new()

=cut

sub init
{
	my $self = shift;
	$self->SUPER::init(@_);
}

=item url($url, [$value])

Adds the $url to the hash if it doesnt exists. Returns the value
it had if it was already there.

=cut

sub url
{
	my ($self, $url, $value) = @_;
	if (defined $value)
	{
		$self->{urlstates}->{$url} = $value;
		return $value;
	}
	return $self->{urlstates}->{$url} || undef;
}

=item url_exists($url)

Returns 1 if url seen before 

=cut

sub url_exists
{
	my ($self, $url) = @_;
	return exists $self->{urlstates}->{$url};
}

=back

=head1 REVISION

	$Revision: 1.4 $

=cut


1;
