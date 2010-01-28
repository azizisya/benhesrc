#!/usr/bin/perl -w

use strict;
use Cwd qw(getcwd);
my $cwd = getcwd;

my $TEST = shift @ARGV;
$TEST ||= '-c';

foreach (<>)
{
	chomp; my $rtr;
	if ($TEST eq '-c'){
		$rtr = system("perl", "-mstrict", "-wc", "-I$cwd/lib", $_);
	}elsif($TEST eq '-p'){
		$rtr = system("podchecker", $_);
	}	
	exit $rtr if $rtr;
}

