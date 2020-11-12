package cm.homeautomation.network;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class NetworkScanResult {

	private Map<String, NetworkDevice> hosts;

	public void setHosts(Map<String, NetworkDevice> checkHosts) {
		this.hosts = checkHosts;

	}

	public Map<String, NetworkDevice> getHosts() {
		return hosts;
	}

}
