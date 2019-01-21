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
	    private long[] bytesInThisInterval = new long[4];
	    private long[] previousBytes = new long[4];
	    private long[] currentBytes= new long[4];
	    private long[] bandwidth = new long[4];
//	    public long bytesInThisInterval2 = 0;
//	    public long previousBytes2 = 0;
//	    public long currentBytes2 = 0;
//	    public long bytesInThisInterval3 = 0;
//	    public long previousBytes3 = 0;
//	    public long currentBytes3 = 0;
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
						int portNumber = pse.getPortNo().getPortNumber() - 1;
						if (pse.getPortNo().getPortNumber() > 0) {
							currentBytes[portNumber] = pse.getTxBytes().getValue();
							bytesInThisInterval[portNumber] = currentBytes[portNumber] - previousBytes[portNumber];
							bandwidth[portNumber] =  (bytesInThisInterval[portNumber] / (PORT_STATISTICS_POLLING_INTERVAL/10000)) / 1000000;
							logger.info("switch desc: {}", sw.getId().toString());
							logger.info("port number: {} bandwidth: {} ", pse.getPortNo().getPortNumber(), bandwidth[portNumber]);
							previousBytes[portNumber] = currentBytes[portNumber];
						/* 
						 * Jako że te bajty się sumują to trzeba zrobić coś w rodzaju: 
						 * bandwidth = 1000000*(current_bytes - previous_bytes)/ (PORT_STATISTICS_POLLING_INTERVAL/1000)
						 * dzielone przez 1000 bo interwał jest w milisekundach, razy 1000000 żeby z bajtów zrobić MB
						 * Problem - trzeba to zrobić dla każdego portu ktory nas interesuje, wiec pewnie current_bytes i previous_bytes to
						 * muszą być tablice - nie mam najmniejszego pojęcia jak sobie z tym poradzić w Javie
						 * Być może trzeba wpisać każdy switch/port ręcznie, w tym momencie jest tutaj for po jakichś odpowiedziach otrzymanych z portów ?
						 * 
							*/
						}
					}
				} catch (InterruptedException | ExecutionException | TimeoutException ex) {
					logger.error("Error during statistics polling", ex);
				}
			}
			logger.debug("run() end");
		}
	}
	public static final int	PORT_STATISTICS_POLLING_INTERVAL = 10000; // in ms
	private static StatisticsCollector stats1;
	private static StatisticsCollector stats2;
	private static StatisticsCollector stats3;
	private static StatisticsCollector stats;
	;
	private StatisticsCollector(IOFSwitch sw) {
		this.sw	= sw;
		new Timer().scheduleAtFixedRate(new PortStatisticsPoller(), 0, PORT_STATISTICS_POLLING_INTERVAL);
	}
	public static StatisticsCollector getInstance(IOFSwitch sw) {
		logger.debug("getInstance() begin {}", sw.getId().toString());
		if(sw.getId().toString().equals("00:00:00:00:00:00:00:01")){
			synchronized (StatisticsCollector.class) {
				if (stats1 == null) {
					stats1 = new StatisticsCollector(sw);
				}
			}
			logger.debug("ABCD stats1");
			stats = stats1;
		} else if(sw.getId().toString().equals("00:00:00:00:00:00:00:02")) {
			synchronized (StatisticsCollector.class) {
				if (stats2 == null) {
					stats2 = new StatisticsCollector(sw);
				}
			}
			logger.debug("ABCD stats2");
			stats = stats2;
		} else if(sw.getId().toString().equals("00:00:00:00:00:00:00:03")) {
			synchronized (StatisticsCollector.class) {
				if (stats3 == null) {
					logger.debug("ABCD stats3");
					stats3 = new StatisticsCollector(sw);
				}
			}
			logger.debug("ABCD stats3");
			stats = stats3;
		}
		logger.debug("getInstance() end");
		return stats;
	}
}