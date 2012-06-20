package org.unbiquitous.driver.execution;

import org.luaj.vm2.Lua;

@SuppressWarnings("unused")
public class MyJarAgent 
						extends JustACoolSuperclass 
						implements JustANiceInterface, JustAnotherNiceInterface{
	private JustAnAttributeClass myAttribute;
	
	private static final AConstantType constant= null;
	
	// Ingored primitive attributes
	private boolean ignoreBool;
	private byte ignoreByte;
	private char ignoreChar;
	private double ignoreDouble;
	private float ignoreFloat;
	private long ignoreLong;
	private int ignoreInt;
	private short ignoreShort;
	
	// Ignored JDK classes
	private Class<?> ignoredClazz;
	private Integer ignoredInteger;
	
	// Ignored Blacklisted classes
	private Lua ignoredLua;
	
	private void doNothing(AMethodParameter a, int ignored){}
	private AMethodReturnType doNothingAgain(){ return null;}
	public static AStaticReturnType doNothingStatic(){ return null;}
}

@SuppressWarnings("unused")
class JustAnAttributeClass{
	private AnotherAtributeClass otherAtribute;
}

@SuppressWarnings("unused")
class AnotherAtributeClass{
	// dont fall on a loop
	private JustAnAttributeClass trollAttribute;
}

class JustACoolSuperclass{}
interface JustANiceInterface{}
interface JustAnotherNiceInterface{}

class AMethodParameter{}
class AMethodReturnType{}
class AConstantType{}
class AStaticReturnType{}