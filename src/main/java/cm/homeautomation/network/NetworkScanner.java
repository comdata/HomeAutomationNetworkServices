package cm.homeautomation.network;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import org.apache.logging.log4j.LogManager;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import cm.homeautomation.mqtt.client.MQTTSendEvent;
import cm.homeautomation.mqtt.client.NetworkScanEvent;
import io.quarkus.runtime.Startup;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.eventbus.EventBus;

@Startup
@ApplicationScoped
public class NetworkScanner {

	@Inject
	EventBus bus;

	private Map<String, NetworkDevice> availableHosts;

	private static Map<String, Boolean> runningScans = new HashMap<>();

	@ConsumeEvent(value = "NetworkScanEvent", blocking = true)
	public void scanNetwork(NetworkScanEvent event) {
		scanNetworkInternal(event.getSubnet());
	}

	public Map<String, NetworkDevice> getAvailableHosts() {
		return availableHosts;
	}

	public void setAvailableHosts(Map<String, NetworkDevice> availableHosts) {
		this.availableHosts = availableHosts;
	}

	/**
	 * check for the availability of a host
	 * 
	 * @param subnet
	 * @return
	 */
	public Map<String, NetworkDevice> checkHosts(String subnet) {
		System.out.println("Checking subnet: " + subnet);
		setAvailableHosts(new HashMap<>());

		int timeout = 200;
		for (int i = 1; i < 255; i++) {
			String host = subnet + "." + i;

			Runnable runner = () -> {
				try {
					InetAddress currentHost = InetAddress.getByName(host);

					// System.err.println("current host: " + currentHost);
					if (currentHost.isReachable(timeout)) {
						LogManager.getLogger(this.getClass()).info(host + " is reachable");

						String macFromArpCache = getMacFromArpCache(host);

						String key = host + "-" + macFromArpCache;
						if (!getAvailableHosts().keySet().contains(key)) {
							// System.out.println("new host: " + host);
							NetworkDevice device = new NetworkDevice();
							device.setIp(host);
							device.setHostname(currentHost.getHostName());

							device.setMac(macFromArpCache);

							getAvailableHosts().put(key, device);

						}
					}
				} catch (IOException e) {
					LogManager.getLogger(this.getClass()).info(e);
				}
			};
			new Thread(runner).start();

		}

		return getAvailableHosts();
	}

	/**
	 * Try to extract a hardware MAC address from a given IP address using the ARP
	 * cache (/proc/net/arp).<br>
	 * <br>
	 * We assume that the file has this structure:<br>
	 * <br>
	 * IP address HW type Flags HW address Mask Device 192.168.18.11 0x1 0x2
	 * 00:04:20:06:55:1a * eth0 192.168.18.36 0x1 0x2 00:22:43:ab:2a:5b * eth0
	 *
	 * @param ip
	 * @return the MAC from the ARP cache
	 */
	private String getMacFromArpCache(String ip) {
		if (ip == null) {
			return null;
		}

		try (FileReader fr = new FileReader("/proc/net/arp"); BufferedReader br = new BufferedReader(fr)) {

			String line;
			while ((line = br.readLine()) != null) {
				String[] splitted = line.split(" +");
				if (splitted != null && splitted.length >= 4 && ip.equals(splitted[0])) {
					// Basic sanity check
					String mac = splitted[3];
					if (mac.matches("..:..:..:..:..:..")) {
						if ("00:00:00:00:00:00".equals(mac)) {
							return null;
						} else {

							return mac;
						}
					} else {
						return null;
					}
				}
			}
		} catch (Exception e) {
			LogManager.getLogger(this.getClass()).info(e);
		}
		return null;
	}

	public void scanNetworkInternal(String subnet) {

		ObjectMapper mapper = new ObjectMapper();

		Boolean scanRunningObject = runningScans.get(subnet);

		if (scanRunningObject == null) {
			runningScans.put(subnet, Boolean.valueOf(false));
			scanRunningObject = runningScans.get(subnet);
		}

		if (!scanRunningObject.booleanValue()) {
			try {
				runningScans.put(subnet, Boolean.valueOf(true));

				Map<String, NetworkDevice> checkHosts = checkHosts(subnet);

				NetworkScanResult data = new NetworkScanResult();
				data.setHosts(checkHosts);

				String payload = mapper.writeValueAsString(data);

				bus.publish("MQTTSendEvent", new MQTTSendEvent("networkServices/scanResult", payload));

				runningScans.put(subnet, Boolean.valueOf(false));

			} catch (JsonProcessingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}
}
