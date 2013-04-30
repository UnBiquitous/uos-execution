package org.unbiquitous.driver.execution.executeAgent;

import static junit.framework.Assert.assertEquals;
import static org.fest.assertions.api.Assertions.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.adaptabitilyEngine.UosEventListener;
import org.unbiquitous.uos.core.driverManager.DriverData;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDriver;
import org.unbiquitous.uos.core.messageEngine.dataType.json.JSONDevice;
import org.unbiquitous.uos.core.messageEngine.dataType.json.JSONDriver;
import org.unbiquitous.uos.core.messageEngine.messages.ServiceCall;
import org.unbiquitous.uos.core.messageEngine.messages.ServiceResponse;
import org.unbiquitous.uos.core.messageEngine.messages.json.JSONServiceCall;
import org.unbiquitous.uos.core.messageEngine.messages.json.JSONServiceResponse;


public class GatewayMapTest {

	private Gateway delegate;
	private GatewayMap map;
	private UpDevice device;
	private ServiceCall call;
	private ServiceResponse response;

	@Before public void setUp(){
		delegate = mock(Gateway.class);
		map = new GatewayMap(delegate);
		device = new UpDevice("d1").addNetworkInterface("addr", "t");
		call = new ServiceCall("dr1","s1","i1").addParameter("p", "v");
		response = new ServiceResponse().addParameter("teste", "t");
	}
	
	@Test public void convertMapPutToAServiceCall() throws Exception{
		Map<String, Object> callParams = new HashMap<String, Object>();
		callParams.put("device", new JSONDevice(device).toMap());
		callParams.put("serviceCall", new JSONServiceCall(call).toMap());
		
		when(delegate.callService(eq(device), eq(call))).thenReturn(response);
		
		assertThat(map.put("callService",callParams))
						.isEqualTo(new JSONServiceResponse(response).toMap());
		
		verify(delegate).callService(device,call);
	}
	
	@Test public void convertMapPutToAServiceCallWithFullParameters() throws Exception{
		String serviceName	= "s1"; 
		String driverName	= "d1"; 
		String instanceId	= "i1";
		String securityType	= "k1";
		Map<String, Object> parameters = new HashMap<String, Object>();

		Map<String, Object> callParams = new HashMap<String, Object>();
		callParams.put("device", new JSONDevice(device).toMap());
		callParams.put("serviceName", serviceName);
		callParams.put("driverName", driverName);
		callParams.put("instanceId", instanceId);
		callParams.put("securityType", securityType);
		callParams.put("parameters", parameters);
		
		when(delegate.callService(	device, 	serviceName, 	driverName,
									instanceId,	securityType,	parameters))
							.thenReturn(response);
		
		assertThat(map.put("callService",callParams))
						.isEqualTo(new JSONServiceResponse(response).toMap());
		
		verify(delegate).callService(	device, 	serviceName, 	driverName,
										instanceId,	securityType,	parameters);
	}
	
	@Test public void convertMapPutToARegisterForEvent() throws Exception{
		UosEventListener listener = mock(UosEventListener.class);
		String driver		= "d1";
		String eventKey		= "e1";
		
		Map<String, Object> callParams = new HashMap<String, Object>();
		callParams.put("listener", listener); //TODO: Maybe uOS Won't like this 
		callParams.put("device", new JSONDevice(device).toMap());
		callParams.put("driver", driver);
		callParams.put("eventKey", eventKey);
		
		map.put("registerForEvent",callParams);
		
		verify(delegate).registerForEvent(listener, device, driver, eventKey);
	}
	
	@Test public void convertMapPutToARegisterForEventWithFullParameters() throws Exception{
		UosEventListener listener = mock(UosEventListener.class);
		String driver		= "d1";
		String instanceId	= "i1";
		String eventKey		= "e1";
		
		Map<String, Object> callParams = new HashMap<String, Object>();
		callParams.put("listener", listener); //TODO: Maybe uOS Won't like this 
		callParams.put("device", new JSONDevice(device).toMap());
		callParams.put("driver", driver);
		callParams.put("instanceId", instanceId);
		callParams.put("eventKey", eventKey);
		
		map.put("registerForEvent",callParams);
		
		verify(delegate).registerForEvent(	listener, device, driver,
											instanceId, eventKey);
	}
	
	//TODO: public void unregisterForEvent(UosEventListener listener) 
	//TODO: public void unregisterForEvent(UosEventListener listener, UpDevice device, String driver, String instanceId, String eventKey)
	//TODO: public void sendEventNotify(Notify notify, UpDevice device)
	
	
	@Test public void convertMapGetToAgetCurrentDevice() throws Exception{
		when(delegate.getCurrentDevice()).thenReturn(device);
		
		assertEquals(new JSONDevice(device).toMap(),(Map)map.get("getCurrentDevice"));
		
		verify(delegate).getCurrentDevice();
	}
	
	@Test public void convertMapPutToAgetCurrentDevice() throws Exception{
		when(delegate.getCurrentDevice()).thenReturn(device);
		
		assertEquals(new JSONDevice(device).toMap(),(Map)map.put("getCurrentDevice",null));
		
		verify(delegate).getCurrentDevice();
	}
	
	@Test public void convertMapPutToAlistDrivers() throws Exception{
		String driverName	= "d1";
		String instanceID	= "i1";
		List<DriverData> response = new ArrayList<DriverData>(); 
		UpDriver driver = new UpDriver("drv1");
		DriverData driverData = new DriverData(driver, device, instanceID);
		response.add(driverData);
		
		Map<String, Object> expected_data = new HashMap<String, Object>();
		expected_data.put("driver",new JSONDriver(driver).toMap());
		expected_data.put("device",new JSONDevice(device).toMap());
		expected_data.put("instanceID",instanceID);
		List<Map<String, Object>> expected = new ArrayList<Map<String, Object>>();
		expected.add(expected_data);
		
		when(delegate.listDrivers(driverName)).thenReturn(response);
		
		Map<String, Object> callParams = new HashMap<String, Object>();
		callParams.put("driverName", driverName);  
		
		assertEquals(expected,map.put("listDrivers",callParams));
		
		verify(delegate).listDrivers(driverName);
	}
}

