package pl.edu.agh.kt;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.projectfloodlight.openflow.protocol.OFPortStatsEntry;
import org.projectfloodlight.openflow.protocol.OFPortStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsReply;
import org.projectfloodlight.openflow.protocol.OFStatsRequest;
import org.projectfloodlight.openflow.types.DatapathId;
import org.projectfloodlight.openflow.types.OFPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.common.util.concurrent.ListenableFuture;
import net.floodlightcontroller.core.IOFSwitch;
import net.floodlightcontroller.topology.NodePortTuple;

public class StatisticsCollector {

	private static final Logger logger = LoggerFactory.getLogger(StatisticsCollector.class);
	private IOFSwitch sw;
	public class PortStatisticsPoller extends TimerTask {
//	    private long[] bytesInThisInterval = new long[4];
//	    private long[] previousBytes = new long[4];
//	    private long[] currentBytes= new long[4];
//	    private long[] bandwidth = new long[4];
	    public long bytesInThisIntervalReceived = 0;
	    public long previousBytesReceived = 0;
	    public long currentBytesReceived = 0;
	    private long bandwidth = 0;

		private final Logger logger	= LoggerFactory.getLogger(PortStatisticsPoller.class);
		private static final int TIMEOUT = PORT_STATISTICS_POLLING_INTERVAL	/ 2;
		@Override
		public void	run() {
			logger.debug("run() begin");
			synchronized (StatisticsCollector.this) {
				if (sw == null) { // no switch
					logger.error("run() end (no switch)");
					return;
				}

				ListenableFuture<?> future;
				List<OFStatsReply> values = null;
				OFStatsRequest<?> req = null;
				req	= sw.getOFFactory().buildPortStatsRequest().setPortNo(OFPort.ANY).build();
				try	{
					if(req != null) {
						future = sw.writeStatsRequest(req);
						values = (List<OFStatsReply>) future.get(PORT_STATISTICS_POLLING_INTERVAL * 1000 / 2, TimeUnit.MILLISECONDS);
					}
					OFPortStatsReply psr = (OFPortStatsReply) values.get(0);
					for (OFPortStatsEntry pse :	psr.getEntries()) {
						if (pse.getPortNo().getPortNumber() == 1) {
							currentBytesReceived = pse.getRxBytes().getValue();
							bytesInThisIntervalReceived = currentBytesReceived - previousBytesReceived;
							bandwidth =  (bytesInThisIntervalReceived / (PORT_STATISTICS_POLLING_INTERVAL/1000)) / 1000; //bajty na sekundę (nawias) przez 1000 a więc KB
							logger.info("switch desc: {}", sw.getId().toString());
							logger.info("port number: {} bandwidth: {} ", pse.getPortNo().getPortNumber(), bandwidth);

							if (sw.getId().toString().equals("00:00:00:00:00:00:00:01")) {
								bandwidths[0] = bandwidth;
							} else if(sw.getId().toString().equals("00:00:00:00:00:00:00:02")) {
								bandwidths[1] = bandwidth;
							} else if(sw.getId().toString().equals("00:00:00:00:00:00:00:03")) {
								bandwidths[2] = bandwidth;
							} else {
								logger.error("run() unexected switch address!");
								return;
							}



							previousBytesReceived = currentBytesReceived;
						}
//						int portNumber = pse.getPortNo().getPortNumber() - 1;
//						if (pse.getPortNo().getPortNumber() > 0) {
//							currentBytes[portNumber] = pse.getTxBytes().getValue();
//							bytesInThisInterval[portNumber] = currentBytes[portNumber] - previousBytes[portNumber];
//							bandwidth[portNumber] =  (bytesInThisInterval[portNumber] / (PORT_STATISTICS_POLLING_INTERVAL/1000)) / 1000; //bajty na sekundę (nawias) przez 1000 a więc KB
//							logger.info("switch desc: {}", sw.getId().toString());
//							logger.info("port number: {} bandwidth: {} ", pse.getPortNo().getPortNumber(), bandwidth[portNumber]);
//							previousBytes[portNumber] = currentBytes[portNumber];
//						}
					}
				} catch (InterruptedException | ExecutionException | TimeoutException ex) {
					logger.error("Error during statistics polling", ex);
				}
			}
			logger.debug("run() end");
		}
	}
	public static final int	PORT_STATISTICS_POLLING_INTERVAL = 5000; // in ms
	private static StatisticsCollector statscollector1;
	private static StatisticsCollector statscollector2;
	private static StatisticsCollector statscollector3;
	private static double[] bandwidths = new double[] {0,0,0};

	private StatisticsCollector(IOFSwitch sw) {
		this.sw	= sw;
		new Timer().scheduleAtFixedRate(new PortStatisticsPoller(), 0, PORT_STATISTICS_POLLING_INTERVAL);
	}

	public static StatisticsCollector getInstance(IOFSwitch sw) {
		StatisticsCollector statscolector = null;
		logger.debug("getInstance() begin {}", sw.getId().toString());
		if(sw.getId().toString().equals("00:00:00:00:00:00:00:01")){
			synchronized (StatisticsCollector.class) {
				if (statscollector1 == null) {
					statscollector1 = new StatisticsCollector(sw);
				}
			}
			logger.debug("ABCD statscollector1");
			statscolector = statscollector1;
		} else if(sw.getId().toString().equals("00:00:00:00:00:00:00:02")) {
			synchronized (StatisticsCollector.class) {
				if (statscollector2 == null) {
					statscollector2 = new StatisticsCollector(sw);
				}
			}
			logger.debug("ABCD statscollector2");
			statscolector = statscollector2;
		} else if(sw.getId().toString().equals("00:00:00:00:00:00:00:03")) {
			synchronized (StatisticsCollector.class) {
				if (statscollector3 == null) {
					logger.debug("ABCD statscollector3");
					statscollector3 = new StatisticsCollector(sw);
				}
			}
			logger.debug("ABCD statscollector3");
			statscolector = statscollector3;
		}
		logger.debug("getInstance() end");
		return statscolector;
	}

	public static double[] getBandwidths() {
		logger.warn("Sending statistics to SdnLabListener");
		return bandwidths;
	}
}
