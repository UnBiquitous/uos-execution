package org.unbiquitous.driver.execution.executionUnity;

import java.io.IOException;
import java.util.Iterator;
import java.util.Set;

import org.luaj.vm2.Globals;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

class ExecutionUnitySerializer {
	private static final ObjectMapper mapper = new ObjectMapper();

	private String script;
	private Set<String> stateKeys;
	private Globals _G;

	public ExecutionUnitySerializer(String script, Set<String> stateKeys, Globals _G) {
		super();
		this.script = script;
		this.stateKeys = stateKeys;
		this._G = _G;
	}

	public ObjectNode toJSON() {
		ObjectNode unity = mapper.createObjectNode();
		unity.put("script", script);
		unity.set("state", buildStateJSON());
		return unity;
	}

	private ObjectNode buildStateJSON() {
		ObjectNode state = mapper.createObjectNode();
		for (String key : stateKeys) {
			state.set(key, mapper.valueToTree(_G.get(key).toString()));
		}
		return state;
	}

	public static ExecutionUnity fromJSON(JsonNode json) {
		try {
			JsonNode script = json.get("script");
			ExecutionUnity ex = new ExecutionUnity((script != null && script.isTextual()) ? script.asText() : null);
			populateState(json, ex);
			return ex;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private static void populateState(JsonNode json, ExecutionUnity ex) throws IOException {
		JsonNode state = json.get("state");
		if (state != null) {
			Iterator<String> it = state.fieldNames();
			while (it.hasNext()) {
				String key = it.next();
				ex.setState(key, mapper.treeToValue(state.get(key), String.class));
			}
		}
	}
}
