#!/usr/bin/perl -w

use strict;
use lib '../lib';
use Labrador::Crawler::Dispatcher_Client;
use Getopt::Long;

my $SAVEEACH =0; my $ONCE = 0;

GetOptions('timedstats|t' => \$SAVEEACH, 'once|o' => \$ONCE);

if ($SAVEEACH)
{
	mkdir "../data/stats/" unless -e "../data/stats/";
}

my $client = new Labrador::Crawler::Dispatcher_Client($ARGV[0]||'localhost', 2680);
$client->connect;


while (1)
{
	my $time = time;
	open(FILEO, ">../data/stats.scoreboard");
	if($SAVEEACH)
	{
		open(FILEO2, ">../data/stats/$time.scoreboard") || die "Couldn't write to scoreboard folder : $!";
	}
	print "\e[2J";
	my %stats = $client->MONITOR();
	foreach my $key (grep{$_ !~ /^urls_(finished|sec).*/} sort keys %stats)
	{
		my $value = $stats{$key};
		print "$key: $value\n";
		print FILEO "$key: $value\n";
		print FILEO2 "$key: $value\n" if $SAVEEACH;
	}
	close FILEO; close FILEO2 if $SAVEEACH;
	last if $ONCE;
	sleep 5;
}

exit 0;
