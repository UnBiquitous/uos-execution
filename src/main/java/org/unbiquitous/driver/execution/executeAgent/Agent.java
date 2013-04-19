package org.unbiquitous.driver.execution.executeAgent;

import java.io.Serializable;

import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;


public abstract class Agent implements Serializable{
	private static final long serialVersionUID = 8386943272111021563L;

	@SuppressWarnings("unused")
	transient private Gateway gateway; 
	
	public abstract void run(Gateway gateway);
	
	public void init(Gateway gateway){	this.gateway = gateway;	}
	
}
