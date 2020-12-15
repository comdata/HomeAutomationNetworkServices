package cm.homeautomation.mqtt.client;

import java.io.IOException;
import java.util.UUID;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.log4j.LogManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.context.ManagedExecutor;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;

import com.fasterxml.jackson.databind.ObjectMapper;

import cm.homeautomation.network.NetworkWakeupEvent;
import io.quarkus.runtime.StartupEvent;
import io.vertx.core.eventbus.EventBus;

@Singleton
public class ReactiveMQTTReceiverClient implements MqttCallback {

	@Inject
	EventBus bus;

	@ConfigProperty(name = "mqtt.host")
	String host;

	@ConfigProperty(name = "mqtt.port")
	int port;

	@Inject
	ManagedExecutor executor;

	private MqttClient client;

	private MemoryPersistence memoryPersistence = new MemoryPersistence();


	void startup(@Observes StartupEvent event) {
		initClient();

	}

	private void initClient() {
		try {
			System.out.println("Connecting MQTT");
			UUID uuid = UUID.randomUUID();
			String randomUUIDString = uuid.toString();

			client = new MqttClient("tcp://" + host + ":" + port, "HomeAutomation/" + randomUUIDString,
					memoryPersistence);

			client.setCallback(this);

			MqttConnectOptions connOpt = new MqttConnectOptions();
			connOpt.setAutomaticReconnect(true);
			connOpt.setCleanSession(false);
			connOpt.setKeepAliveInterval(10);
			connOpt.setConnectionTimeout(5);
			connOpt.setMaxInflight(10);
			connOpt.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);

			client.connect(connOpt);

			client.subscribe("networkServices/#");
			System.out.println("Connected to MQTT");
		} catch (MqttException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void connectionLost(Throwable cause) {

		try {
			client.close();
			client.disconnect();
		} catch (MqttException e1) {
			LogManager.getLogger(this.getClass()).error("force close failed.", e1);
		}
		LogManager.getLogger(this.getClass()).info("trying reconnect to MQTT broker");
		initClient();
	}

	@Override
	public void messageArrived(String topic, MqttMessage message) throws Exception {

		String messageContent = new String(message.getPayload());

		Runnable runThread = () -> {
			// Process the received message

			LogManager.getLogger(this.getClass()).debug("Topic: " + topic + " " + messageContent);
			System.out.println("Topic: " + topic + " " + messageContent);

			if (topic.startsWith("networkServices/wakeup")) {
				handleWOL(messageContent);
			}

			if (topic.equals("networkServices/scan")) {
				handleScan(messageContent);
			}

		};
		executor.runAsync(runThread);

	}

	private void handleScan(String messageContent) {
		// System.out.println("Got Network Scan request");
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			NetworkScanEvent networkScanEvent = objectMapper.readValue(messageContent, NetworkScanEvent.class);
			bus.publish("NetworkScanEvent", networkScanEvent);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	private void handleWOL(String messageContent) {
		System.out.println("sending a wakeup event");
		try {
			ObjectMapper objectMapper = new ObjectMapper();
			NetworkWakeupEvent networkWakeupEvent = objectMapper.readValue(messageContent, NetworkWakeupEvent.class);
			// System.out.println("Mac:" + networkWakeupEvent.getMac());
			bus.publish("NetworkWakeUpEvent", networkWakeupEvent);
			// System.out.println("Send wakeup event");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void deliveryComplete(IMqttDeliveryToken token) {
		// TODO Auto-generated method stub
		
	}

}
