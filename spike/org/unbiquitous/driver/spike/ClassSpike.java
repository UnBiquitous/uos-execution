package org.unbiquitous.driver.spike;

import java.lang.reflect.InvocationTargetException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Properties;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Attribute;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.InnerClass;
import org.apache.bcel.classfile.InnerClasses;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.util.ClassPath.ClassFile;
import org.unbiquitous.driver.execution.executeAgent.MyJarAgent;

@SuppressWarnings("unused")
public class ClassSpike {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		dependencyDiscovery();
//		properyFun();
//		threadStackFun();
//		for(Object key :properties.keySet()){
//			printProp(properties, key.toString());
//		}
	}

	private static void threadStackFun() {
		System.out.println("Stack");
		Runnable r = new Runnable() {
			public void run() {
				Runnable r = new Runnable() {
					
					public void run() {
						for(StackTraceElement s : Thread.currentThread().getStackTrace()){
							System.out.println("\t"+s);
							System.out.println("\t\t"+s.getMethodName());
							System.out.println("\t\t"+s.getClassName());
						}
					}
				};
				r.run();
			}
		};
		r.run();
	}

	private static void properyFun() throws UnknownHostException {
		final Properties properties = System.getProperties();
		System.out.println("MyNameIs:"+InetAddress.getLocalHost().getHostName());
		printProp(properties, "os.arch");
		printProp(properties, "os.name");
		printProp(properties, "os.version");
		printProp(properties, "user.dir");
		printProp(properties, "user.name");
		printProp(properties, "user.home");
		System.out.println();
		printProp(properties, "java.version");
		printProp(properties, "java.specification.version");
		printProp(properties, "java.specification.vendor");
		printProp(properties, "java.specification.name");///////////////
		printProp(properties, "java.vm.version");
		printProp(properties, "java.vm.vendor");
		printProp(properties, "java.vm.name");
		printProp(properties, "java.vm.specification.version");
		printProp(properties, "java.vm.specification.vendor");
		printProp(properties, "java.vm.specification.name");
		System.out.println();
		System.out.println(System.getenv("ANDROID_HOME"));
	}

	private static void printProp(final Properties properties, final String prop) {
		System.out.println(prop+":"+properties.getProperty(prop));
	}

	private static void dependencyDiscovery() throws ClassNotFoundException,
			NoSuchMethodException, IllegalAccessException,
			InvocationTargetException {
		final Class<MyJarAgent> evaluatedClass = MyJarAgent.class;
		
		bcelWay(evaluatedClass);
		reflectionWay(evaluatedClass);
	}

	private static void reflectionWay(final Class<MyJarAgent> evaluatedClass) {
		System.out.println("\n\n----------reflectionWay----------\n\n");
		final Class<?>[] declaredClasses = evaluatedClass.getDeclaredClasses();
		System.out.println(declaredClasses.length);
		for (Class<?> c :declaredClasses)
			System.out.println(c);
		System.out.println("other way inner");
		for(Class<?> c :evaluatedClass.getDeclaredClasses()){
			System.out.println(c);
		}
		
		System.out.println("super:"+evaluatedClass.getSuperclass());
		System.out.println("interfaces:");
		for(Class<?> i :evaluatedClass.getInterfaces()){
			System.out.println("\t"+i);
		}
		System.out.println("attributes:");
		for(java.lang.reflect.Field f :evaluatedClass.getDeclaredFields()){
			System.out.println("\t"+f);
			System.out.println("\t\t"+f.getType());
			System.out.println("\t\t\t"+f.getType().getComponentType());
			System.out.println("\t\t"+f.getGenericType());
			System.out.println("\t\t"+f.getModifiers());
			System.out.println("\t\t"+f.getDeclaringClass());
		}
		System.out.println("methods:");
		for (java.lang.reflect.Method m:evaluatedClass.getMethods()){
			System.out.println("\t"+m.getName());
			for(Class<?> p: m.getParameterTypes()){
				System.out.println("\t\t"+p);
			}
			System.out.println("\tr\t"+m.getReturnType());
		}
	}

	private static void bcelWay(final Class<MyJarAgent> evaluatedClass)
			throws ClassNotFoundException {
		System.out.println("\n\n----------bcelWay----------\n\n");
		final JavaClass clazz = Repository.lookupClass(evaluatedClass);
		final ClassFile clazzFile = Repository.lookupClassFile(evaluatedClass.getName());
		System.out.println(clazzFile.getPath());

		System.out.println("superclasses");
		for( JavaClass sClazz : clazz.getSuperClasses())
			System.out.println(sClazz.getClassName());
		System.out.println("fields");
		for( Field field : clazz.getFields()){
			System.out.println("\t"+field);
			System.out.println("\t\t"+field.getType());
			System.out.println("\t\t"+field.getType().getClass());
			System.out.println("\t\t"+field.getType().getSignature());
			if (field.getType() instanceof ArrayType){
				ArrayType at  = (ArrayType)field.getType();
				System.out.println("\t\t\t"+at.getElementType());
			}
		}
		System.out.println("methods");
		for( Method method : clazz.getMethods()){
			System.out.println(method);
//			System.out.println("\t"+method.getLocalVariableTable());
//			System.out.println("\t"+method.getAttributes());
//			System.out.println("\t"+method.getConstantPool());
			System.out.println("\t"+method.getReturnType());
			System.out.println("\tv:");
			for (LocalVariable v : method.getLocalVariableTable()
					.getLocalVariableTable()){
				System.out.println("\t\t"+v);
				System.out.println("\t\t\t"+v.getName());
				System.out.println("\t\t\t"+v.getNameIndex());
				System.out.println("\t\t\t"+v.getSignature());
			}
			System.out.println("\te:");
			if (method.getExceptionTable() != null){
				for(String e:method.getExceptionTable().getExceptionNames()){
					System.out.println("\t\t"+e);
				}
			}
		}
		System.out.println("Attributes");
		for (Attribute att: clazz.getAttributes()){
			System.out.print("\t"+att);
			System.out.println("\t"+att.getClass());
			if (att instanceof InnerClasses){
				final InnerClasses innerClasses = (InnerClasses)att;
				System.out.println(innerClasses.getNameIndex());
				for (InnerClass inner: innerClasses.getInnerClasses()){
					System.out.println("\t\t"+inner);
				}
			}
		}
	}

}
