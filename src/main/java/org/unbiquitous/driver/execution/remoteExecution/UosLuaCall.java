package org.unbiquitous.driver.execution.remoteExecution;

import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;
import org.luaj.vm2.lib.VarArgFunction;

public class UosLuaCall extends VarArgFunction {
	
	private static CallValues values = new CallValues();
	
	public static CallValues values(){return values;}
	
	public UosLuaCall() {}
	
	public LuaValue invoke(Varargs args) {
		
		switch ( opcode ) {
	 		case 0: {
	 			LuaValue t = tableOf();
	 			this.bind(t, UosLuaCall.class, new String[] {"set", "get"}, 1 );
	 			env.set("Uos", t);
	 			return t;
	 		}
	 		case 1: { // set
	 			long	id = Long.parseLong(args.optstring(1, LuaString.valueOf("")).tojstring());
	 			String key = args.optstring(2, LuaString.valueOf("")).tojstring();
	 			String value = args.optstring(3, LuaString.valueOf("")).tojstring();
	 			values.setValue(id,key,value);
	 			return LuaValue.valueOf(value);
	 		}
	 		case 2: { // get
	 			long	id = Long.parseLong(args.optstring(1, LuaString.valueOf("")).tojstring());
	 			String key = args.optstring(2, LuaString.valueOf("")).tojstring();
	 			String value = values.getValue(id,key);
				return value == null?LuaValue.NIL:LuaValue.valueOf(value);
	 		}
	 		default: return error("bad opcode: "+opcode);
 		}

	}

}
