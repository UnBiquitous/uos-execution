package org.unbiquitous.driver.execution.executeAgent;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.logging.Logger;

import org.unbiquitous.json.JSONArray;
import org.unbiquitous.uos.core.UOSLogging;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.adaptabitilyEngine.ServiceCallException;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;
import org.unbiquitous.uos.core.messageEngine.messages.ServiceCall;
import org.unbiquitous.uos.core.messageEngine.messages.ServiceCall.ServiceType;
import org.unbiquitous.uos.core.messageEngine.messages.ServiceResponse;


/**
 * Class used to migrate agents between devices:
 * The various versions of AgentUtil.move() are responsible for doing this job.
 * 
 * {@link #move(Serializable, UpDevice, Gateway)} : 
 * 		Ensures that the agent will be moved to the target device.
 * 		The target package will be automatically created.
 * 
 * {@link #move(Serializable, File, UpDevice, Gateway)} :
 * 		Sends the agent to the target device along with the informed package.
 * 
 * {@link #move(Serializable, UpDevice, Gateway, boolean)} :
 * 		Works just like the move, but you can specify if no package is 
 * 		needed to me sent (usually when the same classpath is shared among
 * 		devices).
 * 
 * @author Fabricio Buzeto
 *
 */

public class AgentUtil {

	private static final Logger logger = UOSLogging.getLogger();
	
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
		move(agent, target, gateway, true);
	}
	
	public void move(Serializable agent, UpDevice target, Gateway gateway, boolean sendPackage) throws Exception {
		if (agent.getClass().getModifiers() != Modifier.PUBLIC)
			throw new RuntimeException("Agent class must be public");
		
		ServiceCall listKnownClasses = new ServiceCall( "uos.ExecutionDriver","listKnownClasses");
		ServiceResponse rl = gateway.callService(target, listKnownClasses);
		List<String> knownClasses ;
		if(rl.getResponseData("classes") instanceof JSONArray){
			knownClasses = ((JSONArray) rl.getResponseData("classes")).toArray();
		}else{
			knownClasses = (List<String>) rl.getResponseData("classes");
		}
		
		ServiceResponse r = callExecute(target, gateway);
		sendAgent(agent, r);
		if (sendPackage){
			sendPackage(agent, target, r, knownClasses);
		}
	}
	private void sendPackage(Serializable agent, UpDevice target,
			ServiceResponse r, List<String> knownClasses) throws Exception {
		logger.fine("Target platform is: "+target.getProperty("platform"));
		if ("Dalvik".equalsIgnoreCase((String)target.getProperty("platform"))){
			sendDalvik(agent, r, knownClasses);
		}else{
			sendJar(agent, r, knownClasses);
		}
	}

	private void sendJar(Serializable agent, ServiceResponse r, List<String> knownClasses) throws Exception{
		sendPackage(r, toolbox.packageJarFor(agent.getClass(), knownClasses));
	}

	private void sendDalvik(Serializable agent, ServiceResponse r, List<String> knownClasses) throws Exception{
		sendPackage(r, toolbox.packageDalvikFor(agent.getClass(), knownClasses));
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
