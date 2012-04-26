package org.unbiquitous.driver.execution;

import static org.junit.Assert.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;
import org.unbiquitous.driver.execution.ExecutionDriver;

import br.unb.unbiquitous.ubiquitos.uos.driverManager.UosDriver;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpDriver;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpService;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpService.ParameterType;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceResponse;

public class ExecutionDriverTest {

	private ExecutionDriver driver;
	
	@Before public void setUp(){
		driver = new ExecutionDriver();
	}
	
	@Test public void uPDriverInterfaceIsConsistent(){
		assertTrue(driver instanceof UosDriver);
		UpDriver uDriver = driver.getDriver();
		assertNotNull(uDriver);
		assertEquals("uos.ExecutionDriver",uDriver.getName());
		
		//assert services
		assertNotNull(uDriver.getServices());
		assertEquals(2, uDriver.getServices().size());
		
		// Assert remoteExecution
		UpService remoteExecution = uDriver.getServices().get(0);
		assertNotNull(remoteExecution);
		assertEquals("remoteExecution", remoteExecution.getName());
		assertNotNull(remoteExecution.getParameters());
		assertEquals(1, remoteExecution.getParameters().size());
		assertTrue(remoteExecution.getParameters().containsKey("code"));
		assertEquals(ParameterType.MANDATORY,remoteExecution.getParameters().get("code"));
		
		// Assert remoteExecution
		UpService executeAgent = uDriver.getServices().get(1);
		assertNotNull(executeAgent);
		assertEquals("executeAgent", executeAgent.getName());
		assertNull(executeAgent.getParameters());
//		assertEquals(executeAgent.) tem que ser stream?
	}
	
}