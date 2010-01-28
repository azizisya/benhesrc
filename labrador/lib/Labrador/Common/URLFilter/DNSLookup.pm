#!/usr/bin/perl -w

package Labrador::Common::URLFilter::DNSLookup;

#inherit from Labrador::Common::URLFilter
use base qw(Labrador::Common::URLFilter);
use URI;
use strict;

=head1 NAME

Labrador::Common::URLFilter::DNSLookup

=head1 DESCRIPTION

Forks a separate process and passes URLs across a pipe for the other
process to resolve the DNS for. This should be used for large crawls
where resolving DNS maybe a bottleneck.

=head1 EXAMPLE

	URLFilter DNSLookup

=cut


#TODO IS THIS definently asynchronous?

sub filter
{
	my ($self, $uri, $url) = @_;

	#pass the hostname to the resolver process 	
	my $resolver = $self->{resolver};
	
	my $hostname = $uri->host();

	#TODO don't do if we've seen $hostname before

	print $resolver "$hostname\n";	

	return 1;
}

sub init
{
	my $self = shift;
	my ($resolver, $pid);
	do {
		$pid = open($resolver, "|-");
		unless (defined $pid){
			warn "Couldn't fork a DNS resolver process: $!";
			#TODO could implement a retry counter check
		}
	} until defined $pid;

	
	if ($pid)
	{#parent
		$self->{resolver} = $resolver;
	}
	else
	{#child
		_resolve();
		exit;
	}

}

sub _resolve
{
	while(<>)
	{
		chomp $_;	
		#resolve the request, and throw away the result
		#(all we're looking for is the DNS server to cache it)
		my @results = gethostbyname $_;
	}
}

=head1 REVISION

	$Revision: 1.10 $

=cut

1;

