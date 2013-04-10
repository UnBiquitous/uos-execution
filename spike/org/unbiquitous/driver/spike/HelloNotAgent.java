package org.unbiquitous.driver.spike;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.NotifyException;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.ServiceCallException;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.UosEventListener;
import br.unb.unbiquitous.ubiquitos.uos.driverManager.DriverData;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpDevice;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.Notify;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceResponse;

public class HelloNotAgent implements Serializable {
	private static final long serialVersionUID = -2961159464632336104L;
	
	public void run(final Map gateway) {
		System.out.println("Eu sou um agente babaca que imprime "+gateway.get("CurrentDevice"));
		Logger.getLogger(HelloNotAgent.class.getName()).info("Eu sou um agente babaca que imprime "+gateway.get("CurrentDevice"));
		gateway.put("listDrivers", "driver");
	}
}
