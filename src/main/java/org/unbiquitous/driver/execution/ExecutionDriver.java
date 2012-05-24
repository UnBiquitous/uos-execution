package org.unbiquitous.driver.execution;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

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

import com.sun.org.apache.bcel.internal.classfile.ClassParser;
import com.sun.org.apache.bcel.internal.classfile.JavaClass;

public class ExecutionDriver implements UosDriver {

	private static final Logger logger = Logger.getLogger(ExecutionDriver.class);
	
	private long script_id = 0;

	private UpDriver driver;
	private Gateway gateway;
	
	public ExecutionDriver(){
		driver = new UpDriver("uos.ExecutionDriver");
		driver.addService("remoteExecution").addParameter("code", ParameterType.MANDATORY);
		driver.addService("executeAgent");
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
			if (className != null){
				clazz = ctx.getDataInputStream(1);
			}else{
				clazz = null;
			}
			new Thread(new AgentHandler(className, clazz, agent)).start();
		} catch (Throwable e) {
			response.setError("Something unexpected happened.");
			e.printStackTrace();
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
				if (className != null){
					while (clazz.available() == 0){}
					load(className, clazz);
				}
				while (agent.available() == 0){}
				ObjectInputStream reader = new ObjectInputStream(agent);
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
	
	protected InputStream findClass(Class clazz) throws IOException {
		String className = clazz.getName().replace('.', File.separatorChar);
		for (String entryPath : System.getProperty("java.class.path").split(System.getProperty("path.separator"))) {
			File entry = new File(entryPath);
			if (entry.isDirectory()){
				File found = findClassFileOnDir(className, entry);
				if (found != null)	return new FileInputStream(found);
			}else if (entry.getName().endsWith(".jar")) {
				InputStream result = findClassFileOnAJar(className, entry);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
	}

	private InputStream findClassFileOnAJar(String className, File jar) throws FileNotFoundException, IOException {
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(jar)));
		ZipEntry j;
		List<Byte> bytes = new ArrayList<Byte>();
		while ((j = zis.getNextEntry()) != null) {
			final int BUFFER = 2048;
			byte data[] = new byte[BUFFER];
			int count = 0;
			while ((count = zis.read(data, 0, BUFFER)) != -1) {
				if (j.getName().endsWith(className + ".class")) {
					for (int i = 0; i < count; i++)
						bytes.add(data[i]);
				}
			}
			if (!bytes.isEmpty()) {
				byte[] buf = new byte[bytes.size()];
				for (int i = 0; i < bytes.size(); i++)
					buf[i] = bytes.get(i);
				zis.close();
				return new ByteArrayInputStream(buf);
			}

		}
		zis.close();
		return null;
	}

	private File findClassFileOnDir(String classname, File entry) throws IOException{
		if (entry.isDirectory()){
			for (File child : entry.listFiles()){
				File found = findClassFileOnDir(classname,child);
				if (found != null)	return found;
			}
		}else if (entry.getPath().endsWith(classname+".class")){
			return entry;
	    }
		return null;
	}
	
	@SuppressWarnings("rawtypes")
	protected Object load(String className, InputStream clazz) throws Exception {
		File classDir = createClassFileDir(className, clazz);
		
		addPathToClassLoader(classDir);
		
		Class c = Class.forName(className);
		return c.newInstance();
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void addPathToClassLoader(File classDir)
			throws NoSuchMethodException, IllegalAccessException,
			InvocationTargetException, MalformedURLException {
		URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		Class sysclass = URLClassLoader.class;
		Method method = sysclass.getDeclaredMethod("addURL", URL.class);
		method.setAccessible(true);
		method.invoke(sysloader, new Object[] { classDir.toURI().toURL() });
	}

	private File createClassFileDir(String className, InputStream clazz)
			throws IOException, FileNotFoundException {
		File tempDir = File.createTempFile("uExeTmp", ""+System.nanoTime());
		tempDir.delete(); // Delete temp file
		tempDir.mkdir();  // and transform it to a directory
		
		File classFile = new File(tempDir.getPath()+"/"+className.replace('.', '/')+".class");
		classFile.getParentFile().mkdirs();
		classFile.createNewFile();
		FileOutputStream writer = new FileOutputStream(classFile);
		int b = 0;
		while((b = clazz.read()) != -1) writer.write(b);
		writer.close();
		return tempDir;
	}
}
