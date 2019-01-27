package pl.edu.agh.kt;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.projectfloodlight.openflow.protocol.OFFlowMod;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFPacketOut;
import org.projectfloodlight.openflow.protocol.OFVersion;
import org.projectfloodlight.openflow.protocol.action.OFAction;
import org.projectfloodlight.openflow.protocol.action.OFActionOutput;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlDst;
import org.projectfloodlight.openflow.protocol.action.OFActionSetNwSrc;
import org.projectfloodlight.openflow.protocol.action.OFActionSetDlSrc;
import org.projectfloodlight.openflow.protocol.match.Match;
import org.projectfloodlight.openflow.protocol.match.MatchField;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IPv4Address;
import org.projectfloodlight.openflow.types.IPv6Address;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.projectfloodlight.openflow.types.MacAddress;
import org.projectfloodlight.openflow.types.OFBufferId;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.OFVlanVidMatch;
import org.projectfloodlight.openflow.types.TransportPort;
import org.projectfloodlight.openflow.types.VlanVid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.devicemanager.IDevice;
import net.floodlightcontroller.devicemanager.IDeviceService;
import net.floodlightcontroller.devicemanager.internal.DeviceManagerImpl;
import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Data;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPacket;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.util.OFMessageUtils;

public class Flows {

	private static final Logger logger = LoggerFactory.getLogger(Flows.class);

	public static short FLOWMOD_DEFAULT_IDLE_TIMEOUT = 5; // in seconds
	public static short FLOWMOD_DEFAULT_HARD_TIMEOUT = 0; // infinite
	public static short FLOWMOD_DEFAULT_PRIORITY = 100;

	protected static boolean FLOWMOD_DEFAULT_MATCH_VLAN = true;
	protected static boolean FLOWMOD_DEFAULT_MATCH_MAC = true;
	protected static boolean FLOWMOD_DEFAULT_MATCH_IP_ADDR = true;
	protected static boolean FLOWMOD_DEFAULT_MATCH_TRANSPORT = true;
	private static IPv4 ipv4;
	private static TCP tcp;

	public Flows() {
		logger.info("Flows() begin/end");
	}



	public static void sendPacketOut(IOFSwitch sw) {
		// TODO punkt 3 instrukcji
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

	public static void addFlow(IOFSwitch sw, OFPacketIn pin, FloodlightContext cntx) {
		//allow arp
		logger.info("addFlow");

		Ethernet eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		if (eth.getEtherType() == EthType.IPv4) {
			ipv4 = (IPv4) eth.getPayload();
			if (ipv4.getProtocol() == IpProtocol.TCP) {
				tcp = (TCP) ipv4.getPayload();
				if(tcp.getDestinationPort().getPort()==80 || tcp.getSourcePort().getPort()==80) {
					logger.info("GOT IN !!!______________________________________________");
					if(sw.getId().toString().equals("00:00:00:00:00:00:00:04") && pin.getInPort().getPortNumber() == 1) {
						//vipProxyArpReply(sw, pin, cntx);
						logger.info("GOT IN !!! S4 to S2");
						fromS4toS2(sw, pin, cntx);
					} else if(sw.getId().toString().equals("00:00:00:00:00:00:00:02") && pin.getInPort().getPortNumber() == 2) {
						logger.info("S2 to Sv2");
						fromS2toSV2(sw, pin, cntx);
					} else if(sw.getId().toString().equals("00:00:00:00:00:00:00:02") && pin.getInPort().getPortNumber() == 1) {
						logger.info("S2 to S4");
						fromS2toS4(sw, pin, cntx);
					} else if(sw.getId().toString().equals("00:00:00:00:00:00:00:04") && pin.getInPort().getPortNumber() == 3){
						logger.info("S4 to H1");
						fromS4toH1(sw, pin, cntx);
					}
				}
			}

		}
		/*else {
        	simpleAdd(sw, pin, cntx);
        }*/
	}

	public static void fromS4toH1(IOFSwitch sw, OFPacketIn pin, FloodlightContext cntx) {
		// FlowModBuilder
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
		// match
		Match.Builder mb = sw.getOFFactory().buildMatch();
		mb.setExact(MatchField.IN_PORT, OFPort.of(3));
		mb.setExact(MatchField.IPV4_SRC, IPv4Address.of("10.0.0.5"));
		mb.setExact(MatchField.TCP_SRC, TransportPort.of(80));
		Match m = mb.build();

		// actions
		OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
		List<OFAction> actions = new ArrayList<OFAction>();
		// przekazuje na port 2
		aob.setPort(OFPort.of(1));
		aob.setMaxLen(Integer.MAX_VALUE);
		actions.add(aob.build());

		fmb.setMatch(m).setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT).setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
		.setBufferId(pin.getBufferId()).setOutPort(OFPort.of(1)).setPriority(FLOWMOD_DEFAULT_PRIORITY);
		fmb.setActions(actions);
		// write flow to switch

		logger.info("flow from s4 to h1");
		try {
			sw.write(fmb.build());
			logger.info("Flow from port {} forwarded to port {}; match: {}",
					new Object[] { pin.getInPort().getPortNumber(), OFPort.of(2).getPortNumber(), m.toString() });
		} catch (Exception e) {
			logger.error("error {}", e);
		}

	}

