package org.unbiquitous.driver.spike;

import java.io.Serializable;

import org.unbiquitous.driver.execution.Agent;

import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;

class HelloAgent extends Agent implements Serializable{
	private static final long serialVersionUID = -5711645697892564502L;
	public void run(Gateway gateway) {
		while(true){
			System.out.println("Eu sou um agente babaca que imprime "+gateway.getCurrentDevice());
		}
	}
}