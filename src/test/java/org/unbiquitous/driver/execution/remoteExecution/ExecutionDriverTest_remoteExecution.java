package org.unbiquitous.driver.execution.remoteExecution;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.unbiquitous.driver.execution.ExecutionDriver;

import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceResponse;

public class ExecutionDriverTest_remoteExecution {
	
private ExecutionDriver driver;
	
	@Before public void setUp(){
		driver = new ExecutionDriver();
	}
	
	@Test public void AllowsRemoteExecution(){
		ServiceCall call = new ServiceCall();
		StringBuffer script = new StringBuffer();
		script.append("function plusFive(a)\n");
		script.append("	return a+5\n");
		script.append("end\n");
		script.append("set('value',plusFive(get('value')))\n");
		call.addParameter("code", script.toString());
		call.addParameter("value", "5");
		ServiceResponse response = new ServiceResponse();
		driver.remoteExecution(call, response, null);
		assertEquals("10",response.getResponseData("value"));
	}
	
	@Test public void AllowsMultipleExecutionsWithoutInterference(){
		StringBuffer script = new StringBuffer();
		script.append("function plusFive(a)\n");
		script.append("	return a+5\n");
		script.append("end\n");
		script.append("set('value',plusFive(get('value')))\n");

		ServiceCall call1 = new ServiceCall();
		call1.addParameter("code", script.toString());
		call1.addParameter("value", "5");
		ServiceResponse response1 = new ServiceResponse();
		driver.remoteExecution(call1, response1, null);
		assertEquals("10",response1.getResponseData("value"));
		
		ServiceCall call2 = new ServiceCall();
		call2.addParameter("code", script.toString());
		call2.addParameter("value", "20");
		ServiceResponse response2 = new ServiceResponse();
		driver.remoteExecution(call2, response2, null);
		assertEquals("25",response2.getResponseData("value"));
	}
	
	
	@Test public void AllowsRemoteExecutionWithVariousValues(){
		ServiceCall call = new ServiceCall();
		StringBuffer script = new StringBuffer();
		script.append("function plus(a,b,c)\n");
		script.append("	return a+b+c\n");
		script.append("end\n");
		script.append("set('value',plus(get('v1'),get('v2'),get('v3')))\n");
		call.addParameter("code", script.toString());
		call.addParameter("v1", "1");
		call.addParameter("v2", "2");
		call.addParameter("v3", "3");
		ServiceResponse response = new ServiceResponse();
		driver.remoteExecution(call, response, null);
		assertEquals("6",response.getResponseData("value"));
	}
	
}