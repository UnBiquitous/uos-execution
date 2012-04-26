package org.unbiquitous.driver.execution;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;

import org.junit.Before;
import org.junit.Test;
import org.unbiquitous.driver.execution.ExecutionDriver;

import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;
import br.unb.unbiquitous.ubiquitos.uos.application.UOSMessageContext;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceResponse;

public class ExecutionDriverTest_executeAgent {

	private ExecutionDriver driver;
	private ServiceResponse response;
	
	@Before public void setUp(){
		driver = new ExecutionDriver();
		response = new ServiceResponse();
		
	}
	
	@Test public void runTheCalledAgentOnAThread() throws Exception{
		MyAgent a = new MyAgent();
		a.sleepTime = 1000;
		
		Integer before = AgentSpy.count;
		
		driver.executeAgent(null,response,createAgentMockContext(a));
		
		assertNull("No error should be found.",response.getError());
		assertEquals((Integer)(before),AgentSpy.count);
		Thread.sleep(a.sleepTime + 500);
		assertEquals((Integer)(before+1),AgentSpy.count);
	}

	@Test public void dontAcceptANonAgentAgent() throws Exception{
		NonAgent a = new NonAgent();
		
		driver.executeAgent(null,response,createAgentMockContext(a));
		
		assertNotNull("An error is expected.",response.getError());
		assertEquals("The informed Agent is not a valid one.",response.getError());
	}
	
	@Test public void dontBreakWithoutAnAgent() throws Exception{
		driver.executeAgent(null,response,new UOSMessageContext(){
			public DataInputStream getDataInputStream() {
				return null;
			}
		});
		
		assertNotNull("An error is expected.",response.getError());
		assertEquals("No Agent was tranfered.",response.getError());
	}
	
	@Test public void dontBreakWhenSomethingBadHappens() throws Exception{
		driver.executeAgent(null,response,new UOSMessageContext(){
			public DataInputStream getDataInputStream() {
				throw new RuntimeException();
			}
		});
		
		assertNotNull("An error is expected.",response.getError());
		assertEquals("Something unexpected happened.",response.getError());
	}
	
	@Test public void theAgentMustHaveAccessToTheGateway() throws Exception{
		MyAgent a = new MyAgent();
		
		Gateway g = mock(Gateway.class);
		
		driver.init(g, null);
		driver.executeAgent(null,response,createAgentMockContext(a));
		
		assertNull("No error should be found.",response.getError());
		Thread.sleep(100);
		assertEquals(g, AgentSpy.lastAgent.gateway);
	}
	
	//TODO: We must assure that the properties are all transient
	//TODO: check if there is a way to do it with OSGi
	//TODO: Agent must be able to request to move itself
	//TODO? Must the agent have a lifecycle?
	//TODO? Do we need to control the execution of the Agent.
	
	private UOSMessageContext createAgentMockContext(Serializable a)
			throws IOException {
		final PipedInputStream in = new PipedInputStream();
		new ObjectOutputStream(new PipedOutputStream(in)).writeObject(a);
		
		return new UOSMessageContext(){
			public DataInputStream getDataInputStream() {
				return new DataInputStream(in);
			}
		};
	}
}

class NonAgent implements Serializable{
	private static final long serialVersionUID = -6537712082673542107L;
}

class AgentSpy{
	static Integer count = 0;
	static MyAgent lastAgent = null;
}

class MyAgent implements Agent{
	private static final long serialVersionUID = -8267793981973238896L;
	public Integer sleepTime = 0;
	public Gateway gateway;
	public void run(Gateway gateway){
		AgentSpy.lastAgent = this;
		this.gateway = gateway; 
//		/AgentSpy.count++;
		try {	Thread.sleep(sleepTime);	} catch (Exception e) {}
		AgentSpy.count++;
	}
}