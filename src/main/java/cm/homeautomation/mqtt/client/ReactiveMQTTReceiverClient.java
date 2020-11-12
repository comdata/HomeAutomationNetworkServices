package cm.homeautomation.mqtt.client;

import java.util.UUID;

import javax.enterprise.event.Observes;
import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.log4j.LogManager;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;

import io.quarkus.runtime.StartupEvent;
import io.vertx.core.eventbus.EventBus;

@Singleton
public class ReactiveMQTTReceiverClient {
	private static ObjectMapper mapper = new ObjectMapper();

	@Inject
	EventBus bus;
	
	@ConfigProperty(name = "mqtt.host")
	String host;
	
	@ConfigProperty(name="mqtt.port")
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

//					topicFilter("zigbee2mqtt").topicFilter("ebusd")

				client.subscribeWith().topicFilter("networkServices/#").callback(publish -> {

					Runnable runThread = () -> {
						// Process the received message

						String topic = publish.getTopic().toString();
						String messageContent = new String(publish.getPayloadAsBytes());
						LogManager.getLogger(this.getClass()).debug("Topic: " + topic + " " + messageContent);

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

}