	public static void fromS2toS4(IOFSwitch sw, OFPacketIn pin, FloodlightContext cntx) {
		// FlowModBuilder
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
		// match
		Match.Builder mb = sw.getOFFactory().buildMatch();
		mb.setExact(MatchField.IN_PORT, OFPort.of(1));
		mb.setExact(MatchField.IPV4_SRC, IPv4Address.of("10.0.0.4"));
		Match m = mb.build();

		// actions
		OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
		List<OFAction> actions = new ArrayList<OFAction>();

		//podmieniam adres docelowy
		OFActionSetNwSrc.Builder nsrc = sw.getOFFactory().actions().buildSetNwSrc();
		nsrc.setNwAddr(IPv4Address.of("10.0.0.4"));
		actions.add(nsrc.build());

		//podmieniam MAC docelowy
		OFActionSetDlSrc.Builder dlsrc = sw.getOFFactory().actions().buildSetDlSrc();
		dlsrc.setDlAddr(MacAddress.of("00:00:00:00:00:04"));
		actions.add(dlsrc.build());

		// przekazuje na port 2
		aob.setPort(OFPort.of(2));
		aob.setMaxLen(Integer.MAX_VALUE);
		actions.add(aob.build());

		fmb.setMatch(m).setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT).setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
		.setBufferId(pin.getBufferId()).setOutPort(OFPort.of(1)).setPriority(FLOWMOD_DEFAULT_PRIORITY);
		fmb.setActions(actions);
		// write flow to switch

