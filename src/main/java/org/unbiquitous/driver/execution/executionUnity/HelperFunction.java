package org.unbiquitous.driver.execution.executionUnity;

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
		String[] sargs = Converter.varargsToStringArray(args);
		return delegateToHelper(helper, sargs);
	}

	private Varargs delegateToHelper(ExecutionHelper helper, String[] sargs) {
		if(helper != null){
			Object value = helper.invoke(sargs);
			if(value != null){
				return Converter.objectArrayToLuaVarArgs(value);	
			}
		}
		return varargsOf(new LuaValue[]{LuaValue.NIL});
	}
}