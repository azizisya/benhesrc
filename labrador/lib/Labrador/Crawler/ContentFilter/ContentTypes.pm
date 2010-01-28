package Labrador::Crawler::ContentFilter::ContentTypes;

use base qw(Labrador::Crawler::ContentFilter);
use strict;

=head1 NAME

Labrador::Crawler::ContentFilter::ContentTypes

=head1 DESCRIPTION

This content filter responsible for filtering out documents that have a content
type that is not specified in the config file as one that should have it's links
followed, or indexed.

=head1 CONFIGURATION

This module loads the following 

=over 4

=item FollowContentTypeWhitelist

=item IndexContentTypeWhitelist

=back

NB: Note that if the content types you include contain regular expression meta charcters, then you should escape them. For example, escaping + (eg in application/rdf+xml) with a backslash.

=head1 METHODS

=over 4

=item init

=cut

sub init
{
	my $self = shift;
	$self->{follow_content_types} = 
		join '|', $self->{config}->get_scalar('FollowContentTypeWhitelist');
	
	$self->{index_content_types} = 
		join '|', $self->{config}->get_scalar('IndexContentTypeWhitelist');

}

=item filter($document, $privs)

=cut

sub filter
{
	my ($self, $document, $privs) = @_;
	my $content_type = $document->content_type;

	#this is a whitelisting module, so set 0 first, then reenable if allowed
	$privs->{'follow'} = 0; $privs->{'index'} = 0;
	$privs->{'follow'} = 1 if $content_type =~ /$self->{follow_content_types}/i;
	$privs->{'index'} = 1 if $content_type =~ /$self->{index_content_types}/i;

	warn "No index for ".$document->url()." because no match index on $content_type\n"
		if ! $privs->{'index'};
	warn "No follow for ".$document->url()." because no match follow on $content_type\n"
		if ! $privs->{'follow'};
}

=back

=head1 REVISION

	$Revision: 1.2 $

=cut

1;
