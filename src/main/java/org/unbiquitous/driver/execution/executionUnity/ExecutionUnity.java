package org.unbiquitous.driver.execution.executionUnity;

import java.io.StringReader;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;
import org.unbiquitous.driver.execution.executionUnity.ExecutionUnity.ExecutionHelper;
import org.unbiquitous.json.JSONException;
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

class HelperFunction extends VarArgFunction {
	private ExecutionHelper helper;

	public HelperFunction(ExecutionHelper helper) {
		this.helper = helper;
	}
	
	public String name() {
		String name = "help";
		if(helper != null && helper.name() != null){
			name = helper.name();
		}
		return name;
	}
	
	@Override
	public Varargs invoke(Varargs args) {
		String[] sargs = varargsToStringArray(args);
		return delegateToHelper(helper, sargs);
	}

	private Varargs delegateToHelper(ExecutionHelper helper, String[] sargs) {
		if(helper != null){
			return varargsOf(new LuaValue[]{LuaString.valueOf(helper.invoke(sargs))});	
		}
		return varargsOf(new LuaValue[]{LuaValue.NIL});
	}

	private String[] varargsToStringArray(Varargs args) {
		String []sargs = new String[args.narg()]; 
		for(int i = 0; i < args.narg(); i++){
			sargs[i] = args.arg(i+1).tojstring();
		}
		return sargs;
	}
}
