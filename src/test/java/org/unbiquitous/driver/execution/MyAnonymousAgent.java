package org.unbiquitous.driver.execution;

import java.io.Serializable;

import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;

public class MyAnonymousAgent implements Serializable {
	private static final long serialVersionUID = 7764471511806687355L;
	
	public static class AgentSpy{
		public static Integer count = 0;
		public static MyAnonymousAgent lastAgent = null;
	}
	
	public Integer sleepTime = 0;
	public Gateway gateway;
	public void run(Gateway gateway){
		AgentSpy.lastAgent = this;
		this.gateway = gateway; 
		try {	Thread.sleep(sleepTime);	} catch (Exception e) {}
		AgentSpy.count++;
	}
}
