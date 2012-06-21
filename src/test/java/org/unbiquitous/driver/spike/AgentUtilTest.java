package org.unbiquitous.driver.spike;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.unbiquitous.driver.execution.AgentUtil;
import org.unbiquitous.driver.execution.MyAgent;

import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;
import br.unb.unbiquitous.ubiquitos.uos.application.UOSMessageContext;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpDevice;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceResponse;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall.ServiceType;

public class AgentUtilTest {

	//see MoveSpike.moveTo
	// OK Create a call to 'uos.ExecutionDriver'.'executeAgent'
	// transport class as jar
	// send agent serialized
	
	@Test public void movingCallsExecuteAgentOnExecutionDriver() throws Exception{
		
		Gateway gateway = mock(Gateway.class);
		
		final UpDevice target = new UpDevice("target");
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
		final MyAgent agent = new MyAgent();
		Gateway gateway = mock(Gateway.class);
		
		ByteArrayOutputStream agentSpy = new ByteArrayOutputStream();
		new ObjectOutputStream(agentSpy).writeObject(agent);
		final byte[] agentArray = agentSpy.toByteArray();
		
		final PipedInputStream pipeForAgent = new PipedInputStream();
		final DataInputStream agentStream = new DataInputStream(pipeForAgent);
		final PipedOutputStream whereToWriteAgent = new PipedOutputStream(pipeForAgent);
		
		final UOSMessageContext ctx = new UOSMessageContext(){
			public DataOutputStream getDataOutputStream() {	
				return new DataOutputStream(whereToWriteAgent);	
			}
			public DataOutputStream getDataOutputStream(int index) {
				return new DataOutputStream(whereToWriteAgent);
			}
		};
		
		when(gateway.callService(any(UpDevice.class), any(ServiceCall.class)))
				.thenReturn(new ServiceResponse(){
					@Override
					public UOSMessageContext getMessageContext() {
						return ctx;
					}
				});
		
		final UpDevice target = new UpDevice("target");
		AgentUtil.move(agent,target,gateway);
		
		//Check return
		fail("we're not checking the value");
	}
	
}
