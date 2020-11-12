package cm.homeautomation.network;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;

import javax.enterprise.context.ApplicationScoped;

import org.apache.logging.log4j.LogManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.runtime.Startup;
import io.quarkus.vertx.ConsumeEvent;

@Startup
@ApplicationScoped
public class WakeUpService {

	@ConfigProperty(name="network.wakeup.broadcast.address")
	private String BROADCAST_IP_ADDRESS = "192.168.1.255";
	@ConfigProperty(name="network.wakeup.broadcast.port")
	private int PORT = 9;

	@ConsumeEvent(value = "NetworkWakeUpEvent", blocking = true)
	public void wakeUp(NetworkWakeupEvent event) {
		System.out.println("Sending wakeup");
		try (DatagramSocket socket = new DatagramSocket();) {
			String macStr = event.getMac();
			final byte[] macBytes = getMacBytes(macStr);
			final byte[] bytes = new byte[6 + (16 * macBytes.length)];
			for (int i = 0; i < 6; i++) {
				bytes[i] = (byte) 0xff;
			}
			for (int i = 6; i < bytes.length; i += macBytes.length) {
				System.arraycopy(macBytes, 0, bytes, i, macBytes.length);
			}

			final InetAddress address = InetAddress.getByName(BROADCAST_IP_ADDRESS);
			final DatagramPacket packet = new DatagramPacket(bytes, bytes.length, address, PORT);

			for (int i = 0; i < 10; i++) {
				socket.send(packet);
				Thread.sleep(1000);
			}

			LogManager.getLogger(this.getClass()).info("Wake-on-LAN packet sent.");
			System.out.println("wakeup sent.");

		} catch (final Exception e) {
			e.printStackTrace();
			LogManager.getLogger(this.getClass()).info("Failed to send Wake-on-LAN packet: + e");
		}

	}

	private byte[] getMacBytes(final String macStr) {
		final byte[] bytes = new byte[6];
		final String[] hex = macStr.split("(\\:|\\-)");
		if (hex.length != 6) {
			throw new IllegalArgumentException("Invalid MAC address.");
		}
		try {
			for (int i = 0; i < 6; i++) {
				bytes[i] = (byte) Integer.parseInt(hex[i], 16);
			}
		} catch (final NumberFormatException e) {
			throw new IllegalArgumentException("Invalid hex digit in MAC address.");
		}
		return bytes;
	}
}
