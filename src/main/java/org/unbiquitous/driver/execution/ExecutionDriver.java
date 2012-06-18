package org.unbiquitous.driver.execution;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectStreamClass;
import java.util.List;

import org.apache.log4j.Logger;
import org.luaj.vm2.LoadState;
import org.luaj.vm2.lib.jse.JsePlatform;

import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;
import br.unb.unbiquitous.ubiquitos.uos.application.UOSMessageContext;
import br.unb.unbiquitous.ubiquitos.uos.driverManager.UosDriver;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpDriver;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpService.ParameterType;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceResponse;

/**
 * This driver enables Code Mobility to the middleware.
 * Using it the Execution Capability of a Device is shared as a resource
 * for others to take advantge.
 * 
 * Its services are:
 * 
 * {@link #remoteExecution(ServiceCall, ServiceResponse, UOSMessageContext)}
 * 
 * This service expects a Lua script through the "code" parameter. Any 
 * other parameter is available for the execution of the scripted code.
 * Access to the parameters is done through the <code>get(<key>)</code> function.
 * The response parameters are set using the <code>set(<key>)</code> function.
 * 
 * {@link #executeAgent(ServiceCall, ServiceResponse, UOSMessageContext)}
 * 
 * This service receives a Serialized {@link Agent} on channel 0 and resumes its 
 * execution. No other class must be transfered, aside from the Agent itself, 
 * uOS classes and JDK classes. Any other class should be transient to the object.
 * 
 * In case of the Agent to be of a class not present on the targeted device.
 * The class can be transfered using the channel 1 and informing the class name
 * through the "class" parameter
 * 
 * @author Fabricio Nogueira Buzeto
 *
 */
public class ExecutionDriver implements UosDriver {

	private static final Logger logger = Logger.getLogger(ExecutionDriver.class);
	
	private long script_id = 0;

	private UpDriver driver;
	private Gateway gateway;
	private ClassToolbox util;
	
	public ExecutionDriver(){
		driver = new UpDriver("uos.ExecutionDriver");
		driver.addService("remoteExecution").addParameter("code", ParameterType.MANDATORY);
		driver.addService("executeAgent");
		util = new ClassToolbox(); //TODO: should be able to inject
	}
	
	public UpDriver getDriver() {	return driver;	}

	public void init(Gateway gateway, String instanceId) {this.gateway = gateway;}

	public void destroy() {}

	public List<UpDriver> getParent() {	return null;	}
	
	public void remoteExecution(ServiceCall call, ServiceResponse response,
			UOSMessageContext object) {
		try {
			script_id++;
			
			for(String key: call.getParameters().keySet()){
				UosLuaCall.values().setValue(script_id, key,call.getParameter(key));
			}
			
			StringBuffer script = new StringBuffer();
			script.append("UOS_ID="+(script_id)+"\n");
			script.append("require( '"+UosLuaCall.class.getName()+"' )\n");
			script.append("function set( key, value) \n");
			script.append("	Uos.set(UOS_ID,key,value)\n");
			script.append("end\n");
			script.append("function get( key) \n");
			script.append("	return Uos.get(UOS_ID,key)\n");
			script.append("end\n");
			script.append(call.getParameter("code"));
			InputStream file = new StringInputStream(script.toString());
			LoadState.load( file, "script_"+script_id, JsePlatform.standardGlobals() ).call();
			
			response.addParameter("value", UosLuaCall.values().getValue(script_id, "value"));
		} catch (IOException e) {
			logger.error("Error handling Execution call. Cause:",e);
			response.setError("Error handling Execution call. Cause:"+e.getMessage());
		}
	}

	public void executeAgent(ServiceCall call, ServiceResponse response,
			UOSMessageContext ctx) {
		try {
			if(ctx.getDataInputStream() == null){
				response.setError("No Data Stream, containing agent, was found.");
				return;
			}
			final DataInputStream agent = ctx.getDataInputStream();
			final DataInputStream clazz;
			final String className = call.getParameter("class");
			//TODO: should receive the size of these guys so i could avoid ... 
			//		caching problems. And even could check if the received data
			//		is in accordance.
			if (className != null){
				clazz = ctx.getDataInputStream(1);
			}else{
				clazz = null;
			}
			new Thread(new AgentHandler(className, clazz, agent)).start();
		} catch (Throwable e) {
			response.setError("Something unexpected happened.");
			logger.error(e);
		}
	}

	class AgentHandler implements Runnable{
		private String className;
		private DataInputStream clazz;
		private DataInputStream agent;
		
		public AgentHandler(String className, DataInputStream clazz,DataInputStream agent) {
			this.className = className;
			this.clazz = clazz;
			this.agent = agent;
		}

		public void run() {
			try {
				final ClassLoader loader;
				if (className != null){
					while (clazz.available() == 0){}
					loader = util.load(className, clazz);
				}else{
					loader = null;
				}
				while (agent.available() == 0){}
				ObjectInputStream reader = new ObjectInputStream(agent){
					@Override
					protected Class<?> resolveClass(ObjectStreamClass desc)
							throws IOException, ClassNotFoundException {
						try{
							return loader.loadClass(desc.getName());
						}catch(Exception e){
							return super.resolveClass(desc);
						}
					}
				};
				Object o = reader.readObject();
				if (o instanceof Agent){
					((Agent)o).run(gateway);
				}else{
					//response.setError("The informed Agent is not a valid one.");
				}
			} catch (Exception e) {
				logger.error(e);
			}
		};
	}
	
}
