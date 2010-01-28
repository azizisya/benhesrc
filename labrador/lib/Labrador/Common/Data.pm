package Labrador::Common::Data;

use strict;

#TODO we should be able to just load an import these
#as required
use Tie::File;
use GDBM_File;
use Sys::Hostname;

=head1 NAME

Labrador::Common::Data

=head1 SYPNOPSIS

	use Labrador::Common::Data;
	my $data = new Labrador::Common::Data($config);
	$data->register_variable("AnArray", 'ARRAY', 1);
	my $array_ref = $data->obtain_variable("AnArray");

=head1 DESCRIPTION

This module is a generic global data store, supporting persistence.

NB: This module's API should be treated as unstable, as I intend to
redesign it internally, so that separate subclasses can be created.

=head1 METHODS

=over 4

=item new($config)

Constructs a new data object. Only parameter is an instance of the the config
object,

=cut

sub new
{
	my ($package, $config) = @_;
		
	my $details = {	variables=>{},  ties=>{}, types=>{},
					persistence=>{HASH=>'GDBM_File', ARRAY=>'Tie::File' },
					partials=>[],
					base => $config->get_scalar('Base'),
					default_persistence => 
						($config->get_scalar('PersistenceLevel', 1)||1),
					checkpoint_time =>
						($config->get_scalar('PersistenceCheckpointEvery', 1)||1200),
					fileprefix => (hostname()||'localhost').$$
		};

	my $self = bless $details, $package;
	
	#TODO defaults for these!	
	foreach my $line (@{$config->get_array('DataPersistence')})
	{
		$self->{persistence}->{uc $line->[0]} = $line->[1];
		_load_module($line->[1]);
	}
	
	Labrador::Dispatcher::Connections::idle_alarm(
		$self->{checkpoint_time}
		,'PartialPersistenceCheckPointing',
		sub {$self->checkpoint;}
	) if $ENV{DISPATCHER};

	return $self;	
}

=item register_array($name, $persistent)

Register an array for use

=cut

sub register_array
{
	my ($self, $name, $persistent) = @_;
	return $self->register_variable($name, 'ARRAY', $persistent);
}

=item obtain_variable

=cut

sub obtain_variable
{
	my ($self, $name) = @_;
	return $self->{variables}->{$name};
}

=item register_variable($name, $type, $persistent)

Registers a variable for use. $type is one of 'ARRAY', 'HASH', 'SCALAR'.
$persistent marks whether the contents of the variable should be stored
on disk.

=cut

sub register_variable
{
	my ($self, $name, $type, $persistent) = @_;
	return 1 if exists ($self->{variables}->{$name});
	my $return; my $variable;
	$persistent ||=0;
	

	if ($type eq 'ARRAY')
	{
		$return = 1;
		$variable = [];
	}
	elsif ($type eq 'HASH')
	{
		$return = 1;
		$variable = {};
	}
	elsif ($type eq 'SCALAR')
	{
		my $var;
		$return = 1;
		$variable = \$var;
	}
	
	if ($persistent and $self->{default_persistence} > $persistent)
	{
		$persistent = $self->{default_persistence};
	}

	$self->{variables}->{$name} = $variable;
	$self->{types}->{$name} = $type;
	if ($persistent == 1)
	{
		#persistent structure - tie to disk
		$self->_tie($name, $type, $variable);
	}
	elsif ($persistent ==2)
	{
		#ooh, partially persistent
		push @{$self->{partials}}, $name;
		$self->init_variable($name, $type);
	}
	
	return 1;
}

sub init_variable
{
	my ($self, $name) = @_;
	my $type = $self->{types}->{$name};

	my $tied = $self->_tie_partial($name, $type);

	if ($type eq 'ARRAY')
	{
		@{$self->{variables}->{$name}} = @{$tied};
	}
	elsif ($type eq 'HASH')
	{
		%{$self->{variables}->{$name}} = %{$tied};
	}
	elsif ($type eq 'SCALAR')
	{
		#TODO implement tied scalars
	}
	$self->_untie_partial($name);
}

=item checkpoint

For all partially persistent data structures, updates the 
copy of the data structure on disk.

=cut

sub checkpoint
{
	my $self = shift;
	return unless scalar @{$self->{partials}};
	warn "Started checkpointing at ".(scalar localtime)."\n";
	foreach my $name (@{$self->{partials}})
	{
		#warn "Checkpointing $name\n";
		my $type = $self->{types}->{$name};
		
		my $tied = $self->_tie_partial($name, $type);		
		
		if ($type eq 'ARRAY')
		{
			@{$tied} = @{$self->{variables}->{$name}};
		}
		elsif ($type eq 'HASH')
		{
			%{$tied} = %{$self->{variables}->{$name}};
		}
		elsif ($type eq 'SCALAR')
		{
			#TODO implement tied scalars
		}
		$self->_untie_partial($name);
	}
	warn "Finished Checkpointing at ".(scalar localtime)."\n";
}

=item shutdown

Unties any persistent variables, so this object can be safely destroyed.
Automatically calls checkpoint first.

=cut

sub shutdown
{
	my $self = shift;
	$self->checkpoint;
		
	foreach my $name (keys %{$self->{ties}})
	{
		$self->_untie($name);
	}
}

=back

=head1 PRIVATE METHODS

Should not be called directly

=over 4

=item _tie_partial($name, $type)

Tie a partial persistent variable.

=cut

sub _tie_partial
{
	my ($self, $name, $type) = @_;
	my $ref = $self->_tie("persis_$name", $type);
	$self->{variables}->{"persis_$name"} = $ref;
	
}

=item _untie_partial($name)

Unties a partially persistent variable.

=cut

sub _untie_partial
{
	my ($self, $name) = @_;
	return $self->_untie("persis_$name");
}

=item _untie($name)

Unties a fully partitially persistent variable

=cut

sub _untie
{
	my ($self, $name) = @_;
	my $type = $self->{ties}->{$name};
	untie %{$self->{variables}->{$name}} if ($type eq 'HASH');
	untie @{$self->{variables}->{$name}} if ($type eq 'ARRAY');
	#TODO implement tied scalars	
	#untie ${$self->{variables}->{$name}} if ($type eq 'SCALAR');
	delete $self->{variables}->{$name};
}

=item _tie($name, $type, $ref)

Ties a fully persistent variable

=cut

sub _tie
{
	my ($self, $name, $type, $ref) = @_;
	if ($type eq 'ARRAY')
	{
		$ref = [] unless ref $ref;
		tie(@{$ref}, $self->{persistence}->{$type}, 
			$self->{base}."data/".$self->{fileprefix}."$name.array")
			or die "Problem tieing $name to ".$self->{base}.
				"data/$name.array".":$!\n";
	}
	elsif ($type eq 'HASH')
	{
		$ref = {} unless ref $ref;
		tie %{$ref}, $self->{persistence}->{$type}, 
			$self->{base}."data/".$self->{fileprefix}."$name.gdbm", &GDBM_WRCREAT, 0640;
	}
	elsif ($type eq 'SCALAR')
	{
		my $var;
		$ref = \$var unless ref $ref;
		die "No support for tied scalars yet! ($name)";
	}
	#push @{$self->{ties}->{$type}}, $ref;
	$self->{ties}->{$name} = $type;
	return $ref;
}

=item _load_module($name)

Load the module called $name.

=cut

sub _load_module {
    eval "require $_[0]";
    die $@ if $@;
    #$_[0]->import(@_[1 .. $#_]);
}


=head1 REVISION

$Revision: 1.11 $

=cut

1;
