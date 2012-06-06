package org.unbiquitous.driver.spike;

import static org.mockito.Mockito.*;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.log4j.Logger;
import org.luaj.vm2.LuaInteger;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.mockito.Mockito;
import org.unbiquitous.driver.execution.Agent;
import org.unbiquitous.driver.execution.CallValues;
import org.unbiquitous.driver.execution.ExecutionDriver;

import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;
import br.unb.unbiquitous.ubiquitos.uos.context.UOSApplicationContext;
import br.unb.unbiquitous.ubiquitos.uos.driverManager.DriverData;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpDevice;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall.ServiceType;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceResponse;

public class MoveSpike {

	private static final Logger logger = Logger.getLogger(MoveSpike.class);;

	public static void main(String[] args) throws Exception{
		
		HelloAgent hello = new HelloAgent();
//		hello.getClass().getResource(null);
		move();
//		System.out.println(hello.getClass()+"\t>>\t"+findClass(hello));
//		System.out.println(new ExecutionDriver().getClass()+"\t>>\t"+findClass(new ExecutionDriver()));
//		System.out.println(new UOSApplicationContext().getClass()+"\t>>\t"+findClass(new UOSApplicationContext()));
//		System.out.println(new Mockito().getClass()+"\t>>\t"+findClass(new Mockito()));
//		System.out.println(Class.class.getResourceAsStream(new Mockito().getClass().getName().replace('.', File.separatorChar)+".class"));
//		System.out.println(new StringBuffer().getClass()+"\t>>\t"+findClass(new StringBuffer()));
	}
	
	private static String findClass(Object a) throws FileNotFoundException, IOException{
		for (String classpathEntry : System.getProperty("java.class.path").split(System.getProperty("path.separator"))) {
			File entry = new File(classpathEntry);
			if (entry.isDirectory()){
				String found = findFile(a.getClass().getSimpleName(), entry);
				if (found != null)	return found;
			}else if (classpathEntry.endsWith(".jar")) {
				final int BUFFER = 2048;
				try {
			         FileInputStream fis = new FileInputStream(entry);
			         ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
			         ZipEntry j;
			         while((j = zis.getNextEntry()) != null) {
			            System.out.println("Extracting: " +j);
			            StringBuilder b = new StringBuilder();
			            byte data[] = new byte[BUFFER];
			            int count = 0;
			            while ((count = zis.read(data, 0, BUFFER)) != -1) {
			            	if (j.getName().endsWith(".html")){
			            		for (int i = 0 ; i < count ; i++)
			            			b.append((char)data[i]);
			            	}
			            }
			            if (j.getName().endsWith(".html")){
			            	System.out.println(entry);
			            	System.out.println(b.toString());
			            	return null;
			            }
			         }
			         zis.close();
			      } catch(Exception e) {
			         e.printStackTrace();
			      }
		    }else if (classpathEntry.endsWith(".class") && classpathEntry.startsWith(a.getClass().getSimpleName())){ //FIXME: lots of problems here
		    	return classpathEntry;
		    }
		}
		return null;
	}
	
	private static void spikeado(String path){
		final int BUFFER = 2048;
		try {
	         FileInputStream fis = new FileInputStream(path);
	         ZipInputStream zis = new ZipInputStream(new BufferedInputStream(fis));
	         ZipEntry entry;
	         while((entry = zis.getNextEntry()) != null) {
	            System.out.println("Extracting: " +entry);
	            StringBuilder b = new StringBuilder();
	            byte data[] = new byte[BUFFER];
	            while (zis.read(data, 0, BUFFER) != -1) {
	            	if (entry.getName().endsWith(".html")){
	            		b.append(new String(data));
	            	}
	            }
	            if (entry.getName().endsWith(".html")){
	            	System.out.println(path);
	            	System.out.println(b.toString());
	            	return;
	            }
	         }
	         zis.close();
	      } catch(Exception e) {
	         e.printStackTrace();
	      }
	}
	
	private static String findFile(String classname, File entry){
		if (entry.isDirectory()){
			for (File child : entry.listFiles()){
				String found = findFile(classname,child);
				if (found != null)	return found;
			}
		}else if (entry.getName().endsWith(".class") && entry.getName().startsWith(classname)){ //FIXME: lots of problems here
	    	return entry.getPath();
	    }
		return null;
	}
	
