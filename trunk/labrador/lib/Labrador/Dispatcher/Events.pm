package Labrador::Dispatcher::Events;
use lib '../..';
use Labrador::Dispatcher::Commands;
use strict;
use vars qw($VERSION);
use Tie::RefHash;
$VERSION = sprintf("%d.%02d", q$Revision: 1.9 $ =~ /(\d+)\.(\d+)/);

=head1 NAME

Labrador::Dispatcher::Events

=head1 SYNOPSIS

	use Labrador::Dispatcher::Events;

=head1 DESCRIPTION

Called by Connections.pm on occurrence of certain events occurring. Instantiates the 
Command handler, which callsback to register it's interest in different commands.
The Event handler can then dispatch Commands invocations straight to the correct
method in the Command handler. Cute, eh?

=head1 METHODS

=over 4

=item new($config)

Instantitate the object. Pass a reference to a Labrador::Common::Config instance to the
constructor.

=cut

sub new
{
	my ($package, $config, @args) = @_;	
	my $self = bless {config => $config}, $package;
	$self->init();	
	return $self;
}

=item init()

Initialises the object to it's initial state, including instantiating a Command handler.
Called by the constructor.

=cut

sub init
{
	my $self = shift;
	$self->{'clientnames'} = {};
	$self->{'clientprivs'} = {};
    tie %{$self->{'clientnames'}}, 'Tie::RefHash';
	tie %{$self->{'clientprivs'}}, 'Tie::RefHash';

    $self->{'eventmap'} = {};
    $self->{'eventpriv'} = {};
    $self->{'commands'} = new Labrador::Dispatcher::Commands($self->{config});
    $self->{'commands'}->init($self);

	$self->{'command_invocations'} = 0;
}

=item register_command($verb, $command_code, [$privileged])

Called by the Commands handler (Commands.pm) to register it's
interest in $verb, with a code reference to call when the command
is invoked by a client. The privilege level of this command is
optionally included (default 1). Current privilege states are 
TODO

=cut

sub register_command
{
	my ($self, $verb, $command_code, $privilege) = @_;
	$privilege = 1 unless defined $privilege;
	$self->{'eventmap'}->{$verb} = $command_code;
	$self->{'eventpriv'}->{$verb} = $privilege;
	return 1;	
}
=item client_authorise($client, $clientname, $privs)

=cut

sub client_authorise
{
	my ($self, $client, $clientname, $privs) = @_;
	$self->{'clientnames'}->{$client} = $clientname;
	$self->{'clientprivs'}->{$client} = $privs || 1;
	return 1;	
}

=item client_disconnect($client)

=cut

sub client_disconnect
{
	my ($self, $client) = @_;
	Labrador::Dispatcher::Connections::disconnect_client($client);
}

=back

=head1 EVENTS

Events are an interface supplied to the networking code, that allows
it to inform the rest of the dispatcher of network level events occurring.
These include verb command requests, connections and disconnections.

=over 4

=item event_command

=cut

sub event_command
{
	my ($self, $invocation) = @_;
	my $return = '';
	
	my $verb = $invocation->{verb};
	if (exists $self->{'eventmap'}->{$verb} )
	{
		#command exists, now check privelages
		my $privilege = $self->{'clientprivs'}->{$invocation->{client}} ||0;
	
		#TODO check privilege checks work correctly	
		if (($privilege & $self->{'eventpriv'}->{$verb}) >= $self->{'eventpriv'}->{$verb})
		{
			#privs OK, now run the command
			
			#set the clientname, if known
			if ($privilege)
			{
				$invocation->{clientname} = $self->{clientnames}->{$invocation->{client}};
			}
			
			#TODO it's possible to do this in one line
			my $command = $self->{eventmap}->{$verb};
			$return = &$command($invocation);
			$self->{'command_invocations'}++;
		}		
		else
		{
			$return = "303 Permission Denied\n";
		}
	}
	else
	{
		$return = "301 Unimplemented\n";
	}

	return $return;
}

=item event_connect($client)

Called when a client connects.

=cut

sub event_connect
{
	#TODO not sure anything has to happen here
	my ($self, $client) = @_;

	#warn "Accepted connection from ".$client->peerhost()."\n";

}

=item event_disconnect($client)

This event occurs if a client is unexpectedly disconnected - 
in normal situations, the dispatcher should disconnect the
client, not vice-versa.

=cut

sub event_disconnect
{
	my ($self, $client) = @_;
	warn "Accepted disconnection from ".$client->peerhost()."\n";	
}

=item event_idle

Event occurs if Connections.pm is bored, has nothing to do and wishes
to give up some processing time to us. 

B<NB: The quicker this method returns, the quicker Connections.pm
can get back to processing client requests. Don't be too long in here!>

=cut

sub event_idle
{
	my $self = shift;
}

=back

=head1 CVS

	$Revision: 1.9 $

=cut

1;
