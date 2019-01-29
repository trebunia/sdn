package pl.edu.agh.kt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwSrc;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlSrc;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.VlanVid;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.packet.ICMP;

public class Flows {

	private static final Logger logger = LoggerFactory.getLogger(Flows.class);

	public static short FLOWMOD_DEFAULT_IDLE_TIMEOUT = 5; // in seconds
	public static short FLOWMOD_DEFAULT_HARD_TIMEOUT = 0; // infinite
	public static short FLOWMOD_DEFAULT_PRIORITY = 100;

	protected static boolean FLOWMOD_DEFAULT_MATCH_VLAN = true;
	protected static boolean FLOWMOD_DEFAULT_MATCH_MAC = true;
	protected static boolean FLOWMOD_DEFAULT_MATCH_IP_ADDR = true;
	protected static boolean FLOWMOD_DEFAULT_MATCH_TRANSPORT = true;

	protected static String MAIN_ADDR = "10.0.0.4"; //sv1 will be default

	public Flows() {
		logger.info("Flows() begin/end");
	}



	public static void sendPacketOut(IOFSwitch sw) {

	}

	public static void simpleAdd(IOFSwitch sw, OFPacketIn pin, FloodlightContext cntx, OFPort outPort) {
		// FlowModBuilder
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
		// match
		//		Match.Builder mb = sw.getOFFactory().buildMatch();
		//		mb.setExact(MatchField.IN_PORT, pin.getInPort());
		//		Match m = mb.build();
		Match m = createMatchFromPacket(sw, pin.getInPort(), cntx);

		// actions
		OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
		List<OFAction> actions = new ArrayList<OFAction>();
		aob.setPort(outPort);
		aob.setMaxLen(Integer.MAX_VALUE);
		actions.add(aob.build());
		fmb.setMatch(m).setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT).setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
		.setBufferId(pin.getBufferId()).setOutPort(outPort).setPriority(FLOWMOD_DEFAULT_PRIORITY);
		fmb.setActions(actions);
		// write flow to switch
		try {
			sw.write(fmb.build());
			logger.info("Flow from port {} forwarded to port {}; match: {}",
					new Object[] { pin.getInPort().getPortNumber(), outPort.getPortNumber(), m.toString() });
		} catch (Exception e) {
			logger.error("error {}", e);
		}
	}

	public static void addEtriesForAllNeededSwitchesForFLow(IOFSwitch swRight, IOFSwitch swLeft, OFPacketIn pin, FloodlightContext cntx, OFPort outPort, String serverIPAddr, int serverPort, String hostIPAddr, int hostPort, int innerPortTowardsRightSwitch, int innerPortTowardsLeftSwitch, String serverMac) {

		// packetOut

		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx,	IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		if (eth.getEtherType() == EthType.IPv4) {
			IPv4 ipv4 = (IPv4) eth.getPayload();

			ipv4.setDestinationAddress(serverIPAddr);
			eth.setDestinationMACAddress(serverMac);

			//
			// 	// IP
			// 		IPv4 l3	= new IPv4();
			// 		l3.setSourceAddress(IPv4Address.of("192.168.1.1"));
			// 		l3.setDestinationAddress(IPv4Address.of("192.168.1.255"));
			// 		l3.setTtl((byte) 64);
			// 		l3.setProtocol(IpProtocol.TCP);
			//
			// 	// UDP
			// 		TCP	l4 = new TCP();
			// 		//l4.setSourcePort(TransportPort.of(65003));
			// 		//l4.setDestinationPort(TransportPort.of(53));
			//
			// 	// serializacja
			eth.setPayload(ipv4);

			byte []	serializedData = eth.serialize();

			// Create Packet-Out and Write to Switch
			OFPacketOut po = swRight.getOFFactory().buildPacketOut().setData(serializedData)
					.setActions(Collections.singletonList((OFAction)swRight.getOFFactory().actions().output(OFPort.of(innerPortTowardsLeftSwitch), 0xffFFffFF)))
					.setInPort(OFPort.CONTROLLER).build();
			swRight.write(po);
			logger.info("****************** PACKET OUT SENT *************");
		}



		//1. Switch PRAWY -> Switch LEWY
		// FlowModBuilder
		OFFlowMod.Builder fmb = swRight.getOFFactory().buildFlowAdd();
		// match
		Match.Builder mb = swRight.getOFFactory().buildMatch();
		mb.setExact(MatchField.IN_PORT, OFPort.of(1));
		mb.setExact(MatchField.IPV4_SRC, IPv4Address.of(hostIPAddr));
		mb.setExact(MatchField.TCP_SRC, TransportPort.of(hostPort));
		mb.setExact(MatchField.IPV4_DST, IPv4Address.of(MAIN_ADDR));
		mb.setExact(MatchField.TCP_DST, TransportPort.of(80));
		Match m = mb.build();

		// actions
		OFActionOutput.Builder aob = swRight.getOFFactory().actions().buildOutput();
		List<OFAction> actions = new ArrayList<OFAction>();

		//podmieniam adres docelowy
		OFActionSetNwDst.Builder ndst = swRight.getOFFactory().actions().buildSetNwDst();
		ndst.setNwAddr(IPv4Address.of(serverIPAddr));
		actions.add(ndst.build());

		//podmieniam MAC docelowy
		OFActionSetDlDst.Builder dldst = swRight.getOFFactory().actions().buildSetDlDst();
		dldst.setDlAddr(MacAddress.of(serverMac));
		actions.add(dldst.build());

		// przekazuje na port 2
		aob.setPort(OFPort.of(innerPortTowardsLeftSwitch));
		aob.setMaxLen(Integer.MAX_VALUE);
		actions.add(aob.build());

		fmb.setMatch(m).setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT).setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
		.setBufferId(pin.getBufferId()).setOutPort(OFPort.of(innerPortTowardsLeftSwitch)).setPriority(FLOWMOD_DEFAULT_PRIORITY);
		fmb.setActions(actions);
		// write flow to switch

		try {
			swRight.write(fmb.build());
			logger.info("Flow from port {} forwarded to port {}; match: {}",
					new Object[] { pin.getInPort().getPortNumber(), OFPort.of(innerPortTowardsLeftSwitch).getPortNumber(), m.toString() });
		} catch (Exception e) {
			logger.error("error {}", e);
		}


		//2. Switch LEWY -> server
		mb = swLeft.getOFFactory().buildMatch();
		mb.setExact(MatchField.IN_PORT, OFPort.of(innerPortTowardsRightSwitch));
		mb.setExact(MatchField.IPV4_SRC, IPv4Address.of(hostIPAddr));
		mb.setExact(MatchField.TCP_SRC, TransportPort.of(hostPort));
		mb.setExact(MatchField.IPV4_DST, IPv4Address.of(serverIPAddr));
		mb.setExact(MatchField.TCP_DST, TransportPort.of(serverPort));
		m = mb.build();

		// actions
		aob = swLeft.getOFFactory().actions().buildOutput();
		actions = new ArrayList<OFAction>();

		// przekazuje na port 1
		aob.setPort(OFPort.of(1));
		aob.setMaxLen(Integer.MAX_VALUE);
		actions.add(aob.build());

		fmb.setMatch(m).setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT).setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
		.setBufferId(OFBufferId.NO_BUFFER).setOutPort(OFPort.of(1)).setPriority(FLOWMOD_DEFAULT_PRIORITY);
		fmb.setActions(actions);
		// write flow to switch

		try {
			swLeft.write(fmb.build());
			logger.info("Flow from port {} forwarded to port {}; match: {}",
					new Object[] { pin.getInPort().getPortNumber(), OFPort.of(1).getPortNumber(), m.toString() });
		} catch (Exception e) {
			logger.error("error {}", e);
		}


		//3. Switch LEWY -> Switch PRAWY
		mb = swLeft.getOFFactory().buildMatch();
		mb.setExact(MatchField.IN_PORT, OFPort.of(1));
		mb.setExact(MatchField.IPV4_SRC, IPv4Address.of(serverIPAddr));
		mb.setExact(MatchField.TCP_SRC, TransportPort.of(serverPort));
		mb.setExact(MatchField.IPV4_DST, IPv4Address.of(hostIPAddr));
		mb.setExact(MatchField.TCP_DST, TransportPort.of(hostPort));
		m = mb.build();

		// actions
		aob = swLeft.getOFFactory().actions().buildOutput();
		actions = new ArrayList<OFAction>();

		// przekazuje na port 2
		aob.setPort(OFPort.of(innerPortTowardsRightSwitch));
		aob.setMaxLen(Integer.MAX_VALUE);
		actions.add(aob.build());

		fmb.setMatch(m).setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT).setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
		.setBufferId(OFBufferId.NO_BUFFER).setOutPort(OFPort.of(innerPortTowardsRightSwitch)).setPriority(FLOWMOD_DEFAULT_PRIORITY);
		fmb.setActions(actions);
		// write flow to switch

		try {
			swLeft.write(fmb.build());
			logger.info("Flow from port {} forwarded to port {}; match: {}",
					new Object[] { pin.getInPort().getPortNumber(), OFPort.of(innerPortTowardsRightSwitch).getPortNumber(), m.toString() });
		} catch (Exception e) {
			logger.error("error {}", e);
		}


		//4. Switch PRAWY -> Host
		mb = swRight.getOFFactory().buildMatch();
		mb.setExact(MatchField.IN_PORT, OFPort.of(innerPortTowardsLeftSwitch));
		mb.setExact(MatchField.IPV4_DST, IPv4Address.of(serverIPAddr));
		mb.setExact(MatchField.TCP_SRC, TransportPort.of(serverPort));
		mb.setExact(MatchField.IPV4_DST, IPv4Address.of(hostIPAddr));
		mb.setExact(MatchField.TCP_DST, TransportPort.of(hostPort));
		m = mb.build();

		// actions
		aob = swRight.getOFFactory().actions().buildOutput();
		actions = new ArrayList<OFAction>();

		//podmieniam adres docelowy
		OFActionSetNwSrc.Builder nsrc = swRight.getOFFactory().actions().buildSetNwSrc();
		nsrc.setNwAddr(IPv4Address.of(MAIN_ADDR));
		actions.add(nsrc.build());

		//podmieniam MAC docelowy
		OFActionSetDlSrc.Builder dlsrc = swRight.getOFFactory().actions().buildSetDlSrc();
		dlsrc.setDlAddr(MacAddress.of("00:00:00:00:00:04"));//switch przy oficjalnym serverze
		actions.add(dlsrc.build());

		// przekazuje na port 1
		aob.setPort(OFPort.of(1));
		aob.setMaxLen(Integer.MAX_VALUE);
		actions.add(aob.build());

		fmb.setMatch(m).setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT).setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
		.setBufferId(OFBufferId.NO_BUFFER).setOutPort(OFPort.of(1)).setPriority(FLOWMOD_DEFAULT_PRIORITY);
		fmb.setActions(actions);
		// write flow to switch

		try {
			swRight.write(fmb.build());
			logger.info("Flow from port {} forwarded to port {}; match: {}",
					new Object[] { pin.getInPort().getPortNumber(), OFPort.of(1).getPortNumber(), m.toString() });
		} catch (Exception e) {
			logger.error("error {}", e);
		}
	}

	public static Match createMatchFromPacket(IOFSwitch sw, OFPort inPort, FloodlightContext cntx) {
		// The packet in match will only contain the port number.
		// We need to add in specifics for the hosts we're routing between.
		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		VlanVid vlan = VlanVid.ofVlan(eth.getVlanID());
		MacAddress srcMac = eth.getSourceMACAddress();
		MacAddress dstMac = eth.getDestinationMACAddress();

		Match.Builder mb = sw.getOFFactory().buildMatch();
		mb.setExact(MatchField.IN_PORT, inPort);

		if (FLOWMOD_DEFAULT_MATCH_MAC) {
			mb.setExact(MatchField.ETH_SRC, srcMac).setExact(MatchField.ETH_DST, dstMac);
		}

		if (FLOWMOD_DEFAULT_MATCH_VLAN) {
			if (!vlan.equals(VlanVid.ZERO)) {
				mb.setExact(MatchField.VLAN_VID, OFVlanVidMatch.ofVlanVid(vlan));
			}
		}

		// TODO Detect switch type and match to create hardware-implemented flow
		if (eth.getEtherType() == EthType.IPv4) { /*
		 * shallow check for
		 * equality is okay for
		 * EthType
		 */
			IPv4 ip = (IPv4) eth.getPayload();
			IPv4Address srcIp = ip.getSourceAddress();
			IPv4Address dstIp = ip.getDestinationAddress();

			if (FLOWMOD_DEFAULT_MATCH_IP_ADDR) {
				mb.setExact(MatchField.ETH_TYPE, EthType.IPv4).setExact(MatchField.IPV4_SRC, srcIp)
				.setExact(MatchField.IPV4_DST, dstIp);
			}

			if (FLOWMOD_DEFAULT_MATCH_TRANSPORT) {
				/*
				 * Take care of the ethertype if not included earlier, since
				 * it's a prerequisite for transport ports.
				 */
				if (!FLOWMOD_DEFAULT_MATCH_IP_ADDR) {
					mb.setExact(MatchField.ETH_TYPE, EthType.IPv4);
				}

				if (ip.getProtocol().equals(IpProtocol.TCP)) {
					TCP tcp = (TCP) ip.getPayload();
					mb.setExact(MatchField.IP_PROTO, IpProtocol.TCP).setExact(MatchField.TCP_SRC, tcp.getSourcePort())
					.setExact(MatchField.TCP_DST, tcp.getDestinationPort());
				} else if (ip.getProtocol().equals(IpProtocol.UDP)) {
					UDP udp = (UDP) ip.getPayload();
					mb.setExact(MatchField.IP_PROTO, IpProtocol.UDP).setExact(MatchField.UDP_SRC, udp.getSourcePort())
					.setExact(MatchField.UDP_DST, udp.getDestinationPort());
				}
			}
		} else if (eth.getEtherType() == EthType.ARP) { /*
		 * shallow check for
		 * equality is okay for
		 * EthType
		 */
			mb.setExact(MatchField.ETH_TYPE, EthType.ARP);
		}

		return mb.build();
	}
}
