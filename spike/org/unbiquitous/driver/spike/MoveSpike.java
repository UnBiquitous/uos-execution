package org.unbiquitous.driver.spike;

import java.util.List;
import java.util.ListResourceBundle;
import java.util.ResourceBundle;

import org.unbiquitous.driver.execution.executeAgent.AgentUtil;
import org.unbiquitous.uos.core.Logger;
import org.unbiquitous.uos.core.UOSApplicationContext;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.driverManager.DriverData;


public class MoveSpike {

	private static final Logger logger = Logger.getLogger(MoveSpike.class);

	public static void main(String[] args) throws Exception{
		move();
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
		logger.debug("Start Moving to "+drivers.get(0).getDevice());
		AgentUtil.move(hello, drivers.get(0).getDevice(), g);
//		moveTo(hello,drivers.get(0).getDevice(),g);
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