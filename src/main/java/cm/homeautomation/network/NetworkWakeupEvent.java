package cm.homeautomation.network;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@RequiredArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NetworkWakeupEvent {

	@NonNull
	private String mac;

	public String getTitle() {

		return "Network Wakeup";
	}

	public String getMessageString() {

		return "Device with MAC: " + getMac() + " woken up.";
	}

}
