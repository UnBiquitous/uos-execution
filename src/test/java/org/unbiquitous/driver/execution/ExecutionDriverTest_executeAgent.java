package org.unbiquitous.driver.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.unbiquitous.driver.execution.CompilationUtil.compileToClass;
import static org.unbiquitous.driver.execution.CompilationUtil.compileToFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.unbiquitous.driver.execution.MyAgent.AgentSpy;

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
	
	@Test public void toolboxMustBeInitializedWithDependencies(){
		Set<String> dependencies = new HashSet<String>(); 
		//TODO: fix these duplicates
		dependencies.add("junit-3.8.1.jar");
		dependencies.add("junit-4.9.jar");
		dependencies.add("log4j-1.2.16.jar");
		dependencies.add("log4j-1.2.14.jar");
		dependencies.add("hsqldb-1.8.0.10.jar");
		dependencies.add("hsqldb-1.8.0.7.jar");
		dependencies.add("owlapi-3.2.4.jar");
		dependencies.add("jcl-core-2.2.2.jar");
		dependencies.add("objenesis-1.1.jar");
		dependencies.add("cglib-nodep-2.2.jar");
		dependencies.add("HermiT-1.0.jar");
		dependencies.add("hamcrest-core-1.1.jar");
		dependencies.add("mockito-all-1.8.5.jar");
		//TODO: how to handle if the versions changes or something is added
		dependencies.add("uos-core-2.2.0_DEV.jar");
		//TODO: must add myself dynamically
		dependencies.add("/uos_core/target/classes");
		dependencies.add("execution-1.0-SNAPSHOT.jar");
		assertEquals(dependencies,driver.toolbox().blacklist());
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
			+	"import org.unbiquitous.driver.execution.MyAgent.AgentSpy;"
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
		
		File clazzFile = compileToFile(source,clazz, tempDir);
		Class c= compileToClass(source,clazz);
		try {
			Class.forName(clazz);
			fail("Must no have the class on classpath");
		} catch (Exception e) {}
		
		Serializable a = (Serializable) c.getMethod("getFoo2", (Class[])null)
											.invoke(null, new Object[]{});
		
		execute(a,new FileInputStream(clazzFile), clazz);
		
		assertNull("No error should be found.",response.getError());
		
		assertEventuallyTrue("Must increment the SpyCount eventually",1000, 
				new EventuallyAssert(){
					public boolean assertion(){
						return (Integer)(before+17) == AgentSpy.count;
					}
				});
	}
	
	@Test public void rejectsACorruptedAgentObject() throws Exception{
		final Integer before = AgentSpy.count;
		MyAgent a = new MyAgent();
		
		dontexecute(a);
		
		Thread.sleep(1000);
		assertEquals("SpyCount must be unchanged",(Integer)(before),AgentSpy.count);
	}
	
	@Test public void rejectsACorruptedClassStream() throws Exception{
		final Integer before = AgentSpy.count;
		
		String source = 
				"package org.unbiquitous.driver.execution;"
			+	"import org.unbiquitous.driver.execution.MyAgent.AgentSpy;"
			+	"import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;"
			+	"public class Foo3 extends org.unbiquitous.driver.execution.Agent {"
			+	"	int increment = 1;"
			+	"	public static Foo3 getFoo3(){"
			+	"		Foo3 f = new Foo3();"
			+	"		f.increment=13;"
			+	"		return f;"
			+	"	}"
			+	"	public void run(Gateway gateway){"
			+	"		AgentSpy.count+=increment;"
			+	"	}"
			+	"}";
		final String clazz = "org.unbiquitous.driver.execution.Foo3";
		
		File clazzFile = compileToFile(source,clazz, tempDir);
		Class<?> c= compileToClass(source,clazz);
		
		
		Serializable a = (Serializable) c.getMethod("getFoo3", (Class[])null)
											.invoke(null, new Object[]{});
		
		dontexecute(a,new FileInputStream(clazzFile), clazz);
		
		Thread.sleep(1000);
		
		assertEquals("SpyCount must be unchanged",(Integer)(before),AgentSpy.count);
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
	//TODO? Must the agent have a lifecycle?
	//TODO? Do we need to control the execution of the Agent.
	//TODO: Agent must be able to request to move itself
	//TODO: must to the move of the agent like at MoveSpike.moveTo
	//TODO: Agent class cannot be inner class and must be public
	
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
		execute(a, code, clazz, false, false);
	}
	
	private void dontexecute(Serializable a) throws Exception{
		execute(a, null, null, true, false);
	}
	
	private void dontexecute(Serializable a, final InputStream code, String clazz) throws Exception{
		execute(a, code, clazz, false, true);
	}
	
	private void execute(Serializable a, final InputStream code, String clazz, 
							boolean mustFailAgent, final boolean mustFailCode) throws Exception{
		final PipedInputStream pipeForAgent = new PipedInputStream();
		final DataInputStream agentStream = new DataInputStream(pipeForAgent);
		PipedOutputStream whereToWriteAgent = new PipedOutputStream(pipeForAgent);
		
		ByteArrayOutputStream agentSpy = new ByteArrayOutputStream();
		new ObjectOutputStream(agentSpy).writeObject(a);
		final byte[] agentArray = agentSpy.toByteArray();
		
		driver.executeAgent(
				new ServiceCall()
					// These parameters remains a question since when the
					// file or serialized object fail during transfer the 
					// execution simply doesn't occur.
					//
					//.addParameter("agent_size",""+agentArray.length) should I?
					//.addParameter("code_size",""+codeArray.length) should I?
					.addParameter("class", clazz)
					,
				response,
				new UOSMessageContext(){
					public DataInputStream getDataInputStream() {	
						return agentStream;	
					}
					public DataInputStream getDataInputStream(int index) {
						if (index == 0){
							return agentStream;
						}else if (index == 1){
							if (mustFailCode){
								try {
									final ByteArrayOutputStream codeSpy = 
											new ByteArrayOutputStream();
									int b;
									while ((b = code.read()) != -1) codeSpy.write(b);
									final byte[] codeArray = codeSpy.toByteArray();
									ByteArrayInputStream baos = 
											new ByteArrayInputStream(
													codeArray,0,codeArray.length-10);
									return new DataInputStream(baos);
								} catch (IOException e) {}
							}else{
								return new DataInputStream(code);
							}
						}
						return null;
					}
				});
		
		if(mustFailAgent){
			for(int i =0 ; i < agentArray.length -10; i++){
				whereToWriteAgent.write(agentArray[i]);
			}
		}else{
			whereToWriteAgent.write(agentArray);
		}
		
//		outAgent
	}
}

class NonAgent implements Serializable{
	private static final long serialVersionUID = -6537712082673542107L;
	public void run(Gateway gateway){
		AgentSpy.count++;
	}
}

class DummyAgent extends Agent{
	private static final long serialVersionUID = 6755442789702096965L;

	public void run(Gateway gateway) {}
}

