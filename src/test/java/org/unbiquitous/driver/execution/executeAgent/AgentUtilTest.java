package org.unbiquitous.driver.execution.executeAgent;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.unbiquitous.driver.execution.executeAgent.CompilationUtil.assertStream;
import static org.unbiquitous.driver.execution.executeAgent.CompilationUtil.zipEntries;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.unbiquitous.driver.execution.executeAgent.dummy.DummyAgent;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.adaptabitilyEngine.ServiceCallException;
import org.unbiquitous.uos.core.application.UOSMessageContext;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;
import org.unbiquitous.uos.core.messageEngine.messages.ServiceCall;
import org.unbiquitous.uos.core.messageEngine.messages.ServiceResponse;
import org.unbiquitous.uos.core.messageEngine.messages.ServiceCall.ServiceType;


public class AgentUtilTest {
	@Rule
    public TemporaryFolder folder= new TemporaryFolder();
	
	private static ClassToolbox box;
	
	@BeforeClass public static void init(){
		box = new ClassToolbox();
	}
	
	@Test public void movingCallsExecuteAgentOnExecutionDriver() throws Exception{
		Gateway gateway = mockGateway(new ByteArrayOutputStream(), new ByteArrayOutputStream());
		
		UpDevice target = new UpDevice("target");
		AgentUtil.move(new MyAgent(),target,gateway);
		
		ArgumentCaptor<UpDevice> deviceCaptor = ArgumentCaptor.forClass(UpDevice.class);
		ArgumentCaptor<ServiceCall> callCaptor = ArgumentCaptor.forClass(ServiceCall.class);
		
		verify(gateway).callService(deviceCaptor.capture(), callCaptor.capture());
		
		assertEquals(target, deviceCaptor.getValue());
		assertEquals("uos.ExecutionDriver", callCaptor.getValue().getDriver());
		assertEquals("executeAgent", callCaptor.getValue().getService());
		assertEquals("Must have 2 channels (agent and jar)",2, 
											callCaptor.getValue().getChannels());
		assertEquals("Service is of type stream",
					ServiceType.STREAM, callCaptor.getValue().getServiceType());
		assertEquals("Default type of move is jar",
							"true", callCaptor.getValue().getParameter("jar"));
		
	}

	@Test public void movingSendsAgentSerialized() throws Exception{
		MyAgent agent = new MyAgent();
		ByteArrayOutputStream agentSpy = new ByteArrayOutputStream();
		//Using buffered i can check if close was properly called
		Gateway gateway = mockGateway(new BufferedOutputStream(agentSpy),
												new ByteArrayOutputStream());
		
		UpDevice target = new UpDevice("target");
		AgentUtil.move(agent,target,gateway);
		
		assertArrayEquals(serialize(agent), agentSpy.toByteArray());
	}
	
	@Test public void movingSendsAgentJar() throws Exception{
		MyAgent agent = new MyAgent();
		File jarSpy = File.createTempFile("uOSAUtilTmpJar", ".jar");
		//Using buffered i can check if close was properly called
		Gateway gateway = mockGateway(new ByteArrayOutputStream(),
						new BufferedOutputStream(new FileOutputStream(jarSpy)));
		
		UpDevice target = new UpDevice("target");
		AgentUtil.move(agent,target,gateway);
		
		File jar = box.packageJarFor(agent.getClass());
		
		assertEquals(zipEntries(jar), zipEntries(jarSpy));
	}
	
	@Test public void movingSendsSpecificPackage() throws Exception{
		Agent agent = new DummyAgent();
		File jarSpy = File.createTempFile("uOSAUtilTmpJar", ".jar");
		//Using buffered i can check if close was properly called
		Gateway gateway = mockGateway(new ByteArrayOutputStream(),
				new BufferedOutputStream(new FileOutputStream(jarSpy)));
		
		File dummyPkg = File.createTempFile("dummy", ".jar");
		FileOutputStream writer = new FileOutputStream(dummyPkg);
		writer.write("Hello Package".getBytes());
		writer.close();
		
		UpDevice target = new UpDevice("target");
		AgentUtil.move(agent,dummyPkg, target,gateway);
		
		assertStream(new FileInputStream(dummyPkg), 
												new FileInputStream(jarSpy));
	}
	
	@Test public void movingSendsDalvikJarWhenTargetIsAndroid() throws Exception{
		MyAgent agent = new MyAgent();
		File jarSpy = File.createTempFile("uOSAUtilTmpJar", ".jar");
		//Using buffered i can check if close was properly called
		Gateway gateway = mockGateway(new ByteArrayOutputStream(),
						new BufferedOutputStream(new FileOutputStream(jarSpy)));
		
		UpDevice target = new UpDevice("target");
		target.addProperty("platform", "Dalvik");
		AgentUtil.move(agent,target,gateway);
		
		File jar = box.packageJarFor(agent.getClass());
		File dalvik = box.convertToDalvik(folder.getRoot(), jar, 
												System.getenv("ANDROID_HOME"));  
		assertEquals(zipEntries(dalvik), zipEntries(jarSpy));
	}

	@Test(expected=RuntimeException.class) 
	public void rejectsAgentsAsInnerClasses() throws Exception{
		Gateway gateway = mockGateway(new ByteArrayOutputStream(), new ByteArrayOutputStream());
		
		UpDevice target = new UpDevice("target");
		AgentUtil.move(new Agent() {
			private static final long serialVersionUID = 2420826408820982276L;
			public void run(Gateway gateway) {}
		},target,gateway);
	}
	
	@Test(expected=RuntimeException.class) 
	public void rejectsAgentsAsNotPublicClasses() throws Exception{
		Gateway gateway = mockGateway(new ByteArrayOutputStream(), new ByteArrayOutputStream());
		
		UpDevice target = new UpDevice("target");
		AgentUtil.move(new ShyAgent(),target,gateway);
	}
	
	@Test public void GatewayMustBeTransient() throws Exception{
		Gateway gateway = mockGateway(new ByteArrayOutputStream(), new ByteArrayOutputStream());
		
		UpDevice target = new UpDevice("target");
		final MyAgent agent = new MyAgent();
		agent.init(gateway);
		AgentUtil.move(agent,target,gateway);
	}
	
	//TODO: We must assure that the properties are all transient
	
	private Gateway mockGateway(final OutputStream spy,
								final OutputStream jarSpy)
			throws ServiceCallException {
		Gateway gateway = mock(Gateway.class);
		when(gateway.callService(any(UpDevice.class), any(ServiceCall.class)))
		.thenReturn(new ServiceResponse(){
			@Override
			public UOSMessageContext getMessageContext() {
				return new UOSMessageContext(){
					public DataOutputStream getDataOutputStream() {	
						return new DataOutputStream(spy);	
					}
					public DataOutputStream getDataOutputStream(int index) {
						switch(index){
							case 0 : return new DataOutputStream(spy);
							case 1 : return new DataOutputStream(jarSpy);
							default : return null;
						}
					}
				};
			}
		});
		return gateway;
	}
	
	private byte[] serialize(final MyAgent agent) throws IOException {
		ByteArrayOutputStream arraySpy = new ByteArrayOutputStream();
		ObjectOutputStream objectWriter = new ObjectOutputStream(arraySpy);
		objectWriter.writeObject(agent);
		return arraySpy.toByteArray();
	}
	
}

class ShyAgent extends Agent{
	private static final long serialVersionUID = 1L;

	public void run(Gateway gateway) {}
}