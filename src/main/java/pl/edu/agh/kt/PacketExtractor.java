package pl.edu.agh.kt;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.types.EthType;
import org.projectfloodlight.openflow.types.IpProtocol;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.packet.ARP;
import net.floodlightcontroller.packet.Ethernet;
import net.floodlightcontroller.packet.IPv4;
import net.floodlightcontroller.packet.TCP;
import net.floodlightcontroller.packet.UDP;
import net.floodlightcontroller.packet.ICMP;


public class PacketExtractor {
	private static final Logger logger = LoggerFactory.getLogger(PacketExtractor.class);
	private FloodlightContext cntx;
	protected IFloodlightProviderService floodlightProvider;
	private Ethernet eth;
	private IPv4 ipv4;
	private ARP arp;
	private TCP tcp;
	private UDP udp;
	private OFMessage msg;

	public PacketExtractor(FloodlightContext cntx, OFMessage msg) {
		this.cntx = cntx;
		this.msg = msg;
		logger.info("PacketExtractor: Constructor method called");
	}

	public PacketExtractor() {
		logger.info("PacketExtractor: Constructor method called");
	}

	public void packetExtract(FloodlightContext cntx) {
		this.cntx = cntx;
		//extractEth();
	}

	public String getSrcIPAddress() {
		eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);

		if (eth.getEtherType() == EthType.IPv4) {
			ipv4 = (IPv4) eth.getPayload();
			return ipv4.getSourceAddress().toString();
		}
		else logger.info("Not an IP packet");

		return "";
	}

	public int getSrcTcpPort() {
		eth = IFloodlightProviderService.bcStore.get(cntx, IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
		if (eth.getEtherType() == EthType.IPv4) {
			ipv4 = (IPv4) eth.getPayload();
			if (ipv4.getProtocol() == IpProtocol.TCP) {
				tcp = (TCP) ipv4.getPayload();
				return tcp.getSourcePort().getPort();
			}
			else logger.info("Not a TCP packet");
		}
		else logger.info("Not an IP packet");

		return 0;
	}

	public void checkIfArpPacket() {
		if (eth.getEtherType() == EthType.ARP) {
			logger.info("ARP packet_in!!!");
		}
	}

	public void extractEth() {
			eth = IFloodlightProviderService.bcStore.get(cntx,
					IFloodlightProviderService.CONTEXT_PI_PAYLOAD);
			logger.info("Frame: src mac {}", eth.getSourceMACAddress());
			logger.info("Frame: dst mac {}", eth.getDestinationMACAddress());
			logger.info("Frame: ether_type {}", eth.getEtherType());
			if (eth.getEtherType() == EthType.ARP) {
				arp = (ARP) eth.getPayload();
				extractArp(arp);
			}
			if (eth.getEtherType() == EthType.IPv4) {
				ipv4 = (IPv4) eth.getPayload();
				extractIp(ipv4);
			}
	}

	public void extractIp(IPv4 ipv4) {
		logger.info("Packet: src IP {}", ipv4.getSourceAddress());
		logger.info("Packet: dst IP {}", ipv4.getDestinationAddress());

		if (ipv4.getProtocol() == IpProtocol.TCP) {
			tcp = (TCP) ipv4.getPayload();
			extractTCP(tcp);
		}

		if (ipv4.getProtocol() == IpProtocol.UDP) {
			udp = (UDP) ipv4.getPayload();
			extractUDP(udp);
		}

		// if (ipv4.getProtocol() == IpProtocol.ICMP) {
		// 	icmp = (ICMP) ipv4.getPayload();
		// 	extractICMP(icmp);
		// }

	}

	public void extractArp(ARP arp) {
		logger.info("*******************ARP FRAME IN*******************");
		logger.info("ARP Frame: sender MAC {}", arp.getSenderHardwareAddress());
		logger.info("ARP Frame: target MAC {}", arp.getTargetHardwareAddress());
		logger.info("ARP Frame: target IP {}", arp.getTargetProtocolAddress());
	}

	public void extractTCP(TCP tcp) {
		logger.info("*******************TCP SEGMENT IN*******************");
		logger.info("TCP seg: dst port {}", tcp.getDestinationPort());
		logger.info("TCP seg: src port {}", tcp.getSourcePort());
	}

	public void extractUDP(UDP udp) {
		logger.info("*******************UDP SEGMENT IN*******************");
		logger.info("UDP seg: src port {}", udp.getDestinationPort());
		logger.info("UDP seg: dst port {}", udp.getSourcePort());
	}

	public void extractICMP(ICMP icmp) {
		logger.info("*******************ICMP SEGMENT IN*******************");
		logger.info("ICMP code: {}", icmp.getIcmpCode());
	}
}
