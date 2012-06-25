package org.unbiquitous.driver.execution;

import java.util.List;

import org.luaj.vm2.Lua;

@SuppressWarnings("unused")
public class MyJarAgent 
						extends JustACoolSuperclass 
						implements JustANiceInterface, JustAnotherNiceInterface{
	private JustAnAttributeClass myAttribute;
	private JustAnArrayAtributeClass[] myArrayAttribute;
	private JustAnMultiArrayAtributeClass[][][][] myMultiArrayAttribute;
	// TODO: http://stackoverflow.com/questions/1942644/get-generic-type-of-java-util-list
//	private List<JustAGenericReferencedAtributeClass> myGenericList;
	
	private static final AConstantType constant= null;
	
	class Inner{
		private int attribute;
	}
	
	// Ingored primitive attributes
	private boolean ignoreBool;
	private byte ignoreByte;
	private char ignoreChar;
	private double ignoreDouble;
	private float ignoreFloat;
	private long ignoreLong;
	private int ignoreInt;
	private short ignoreShort;
	private int[][][] ignoreIntArray;
	
	// Ignored JDK classes
	private Class<?> ignoredClazz;
	private Integer ignoredInteger;
	
	// Ignored Blacklisted classes
	private Lua ignoredLua;
	
	private void doNothing(AMethodParameter a, int ignored, 
							AMethodArrayParameter[][][][] multiArray,
							int[][][] ignoreIntArray){}
	private AMethodReturnType doNothingAgain(){ return null;}
	private AMethodArrayReturnType[][][][] keepDoingNothingAgain(){ return null;}
	public static AStaticReturnType doNothingStatic(){ return null;}
	public void innerParameter(){ AInnerMethodUsedType type = null;}
	public void thrower() throws AnException{}
}

@SuppressWarnings("unused")
class JustAnAttributeClass{
	private AnotherAtributeClass otherAtribute;
}
class JustAnArrayAtributeClass{}
class JustAnMultiArrayAtributeClass{}
class JustAGenericReferencedAtributeClass{}

@SuppressWarnings("unused")
class AnotherAtributeClass{
	// dont fall on a loop
	private JustAnAttributeClass trollAttribute;
}

class JustACoolSuperclass{}
interface JustANiceInterface{}
interface JustAnotherNiceInterface{}

class AMethodParameter{}
class AMethodArrayParameter{}
class AMethodReturnType{}
class AMethodArrayReturnType{}
class AConstantType{}
class AStaticReturnType{}
class AInnerMethodUsedType{}
class AnException extends Exception{
	private static final long serialVersionUID = 1L;
}