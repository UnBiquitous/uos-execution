package org.unbiquitous.driver.spike;

import java.io.File;
import java.io.FileInputStream;
import java.io.Serializable;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import org.unbiquitous.driver.execution.executeAgent.AgentUtil;
import org.unbiquitous.driver.execution.executeAgent.ClassToolbox;
import org.unbiquitous.uos.core.UOSApplicationContext;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;
import org.unbiquitous.uos.network.socket.connectionManager.EthernetTCPConnectionManager;
import org.unbiquitous.uos.network.socket.radar.EthernetPingRadar;

public class AndroidMoveSpike {

	public static void main(String[] args) throws Exception{
		UOSApplicationContext u = new UOSApplicationContext();
		ResourceBundle prop = new ListResourceBundle() {
			protected Object[][] getContents() {
				return new Object[][] {
					{"ubiquitos.message.response.timeout", "100"}, //Optional
					{"ubiquitos.message.response.retry", "30"},//Optional
					{"ubiquitos.connectionManager",
						EthernetTCPConnectionManager.class.getName()},
					{"ubiquitos.radar",
						EthernetPingRadar.class.getName()},
					{"ubiquitos.eth.tcp.port", "14984"}, 
					{"ubiquitos.eth.tcp.passivePortRange", "14985-15000"}, 
					{"ubiquitos.eth.udp.port", "15001"}, 
					{"ubiquitos.eth.udp.passivePortRange", "15002-15017"}, 
		        };
			}
		};
		u.init(prop);
		
		Gateway gate = u.getGateway();
		
		
		String agentClass = "org.unbiquitous.driver.execution.spike.HelloFromAndroidAgent";
		String jarFile = "auos.exe_spiker.jar";
		String apkFile = "auos.exe_spiker.apk";
		ClassToolbox box = new ClassToolbox();
		ClassLoader loader = box.load(new FileInputStream(jarFile));
       
        Class<?> clazz = loader.loadClass(agentClass);
        Serializable agent = (Serializable)clazz.newInstance();

        outter:
        while(true){
        	for (UpDevice device : gate.listDevices()){
        		if (!((String)device.getProperty("platform")).contains("Java")){
        			AgentUtil.getInstance().move(	agent, new File(apkFile), 
        					device, u.getGateway());
        			break outter;
        		}else{
        			System.out.println(String.format("Ignoring device %s", device));
        		}
        		System.out.println("Finished Round.");
        		Thread.sleep(5000);
        	}
        }
		
//		10.115.135.142

	}
	
}
