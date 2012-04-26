package org.unbiqitous.driver.execution;

import java.io.Serializable;

import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;

public interface Agent extends Serializable{
	
	public void run(Gateway gateway);
}
