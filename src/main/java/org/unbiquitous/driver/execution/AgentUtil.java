package org.unbiquitous.driver.execution;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.ObjectOutputStream;

import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpDevice;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall.ServiceType;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceResponse;

public class AgentUtil {

	private static ClassToolbox toolbox = new ClassToolbox();

	public static void move(MyAgent myAgent, UpDevice target, Gateway gateway) throws Exception {
		ServiceCall execute = new ServiceCall( "uos.ExecutionDriver","executeAgent");
		execute.setChannels(2);
		execute.setServiceType(ServiceType.STREAM);
		execute.addParameter("jar", "true");

		ServiceResponse r = gateway.callService(target, execute);
		
		ObjectOutputStream writer_agent = new ObjectOutputStream(
								r.getMessageContext().getDataOutputStream(0));
		writer_agent.writeObject(myAgent);
		writer_agent.close();
		
		final File jar = toolbox.packageJarFor(myAgent.getClass());
		FileInputStream reader = new FileInputStream(jar);
		byte[] buff = new byte[1024];
		int read = 0;
		final DataOutputStream jar_writer = r.getMessageContext().getDataOutputStream(1);
		while ((read = reader.read(buff)) != -1){
			jar_writer.write(buff, 0, read);
		}
		jar_writer.close();
	}

}
