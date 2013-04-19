package org.unbiquitous.driver.spike;

import java.io.Serializable;
import java.util.logging.Logger;

import org.unbiquitous.driver.execution.executeAgent.Agent;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;


public class HelloAgent extends Agent implements Serializable{
	private static final long serialVersionUID = -5711645697892564502L;
	public void run(Gateway gateway) {
//		JOptionPane.showConfirmDialog(null , "It's alive !!!");
		while(true){
			System.out.println("Eu sou um agente babaca que imprime "+gateway.getCurrentDevice());
			Logger.getLogger(HelloAgent.class.getName()).info("Eu sou um agente babaca que imprime "+gateway.getCurrentDevice());
		}
	}
}