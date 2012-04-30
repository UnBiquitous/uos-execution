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
		
		final Integer before = AgentSpy.count;
		
		execute(a);
		
		assertNull("No error should be found.",response.getError());
		assertEquals((Integer)(before),AgentSpy.count);
		assertEventuallyTrue("Must increment the SpyCount eventually", 
				a.sleepTime + 1000, new EventuallyAssert(){
				public boolean assertion(){
					return (Integer)(before+1) == AgentSpy.count;
				}
			});
	}

	//FIXME: How to test this?
	@Test public void dontAcceptANonAgentAgent() throws Exception{
		NonAgent a = new NonAgent();
		final Integer before = AgentSpy.count;
		execute(a);
		
		//assertNotNull("An error is expected.",response.getError());
		Thread.sleep(2000);
		assertEquals("Mustn't increment the SpyCount eventually",
				(Integer)(before),AgentSpy.count);
		//assertEquals("The informed Agent is not a valid one.",response.getError());
		
	}
	
	@Test public void dontBreakWithoutAnAgent() throws Exception{
		driver.executeAgent(null,response,new UOSMessageContext(){
			public DataInputStream getDataInputStream() {
				return null;
			}
		});
		
		assertNotNull("An error is expected.",response.getError());
		assertEquals("No Data Stream, containing agent, was found.",response.getError());
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
		
		final Gateway g = mock(Gateway.class);
		
		driver.init(g, null);
		execute(a);
		
		assertNull("No error should be found.",response.getError());
		assertEventuallyTrue("The gateway mus be the same as the one informed", 
				1000, new EventuallyAssert(){
				public boolean assertion(){
					return AgentSpy.lastAgent != null && g == AgentSpy.lastAgent.gateway;
				}
			});
		assertEquals(g, AgentSpy.lastAgent.gateway);
	}
	
	//TODO: SHouldn't wait 4 ever for a agent to be received
	//TODO: We must assure that the properties are all transient
	//TODO: check if there is a way to do it with OSGi
	//TODO: Agent must be able to request to move itself
	//TODO? Must the agent have a lifecycle?
	//TODO? Do we need to control the execution of the Agent.
	
	static interface EventuallyAssert{
		boolean assertion();
	}
	
	private void assertEventuallyTrue(String msg, long wait, EventuallyAssert assertion) throws InterruptedException{
		long time = 0;
		while (time <= wait && !assertion.assertion()){
			Thread.sleep(10);
			time += 10;
		}
		assertTrue(msg,assertion.assertion());
	}
	
	private void execute(Serializable a) throws Exception{
		final PipedInputStream in = new PipedInputStream();
		final DataInputStream ret = new DataInputStream(in);
		PipedOutputStream out = new PipedOutputStream(in);
		driver.executeAgent(null,response,new UOSMessageContext(){
			public DataInputStream getDataInputStream() {
				return ret;
			}
		});
		new ObjectOutputStream(out).writeObject(a);
	}
}

class NonAgent implements Serializable{
	private static final long serialVersionUID = -6537712082673542107L;
	public void run(Gateway gateway){
		AgentSpy.count++;
	}
}

class AgentSpy{
	static Integer count = 0;
	static MyAgent lastAgent = null;
}

class MyAgent extends Agent{
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

