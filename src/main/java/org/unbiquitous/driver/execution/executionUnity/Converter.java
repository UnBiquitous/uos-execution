package org.unbiquitous.driver.execution.executionUnity;

import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.luaj.vm2.LuaString;
import org.luaj.vm2.LuaTable;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.Varargs;

public class Converter {

	public static String[] varargsToStringArray(Varargs args) {
		String []sargs = new String[args.narg()]; 
		for(int i = 0; i < args.narg(); i++){
			sargs[i] = args.arg(i+1).tojstring();
		}
		return sargs;
	}
	
	public static Varargs objectArrayToLuaVarArgs(Object... params) {
		LuaValue args[] = new LuaValue[params.length];
		for(int i = 0; i < params.length; i++){
			args[i] = objectToLuaValue(params[i]);
		}
		return LuaValue.varargsOf(args);
	}

	@SuppressWarnings("rawtypes")
	public static LuaValue objectToLuaValue(Object original) {
		LuaValue value;
		if(original instanceof Map){
			value = mapToLuaTable((Map) original);
		}else if(original instanceof Number){
			//TODO: We're losing precision for integers
			value = LuaValue.valueOf(Double.parseDouble(original.toString()));
		}else{
			value = LuaValue.valueOf(original.toString());
		}
		return value;
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static LuaValue mapToLuaTable(Map map) {
		LuaTable table = new LuaTable();
		for(Entry e : (Set<Entry>)map.entrySet()){
			table.set(LuaString.valueOf(e.getKey().toString()), 
					objectToLuaValue(e.getValue()));
		}
		return table;
	}
}
