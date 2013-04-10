package org.unbiquitous.driver.execution.executeAgent;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpDevice;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceResponse;

public class GatewayMapTest {

	
	@Test public void convertMapPutToAServiceCall() throws Exception{
		Gateway delegate = mock(Gateway.class);
		GatewayMap map = new GatewayMap(delegate);
//		UpDevice device,
//		String serviceName, 
//		String driverName, 
//		String instanceId,
//		String securityType, 
//		Map<String, String> parameters
//		
		UpDevice device = new UpDevice();
		ServiceCall call = new ServiceCall();
		
		Map<String, Object> callParams = new HashMap<String, Object>();
		callParams.put("device", device);
		callParams.put("serviceCall", call);
		
		ServiceResponse response = new ServiceResponse();
		when(delegate.callService(device, call)).thenReturn(response);
		
		assertSame(response,map.put("callService",callParams));
		
		verify(delegate).callService(device,call);
	}
}
