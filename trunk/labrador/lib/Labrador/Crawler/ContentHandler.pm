package Labrador::Crawler::ContentHandler;


use strict;

=head1 NAME

Labrador::Crawler::ContentHandler

=head1 SYPNOPSIS

=head1 DESCRIPTION

=head1 CONFIGURATION

=head1 METHODS

=over 4

=item new

=cut

sub new
{
	my ($me, $handler_name, $handler_entry, $config, $client) = @_;
	#my ($me, $handler_name) = splice @_, 0, 2;
	my $class = $me.'::'.$handler_name;
	_load_module($class);
	my $self = bless {config => $config, client => $client}, $class;
	$self->init(@{$handler_entry});	
	return $self;
}

sub init {}

=item process_success($url, $ref_outlinks_arr, $ref_followlinks_hash, $document)

Abstact - must be over-ridden. This method is called when a successful request is executed.

=cut

sub process_success;

=item process_redirect($old_url, $new_url, $HTTPResponse)

Abstract - can be over-ridden. This method is called when a redirect occurs.

=cut

sub process_redirect
{}

=item process_failure($url, $HTTPResponse)

Abstract - can be over-ridden. This method is called when a URL fails.

=cut

sub process_failure
{}

=item _load_module($name)

Load the module named $name.

=cut

sub _load_module {
    eval "require $_[0]";
    die $@ if $@;
    #$_[0]->import(@_[1 .. $#_]);
}


=back

=head1 REVISION

	$Revision: 1.6 $

=cut

1;
