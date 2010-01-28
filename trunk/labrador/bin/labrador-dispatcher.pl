#!/usr/bin/perl -w
use strict;
use lib '../lib';
use Labrador::Dispatcher::Connections;
use Labrador::Dispatcher::Events;
use Labrador::Common::Config;
use Labrador::Dispatcher::Processing;


#give us a sensible name in ps(1) display, but don't confure
#the perl debugger
$0 = 'labrador-dispatcher' unless exists $ENV{PERLDB_PIDS};
$ENV{DISPATCHER}=1;


#load the configuration file
my $filename = '../data/etc/labrador.cfg';


#TODO more advanced command line parsing *could* take place here
if (scalar @ARGV)
{
	#take the command line stated things in preference to the default
	$filename = $ARGV[0] if -e $ARGV[0];
}

#load the configuration from config file
my $config = new Labrador::Common::Config(filename => $filename);
$config->set_default_scalar("Base", "../");

#instantiate the event handler
my $event_handler = new Labrador::Dispatcher::Events($config);

#and all the URL handler stuff
Labrador::Dispatcher::Processing::init($config);

#TODO should default be stored some other way
Labrador::Dispatcher::Connections::run($config->get_scalar('DispatcherPort', 1) || 2680, $event_handler);


__END__

=head1 NAME

	labrador-dispatcher.pl
	
=head1 DESCRIPTION

This script is responsible for starting the dispatcher for the labrador
web crawler, which allocates work to separate sub crawlers.

Underlying technology is OO Perl, based on DBM files, plain text queues,
and non-blocking sockets network layer.

=head1 TODO

TODO list for this file:

=over 4

=item More advanced command line parsing?

=item Signal handling?

=item Self daemonizing?

=item chdir confirmation

=cut
