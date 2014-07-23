package org.unbiquitous.driver.execution.executionUnity;

import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;
import org.unbiquitous.driver.execution.executionUnity.ExecutionUnity.ExecutionHelper;

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