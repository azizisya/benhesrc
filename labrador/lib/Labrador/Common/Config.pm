package Labrador::Common::Config;
use strict;

=head1 NAME

Labrador::Common::Config

=head1 SYNOPSIS

	use Labrador::Common::Config;
	my $config = new Labrador::Common::Config(filename => '../data/etc/labrador.cfg');
	my $port = $config->get_scalar('DispatcherPort');
	my $urlalloc = $config->get_scalar('URLAlloc', 1) || 'Delay';

=head1 DESCRIPTION

Loads config file from file or a string. The string options means that the configuration file can be transferred across the network from the dispatcher.

=head1 METHODS

=over 4

=item new(%options)

%options = filename =E<gt> $filename OR text =E<gt> \@text

=cut

sub new
{
	my $package = shift;
	my %options = @_;
	
	my $self = bless {}, $package;

	my @text;
	#use appropriate consucting form
	if (exists $options{filename})
	{
		@text = _getfile($options{filename});
	}
	elsif (exists $options{text})
	{
		@text = @{$options{text}};
	}
	else
	{
		#todo die or croak?
		warn "Incorrect usage: either specify filename or text";
	}
	$self->_parse(@text);
	

	return $self;
}

=item get_scalar($name, $optional)

Get the value of the directive named $name in the config file.
Return undef if $optional is set and the parameter isn't found.

=cut

sub get_scalar
{
	my ($self, $name, $optional) = @_;
	$name = lc $name;
	
	if (! exists $self->{directives}->{$name})
	{
		die "Directive $name not found in configuration file\n" unless $optional;
		return undef;
	}
	return wantarray
			?	@{ $self->{directives}->{$name}->[0] }
			:	$self->{directives}->{$name}->[0]->[0];
}

=item get_array($name, $optional)

Get all values of named $name in the config file. Return undef if $optional is set and 
and the parameter isn't found.

=cut

sub get_array
{
	my ($self, $name, $optional) = @_;
	$name = lc $name;
	if (! exists $self->{directives}->{$name})
    {
		die "Directive $name not found in configuration file\n" unless $optional;
		return undef;
	}

	return $self->{directives}->{$name};	
}

=item get_file_text()

Returns a reference to (or an array of all lines) of the text of the config file, with all
comments and blank lines omitted.

=cut

sub get_file_text
{
	my $self = shift;
	return wantarray ? @{$self->{text}} : $self->{text};
}

=item set_default

Set a default for a parameter if it isn't in the configuration file that was parsed.
Most cases should use the get_ directives with $optional set. This method is provided
for if a higher module wants to override a default.

=cut

sub set_default_scalar
{
	my ($self, $name, $value) = @_;
	$name = lc $name;
	unless (exists $self->{directives}->{$name})
	{
		$self->{directives}->{$name} = $value;
		push @{$self->{text}}, "$name $value";
	}
}

#private methods

sub _parse
{
	my ($self, @text) = @_;
	#remove comments
	s/#.*//g foreach @text;

	#remove empty lines
	@text = grep(length $_, @text);

	#save text, as we may need to give it out to a client later
	$self->{text} = \@text;
	
	#initialise the hash to save configuration file directives
	$self->{directives} = {};
	foreach my $line (@text)
	{
		#obtain the verb
		my ($directive, $params) = $line =~ m/^\s*(\w+)\s+(.*)$/;
		$directive = lc $directive; #case-insensitive
		#add on the parameters to the verb
		$self->{directives}->{$directive} ||= [];
		push @{$self->{directives}->{$directive}}, _parse_params($params);
	}
}

#NB:if you're not a regex master, don't even try to understand this.
#Loosly based on http://www.perlmonks.org/index.pl?node_id=5722
#returns a reference to an array. it's like split /\s+/ but
#instead ignores spaces that are included in "". " can be
#escaped by \
sub _parse_params
{
	my $params = shift;
	my @new = ();
	
	#remove any prefixed and trailing spaces	
	$params =~ s/^\s*//g;
	$params =~ s/\s*$//g;

	#$+ is the last grouping(brackets) matched
	push(@new, $+) while $params =~ m{
    # the first part groups the phrase inside the quotes
    "([^\"\\]*(?:\\.[^\"\\]*)*)"\s*
      | ([^\s]+)\s*
      | \s+
    }gx;	
    return \@new; ## list of values that were space-spearated
}

sub _getfile
{
	my $filename = shift;
	my @data;

	open(FILE_CONFIG_I, "<$filename") or die "Couldn't open configuration file $filename : $!\n";
	foreach (<FILE_CONFIG_I>)
	{
		chomp;
		push @data, $_;
	}
	close FILE_CONFIG_I;
	return @data;
}

=back

=head1 REVISION

	$Revision: 1.8 $

=cut

1;
