#!/usr/bin/perl -w

use strict;
use lib '../lib';

use Getopt::Long;
use Labrador::Common::Config;
use Labrador::Crawler::Dispatcher_Client;
use Labrador::Crawler::CrawlerHandler;

use Errno qw(EAGAIN);

#Ignore the death of our children
$SIG{CHLD} = 'IGNORE';

my $SINGLE =0;

GetOptions('single|s' => \$SINGLE);

#give us a sensible name in ps(1) display, but don't confuse
#the perl debugger for restarting
$0 = 'labrador-crawler (master)' unless $^D;

@ARGV = ('localhost', 2680) unless scalar @ARGV;

#warn "instantiating client\n";
my $client = new Labrador::Crawler::Dispatcher_Client(@ARGV);
my $work = 0; my $attempts =10;

$client->connect;
my @config_text = $client->CONF();
while ($attempts and ! $work)
{
	$work = $client->WORK;
	sleep 1;
	$attempts--;
}
$client->disconnect;
my %pids;

if ($work)
{
	my $config = new Labrador::Common::Config(text=>\@config_text);

	if ($SINGLE)
	{
		crawler($config, $ARGV[0],$ARGV[1]);
		undef $config;
		exit;
	}
	
	print "Forking $work subcrawlers\n";
	for (my $i=0;$i<$work;$i++)
	{
		my $pid = fork_process("labrador-subcrawler $i", 
			sub {crawler($config, $ARGV[0],$ARGV[1])});
		$pids{$pid} = 1;
		sleep 1;
	}

	my $pid = 0;
	while ($pid > -1 and scalar keys %pids)
	{
		$pid = wait();
		delete $pids{$pid};
	}
}
else
{
	print "No work!";
}

print "$0 - exiting".time()."\n";
exit;

sub crawler
{
	my @args = @_;
	my $manager = new Labrador::Crawler::CrawlerHandler(@args);
	$manager->run();
	$manager->shutdown();
	undef $manager;
}

sub fork_process
{	
	my $id = shift;
	my $run = shift;
	my @args = @_;
	
	FORK:{
		if (my $pid = fork)
		{
			#parent process here
			return $pid;
		}
		elsif (defined $pid)
		{
			#child process
			$0 = $id;
			print "$0 commencing\n";
			&$run(@args);
			exit 0;
		}
		elsif ($! == EAGAIN)
		{
			#weird fork error
			sleep 5; redo FORK;
		}
		else
		{
			die "Couldn't fork: $!\n";	
		}
	}
}

__END__

=head1 NAME

labrador-dispatcher.pl

=cut


