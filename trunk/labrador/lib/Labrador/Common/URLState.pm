package Labrador::Common::URLState;

use constant URLSTATES => 'URLStates';

#-1 is in the master queue
#-2 is in the crawler queue
#-3 is with the crawler
#-4 failed
#>0 is the time we were informed by the crawler it finished crawling the URL

=head1 NAME

Labrador::Common::URLState

=head1 SYNOPSIS

	use Labrador::Common::URLState::Normal;
	my $states = new Labrador::Common::URLState::Normal($data);
	$states->url('http://www.gla.ac.uk/#', time);
	print "Seen http://www.gla.ac.uk/# before" if $states->url_exists(http://www.gla.ac.uk/#');

=head1 DESCRIPTION

Used for recording seen urls. Abstract class, must be implemented. Some papers
recommend using a Bloom filter or digests of the URLs to save space.

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

=cut

sub new
{
	my ($class, $data, @params) = @_;
	my $self = bless { data => $data  }, $class;
	$self->init;
	return $self;	
}

=item init()

Initialises this module. Automagically called by new()

=cut

sub init
{
	my $self = shift;
	$self->{data}->register_variable(URLSTATES, 'HASH', 1);
	$self->{urlstates} = $self->{data}->obtain_variable(URLSTATES);
}

=item url($url, [$value])

Adds the $url to the hash if it doesnt exists. Returns the value
it had if it was already there.

=cut

sub url;

=item url_exists($url)

Returns 1 if url seen before 

=cut

sub url_exists;

=back

=head1 REVISION

	$Revision: 1.4 $

=cut

1;
