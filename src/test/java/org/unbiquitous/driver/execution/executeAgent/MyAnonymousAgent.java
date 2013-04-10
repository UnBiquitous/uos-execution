package org.unbiquitous.driver.execution.executeAgent;

import java.io.Serializable;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class MyAnonymousAgent implements Serializable {
	private static final long serialVersionUID = 7764471511806687355L;
	
	public static class Spy{
		public static Integer count = 0;
		public static MyAnonymousAgent lastAgent = null;
	}
	
	public Integer sleepTime = 0;
	public Map gateway;
	public void run(Map gateway){
		Spy.lastAgent = this;
		this.gateway = gateway; 
		try {	Thread.sleep(sleepTime);	} catch (Exception e) {}
		Spy.count++;
	}
}
