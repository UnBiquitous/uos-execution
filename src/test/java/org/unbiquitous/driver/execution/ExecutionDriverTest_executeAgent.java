package org.unbiquitous.driver.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.unbiquitous.driver.execution.CompilationUtil.compileToClass;
import static org.unbiquitous.driver.execution.CompilationUtil.compileToFile;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;
import br.unb.unbiquitous.ubiquitos.uos.application.UOSMessageContext;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceResponse;

public class ExecutionDriverTest_executeAgent {

	@Rule
    public TemporaryFolder folder= new TemporaryFolder();
	
	private ExecutionDriver driver;
	private ServiceResponse response;
	
	private File tempDir;
	
	@Before public void setUp(){
		driver = new ExecutionDriver();
		response = new ServiceResponse();
		tempDir = folder.getRoot();
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
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test public void runTheCalledAgentFromNewJavaCodeOnAThread() throws Exception{
		final Integer before = AgentSpy.count;
		
		String source = 
				"package org.unbiquitous.driver.execution;"
			+	"import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;"
			+	"public class Foo2 extends org.unbiquitous.driver.execution.Agent {"
			+	"	int increment = 1;"
			+	"	public static Foo2 getFoo2(){"
			+	"		Foo2 f = new Foo2();"
			+	"		f.increment=17;"
			+	"		return f;"
			+	"	}"
			+	"	public void run(Gateway gateway){"
			+	"		AgentSpy.count+=increment;"
			+	"	}"
			+	"}";
		final String clazz = "org.unbiquitous.driver.execution.Foo2";
		
		File origin = compileToFile(source,clazz, tempDir);
		Class c= compileToClass(source,clazz);
		try {
			Class.forName(clazz);
			fail("Must no have the class on classpath");
		} catch (Exception e) {}
		
		Serializable a = (Serializable) c.getMethod("getFoo2", (Class[])null)
											.invoke(null, new Object[]{});
		
		execute(a,new FileInputStream(origin), clazz);
		
		assertEventuallyTrue("Must have the class on classpath eventually",1000, 
				new EventuallyAssert(){
					public boolean assertion(){
						try {
							Class.forName(clazz); return true;
						} catch (Exception e) { return false;}
					}
				});
		assertNull("No error should be found.",response.getError());
		assertEquals((Integer)(before),AgentSpy.count);
		assertEventuallyTrue("Must increment the SpyCount eventually",1000, 
				new EventuallyAssert(){
					public boolean assertion(){
						return (Integer)(before+17) == AgentSpy.count;
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
	//TODO: must to the move of the agent like at MoveSpike.moveTo
	
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
		execute(a,null, null);
	}
	
	private void execute(Serializable a, final InputStream code, String clazz) throws Exception{
		final PipedInputStream inAgent = new PipedInputStream();
		final DataInputStream retAgent = new DataInputStream(inAgent);
		PipedOutputStream outAgent = new PipedOutputStream(inAgent);

		driver.executeAgent(new ServiceCall().addParameter("class", clazz)
								,response,new UOSMessageContext(){
			public DataInputStream getDataInputStream() {	return retAgent;	}
			public DataInputStream getDataInputStream(int index) {
				if (index == 0){
					return retAgent;
				}else if (index == 1){
					return new DataInputStream(code);
				}
				return null;
			}
		});
		new ObjectOutputStream(outAgent).writeObject(a);
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
		try {	Thread.sleep(sleepTime);	} catch (Exception e) {}
		AgentSpy.count++;
	}
}

class DummyAgent extends Agent{
	private static final long serialVersionUID = 6755442789702096965L;

	public void run(Gateway gateway) {}
}

