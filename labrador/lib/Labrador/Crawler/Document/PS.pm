package Labrador::Crawler::Document::PS;

use constant CONVERTOR => '/usr/bin/ps2ascii';
use base qw(Labrador::Crawler::Document);
use File::Temp qw(tmpnam);
use constant TIMEOUT => 120;
use strict;

=head1 NAME

Labrador::Crawler:Document::PS

=head1 SYNOPSIS

=head1 DESCRIPTION

Document handler for Postscript documents
Relies on /usr/bin/ps2ascii being available. 

It may however be preferable to convert to PDF and then use
http://pdftohtml.sourceforge.net/ which would produce HTML.
The reasons for this are explained in PDF.pm

Timeout after 120 seconds.

=head1 METHODS

=over 4

=item init

Converts the file to text

=cut

sub init
{
	my $self = shift;
	return 0 unless $self->SUPER::init(@_);

	#modify the content type, and save the old one as X-Original-Content-Type
	$self->header('X-Original-Content-Type', $self->content_type);
	$self->header('Content-Type', 'text/plain');
	
	#convert the content
	#TODO some error checking
	#TODO move into data(), but only make it happen once	

	#get a temporary file, and write to it
	my $tmpfile = tmpnam();
	open (TMPFILE, ">$tmpfile");
	print TMPFILE ${$self->{'data'}};
	close TMPFILE;
	
	{
		$SIG{ALRM} = sub { die "timeout" };
		eval {
			alarm(TIMEOUT);

			#pipe to convertor
			open (FHCONVERTOR, '-|', CONVERTOR." $tmpfile -");
			my $data = join "\n", <FHCONVERTOR>;
			$self->{'data'} = \$data;
			close FHCONVERTOR;
		};
		if ($@)
		{
			warn "Failed to convert PS - timeout\n";
		}
		elsif($@ and $@ ne 'timeout')
		{
			warn "Unknown error converting PS (ie not timeout) : $@\n";
		}
		alarm(0);
	}

	#TODO should we fail to save the document should it fail this
	#test
	warn "PS Exit code was $?\n" if $?;

	#remove temporary file
	unlink $tmpfile;
	return 1;
}



=back 

=head1 REVISION

	$Revision: 1.5 $

=cut

1;

