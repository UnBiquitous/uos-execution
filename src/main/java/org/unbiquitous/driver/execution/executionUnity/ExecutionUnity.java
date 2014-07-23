package org.unbiquitous.driver.execution.executionUnity;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Set;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.unbiquitous.json.JSONObject;

public class ExecutionUnity {

	private Globals _G;
	private String script;
	private Set<String> stateKeys = new HashSet<String>(); 

	public interface ExecutionHelper {
		String name();
		String invoke(String ... args);
	}
	
	public ExecutionUnity(String script) {
		this(script, null);
		this.script = script;
	}
	
	public ExecutionUnity(String script, final ExecutionHelper helper) {
		loadScript(script);
		addHelper(helper);
	}

	private void loadScript(String script) {
		_G = JsePlatform.standardGlobals();
		LuaClosure s = (LuaClosure) _G.load(new StringReader(script.toString()), "main.lua");
		s.call();
	}

	public Object call(String methodName) {
		LuaValue run = _G.get(methodName);
		return run.invoke().toString();
	}

	public void addHelper(ExecutionHelper helper) {
		HelperFunction function = new HelperFunction(helper);
		_G.set(function.name(), function);
	}

	public void setState(String key, Object value) {
		stateKeys.add(key);
		if(value == null){
			_G.set(key, LuaValue.NIL);
		}else{
			_G.set(key, value.toString());
		}
	}

	public JSONObject toJSON() {
		return new ExecutionUnitySerializer(script, stateKeys, _G).toJSON();
	}

	public static ExecutionUnity fromJSON(JSONObject json) {
		return ExecutionUnitySerializer.fromJSON(json);
	}
}
