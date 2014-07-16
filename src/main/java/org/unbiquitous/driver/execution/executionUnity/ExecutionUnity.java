package org.unbiquitous.driver.execution.executionUnity;

import java.io.StringReader;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.luaj.vm2.lib.jse.JsePlatform;

public class ExecutionUnity {

	private Globals _G;
	private ExecutionHelper helper;

	public interface ExecutionHelper {
		String name();
		String invoke(String ... args);
	}
	
	public ExecutionUnity(String script) {
		this(script, null);
	}
	
	public ExecutionUnity(String script, final ExecutionHelper helper) {
		this.helper = helper;
		loadScript(script);
		String name = "help";
		if(helper != null && helper.name() != null){
			name = helper.name();
		}
		_G.set(name, new VarArgFunction() {
			@Override
			public Varargs invoke(Varargs args) {
				String []sargs = new String[args.narg()]; 
				for(int i = 0; i < args.narg(); i++){
					sargs[i] = args.arg(i+1).tojstring();
				}
				if(helper != null){
					return varargsOf(new LuaValue[]{LuaString.valueOf(helper.invoke(sargs))});	
				}
				return varargsOf(new LuaValue[]{LuaValue.NIL});
			}
		});
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

}