		logger.info("flow s2 -- s4");
		try {
			sw.write(fmb.build());
			logger.info("Flow from port {} forwarded to port {}; match: {}",
					new Object[] { pin.getInPort().getPortNumber(), OFPort.of(2).getPortNumber(), m.toString() });
		} catch (Exception e) {
			logger.error("error {}", e);
		}

	}


	public static void fromS2toSV2(IOFSwitch sw, OFPacketIn pin, FloodlightContext cntx) {
		// FlowModBuilder
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
		// match
		Match.Builder mb = sw.getOFFactory().buildMatch();
		mb.setExact(MatchField.IN_PORT, OFPort.of(2));
		mb.setExact(MatchField.TCP_DST, TransportPort.of(80));
		Match m = mb.build();

		// actions
		OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
		List<OFAction> actions = new ArrayList<OFAction>();

		// przekazuje na port 1
		aob.setPort(OFPort.of(1));
		aob.setMaxLen(Integer.MAX_VALUE);
		actions.add(aob.build());

		fmb.setMatch(m).setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT).setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
		.setBufferId(pin.getBufferId()).setOutPort(OFPort.of(1)).setPriority(FLOWMOD_DEFAULT_PRIORITY);
		fmb.setActions(actions);
		// write flow to switch

		logger.info("flow s2 -- sv2");
		try {
			sw.write(fmb.build());
			logger.info("Flow from port {} forwarded to port {}; match: {}",
					new Object[] { pin.getInPort().getPortNumber(), OFPort.of(2).getPortNumber(), m.toString() });
		} catch (Exception e) {
			logger.error("error {}", e);
		}

	}

	public static void fromS4toS2(IOFSwitch sw, OFPacketIn pin, FloodlightContext cntx) {

		// FlowModBuilder
		OFFlowMod.Builder fmb = sw.getOFFactory().buildFlowAdd();
		// match
		Match.Builder mb = sw.getOFFactory().buildMatch();
		mb.setExact(MatchField.IN_PORT, OFPort.of(1));
		mb.setExact(MatchField.IPV4_DST, IPv4Address.of("10.0.0.4"));
		mb.setExact(MatchField.TCP_DST, TransportPort.of(80));
		Match m = mb.build();


		// actions
		OFActionOutput.Builder aob = sw.getOFFactory().actions().buildOutput();
		List<OFAction> actions = new ArrayList<OFAction>();

		//podmieniam adres docelowy
		OFActionSetNwDst.Builder ndst = sw.getOFFactory().actions().buildSetNwDst();
		ndst.setNwAddr(IPv4Address.of("10.0.0.5"));
		actions.add(ndst.build());

		//podmieniam MAC docelowy
		OFActionSetDlDst.Builder dldst = sw.getOFFactory().actions().buildSetDlDst();
		dldst.setDlAddr(MacAddress.of("00:00:00:00:00:05"));
		actions.add(dldst.build());

		// przekazuje na port 2
		aob.setPort(OFPort.of(3));
		aob.setMaxLen(Integer.MAX_VALUE);
		actions.add(aob.build());


		fmb.setMatch(m).setIdleTimeout(FLOWMOD_DEFAULT_IDLE_TIMEOUT).setHardTimeout(FLOWMOD_DEFAULT_HARD_TIMEOUT)
		.setBufferId(pin.getBufferId()).setOutPort(OFPort.of(3)).setPriority(FLOWMOD_DEFAULT_PRIORITY);
		fmb.setActions(actions);
		// write flow to switch

		logger.info("flow s4 -- s2");

		try {
			sw.write(fmb.build());
			logger.info("Flow from port {} forwarded to port {}; match: {}",
					new Object[] { pin.getInPort().getPortNumber(), OFPort.of(2).getPortNumber(), m.toString() });
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
	public static void pushPacket(IPacket packet,
			IOFSwitch sw,
			OFBufferId bufferId,
			OFPort inPort,
			OFPort outPort,
			FloodlightContext cntx,
			boolean flush) {
		OFPacketOut.Builder pob = sw.getOFFactory().buildPacketOut();

		// set actions
		List<OFAction> actions = new ArrayList<OFAction>();
		actions.add(sw.getOFFactory().actions().buildOutput().setPort(outPort).setMaxLen(Integer.MAX_VALUE).build());

		pob.setActions(actions);

		// set buffer_id, in_port
		pob.setBufferId(bufferId);
		//OFMessageUtils.setInPort(pob, inPort);

		// set data - only if buffer_id == -1
		if (pob.getBufferId() == OFBufferId.NO_BUFFER) {
			if (packet == null) {
				logger.info("BufferId is not set and packet data is null. " +
						"Cannot send packetOut. " +
						"srcSwitch={} inPort={} outPort={}",
						new Object[] {sw, inPort, outPort});
				return;
			}
			byte[] packetData = packet.serialize();
			pob.setData(packetData);
		}

		sw.write(pob.build());
	}
}
