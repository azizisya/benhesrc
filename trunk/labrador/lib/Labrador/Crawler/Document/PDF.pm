package Labrador::Crawler::Document::PDF;

use base qw(Labrador::Crawler::Document);
use constant CONVERTOR => '/usr/bin/pdftotext';
use File::Temp qw(tmpnam);
use constant TIMEOUT => 120;
use strict;

=head1 NAME

Labrador::Crawler:Document::PDF

=head1 DESCRIPTION

Implements a PDF (Adobe Acrobat) converting document handler.
Relies on /usr/bin/pdftotext to convert the document.

It may be preferable to use http://pdftohtml.sourceforge.net/ as
a convertor as it inserts stylisation, which would allow an indexer
to weight terms differently according to how bold they were etc.

Timeout after 120 seconds

=head1 METHODS

=over 4

=item init

=cut

sub init
{
	my $self = shift;
	return 0 unless $self->SUPER::init(@_);
	
	#convert the content
	
	#TODO some error checking
	#TODO move into data(), but cache result	
	
	#get a temporary file, and write to it
	my $tmpfile = tmpnam();
	open (TMPFILE, ">$tmpfile");
	print TMPFILE ${$self->{'data'}};
	close TMPFILE;
	
	{
		local $SIG{ALRM} = sub { die "timeout" };
		eval {
			alarm(TIMEOUT);

			#pipe to convertor
			open (FHCONVERTOR, '-|', CONVERTOR." $tmpfile -");
			my $data = join "\n", <FHCONVERTOR>;
			$self->{'data'} = \$data;
			close FHCONVERTOR;
			#modify the content type, and save the old one as X-Original-Content-Type
			$self->header('X-Original-Content-Type', $self->content_type);
			$self->header('Content-Type', 'text/plain');
			alarm(0);
		};
		if (($? >> 8) != 0)
		{
			warn "$0 Failed to convert PDF - exist code for pdftotext was ".($?>>0)."\n";
		}
		elsif ($@ eq 'timeout')
		{
			warn "$0 Failed to convert PDF - timeout\n";
		}
		elsif ($@ and $@ ne 'timeout')
		{
			warn "$0 Unknown error converting PDF (ie perl exception, not timeout) : $@\n";
		}
		alarm(0);
	}
	
	#remove temporary file
	unlink $tmpfile;

	#modify the content type, and save the old one as X-Original-Content-Type
	$self->header('X-Original-Content-Type', $self->content_type);
	$self->header('Content-Type', 'text/plain');

	return 1;
}

=back 

=head1 REVISION

	$Revision: 1.4 $

=cut

1;

