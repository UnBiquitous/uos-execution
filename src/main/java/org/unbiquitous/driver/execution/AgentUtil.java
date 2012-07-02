package org.unbiquitous.driver.execution;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Modifier;

import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.ServiceCallException;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpDevice;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall.ServiceType;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceResponse;

public class AgentUtil {

	private static ClassToolbox toolbox = new ClassToolbox();

	public static void move(Agent agent, UpDevice target, Gateway gateway) throws Exception {
		if (agent.getClass().getModifiers() != Modifier.PUBLIC)
			throw new RuntimeException("Agent class must be public");
		
		ServiceResponse r = callExecute(target, gateway);
		sendAgent(agent, r);
		if ("Dalvik".equalsIgnoreCase(target.getProperty("platform"))){
			sendDalvik(agent, r);
		}else{
			sendJar(agent, r);
		}
	}

	private static void sendJar(Agent agent, ServiceResponse r)
			throws Exception, FileNotFoundException, IOException {
		final File jar = toolbox.packageJarFor(agent.getClass());
		FileInputStream reader = new FileInputStream(jar);
		byte[] buff = new byte[1024];
		int read = 0;
		final DataOutputStream jar_writer = r.getMessageContext().getDataOutputStream(1);
		while ((read = reader.read(buff)) != -1){
			jar_writer.write(buff, 0, read);
		}
		jar_writer.close();
	}

	private static void sendDalvik(Agent agent, ServiceResponse r)
			throws Exception, FileNotFoundException, IOException {
		File jar = toolbox.packageDalvikFor(agent.getClass());
		FileInputStream reader = new FileInputStream(jar);
		byte[] buff = new byte[1024];
		int read = 0;
		final DataOutputStream jar_writer = r.getMessageContext().getDataOutputStream(1);
		while ((read = reader.read(buff)) != -1){
			jar_writer.write(buff, 0, read);
		}
		jar_writer.close();
	}
	
	private static void sendAgent(Agent agent, ServiceResponse r)
			throws IOException {
		ObjectOutputStream writer_agent = new ObjectOutputStream(
								r.getMessageContext().getDataOutputStream(0));
		writer_agent.writeObject(agent);
		writer_agent.close();
	}

	private static ServiceResponse callExecute(UpDevice target, Gateway gateway)
			throws ServiceCallException {
		ServiceCall execute = new ServiceCall( "uos.ExecutionDriver","executeAgent");
		execute.setChannels(2);
		execute.setServiceType(ServiceType.STREAM);
		execute.addParameter("jar", "true");

		ServiceResponse r = gateway.callService(target, execute);
		return r;
	}

}
