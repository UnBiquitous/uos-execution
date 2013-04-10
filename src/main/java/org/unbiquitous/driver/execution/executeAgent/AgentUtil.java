package org.unbiquitous.driver.execution.executeAgent;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.lang.reflect.Modifier;

import br.unb.unbiquitous.ubiquitos.Logger;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.ServiceCallException;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpDevice;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall.ServiceType;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceResponse;

//TODO: Doc
public class AgentUtil {

	private static ClassToolbox toolbox = new ClassToolbox();

	public static void move(Agent agent, File pkg, UpDevice target, Gateway gateway) throws Exception {
		toolbox.setPackageFor(agent.getClass(), pkg);
		move(agent, target, gateway);
	}
	
	public static void move(Agent agent, UpDevice target, Gateway gateway) throws Exception {
		if (agent.getClass().getModifiers() != Modifier.PUBLIC)
			throw new RuntimeException("Agent class must be public");
		
		ServiceResponse r = callExecute(target, gateway);
		sendAgent(agent, r);
		Logger.getLogger(AgentUtil.class).debug("Target platform is: "+target.getProperty("platform"));
		if ("Dalvik".equalsIgnoreCase((String)target.getProperty("platform"))){
			sendDalvik(agent, r);
		}else{
			sendJar(agent, r);
		}
	}

	private static void sendJar(Agent agent, ServiceResponse r) throws Exception{
		sendPackage(r, toolbox.packageJarFor(agent.getClass()));
	}

	private static void sendDalvik(Agent agent, ServiceResponse r) throws Exception{
		sendPackage(r, toolbox.packageDalvikFor(agent.getClass()));
	}

	private static void sendPackage(ServiceResponse r, File pkg) throws IOException {
		FileInputStream reader = new FileInputStream(pkg);
		byte[] buff = new byte[1024];
		int read = 0;
		final DataOutputStream jar_writer = r.getMessageContext().getDataOutputStream(1);
		while ((read = reader.read(buff)) != -1){
			jar_writer.write(buff, 0, read);
		}
		jar_writer.close();
		reader.close();
	}
	
	private static void sendAgent(Agent agent, ServiceResponse r) throws IOException {
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
