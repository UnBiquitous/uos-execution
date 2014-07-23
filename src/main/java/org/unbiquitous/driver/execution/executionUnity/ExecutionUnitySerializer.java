package org.unbiquitous.driver.execution.executionUnity;

import java.util.Iterator;
import java.util.Set;

import org.luaj.vm2.Globals;
import org.unbiquitous.json.JSONException;
import org.unbiquitous.json.JSONObject;

class ExecutionUnitySerializer {
	private String script;
	private Set<String> stateKeys;
	private Globals _G;
	
	public ExecutionUnitySerializer(String script, Set<String> stateKeys,
			Globals _G) {
		super();
		this.script = script;
		this.stateKeys = stateKeys;
		this._G = _G;
	}

	public JSONObject toJSON() {
		try {
			JSONObject unity = new JSONObject();
			unity.put("script", script);
			unity.put("state", buildStateJSON());
			return unity;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	private JSONObject buildStateJSON() throws JSONException {
		JSONObject state = new JSONObject();
		for(String key:stateKeys){
			state.put(key, _G.get(key));
		}
		return state;
	}

	public static ExecutionUnity fromJSON(JSONObject json) {
		try {
			ExecutionUnity ex = new ExecutionUnity(json.optString("script"));
			populateState(json, ex);
			return ex;
		} catch (JSONException e) {
			throw new RuntimeException(e);
		}
	}

	@SuppressWarnings("unchecked")
	private static void populateState(JSONObject json, ExecutionUnity ex)
			throws JSONException {
		JSONObject state = json.optJSONObject("state");
		if(state != null){
			Iterator<String> it = state.keys();
			while(it.hasNext()){
				String key = it.next();
				ex.setState(key, state.get(key));
			}
		}
	}
}