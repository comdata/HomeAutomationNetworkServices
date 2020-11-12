package cm.homeautomation.mqtt.client;

import java.io.IOException;
import java.util.UUID;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.log4j.LogManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

import cm.homeautomation.network.NetworkWakeupEvent;
import cm.homeautomation.ssh.client.SSHCommand;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.eventbus.EventBus;

@Singleton
public class ReactiveMQTTReceiverClient {

	@Inject
	EventBus bus;

	@ConfigProperty(name = "mqtt.host")
	String host;

	@ConfigProperty(name = "mqtt.port")
	int port;

	private Mqtt3AsyncClient buildAClient(String host, int port) {
		return MqttClient.builder().useMqttVersion3().identifier(UUID.randomUUID().toString()).serverHost(host)
				.serverPort(port).automaticReconnect().applyAutomaticReconnect().buildAsync();
	}

	void startup(@Observes StartupEvent event) {
		initClient();

	}

	private void initClient() {

		Mqtt3AsyncClient client = buildAClient(host, port);

		client.connect().whenComplete((connAck, throwable) -> {
			if (throwable != null) {
				// Handle connection failure
			} else {
				client.subscribeWith().topicFilter("networkServices/#").callback(publish -> {

					Runnable runThread = () -> {
						// Process the received message

						String topic = publish.getTopic().toString();
						String messageContent = new String(publish.getPayloadAsBytes());
						LogManager.getLogger(this.getClass()).debug("Topic: " + topic + " " + messageContent);
						System.out.println("Topic: " + topic + " " + messageContent);

						if (topic.startsWith("networkServices/wakeup")) {
							handleWOL(messageContent);
						}

						if (topic.equals("networkServices/scan")) {
							handleScan(messageContent);
						}
						
						if (topic.equals("networkServices/sshCommand")) {
							handleSshCommand(messageContent);
						}


					};
					new Thread(runThread).start();
				}).send().whenComplete((subAck, e) -> {
					if (e != null) {
						// Handle failure to subscribe
						LogManager.getLogger(this.getClass()).error(e);
					} else {
						// Handle successful subscription, e.g. logging or incrementing a metric
						LogManager.getLogger(this.getClass())
								.debug("successfully subscribed. Type: " + subAck.getType().name());
					}
				});
			}
		});

	}

	private void handleScan(String messageContent) {
		System.out.println("Got Network Scan request");
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			NetworkScanEvent networkScanEvent = objectMapper.readValue(messageContent, NetworkScanEvent.class);
			bus.publish("NetworkScanEvent", networkScanEvent);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void handleSshCommand(String messageContent) {
		System.out.println("Got Ssh request");
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			SSHCommand sshCommandEvent = objectMapper.readValue(messageContent, SSHCommand.class);
			bus.publish("SSHCommand", sshCommandEvent);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private void handleWOL(String messageContent) {
		System.out.println("sending a wakeup event");
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			NetworkWakeupEvent networkWakeupEvent = objectMapper.readValue(messageContent, NetworkWakeupEvent.class);
			System.out.println("Mac:" + networkWakeupEvent.getMac());
			bus.publish("NetworkWakeUpEvent", networkWakeupEvent);
			System.out.println("Send wakeup event");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
