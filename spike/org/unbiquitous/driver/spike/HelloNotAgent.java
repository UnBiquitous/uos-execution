package org.unbiquitous.driver.spike;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.adaptabitilyEngine.NotifyException;
import org.unbiquitous.uos.core.adaptabitilyEngine.ServiceCallException;
import org.unbiquitous.uos.core.adaptabitilyEngine.UosEventListener;
import org.unbiquitous.uos.core.driverManager.DriverData;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;
import org.unbiquitous.uos.core.messageEngine.messages.Notify;
import org.unbiquitous.uos.core.messageEngine.messages.ServiceCall;
import org.unbiquitous.uos.core.messageEngine.messages.ServiceResponse;


public class HelloNotAgent implements Serializable {
	private static final long serialVersionUID = -2961159464632336104L;
	
	public void run(final Map gateway) {
		System.out.println("Eu sou um agente babaca que imprime "+gateway.get("CurrentDevice"));
		Logger.getLogger(HelloNotAgent.class.getName()).info("Eu sou um agente babaca que imprime "+gateway.get("CurrentDevice"));
		gateway.put("listDrivers", "driver");
	}
}
