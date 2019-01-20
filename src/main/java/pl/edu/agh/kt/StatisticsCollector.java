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
						if (pse.getPortNo().getPortNumber() > 0) {
							logger.info("port number: {} TxBytes: {} ", pse.getPortNo().getPortNumber(), pse.getTxBytes().getValue());
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
	public static final int	PORT_STATISTICS_POLLING_INTERVAL = 1000; // in ms
	private	static StatisticsCollector singleton;

	private StatisticsCollector(IOFSwitch sw) {
		this.sw	= sw;
		new Timer().scheduleAtFixedRate(new PortStatisticsPoller(), 0, PORT_STATISTICS_POLLING_INTERVAL);
	}
	public static StatisticsCollector getInstance(IOFSwitch sw) {
		logger.debug("getInstance() begin");
		synchronized (StatisticsCollector.class) {
			if (singleton == null) {
				logger.debug("Creating StatisticsCollector singleton");
				singleton = new	StatisticsCollector(sw);
			}
		}
		logger.debug("getInstance() end");
		return singleton;
	}
}