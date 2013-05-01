package org.unbiquitous.driver.execution.executeAgent;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.unbiquitous.driver.execution.executeAgent.CompilationUtil.compileToClass;
import static org.unbiquitous.driver.execution.executeAgent.CompilationUtil.compileToFile;
import static org.unbiquitous.driver.execution.executeAgent.CompilationUtil.compileToPath;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.Serializable;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.unbiquitous.driver.execution.ExecutionDriver;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.applicationManager.UOSMessageContext;
import org.unbiquitous.uos.core.messageEngine.messages.ServiceCall;
import org.unbiquitous.uos.core.messageEngine.messages.ServiceResponse;


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
		String[] dependencies = new String[]{ 
		"junit",
		"log4j",
		"hsqldb",
		"owlapi",
		"jcl-core",
		"objenesis",
		"cglib-nodep",
		"HermiT",
		"hamcrest-core",
		"mockito-all",
		"uos-core",
		"uos.tcp_udp.plugin",
		"/uos_core/target/classes",
		"uos-execution"}; // TODO: must be uos-execution
		assertThat(driver.toolbox().blacklist()).containsOnly(dependencies);
	}
	
	@Test public void runTheCalledAgentOnAThread() throws Exception{
		MyAgent a = new MyAgent();
		a.sleepTime = 1000;
		
		final Integer before = MyAgent.AgentSpy.count;
		
		execute(a);
		
		assertNull("No error should be found.",response.getError());
		assertEquals((Integer)(before),MyAgent.AgentSpy.count);
		assertEventuallyTrue("Must increment the SpyCount eventually", 
				a.sleepTime + 1000, new EventuallyAssert(){
				public boolean assertion(){
					return (Integer)(before+1) == MyAgent.AgentSpy.count;
				}
			});
	}
	
	@Test public void runTheCalledAnonymousAgentOnAThread() throws Exception{
		MyAnonymousAgent a = new MyAnonymousAgent();
		a.sleepTime = 1000;
		
		final Integer before = MyAnonymousAgent.Spy.count;
		
		execute(a);
		
		assertNull("No error should be found.",response.getError());
		assertEquals((Integer)(before),MyAnonymousAgent.Spy.count);
		assertEventuallyTrue("Must increment the SpyCount eventually", 
				a.sleepTime + 1000, new EventuallyAssert(){
				public boolean assertion(){
					return (Integer)(before+1) == MyAnonymousAgent.Spy.count;
				}
			});
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Test public void runTheCalledAgentFromNewJavaCodeOnAThread() throws Exception{
		final Integer before = MyAgent.AgentSpy.count;
		
		String source = 
				"package org.unbiquitous.driver.execution.executeAgent;"
			+	"import org.unbiquitous.driver.execution.executeAgent.MyAgent.AgentSpy;"
			+	"import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;"
			+	"public class Foo2 extends org.unbiquitous.driver.execution.executeAgent.Agent {"
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
		final String clazz = "org.unbiquitous.driver.execution.executeAgent.Foo2";
		
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
						return (Integer)(before+17) == MyAgent.AgentSpy.count;
					}
				});
	}
	
	@Test public void runTheCalledAgentFromAjarFile() throws Exception{
		final Integer before = MyAgent.AgentSpy.count;
		
		String source = 
				"package org.unbiquitous.driver.execution.executeAgent;"
			+	"import org.unbiquitous.driver.execution.executeAgent.MyAgent.AgentSpy;"
			+	"import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;"
			+	"public class Foo2 extends org.unbiquitous.driver.execution.executeAgent.Agent {"
			+	"	int increment = 21;"
			+	"	public void run(Gateway gateway){"
			+	"		AgentSpy.count+=increment;"
			+	"	}"
			+	"}";
		final String clazz = "org.unbiquitous.driver.execution.executeAgent.Foo2";
		
		
		File path = compileToPath(new String[]{source},new String[]{clazz},tempDir);
		File jar = File.createTempFile("temp", ".jar");
		JarPackager packager = new JarPackager(new ClassToolbox());
		final ZipOutputStream zos = new ZipOutputStream( new FileOutputStream( jar ) );
		packager.zip(path, path, zos);
		zos.close();
		Class<?> c= compileToClass(source,clazz);
		
		executeFromJar((Serializable) c.newInstance(),new FileInputStream(jar));
		
		assertNull("No error should be found.",response.getError());
		
		assertEventuallyTrue("Must increment the SpyCount eventually",1000, 
				new EventuallyAssert(){
					public boolean assertion(){
						return (Integer)(before+21) == MyAgent.AgentSpy.count;
					}
				});
	}
	
	@Test public void rejectsACorruptedAgentObject() throws Exception{
		final Integer before = MyAgent.AgentSpy.count;
		MyAgent a = new MyAgent();
		
		dontexecute(a);
		
		Thread.sleep(1000);
		assertEquals("SpyCount must be unchanged",(Integer)(before),MyAgent.AgentSpy.count);
	}
	
	@Test public void rejectsACorruptedClassStream() throws Exception{
		final Integer before = MyAgent.AgentSpy.count;
		
		String source = 
				"package org.unbiquitous.driver.execution.executeAgent;"
			+	"import org.unbiquitous.driver.execution.executeAgent.MyAgent.AgentSpy;"
			+	"import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;"
			+	"public class Foo3 extends org.unbiquitous.driver.execution.executeAgent.Agent {"
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
		final String clazz = "org.unbiquitous.driver.execution.executeAgent.Foo3";
		
		File clazzFile = compileToFile(source,clazz, tempDir);
		Class<?> c= compileToClass(source,clazz);
		
		
		Serializable a = (Serializable) c.getMethod("getFoo3", (Class[])null)
											.invoke(null, new Object[]{});
		
		dontexecute(a,new FileInputStream(clazzFile), clazz);
		
		Thread.sleep(1000);
		
		assertEquals("SpyCount must be unchanged",(Integer)(before),MyAgent.AgentSpy.count);
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
					return MyAgent.AgentSpy.lastAgent != null && g == MyAgent.AgentSpy.lastAgent.gateway;
				}
			});
		assertEquals(g, MyAgent.AgentSpy.lastAgent.gateway);
	}
	
	@SuppressWarnings("unchecked")
	@Test public void theAnonymousAgentMustHaveAccessToAMapGateway() throws Exception{
		MyAnonymousAgent a = new MyAnonymousAgent();
		
		final Gateway g = mock(Gateway.class);
		
		driver.init(g, null);
		execute(a);
		
		assertNull("No error should be found.",response.getError());
		assertEventuallyTrue("The gateway mus be the same as the one informed", 
				1000, new EventuallyAssert(){
				public boolean assertion(){
					return MyAnonymousAgent.Spy.lastAgent != null && 
							MyAnonymousAgent.Spy.lastAgent.gateway != null;
				}
			});
		//TODO: validate if it's a DelegateMap
		assertThat(MyAnonymousAgent.Spy.lastAgent.gateway)
													.isInstanceOf(Map.class);
	}
	
	//TODO: SHouldn't wait 4 ever for a agent to be received
	//TODO: check if there is a way to do it with OSGi
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
		execute(a,null, null);
	}
	
	private void execute(Serializable a, final InputStream code, String clazz) throws Exception{
		execute(a, code,null, clazz, false, false);
	}

	private void executeFromJar(Serializable a, final InputStream jar) throws Exception{
		execute(a, null, jar, null, false, false);
	}
	
	private void dontexecute(Serializable a) throws Exception{
		execute(a, null,null, null, true, false);
	}
	
	private void dontexecute(Serializable a, final InputStream code, String clazz) throws Exception{
		execute(a, code,null, clazz, false, true);
	}
	
	private void execute(Serializable a, final InputStream code, final InputStream jar,
			String clazz, boolean mustFailAgent, final boolean mustFailCode) throws Exception{
		
		final PipedInputStream pipeForAgent = new PipedInputStream();
		final DataInputStream agentStream = new DataInputStream(pipeForAgent);
		PipedOutputStream whereToWriteAgent = new PipedOutputStream(pipeForAgent);
		
		ByteArrayOutputStream agentSpy = new ByteArrayOutputStream();
		new ObjectOutputStream(agentSpy).writeObject(a);
		final byte[] agentArray = agentSpy.toByteArray();
		
		final ServiceCall call = new ServiceCall();
			// These parameters remains a question since when the
			// file or serialized object fail during transfer the 
			// execution simply doesn't occur.
			//
			//.addParameter("agent_size",""+agentArray.length) should I?
			//.addParameter("code_size",""+codeArray.length) should I?
		final InputStream bytecode;
		if (clazz != null){
			call.addParameter("class", clazz);
			bytecode = code;
		}else if (jar != null){
			call.addParameter("jar", "true");
			bytecode = jar;
		}else{
			bytecode = null;
		}
		driver.executeAgent(call,response,
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
									while ((b = bytecode.read()) != -1) codeSpy.write(b);
									final byte[] codeArray = codeSpy.toByteArray();
									ByteArrayInputStream baos = 
											new ByteArrayInputStream(
													codeArray,0,codeArray.length-10);
									return new DataInputStream(baos);
								} catch (IOException e) {}
							}else{
								return new DataInputStream(bytecode);
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
		MyAgent.AgentSpy.count++;
	}
}

class DummyAgent extends Agent{
	private static final long serialVersionUID = 6755442789702096965L;

	public void run(Gateway gateway) {}
}

