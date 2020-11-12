package cm.homeautomation.ssh.client;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SSHCommand {
	String userName;
	String password;
	String host;
	int port;
	String command;
}
