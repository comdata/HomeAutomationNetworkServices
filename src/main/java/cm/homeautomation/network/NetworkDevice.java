package cm.homeautomation.network;

import java.util.Date;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties
@Getter
@Setter
public class NetworkDevice {


	private Long id;
	private String mac;
	private String ip;
	private String hostname;
	private Date lastSeen;

}
