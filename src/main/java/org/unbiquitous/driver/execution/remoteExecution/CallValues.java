package org.unbiquitous.driver.execution.remoteExecution;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class CallValues {

	private HashMap<Long, HashMap<String, String>> values = new HashMap<Long, HashMap<String,String>>();
	
	public void setValue(long id, String key, String value) {
		if (values.get(id) == null){
			values.put(id, new HashMap<String, String>());
		}
		values.get(id).put(key, value);
	}

	public String getValue(long id, String key) {
		if (values.get(id) == null){
			return null;
		}
		return values.get(id).
		get(key);
	}

	public List<String> getKeys(long id) {
		if (values.get(id) == null){
			return null;
		}
		ArrayList<String> keys = new ArrayList<String>();
		keys.addAll(values.get(id).keySet());
		return keys;
	}

	public void clearValues() {
		values.clear();
	}
}
