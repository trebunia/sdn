package pl.edu.agh.kt;

import java.util.Collection;
import java.util.Map;

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
//		PacketExtractor extractor = new PacketExtractor();
//		extractor.packetExtract(cntx);

		try {
			 logger.warn("X_X_X_X_X_X_X_X_X_X_X_X_X_X_X_X_____X_X_X_X_X__X_X_X_X_X_X_X_X_X_X_X_X_");
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


		if (sw.getId().toString().equals("00:00:00:00:00:00:00:01")) {
			StatisticsCollector.getInstance(sw);
			logger.info("Switch id: {}", sw.getId().toString());
		} else if(sw.getId().toString().equals("00:00:00:00:00:00:00:02")) {
			StatisticsCollector.getInstance(sw);
			logger.info("Switch id: {}", sw.getId().toString());
		} else if(sw.getId().toString().equals("00:00:00:00:00:00:00:03")) {
			StatisticsCollector.getInstance(sw);
			logger.info("Switch id: {}", sw.getId().toString());
		}
		else if (sw.getId().toString().equals("00:00:00:00:00:00:00:04")) {
			logger.info("Switch id: {}", sw.getId().toString());
			//determine_destination_switch()
		} else if(sw.getId().toString().equals("00:00:00:00:00:00:00:05")) {
			logger.info("Switch id: {}", sw.getId().toString());
			//determine_destination_switch()
		} else if(sw.getId().toString().equals("00:00:00:00:00:00:00:06")) {
			logger.info("Switch id: {}", sw.getId().toString());
			//determine_destination_switch()
		}

//		OFPacketIn pin = (OFPacketIn) msg;
//		OFPort outPort = OFPort.of(0);
//		if (pin.getInPort() == OFPort.of(1)) {
//			outPort = OFPort.of(2);
//		} else
//			outPort = OFPort.of(1);
//		Flows.simpleAdd(sw, pin, cntx, outPort);
		// return Command.STOP;
		return Command.CONTINUE;
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
