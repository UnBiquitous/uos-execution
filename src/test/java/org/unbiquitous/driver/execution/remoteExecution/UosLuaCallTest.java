package org.unbiquitous.driver.execution.remoteExecution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.luaj.vm2.Globals;
import org.luaj.vm2.LuaError;
import org.luaj.vm2.LuaValue;
import org.luaj.vm2.lib.jse.JsePlatform;


public class UosLuaCallTest {
	private Globals global;
	private CallValues values;
	
	@Before
	public void setUp(){
		global = JsePlatform.standardGlobals();
		values = UosLuaCall.values();
	}
	
	@After
	public void tearDown(){
		values.clearValues();
	}
	
	@Test
	public void setsValue() throws Exception{
		StringBuffer script = createBaseScript(1);
		script.append("Uos.set(UOS_ID,'a',5)\n");
		
		global.load( new StringReader(script.toString()), "myscript" ).call();
		
		assertEquals("5",values.getValue(1,"a"));
	}
	
	@Test
	public void setsDifferentValuesForDifferentKeys() throws Exception{
		StringBuffer script = createBaseScript(1);
		script.append("Uos.set(UOS_ID,'a',5)\n");
		script.append("Uos.set(UOS_ID,'b',7)\n");
		
		global.load( new StringReader(script.toString()), "myscript" ).call();
		
		assertEquals("5",values.getValue(1,"a"));
		assertEquals("7",values.getValue(1,"b"));
	}

	@Test
	public void setsDifferentValuesForDifferentUOS_IDs() throws Exception{
		StringBuffer script = createBaseScript(1);
		script.append("Uos.set(UOS_ID,'a',5)\n");
		
		global.load( new StringReader(script.toString()), "myscript").call();
		
		StringBuffer script2 = createBaseScript(2);
		script2.append("Uos.set(UOS_ID,'a',15)\n");
		global.load( new StringReader(script2.toString()), "myscript2").call();
		
		assertEquals("5",values.getValue(1,"a"));
		assertEquals("15",values.getValue(2,"a"));
	}
	
	@Test
	public void getsValue() throws Exception{
		values.setValue(1,"b","7");
		StringBuffer script = createBaseScript(1);
		script.append("Uos.set(UOS_ID,'a',Uos.get(UOS_ID,'b'))\n");
		
		global.load( new StringReader(script.toString()), "myscript").call();
		
		assertEquals("7",values.getValue(1,"a"));
	}
	
	@Test
	public void getsValueFromDifferentUosIDs() throws Exception{
		values.setValue(1,"b","7");
		StringBuffer script = createBaseScript(1);
		script.append("Uos.set(UOS_ID,'a',Uos.get(UOS_ID,'b'))\n");
		
		global.load( new StringReader(script.toString()), "myscript").call();
		
		values.setValue(2,"b","8");
		StringBuffer script2 = createBaseScript(2);
		script2.append("Uos.set(UOS_ID,'a',Uos.get(UOS_ID,'b'))\n");
		
		global.load( new StringReader(script2.toString()), "myscript").call();
		
		assertEquals("7",values.getValue(1,"a"));
		assertEquals("8",values.getValue(2,"a"));
	}
	
	@Test
	public void getsEmptyFromAnUnknownKey() throws Exception{
		StringBuffer script = createBaseScript(1);
		script.append("Uos.set(UOS_ID,'a',Uos.get(UOS_ID,'b'))\n");
		
		global.load( new StringReader(script.toString()), "myscript").call();
		
		assertEquals(LuaValue.NIL.toString(),values.getValue(1,"a"));
	}
	
	@Test(expected=LuaError.class)
	public void failsOnUnkownFunction() throws Exception{
		StringBuffer script = createBaseScript(1);
		script.append("Uos.sbrubbles(UOS_ID,'oh yeah')\n");
		
		global.load( new StringReader(script.toString()), "myscript").call();
	}
	
	@Test
	public void returnsDataKeys(){
		values.setValue(0, "a", "1");
		values.setValue(0, "b", "2");
		values.setValue(1, "c", "3");
		
		assertEquals(2, values.getKeys(0).size());
		assertEquals(1, values.getKeys(1).size());
		assertTrue(values.getKeys(0).contains("a"));
		assertTrue(values.getKeys(0).contains("b"));
		assertEquals("c", values.getKeys(1).get(0));
		assertNull(values.getKeys(2));
	}
	
	@Test
	public void clearsData(){
		values.setValue(0, "a", "1");
		assertNotNull(values.getValue(0, "a"));
		values.clearValues();
		assertNull(values.getValue(0, "a"));
	}
	
	private StringBuffer createBaseScript(int id) {
		StringBuffer script = new StringBuffer();
		script.append("UOS_ID="+id+"\n");
		script.append("require( '"+UosLuaCall.class.getName()+"' )\n");
		return script;
	}
}
