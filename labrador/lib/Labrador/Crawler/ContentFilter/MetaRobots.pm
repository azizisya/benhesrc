package Labrador::Crawler::ContentFilter::MetaRobots;

use base qw(Labrador::Crawler::ContentFilter);
use strict;

=head1 NAME

Labrador::Crawler::ContentFilter::MetaRobots

=head1 DESCRIPTION

This content filter responsible for filtering out documents that state in
their HTML that they do not wish to be indexed, or their links followed.

For more information on this standard, which uses META tags embeeded within
HTML, see http://www.robotstxt.org/wc/faq.html#noindex and 
http://www.w3.org/pub/WWW/Search/9605-Indexing-Workshop/ReportOutcomes/Spidering.txt

B<NB:> You should not turn the ContentFilter out unless you know exactly what
you're doing. Obeying META tags is as important as obeying /robots.txt files.

=head1 METHODS

=over 4

=item filter($document, $privs)

Disabled the C<follow> and C<index> privileges if the HTML META tags specify
they should be.

=cut

sub filter
{
	my ($self, $document, $privs) = @_;
	return unless $document->content_type =~ /text\/html|application\/xhtml\+xml/i;
	
	my $meta_robots = _getmeta_robots($document->contents);

	#disabled, to prevent use of HTML::HeadParser	
	#my $meta_robots = $document->response->header('x-meta-robots') || '';

	$privs->{'follow'} = 0 if $meta_robots =~ /nofollow|none/i;
	$privs->{'index'} = 0 if $meta_robots =~ /noindex|none/i;
}

sub _getmeta_robots
{
	my $ref_data = shift;
	$$ref_data =~ 
			/\<meta\s+name\s*=\s*["']robot(?:s?)["']\s*content\s*=\s*['"]([^'"]*)['"].*\>/im;
    return $1||'';

}

=back

=head1 REVISION

	$Revision: 1.3 $

=cut

1;
