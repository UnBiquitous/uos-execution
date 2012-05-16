package org.unbiquitous.driver.execution.dummy;

import org.unbiquitous.driver.execution.Agent;
import org.unbiquitous.driver.execution.ExecutionDriverTest_executeAgent;

import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;

/**
 *	This class is just a Dummy used at the {@link ExecutionDriverTest_executeAgent}
 *  
 * @author Fabricio Nogueira Buzeto
 */
public class DummyAgent extends Agent {
	private static final long serialVersionUID = -6366707922217209685L;

	public void run(Gateway gateway) {}

}
