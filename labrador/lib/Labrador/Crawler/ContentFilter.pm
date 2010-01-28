package Labrador::Crawler::ContentFilter;

use strict;

=head1 NAME

Labrador::Crawler::ContentFilter

=head1 SYNOPSIS

	use Labrador::Crawler::ContentFilter;
	my $filter = new Labrador::Crawler::ContentFilter(
		'Binary', $config, $dispatcher_client);
	my $privs = {'index' => 0, 'follow' => 0};
	$filter->filter($document, $privs);
	print "May not index\n" unless $privs->{'index'};
	print "May not follow\n" unless $privs->{'follow'};

=head1 DESCRIPTION

Abstract class. Must be implemented. 

Content filters are responsible for looking at content and determining
two things: a) if the content should not be indexed and b) if the links
in the content should not be followed.

=head1 KNOWN CHILDREN

=over 2

=item Binary

Detects binary content by looking for the null character (\0) in the document.

=item ContentTypes

Only index or follow content types allowed in the configuration file

=item MetaRobots

Examines any Meta robots tag in HTML documents

=item Fingerprint

Takes a fingerprint of the document and asks the dispatcher if its seen that finger
print before.

=item WhitelistLanguages

Only index documents that contain stopwords of our desired languages.

=back

=cut

=head1 METHODS

=over 4

=item new($name, $config, $dispatcher_client)

Constructs a new Content Filter object

=cut

sub new
{
	my ($me, $name, $config, $disp_client) = splice @_, 0, 4;
	my $class = $me.'::'.$name;
	_load_module($class);	
	my $self = bless{config => $config, client => $disp_client, name => $name}, $class;
	$self->init(@_);
	return $self;
}

=item name()

Returns the name of this filter - useful for debugging warnings.

=cut

sub name
{
	return $_[0]->{name};
}

=item filter($document, $privs)

Abstract - each child class must provide this method, which alters the
filter settings of $privs ('follow', 'index') according to some heuristic
on the content.

=cut

sub filter;

=item init()

An optional method that is called when the class is started, so that any child
module can be initialiased

=cut


sub init
{

}


=item _load_module($name)

Load the module named $name.

=cut

sub _load_module {
    eval "require $_[0]";
    die $@ if $@;
    #$_[0]->import(@_[1 .. $#_]);
}

=head1 REVISION

	$Revision: 1.4 $

=cut

1;

