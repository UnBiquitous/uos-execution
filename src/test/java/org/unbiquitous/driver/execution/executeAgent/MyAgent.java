package org.unbiquitous.driver.execution.executeAgent;


import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;

public class MyAgent extends Agent{
	
	public static class AgentSpy{
		public static Integer count = 0;
		public static MyAgent lastAgent = null;
	}
	
	private static final long serialVersionUID = -8267793981973238896L;
	public Integer sleepTime = 0;
	public Gateway gateway;
	public void run(Gateway gateway){
		AgentSpy.lastAgent = this;
		this.gateway = gateway; 
		try {	Thread.sleep(sleepTime);	} catch (Exception e) {}
		AgentSpy.count++;
	}
}
