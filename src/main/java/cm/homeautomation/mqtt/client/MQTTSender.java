package cm.homeautomation.mqtt.client;

import java.util.UUID;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.hivemq.client.mqtt.MqttClient;
import com.hivemq.client.mqtt.datatypes.MqttQos;
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient;
import com.hivemq.client.mqtt.mqtt3.message.publish.Mqtt3Publish;

import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.eventbus.EventBus;

/**
 * generic MQTT sender
 * 
 * @author christoph
 *
 */
@Singleton
public class MQTTSender {

	private Mqtt3AsyncClient publishClient = null;
	
	@ConfigProperty(name = "mqtt.host")
	String host;
	
	@ConfigProperty(name="mqtt.port")
	int port;

	@Inject
	EventBus bus;

	public MQTTSender() {
	}

	private void initClient() {
		if (publishClient == null) {

			publishClient = MqttClient.builder().useMqttVersion3().identifier(UUID.randomUUID().toString())
					.serverHost(host).serverPort(port).automaticReconnect().applyAutomaticReconnect().buildAsync();

			publishClient.connect().whenComplete((connAck, throwable) -> {
				if (throwable != null) {
					// Handle connection failure
				} else {

				}
			});
		}

		if (!publishClient.getState().isConnectedOrReconnect()) {
			publishClient.connect();
		}
	}

	public void sendMQTTMessage(String topic, String messagePayload) {
		doSendSyncMQTTMessage(topic, messagePayload);
	}

	public void sendSyncMQTTMessage(String topic, String messagePayload) {
		doSendSyncMQTTMessage(topic, messagePayload);
	}

	public void doSendSyncMQTTMessage(String topic, String messagePayload) {
		//System.out.println("MQTT OUTBOUND " + topic + " " + messagePayload);
	
		bus.publish("MQTTSendEvent", new MQTTSendEvent(topic, messagePayload));

	}

	@ConsumeEvent(value = "MQTTSendEvent", blocking=true)
	public void send(MQTTSendEvent mqttSendEvent) {
		String topic = mqttSendEvent.getTopic();
		String messagePayload = mqttSendEvent.getPayload();

		initClient();
		Mqtt3Publish publishMessage = Mqtt3Publish.builder().topic(topic).qos(MqttQos.AT_LEAST_ONCE)
				.payload(messagePayload.getBytes()).build();
		publishClient.publish(publishMessage);
	}
}