#!/usr/bin/perl -w

use strict;
use lib '../lib';
use Labrador::Crawler::Dispatcher_Client;

my $client = new Labrador::Crawler::Dispatcher_Client(
	$ARGV[0]||'localhost', $ARGV[1]||2680);
$client->connect('admin', 1, 'password', 'cookie');
print join "\n", $client->_command('SHUTDOWN');
undef $client;
exit(0);

