package Labrador::Common::FileWriter;

=head1 NAME

Labrador::Common::FileWriter

=head1 SYPNOPSIS

	use Labrador::Common::FileWriter;
	my $writer = new Labrador::Common::FileWriter('/tmp/testwrites%%.log', 5);
	for(my $i=0;$i<rand(10);$i++)
	{
    	for (my $j=0;$j<rand(10); $i++)
	    {
    	    my $fileh = $writer->write();
	        print $fileh "$i $j\n";
    	}
	}
	$writer->finish;

=head1 DESCRIPTION

Provides a filehandle that rolls over to the next file after a constant number of writes.

=head1 METHODS

=over 4

=item new($filename, $maxwrites, [$compress])

Constructor. $maxwrites is the limit to the number of times write() can be called before a new
filehandle will be opened. If $compress is set, then the file is compressed by calling gzip
when each file is closed.

=cut

sub new
{
	my ($class, $filename, $maxwrites, $compress) = @_;

	die "Filename does NOT have a placeholder for counter" 
		unless $filename =~ /\%\%/;

	my $self = bless {writemax => $maxwrites,
					  filenameformat => $filename,
					  fileopen => 0,
					  filenumber => -1,
					  writecount => 0,
					  compress => $compress || 0
					 }, $class;

	$self->init();
	return $self;
}



sub init
{

}

=item write()

Returns a filehandle of an open file, and incremenents writecounter.
If writecounter's maximum is exceeded, then the current file is closed
and a new file opened.

=cut

sub write
{
	my $self = shift;
	$self->{writecount}++;
	my $fileh;

	if ($self->{fileopen} and $self->{writecount} >= $self->{writemax})
	{
		$self->_close();
		$self->{writecount} = 0;
	}

	if ($self->{fileopen})
	{
		$fileh = $self->{fileh};
	}
	else
	{
		$fileh = $self->_open();
	}

	return $fileh;
}

=item finish

Close the filehandle.

=cut

sub finish
{
	$_[0]->_close;
}

=back

=head1 PRIVATE METHODS

As usual, these should be never manipulated by external objects directly. Documentation
is provided for completeness only.

=over 4

=item _filename

Returns the name of the file to be opened given the object's current state.

=cut

sub _filename()
{
	my $self = shift;
	my $filename = $self->{filenameformat};
	$filename =~ s/\%\%/$self->{filenumber}/g;
	return $filename;
}

=item _open()

Opens a new file.

=cut

sub _open
{
	my $self = shift;
	my $fileh; $self->{filenumber}++;
	open($fileh, '>'.$self->_filename()) or 
		die "couldn't open ".$self->_filename()." to write : $!\n";
	

	$self->{fileh} = $fileh;
	$self->{fileopen} = 1;
	return $fileh;
}

=item _close

Closes the currently open file.

=cut

sub _close
{
	my $self = shift;
	my $fileh = $self->{fileh};
	close $fileh;
	
	#TODO use Compress::Zlib if available
	my $filename = $self->_filename();
	if ($self->{compress} and -e $filename)
	{
		system('gzip', $filename);
	}
	$self->{fileopen} = 0;
}

=item DESTROY
	
Destructor - calls _close if the file is open.

=cut

sub DESTROY
{
	my $self = shift;
	$self->_close() if $self->{fileopen};
}

=back

=head1 REVISION

	$Revision: 1.6 $

=cut

1;

package main;

&FileWritertest if ((join ' ', @ARGV) =~ /--filewritertest/);

sub FileWritertest
{
	my $writer = new Labrador::Common::FileWriter('/tmp/testwrites%%.log', 5);
	my $end_i = 10*rand(1);
	for(my $i=0;$i<10;$i++)
	{
		my $end_j = 10*rand(1);
		for (my $j=$i;$j<10; $j++)
		{
			my $fileh = $writer->write();
			print $fileh "$i $j\n";		
		}
	}
	$writer->finish;
	undef $writer;
}

1;