	public static void move() throws Exception{
		UOSApplicationContext u = new UOSApplicationContext();
		ResourceBundle prop = new ListResourceBundle() {
			protected Object[][] getContents() {
				return new Object[][] {
					{"ubiquitos.message.response.timeout", "100"}, //Optional
					{"ubiquitos.message.response.retry", "30"},//Optional
					{"ubiquitos.connectionManager", "br.unb.unbiquitous.ubiquitos.network.ethernet.connectionManager.EthernetTCPConnectionManager,br.unb.unbiquitous.ubiquitos.network.ethernet.connectionManager.EthernetUDPConnectionManager"},
					{"ubiquitos.radar","br.unb.unbiquitous.ubiquitos.network.ethernet.radar.EthernetPingRadar"},
					{"ubiquitos.eth.tcp.port", "14984"}, 
					{"ubiquitos.eth.tcp.passivePortRange", "14985-15000"}, 
					{"ubiquitos.eth.udp.port", "15001"}, 
					{"ubiquitos.eth.udp.passivePortRange", "15002-15017"}, 
					{"ubiquitos.uos.deviceName","LinuxAgentSource"},
					{"ubiquitos.persistence.hsqldb.database", "db_agent"}, 
					{"ubiquitos.driver.deploylist", "br.unb.unbiquitous.ubiquitos.uos.driver.DeviceDriverImpl"},//;org.unbiquitous.driver.execution.ExecutionDriver"}, 
		        };
			}
		};
		u.init(prop);
		HelloAgent hello = new HelloAgent();
		Gateway g = u.getGateway();
		List<DriverData> drivers = null;
		logger.debug("Searching Drivers.");
		do{
			drivers = g.listDrivers("uos.ExecutionDriver");
		}while(drivers == null || drivers.isEmpty());
		hello.init(u.getGateway());
//		hello.moveTo(new UpDevice("MacAgentTarget"));
		logger.debug("Start Moving.");
		moveTo(hello,drivers.get(0).getDevice(),g);
	}
	
	protected static void moveTo(Agent a, UpDevice to, Gateway g){
		
		ServiceCall move = new ServiceCall("uos.ExecutionDriver", "executeAgent");
		move.setChannels(2);
		move.setServiceType(ServiceType.STREAM);
		move.addParameter("class", a.getClass().getName());
		
		try {
			ServiceResponse r = g.callService(to, move);
			logger.debug("Opening agent stream.");
			ObjectOutputStream writer_agent = new ObjectOutputStream(r.getMessageContext().getDataOutputStream(0));
			logger.debug("Sending agent.");
			writer_agent.writeObject(a);
			writer_agent.close();
			logger.debug("Opening class stream.");
			OutputStream writer_class = r.getMessageContext().getDataOutputStream(1);
			final String classFile = findClass(a);
			logger.debug("Sending class "+classFile);
			InputStream clazz= new FileInputStream(classFile);
			logger.debug("Uffa.");
			int b;
			while((b = clazz.read())!= -1)
				writer_class.write(b);
			writer_class.close();
//			logger.debug("Waiting to finish.");
//			while (writer_agent.)
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void receive() throws Exception{
		UOSApplicationContext u = new UOSApplicationContext();
		ResourceBundle prop = new ListResourceBundle() {
			protected Object[][] getContents() {
				return new Object[][] {
					{"ubiquitos.message.response.timeout", "100"}, //Optional
					{"ubiquitos.message.response.retry", "30"},//Optional
					{"ubiquitos.connectionManager", "br.unb.unbiquitous.ubiquitos.network.ethernet.connectionManager.EthernetTCPConnectionManager,br.unb.unbiquitous.ubiquitos.network.ethernet.connectionManager.EthernetUDPConnectionManager"},
					//{"ubiquitos.radar","br.unb.unbiquitous.ubiquitos.network.ethernet.radar.EthernetArpRadar"},
					{"ubiquitos.eth.tcp.port", "14984"}, 
					{"ubiquitos.eth.tcp.passivePortRange", "14985-15000"}, 
					{"ubiquitos.eth.udp.port", "15001"}, 
					{"ubiquitos.eth.udp.passivePortRange", "15002-15017"}, 
					{"ubiquitos.uos.deviceName","MacAgentTarget"},
					{"ubiquitos.persistence.hsqldb.database", "db_agent"}, 
					{"ubiquitos.driver.deploylist", "br.unb.unbiquitous.ubiquitos.uos.driver.DeviceDriverImpl;org.unbiquitous.driver.execution.ExecutionDriver"}, 
		        };
			}
		};
		u.init(prop);
	}
}

