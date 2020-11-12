package cm.homeautomation.ssh.client;

import java.io.ByteArrayOutputStream;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.jcraft.jsch.ChannelExec;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;

import cm.homeautomation.mqtt.client.MQTTSendEvent;
import io.quarkus.runtime.Startup;
import io.quarkus.vertx.ConsumeEvent;
import io.vertx.core.eventbus.EventBus;

@Startup
@Singleton
public class SshCommandClient {
	
	@Inject
	EventBus bus;

	@ConsumeEvent(value = "SSHCommand", blocking=true)
	public void callSshCommand(SSHCommand sshCommand) throws Exception {
		
		String username=sshCommand.getUserName();
		String password=sshCommand.getPassword();
		String host=sshCommand.getHost();
		int port=sshCommand.getPort();
		String command=sshCommand.getCommand();

		Session session = null;
	    ChannelExec channel = null;
	    
	    try {
	        session = new JSch().getSession(username, host, port);
	        session.setPassword(password);
	        session.setConfig("StrictHostKeyChecking", "no");
	        session.connect();
	        
	        channel = (ChannelExec) session.openChannel("exec");
	        channel.setCommand(command);
	        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
	        channel.setOutputStream(responseStream);
	        channel.connect();
	        
	        while (channel.isConnected()) {
	            Thread.sleep(100);
	        }
	        
	        String responseString = new String(responseStream.toByteArray());
	        

			bus.publish("MQTTSendEvent", new MQTTSendEvent("networkServices/sshCommandResult", responseString));

	        
	        System.out.println(responseString);
	    } finally {
	        if (session != null) {
	            session.disconnect();
	        }
	        if (channel != null) {
	            channel.disconnect();
	        }
	    }
	}
}
