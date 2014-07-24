package org.unbiquitous.driver.execution.executionUnity;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
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

	public Object call(String methodName, Object ... params) {
		LuaValue run = retrieveMethod(methodName);
		Varargs args = convertToLuaVarArgs(params);
		return run.invoke(args).toString();
	}

	private LuaValue retrieveMethod(String methodName) {
		LuaValue run = _G.get(methodName);
		if(run == null || run == LuaValue.NIL){
			throw new ExecutionError("Method '"+methodName+"' not found on execution unity.");
		}
		return run;
	}

	private Varargs convertToLuaVarArgs(Object... params) {
		LuaValue args[] = new LuaValue[params.length];
		for(int i = 0; i < params.length; i++){
			args[i] = convertObjectToLuaValue(params[i]);
		}
		return LuaValue.varargsOf(args);
	}

	@SuppressWarnings("rawtypes")
	private LuaValue convertObjectToLuaValue(Object original) {
		LuaValue value;
		if(original instanceof Map){
			value = convertMapToLuaTable((Map) original);
		}else{
			value = LuaValue.valueOf(original.toString());
		}
		return value;
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private LuaValue convertMapToLuaTable(Map map) {
		LuaValue value;
		LuaTable table = new LuaTable();
		for(Entry e : (Set<Entry>)map.entrySet()){
			table.set(LuaString.valueOf(e.getKey().toString()), 
					convertObjectToLuaValue(e.getValue()));
		}
		value = table;
		return value;
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
