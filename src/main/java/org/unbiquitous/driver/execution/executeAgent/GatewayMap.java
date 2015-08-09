package org.unbiquitous.driver.execution.executeAgent;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.adaptabitilyEngine.NotifyException;
import org.unbiquitous.uos.core.adaptabitilyEngine.ServiceCallException;
import org.unbiquitous.uos.core.adaptabitilyEngine.UosEventListener;
import org.unbiquitous.uos.core.driverManager.DriverData;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;
import org.unbiquitous.uos.core.messageEngine.messages.Call;
import org.unbiquitous.uos.core.messageEngine.messages.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;


@SuppressWarnings("rawtypes")
public class GatewayMap implements Map {
	private static final ObjectMapper mapper = new ObjectMapper();
	
	final Gateway delegate;
	static HashMap globals = new HashMap();

	public GatewayMap(Gateway delegate) {
		this.delegate = delegate;
	}

	@Override
	@SuppressWarnings("unchecked")
	public Object put(Object key, Object value) {
		try {
			String method = (String) key;
			Map parameters = null;
			if (value instanceof Map){
				parameters = (Map) value;
			}
			if (method == "callService"){
				return callService(parameters);
			}else if (method == "registerForEvent"){
				return registerForEvent(parameters);
			}else if (method == "getCurrentDevice") {
				return mapper.readValue(delegate.getCurrentDevice().toString(), Map.class);
			}else if (method == "listDrivers") {
				return listDrivers(parameters);
			}else{
				globals.put(key,value);
			}
			
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
		return null;
	}

	private Object listDrivers(Map parameters) throws IOException {
		List<DriverData> listDrivers = delegate.listDrivers((String)parameters.get("driverName"));
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		if (listDrivers != null){
			for (DriverData data : listDrivers){
				Map<String, Object> new_rep = new HashMap<String, Object>();
				new_rep.put("driver", mapper.readValue(data.getDriver().toString(), Map.class));
				new_rep.put("device", mapper.readValue(data.getDevice().toString(), Map.class));
				new_rep.put("instanceID",data.getInstanceID());
				result.add(new_rep);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private Object callService(Map parameters) throws ServiceCallException, IOException {
		UpDevice device = mapper.treeToValue((JsonNode) parameters.get("device"), UpDevice.class); 
		Response response ;
		if (parameters.size() == 2){
			Call call = mapper.treeToValue((JsonNode) parameters.get("serviceCall"), Call.class);
			response = delegate.callService(	device, call);
		}else{
			response =  delegate.callService(	
					device, 
					(String)parameters.get("serviceName"),
					(String)parameters.get("driverName"),
					(String)parameters.get("instanceId"),
					(String)parameters.get("securityType"),
					(Map)parameters.get("parameters")
					);
		}
		return mapper.readValue(response.toString(), Map.class);
	}
	
	private Object registerForEvent(Map parameters) throws NotifyException, IOException {
		UosEventListener listener	= (UosEventListener) parameters.get("listener");
		UpDevice device = mapper.treeToValue((JsonNode) parameters.get("device"), UpDevice.class); 
		String driver				= (String)parameters.get("driver");
		String eventKey				= (String)parameters.get("eventKey");
		
		if (parameters.size() == 4){
			delegate.register(listener,device,driver,eventKey);
		}else{
			delegate.register(listener,device,driver,
								(String)parameters.get("instanceId"),eventKey);
		}
		return null;
	}
	
	@SuppressWarnings("static-access")
	public Object get(Object key) {
		String method = (String) key;
		if (method == "getCurrentDevice"){
			try {
				return mapper.readValue(delegate.getCurrentDevice().toString(), Map.class);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}else{
			return this.globals.get(key);
		}
	}
	
	public int size() {return 0;}

	public boolean isEmpty() {return false;}

	public boolean containsKey(Object key) {return false;}

	public boolean containsValue(Object value) {return false;}

	public Object remove(Object key) {return null;}

	public void putAll(Map m) {}

	public void clear() {}

	public Set keySet() {return null;}

	public Collection values() {return null;}

	public Set entrySet() {return null;}

}
