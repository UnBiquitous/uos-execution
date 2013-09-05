package org.unbiquitous.driver.execution.remoteExecution;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import org.unbiquitous.driver.execution.ExecutionDriver;
import org.unbiquitous.uos.core.messageEngine.messages.Call;
import org.unbiquitous.uos.core.messageEngine.messages.Response;


public class RemoteExecutionTest {
	
private ExecutionDriver driver;
	
	@Before public void setUp(){
		driver = new ExecutionDriver();
	}
	
	@Test public void AllowsRemoteExecution(){
		Call call = new Call();
		StringBuffer script = new StringBuffer();
		script.append("function plusFive(a)\n");
		script.append("	return a+5\n");
		script.append("end\n");
		script.append("set('value',plusFive(get('value')))\n");
		call.addParameter("code", script.toString());
		call.addParameter("value", "5");
		Response response = new Response();
		driver.remoteExecution(call, response, null);
		assertEquals("10",response.getResponseData("value"));
	}
	
	@Test public void AllowsMultipleExecutionsWithoutInterference(){
		StringBuffer script = new StringBuffer();
		script.append("function plusFive(a)\n");
		script.append("	return a+5\n");
		script.append("end\n");
		script.append("set('value',plusFive(get('value')))\n");

		Call call1 = new Call();
		call1.addParameter("code", script.toString());
		call1.addParameter("value", "5");
		Response response1 = new Response();
		driver.remoteExecution(call1, response1, null);
		assertEquals("10",response1.getResponseData("value"));
		
		Call call2 = new Call();
		call2.addParameter("code", script.toString());
		call2.addParameter("value", "20");
		Response response2 = new Response();
		driver.remoteExecution(call2, response2, null);
		assertEquals("25",response2.getResponseData("value"));
	}
	
	
	@Test public void AllowsRemoteExecutionWithVariousValues(){
		Call call = new Call();
		StringBuffer script = new StringBuffer();
		script.append("function plus(a,b,c)\n");
		script.append("	return a+b+c\n");
		script.append("end\n");
		script.append("set('value',plus(get('v1'),get('v2'),get('v3')))\n");
		call.addParameter("code", script.toString());
		call.addParameter("v1", "1");
		call.addParameter("v2", "2");
		call.addParameter("v3", "3");
		Response response = new Response();
		driver.remoteExecution(call, response, null);
		assertEquals("6",response.getResponseData("value"));
	}
	
}