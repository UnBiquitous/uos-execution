package org.unbiquitous.driver.execution.executeAgent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.unbiquitous.json.JSONException;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.adaptabitilyEngine.NotifyException;
import org.unbiquitous.uos.core.adaptabitilyEngine.ServiceCallException;
import org.unbiquitous.uos.core.adaptabitilyEngine.UosEventListener;
import org.unbiquitous.uos.core.driverManager.DriverData;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;
import org.unbiquitous.uos.core.messageEngine.dataType.json.JSONDevice;
import org.unbiquitous.uos.core.messageEngine.dataType.json.JSONDriver;
import org.unbiquitous.uos.core.messageEngine.messages.ServiceCall;
import org.unbiquitous.uos.core.messageEngine.messages.ServiceResponse;
import org.unbiquitous.uos.core.messageEngine.messages.json.JSONServiceCall;
import org.unbiquitous.uos.core.messageEngine.messages.json.JSONServiceResponse;


@SuppressWarnings("rawtypes")
public class GatewayMap implements Map{

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
				return new JSONDevice(delegate.getCurrentDevice()).toMap();
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

	private Object listDrivers(Map parameters) throws JSONException {
		List<DriverData> listDrivers = delegate.listDrivers((String)parameters.get("driverName"));
		List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
		if (listDrivers != null){
			for (DriverData data : listDrivers){
				Map<String, Object> new_rep = new HashMap<String, Object>();
				new_rep.put("driver",new JSONDriver(data.getDriver()).toMap());
				new_rep.put("device",new JSONDevice(data.getDevice()).toMap());
				new_rep.put("instanceID",data.getInstanceID());
				result.add(new_rep);
			}
		}
		return result;
	}

	@SuppressWarnings("unchecked")
	private Object callService(Map parameters) throws ServiceCallException, JSONException {
		UpDevice device = new JSONDevice((Map) parameters.get("device")).getAsObject();
		ServiceResponse response ;
		if (parameters.size() == 2){
			ServiceCall call = new JSONServiceCall(
											(Map)parameters.get("serviceCall")
													).getAsObject();
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
		return new JSONServiceResponse(response).toMap();
	}
	
	private Object registerForEvent(Map parameters) throws NotifyException, JSONException {
		UosEventListener listener	= (UosEventListener) parameters.get("listener");
		UpDevice device = new JSONDevice((Map) parameters.get("device")).getAsObject();
		String driver				= (String)parameters.get("driver");
		String eventKey				= (String)parameters.get("eventKey");
		
		if (parameters.size() == 4){
			delegate.registerForEvent(listener,device,driver,eventKey);
		}else{
			delegate.registerForEvent(listener,device,driver,
								(String)parameters.get("instanceId"),eventKey);
		}
		return null;
	}
	
	@SuppressWarnings("static-access")
	public Object get(Object key) {
		String method = (String) key;
		if (method == "getCurrentDevice"){
			try {
				return new JSONDevice(delegate.getCurrentDevice()).toMap();
			} catch (JSONException e) {
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
