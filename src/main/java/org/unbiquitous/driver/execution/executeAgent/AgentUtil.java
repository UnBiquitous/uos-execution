package org.unbiquitous.driver.execution.executeAgent;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Modifier;

import org.unbiquitous.uos.core.Logger;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.adaptabitilyEngine.ServiceCallException;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;
import org.unbiquitous.uos.core.messageEngine.messages.ServiceCall;
import org.unbiquitous.uos.core.messageEngine.messages.ServiceResponse;
import org.unbiquitous.uos.core.messageEngine.messages.ServiceCall.ServiceType;


//TODO: Doc
public class AgentUtil {

	private ClassToolbox toolbox = new ClassToolbox();
	private static AgentUtil instance;
	
	public static AgentUtil getInstance(){
		if (AgentUtil.instance != null) return AgentUtil.instance;
		return new AgentUtil(); 
	}
	public static void setInstance(AgentUtil instance){
		AgentUtil.instance = instance;
	}
	

	public void move(Serializable agent, File pkg, UpDevice target, Gateway gateway) throws Exception {
		toolbox.setPackageFor(agent.getClass(), pkg);
		move(agent, target, gateway);
	}
	
	public void move(Serializable agent, UpDevice target, Gateway gateway) throws Exception {
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

	private void sendJar(Serializable agent, ServiceResponse r) throws Exception{
		sendPackage(r, toolbox.packageJarFor(agent.getClass()));
	}

	private void sendDalvik(Serializable agent, ServiceResponse r) throws Exception{
		sendPackage(r, toolbox.packageDalvikFor(agent.getClass()));
	}

	private void sendPackage(ServiceResponse r, File pkg) throws IOException {
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
	
	private void sendAgent(Serializable agent, ServiceResponse r) throws IOException {
		ObjectOutputStream writer_agent = new ObjectOutputStream(
								r.getMessageContext().getDataOutputStream(0));
		writer_agent.writeObject(agent);
		writer_agent.close();
	}

	private ServiceResponse callExecute(UpDevice target, Gateway gateway)
			throws ServiceCallException {
		ServiceCall execute = new ServiceCall( "uos.ExecutionDriver","executeAgent");
		execute.setChannels(2);
		execute.setServiceType(ServiceType.STREAM);
		execute.addParameter("jar", "true");

		ServiceResponse r = gateway.callService(target, execute);
		return r;
	}

}
