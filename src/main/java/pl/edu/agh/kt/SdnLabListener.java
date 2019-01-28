package pl.edu.agh.kt;

import java.util.Collection;
import java.util.Map;
import java.util.stream.*;

import org.projectfloodlight.openflow.protocol.OFMessage;
import org.projectfloodlight.openflow.protocol.OFPacketIn;
import org.projectfloodlight.openflow.protocol.OFType;
import org.projectfloodlight.openflow.types.OFPort;
import org.projectfloodlight.openflow.types.DatapathId;

import net.floodlightcontroller.core.FloodlightContext;
import net.floodlightcontroller.core.IOFMessageListener;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.core.module.FloodlightModuleContext;
import net.floodlightcontroller.core.module.FloodlightModuleException;
import net.floodlightcontroller.core.module.IFloodlightModule;
import net.floodlightcontroller.core.module.IFloodlightService;
import net.floodlightcontroller.core.IFloodlightProviderService;
import net.floodlightcontroller.core.internal.IOFSwitchService;

import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SdnLabListener implements IFloodlightModule, IOFMessageListener {

	private static String[] leftSwitchMac = new String[] {"00:00:00:00:00:00:00:01","00:00:00:00:00:00:00:02","00:00:00:00:00:00:00:03"};
	private static String[] serverIPAddr = new String[] {"10.0.0.4","10.0.0.5","10.0.0.6"};
	private static String[] serverMacTable = new String[] {"00:00:04","00:00:05","00:00:06"};
	private static int[] serverPort = new int[] {80,80,80};
	//private static String[] hostIPTable = new String[] {"10.0.0.1","10.0.0.2","10.0.0.3"};
	private static double[] thresholds = new double[] {0.2,0.3,0.5};

	protected IFloodlightProviderService floodlightProvider;
	protected static Logger logger;

	@Override
	public String getName() {
		return SdnLabListener.class.getSimpleName();
	}

	@Override
	public boolean isCallbackOrderingPrereq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public boolean isCallbackOrderingPostreq(OFType type, String name) {
		// TODO Auto-generated method stub
		return false;
	}

	protected IOFSwitchService switchService;

	@Override
	public net.floodlightcontroller.core.IListener.Command receive(IOFSwitch sw, OFMessage msg,
			FloodlightContext cntx) {

		logger.info("************* NEW PACKET IN *************");

		try {
			// if (switchService.getSwitch(DatapathId.of("00:00:00:00:00:00:00:03")) != null) {
			// 	logger.warn("@@@@@@@@@@@@@@@@@@@2");
			// }
			// IOFSwitch sw1 = switchService.getSwitch(DatapathId.of(sw.getId().toString()));

			// for (DatapathId id : switchService.getAllSwitchDpids()) {
			// 	logger.warn("id: {}", id.toString());
			// }
			// if (switchService.getAllSwitchDpids().contains(DatapathId.of("00:00:00:00:00:00:00:03"))) {
			// 	logger.warn("Zawiera");
			// } else {
			// 	logger.warn("NIE zawiera");
			// }
			//
			// logger.warn("słicz 3: {}", switchService.getSwitch(DatapathId.of("00:00:00:00:00:00:00:03")));
			// logger.warn("_____________________________________________________________________");

		} catch (Exception ex) {
			logger.error("MWMWMWMWMWMWMWMWMWMWMWOWOOOOOOOOOOOOOOOOOOLLLLLLLLLLLLLLLLIIIIIIIIIIIIIIIIAAAAAAAAAAAAAAAAQWEWETTRYTYUOUOUOUOUOUOUOUUOUOUOUOUOIOOIOIOIOIOIOIOIOIOIOIOIOIOIIOIOIOOIOIIOIOIOIOIOIOIOIOIOIOIOXOXOXOXOXOXOXOXOXOXOXOXOXOOXOXOXOXOXOXOXOX", ex);
		}

		OFPacketIn pin = (OFPacketIn) msg;

		logger.info("Switch id: {}", sw.getId().toString());
		logger.info("Interface: {}", pin.getInPort());

		if (sw.getId().toString().equals("00:00:00:00:00:00:00:01") || sw.getId().toString().equals("00:00:00:00:00:00:00:02") || sw.getId().toString().equals("00:00:00:00:00:00:00:03")) {
			StatisticsCollector.getInstance(sw);

			if (pin.getInPort() == OFPort.of(4) || pin.getInPort() == OFPort.of(5) || pin.getInPort() == OFPort.of(6)) {
		 		Flows.simpleAdd(sw, pin, cntx, OFPort.of(1));
		 	}
			else if(pin.getInPort() == OFPort.of(1)) {
				logger.error("Packet_in not expected on port: {}, sending on port 4", pin.getInPort()); //suppress
				Flows.simpleAdd(sw, pin, cntx, OFPort.of(4));
			}
		}
		else if (sw.getId().toString().equals("00:00:00:00:00:00:00:04") || sw.getId().toString().equals("00:00:00:00:00:00:00:05") || sw.getId().toString().equals("00:00:00:00:00:00:00:06")) {

			if (pin.getInPort() == OFPort.of(1)) {//action for other ports already satisfied in this 'if'
				PacketExtractor extractor = new PacketExtractor();
				extractor.packetExtract(cntx);
				String hostIPAddr = extractor.getSrcIPAddress();
				int hostPort = extractor.getSrcTcpPort();

				logger.info("calculating destination server for new flow"); //suppress
				int index = this.CalculateDestinationServerIndex();
				int innerPortTowardsLeftSwitch = index + 2;// interfejs na lewym switchu (2,3 lub 4)
				int innerPortTowardsRightSwitch = 0;
				if (sw.getId().toString().equals("00:00:00:00:00:00:00:04")) innerPortTowardsRightSwitch = 2;//interfejs na prawym switchu
				if (sw.getId().toString().equals("00:00:00:00:00:00:00:05")) innerPortTowardsRightSwitch = 3;
				if (sw.getId().toString().equals("00:00:00:00:00:00:00:06")) innerPortTowardsRightSwitch = 4;

				IOFSwitch swRight = sw;
				IOFSwitch swLeft = switchService.getSwitch(DatapathId.of(leftSwitchMac[index])); //switch od serwera, który został wybrany dla tego flowu

				Flows.addEtriesForAllNeededSwitchesForFLow(swRight, swLeft, pin, cntx, OFPort.of(1), serverIPAddr[index], serverPort[index], hostIPAddr, hostPort, innerPortTowardsRightSwitch, innerPortTowardsLeftSwitch, serverMacTable[index]);
			}
		}
		return Command.CONTINUE;
	}

	public int CalculateDestinationServerIndex() {
		int index = 0;
		double[] bandwidths = StatisticsCollector.getBandwidths();
		double sum = bandwidths[0] + bandwidths[1] + bandwidths[2];
		if (sum == 0.0) return 2;

		double deficit_0 = thresholds[0] - bandwidths[0]/sum;
		double deficit_1 = thresholds[1] - bandwidths[1]/sum;
		double deficit_2 = thresholds[2] - bandwidths[2]/sum;

		if (deficit_0 >= deficit_1 && deficit_0 >= deficit_2) index = 0;
		else if (deficit_1 >= deficit_0 && deficit_1 >= deficit_2) index = 1;
		else if (deficit_2 >= deficit_0 && deficit_2 >= deficit_1) index = 2;

		//logger.info("bandwidths - 1: {}, 2: {}, 3: {}", bandwidths[0], bandwidths[1], bandwidths[2]);
		//logger.info("deficits - 1: {}, 2: {}, 3: {}", deficit_0, deficit_1, deficit_2);
		logger.info("Chosen index: {}}", index);

		return index;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleServices() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<Class<? extends IFloodlightService>, IFloodlightService> getServiceImpls() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Collection<Class<? extends IFloodlightService>> getModuleDependencies() {
		Collection<Class<? extends IFloodlightService>> l = new ArrayList<Class<? extends IFloodlightService>>();
		l.add(IFloodlightProviderService.class);
		return l;
	}

	@Override
	public void init(FloodlightModuleContext context) throws FloodlightModuleException {
		floodlightProvider = context.getServiceImpl(IFloodlightProviderService.class);
		logger = LoggerFactory.getLogger(SdnLabListener.class);
	}

	@Override
	public void startUp(FloodlightModuleContext context) throws FloodlightModuleException {
		this.switchService = context.getServiceImpl(IOFSwitchService.class);
		//switchService.addOFSwitchListener(this);
		floodlightProvider.addOFMessageListener(OFType.PACKET_IN, this);
		logger.info("******************* START **************************");
	}

}
