package org.unbiquitous.driver.spike;

import java.io.StringReader;

import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaClosure;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;


public class LuaSpike {

	public static void main(String[] args) throws Exception {
		StringBuffer script = new StringBuffer();
		script.append("count  = 0 \n");
		script.append("function run() \n");
		script.append("		count = count +1\n");
		script.append("		print ('run', count)\n");
		script.append("end\n");
		script.append("function stop() \n");
		script.append("		return 0\n");
		script.append("end\n");
		script.append("a = {}\n");
		script.append("a['run'] = run\n");
		script.append("a['run']()\n");
		script.append("print 'hello, world'\n");
//		script.append("print a\n");
		script.append("run()\n");
		script.append("print '-------------'");
//		InputStream file = new StringInputStream(script.toString());
//		LuaValue code = LoadState.load( file, "script_", JsePlatform.standardGlobals() ).call();
//		LuaValue _G = JsePlatform.standardGlobals();
//		_G.get("dostring").call( LuaValue.valueOf(script.toString()));
//		LuaValue run = _G.get("run");
		
		Globals _G = JsePlatform.standardGlobals();
		
		LuaClosure s = (LuaClosure) _G.load(new StringReader(script.toString()), "main.lua");
//		InputStream is = new ByteArrayInputStream(script.toString().getBytes());
//		LuaFunction s = (LuaFunction) LuaC.instance.load(is, "script", _G);
		s.call();
		LuaValue run = _G.get("run");
		run.invoke();
		run.invoke();
		
		System.out.println("+++++++++++++");
		
		_G = JsePlatform.standardGlobals();
		s = (LuaClosure) _G.load(new StringReader(script.toString()), "main2.lua");
		s.call();
		LuaValue run2 = _G.get("run");
		run2.invoke();
		System.out.println("----");
		run.invoke();
	}
	
}
