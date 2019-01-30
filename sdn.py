#!/usr/bin/python

from mininet.topo import Topo
from mininet.node import Host
from mininet.net import Mininet
from mininet.util import dumpNodeConnections
from mininet.log import setLogLevel
from mininet.node import RemoteController
from mininet.cli import CLI
from random import randint
import sys
import subprocess
import os.path
import signal

class TopoSDN(Topo):
    def build(self):
	hosts = [ self.addHost( h ) for h in 'h1', 'h2', 'h3' ]
	servers = [ self.addHost ( sv ) for sv in 'sv1', 'sv2', 'sv3' ]
	switches = [ self.addSwitch( s ) for s in 's1', 's2', 's3', 's4', 's5', 's6', 's7' ]
        
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
	self.addLink(switches[0], switches[6])
	self.addLink(switches[1], switches[6])
	self.addLink(switches[2], switches[6])
	self.addLink(switches[3], switches[6])
	self.addLink(switches[4], switches[6])
	self.addLink(switches[5], switches[6])


def gentraffic(self, line):
    "generate random http traffic\nUsage: gentraffic [number_of_flows]"
    net = self.mn
    for i in range(int(line)):
        hips = [ h for h in net.get('h1', 'h2', 'h3') ]
        src = randint(0,len(hips)-1)
        timeout = randint(1,60)
        bandwidth = randint(100, 50000)
        hips[src].cmd('timeout -s KILL {} wget --limit-rate={}K 10.0.0.4/file -O - > /dev/null &'.format(timeout, bandwidth) )
        print "creating flow from {} to 10.0.0.4 - bandwidth {}, timeout {}".format(hips[src].IP(), bandwidth,timeout)



def simpleTest(controllerip):
    if not os.path.isfile("./file"):
        subprocess.call(["dd", "if=/dev/zero", "of=./file", "bs=1M", "count=500"])
    topo = TopoSDN()
    net = Mininet(topo, controller=RemoteController( 'c0', ip=controllerip, port=6653 ))
    net.start()
    sv1, sv2, sv3 = net.get( 'sv1', 'sv2', 'sv3' )

    sv1.setIP('10.0.0.4')                                           
    sv1.setMAC('00:00:00:00:00:04')
    
    sv2.setIP('10.0.0.5')
    sv2.setMAC('00:00:00:00:00:05')
    
    sv3.setIP('10.0.0.6')
    sv3.setMAC('00:00:00:00:00:06')

    sv1.cmd('arp -s 10.0.0.1 00:00:00:00:00:01;arp -s 10.0.0.2 00:00:00:00:00:02;arp -s 10.0.0.3 00:00:00:00:00:03;arp -s 10.0.0.4 00:00:00:00:00:04;arp -s 10.0.0.5 00:00:00:00:00:05;arp -s 10.0.0.6 00:00:00:00:00:06')
    sv2.cmd('arp -s 10.0.0.1 00:00:00:00:00:01;arp -s 10.0.0.2 00:00:00:00:00:02;arp -s 10.0.0.3 00:00:00:00:00:03;arp -s 10.0.0.4 00:00:00:00:00:04;arp -s 10.0.0.5 00:00:00:00:00:05;arp -s 10.0.0.6 00:00:00:00:00:06')
    sv3.cmd('arp -s 10.0.0.1 00:00:00:00:00:01;arp -s 10.0.0.2 00:00:00:00:00:02;arp -s 10.0.0.3 00:00:00:00:00:03;arp -s 10.0.0.4 00:00:00:00:00:04;arp -s 10.0.0.5 00:00:00:00:00:05;arp -s 10.0.0.6 00:00:00:00:00:06')


    sv1.cmd('python -m SimpleHTTPServer 80 &')
    sv2.cmd('python -m SimpleHTTPServer 80 &')
    sv3.cmd('python -m SimpleHTTPServer 80 &')

    h1, h2, h3 = net.get( 'h1', 'h2', 'h3' )

    h1.cmd('arp -s 10.0.0.1 00:00:00:00:00:01;arp -s 10.0.0.2 00:00:00:00:00:02;arp -s 10.0.0.3 00:00:00:00:00:03;arp -s 10.0.0.4 00:00:00:00:00:04;arp -s 10.0.0.5 00:00:00:00:00:05;arp -s 10.0.0.6 00:00:00:00:00:06')
    h2.cmd('arp -s 10.0.0.1 00:00:00:00:00:01;arp -s 10.0.0.2 00:00:00:00:00:02;arp -s 10.0.0.3 00:00:00:00:00:03;arp -s 10.0.0.4 00:00:00:00:00:04;arp -s 10.0.0.5 00:00:00:00:00:05;arp -s 10.0.0.6 00:00:00:00:00:06')
    h3.cmd('arp -s 10.0.0.1 00:00:00:00:00:01;arp -s 10.0.0.2 00:00:00:00:00:02;arp -s 10.0.0.3 00:00:00:00:00:03;arp -s 10.0.0.4 00:00:00:00:00:04;arp -s 10.0.0.5 00:00:00:00:00:05;arp -s 10.0.0.6 00:00:00:00:00:06')

    h1.setIP('10.0.0.1')
    h1.setMAC('00:00:00:00:00:01')
    h2.setIP('10.0.0.2')
    h2.setMAC('00:00:00:00:00:02')

    h3.setIP('10.0.0.3')
    h3.setMAC('00:00:00:00:00:03')

#    flows(net)
    CLI.do_gentraffic = gentraffic
    CLI(net)
    net.stop()

if __name__ == '__main__':
    # Tell mininet to print useful information
    if len(sys.argv) == 1:
        print "How to run ./sdn.py ControllerIP"
        sys.exit(1)

    setLogLevel('info')
    simpleTest(sys.argv[1])
