package org.unbiquitous.driver.execution.remoteExecution;

import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.ThreeArgFunction;
import org.luaj.vm2.lib.TwoArgFunction;
import org.luaj.vm2.lib.VarArgFunction;

public class UosLuaCall extends VarArgFunction {
	
	private static CallValues values = new CallValues();
	
	public static CallValues values(){return values;}
	
	public UosLuaCall() {}
	
	@Override
	public LuaValue call(LuaValue modname, LuaValue env) {
		LuaValue library = tableOf();
		library.set( "set", new set() );
		library.set( "get", new get() );
		env.set( "Uos", library );
		return library;
	}

	static class set extends ThreeArgFunction {
		public LuaValue call(LuaValue arg1, LuaValue arg2, LuaValue arg3) {
			long	id = Long.parseLong(arg1.tojstring());
 			String key = arg2.tojstring();
 			String value = arg3.tojstring();
 			values.setValue(id,key,value);
 			return LuaValue.valueOf(value);
		}
	}
	
	static class get extends TwoArgFunction {
		public LuaValue call(LuaValue arg1, LuaValue arg2) {
			long	id = Long.parseLong(arg1.tojstring());
 			String key = arg2.tojstring();
 			String value = values.getValue(id,key);
			return value == null?LuaValue.NIL:LuaValue.valueOf(value);
		}
	}
	
}
