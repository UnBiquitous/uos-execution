package org.unbiquitous.driver.spike;

import java.io.Serializable;
import java.util.Map;
import java.util.logging.Logger;


public class HelloNotAgent implements Serializable {
	private static final long serialVersionUID = -2961159464632336104L;
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void run(final Map gateway) {
		System.out.println("Eu sou um agente babaca que imprime "+gateway.get("CurrentDevice"));
		Logger.getLogger(HelloNotAgent.class.getName()).info("Eu sou um agente babaca que imprime "+gateway.get("CurrentDevice"));
		gateway.put("listDrivers", "driver");
	}
}
