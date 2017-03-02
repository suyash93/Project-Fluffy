package gash.router.container;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Routing information for the server - internal use only
 *
 * @author gash
 *
 */
@XmlRootElement(name = "conf")
@XmlAccessorType(XmlAccessType.FIELD)
public class GlobalConf {
	private int clusterId;
	private int globalPort;
	private String globalHost;
	private List<GlobalRoutingEntry> routing;


	public HashMap<String, Integer> asHashMap() {
		HashMap<String, Integer> map = new HashMap<String, Integer>();
		if (routing != null) {
			for (GlobalRoutingEntry entry : routing) {
				map.put(entry.host, entry.port);
			}
		}
		return map;
	}

	public void addEntry(GlobalRoutingEntry entry) {
		if (entry == null)
			return;

		if (routing == null)
			routing = new ArrayList<GlobalRoutingEntry>();

		routing.add(entry);
	}

	public int getClusterId() {
		return clusterId;
	}

	public void setClusterId(int clusterId) {
		this.clusterId = clusterId;
	}

	public int getGlobalPort() {
		return globalPort;
	}

	public void setGlobalPort(int globalPort) {
		this.globalPort = globalPort;
	}
	public List<GlobalRoutingEntry> getRouting() {
		return routing;
	}

	public void setRouting(List<GlobalRoutingEntry> conf) {
		this.routing = conf;
	}

	public String getGlobalHost() {
		return globalHost;
	}

	public void setGlobalHost(String globalHost) {
		this.globalHost = globalHost;
	}

	@XmlRootElement(name = "entry")
	@XmlAccessorType(XmlAccessType.PROPERTY)
	public static final class GlobalRoutingEntry {
		private String host;
		private int port;
		private int clusterId;

		public GlobalRoutingEntry() {
		}

		public GlobalRoutingEntry(int clusterId, String host, int port) {
			this.clusterId = clusterId;
			this.host = host;
			this.port = port;
		}

		public String getHost() {
			return host;
		}

		public void setHost(String host) {
			this.host = host;
		}

		public int getPort() {
			return port;
		}

		public void setPort(int port) {
			this.port = port;
		}

		public int getClusterId() {
			return clusterId;
		}

		public void setClusterId(int clusterId) {
			this.clusterId = clusterId;
		}

	}
}