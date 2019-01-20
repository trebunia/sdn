#!/usr/bin/python

from mininet.topo import Topo
from mininet.net import Mininet
from mininet.util import dumpNodeConnections
from mininet.log import setLogLevel
from mininet.node import RemoteController
from mininet.cli import CLI
import sys
import subprocess
import os.path

ControllerIP='192.168.56.1'

class TopoSDN(Topo):
    def build(self):
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

def simpleTest(controllerip):
    if not os.path.isfile("./file"):
        subprocess.call(["dd", "if=/dev/zero", "of=./file", "bs=1M", "count=100"])
    topo = TopoSDN()
    net = Mininet(topo, controller=RemoteController( 'c0', ip=controllerip, port=6653 ))
    net.start()
    sv1, sv2, sv3 = net.get( 'sv1', 'sv2', 'sv3' )
    sv1.cmd('python -m SimpleHTTPServer 80 &')
    sv2.cmd('python -m SimpleHTTPServer 80 &')
    sv3.cmd('python -m SimpleHTTPServer 80 &')
    CLI(net)
    net.stop()

if __name__ == '__main__':
    # Tell mininet to print useful information
    if len(sys.argv) == 1:
        print "How to run ./sdn.py ControllerIP"
        sys.exit(1)

    setLogLevel('info')
    simpleTest(sys.argv[1])
