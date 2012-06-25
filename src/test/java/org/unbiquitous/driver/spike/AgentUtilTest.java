package org.unbiquitous.driver.spike;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.unbiquitous.driver.execution.AgentUtil;
import org.unbiquitous.driver.execution.ClassToolbox;
import org.unbiquitous.driver.execution.MyAgent;

import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.ServiceCallException;
import br.unb.unbiquitous.ubiquitos.uos.application.UOSMessageContext;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpDevice;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall.ServiceType;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceResponse;

public class AgentUtilTest {

	//see MoveSpike.moveTo
	// OK Create a call to 'uos.ExecutionDriver'.'executeAgent'
	// transport class as jar
	// send agent serialized
	
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
		Gateway gateway = mockGateway(agentSpy,new ByteArrayOutputStream());
		
		UpDevice target = new UpDevice("target");
		AgentUtil.move(agent,target,gateway);
		
		assertArrayEquals(serialize(agent), agentSpy.toByteArray());
		//TODO: How to check if close was called?
	}
	
	@Test public void movingSendsAgentJar() throws Exception{
		MyAgent agent = new MyAgent();
		File jarSpy = File.createTempFile("uOSAUtilTmpJar", ".jar");
		Gateway gateway = mockGateway(new ByteArrayOutputStream(),
										new FileOutputStream(jarSpy));
		
		UpDevice target = new UpDevice("target");
		AgentUtil.move(agent,target,gateway);
		
		File jar = new ClassToolbox().packageJarFor(agent.getClass());
		
		assertEquals(zipEntries(jar), zipEntries(jarSpy));
		//TODO: How to check if close was called?
	}

	//TODO: duplicated
	@SuppressWarnings("unchecked")
	private Set<String> zipEntries(File jar) throws ZipException, IOException {
		Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) 
													new ZipFile(jar).entries();
		Set<String> set = new HashSet<String>();
		
		while(entries.hasMoreElements()){
			ZipEntry entry = entries.nextElement();
			set.add(entry.getName());
		}
		return set;
	}
	
	
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
