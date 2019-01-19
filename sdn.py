#!/usr/bin/python

from mininet.topo import Topo

class MyTopo( Topo ):
	"Simple topology example."
	def __init__( self ):
		"Create custom topo."
		# Initialize topology
		Topo.__init__( self )
		# Add hosts and switches
		hosts = [ self.addHost( h ) for h in 'h1', 'h2', 'h3' ]
                servers = [ self.addHost ( sv ) for sv in 'sv1', 'sv2', 'sv3' ]
		switches = [ self.addSwitch( s ) for s in 's1', 's2', 's3', 's4', 's5', 's6' ]
		# Add links
                # links hosts - switches
                self.addLink(hosts[0], switches[3])
                self.addLink(hosts[1], switches[4])
                self.addLink(hosts[2], switches[5])

                # links servers - switches
                self.addLink(servers[0], switches[0])
                self.addLink(servers[1], switches[1])
                self.addLink(servers[2], switches[2])

                # links between switches
                self.addLink(switches[0], switches[3])
                self.addLink(switches[0], switches[4])
                self.addLink(switches[0], switches[5])

                self.addLink(switches[1], switches[3])
                self.addLink(switches[1], switches[4])
                self.addLink(switches[1], switches[5])

                self.addLink(switches[2], switches[3])
                self.addLink(switches[2], switches[4])
                self.addLink(switches[2], switches[5])

topos = { 'mytopo': ( lambda: MyTopo() ) }

