package Labrador::Common::Partition;

use strict;

=head1 NAME

Labrador::Common::Partition

=head1 DESCRIPTION

Base class for all partitioning schemes. URLs are only in a crawler's partition
if the URLs of the same hash value have been assigned to it.

=head1 METHODS

=over 4

=item new($data)

Constructor. Automagically calls init().

=cut

sub new
{
	my ($class, $data, @args) = @_;
	die "Labrador::Common::Partition is an abstract class: ".caller()."\n"
		if $class eq 'Labrador::Common::Partition';
	my $self = bless {data=> $data}, $class;
	
	$self->init();
	return $self;
}

=item init()

Loads data structures.

=cut

sub init
{
	my $self = shift;
	$self->{data}->register_variable('SeenHost', 'HASH', 0);
	$self->{hashes} = $self->{data}->obtain_variable('SeenHost');
	
	$self->{data}->register_variable('PartitionSizes', 'HASH', 0);
	$self->{partitionsizes} = $self->{data}->obtain_variable('PartitionSizes');
}

=item new_urls($partition, @urls)

Registers the hash values of the given URLs to the provided
partition.

=cut

sub new_urls
{
	my ($self, $partition, @urls) = @_;
	
	foreach my $url (@urls)
	{
		my $hash = $self->hash($url);
		$self->_assign($hash, $partition) if  #add to seen host hash
		#TODO do we need this line? it just keeps partitionsizes more realistic
			! $self->in_partition($partition, $url);
	}
}
	
=item in_partition($partition, $url)

Returns true if $url is in the partition owned by partition named $partition.

=cut

sub in_partition
{
	my ($self, $partition, $url) = @_;
	return ($self->{hashes}->{$self->hash($url)}||'') eq $partition;
}


=item asssigned($url)

Returns true if the hash of $url has been assigned to a partition

=cut

sub assigned
{
	my ($self, $url) = @_;
	return $self->{hashes}->{$self->hash($url)} || 0;
}

=item delete_partition($partition)

This is expensive, TRUST ME

=cut

sub delete_partition
{
	my ($self, $partition) = @_;
	my $size = $self->{partitionsizes}->{$partition} || 0;
	delete $self->{partitionsizes}->{$partition};
	return unless $size;
	
	my $count = 0;
	foreach my $value (keys %{$self->{hashes}})
	{
		if ($self->{hashes}->{$value} eq $partition)
		{
			delete $self->{hashes}->{$value};
			return $size if ++$count == $size;
		}
	}
	return $size;
}

=item hash($url)

Returns the value to be hashed on.

=cut

sub hash;

=item size($partition)

Returns the number of hash values assigned to the partition named $partition.
Returns 0 if the $partition is unknown.

=cut

sub size
{
	return $_[0]->{partitionsizes}->{$_[1]} || 0;
}

=item sizes()

Returns a hash of partition sizes.

=cut

sub sizes
{
	return %{$_[0]->{partitionsizes}};
}

=back

=cut

sub _assign
{
	my ($self, $hash, $partition) = @_;
	$self->{hashes}->{$hash} = $partition;
	$self->{partitionsizes}->{$partition}||=0;
	$self->{partitionsizes}->{$partition}++;
}


=head1 REVISION

	$Revision: 1.5 $

=cut

1;



