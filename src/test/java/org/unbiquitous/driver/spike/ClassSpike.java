package org.unbiquitous.driver.spike;

import org.abstractmeta.toolbox.compilation.compiler.JavaSourceCompiler;
import org.abstractmeta.toolbox.compilation.compiler.impl.JavaSourceCompilerImpl;
import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.util.ClassPath.ClassFile;
import org.unbiquitous.driver.execution.MyJarAgent;

public class ClassSpike {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		final Class<MyJarAgent> evaluatedClass = MyJarAgent.class;
		final Class<?>[] declaredClasses = evaluatedClass.getDeclaredClasses();
		System.out.println(declaredClasses.length);
		for (Class<?> c :declaredClasses)
			System.out.println(c);
		
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
		}
		System.out.println("methods");
		for( Method method : clazz.getMethods()){
			System.out.println(method);
			System.out.println("\t"+method.getLocalVariableTable());
			System.out.println("\t"+method.getAttributes());
			System.out.println("\t"+method.getConstantPool());
			System.out.println("\t"+method.getReturnType());
		}
		

		String source = "package org.unbiquitous.driver.execution;"
				+ "public class Foo extends org.unbiquitous.driver.execution.MyAgent {"
				+ "public int plusOne(Integer i){" + "	return i+1;" + "}" + "}";

		String clazz2 = "org.unbiquitous.driver.execution.Foo";
		JavaSourceCompiler compiler = new JavaSourceCompilerImpl();
		JavaSourceCompiler.CompilationUnit unit = compiler.createCompilationUnit();
		unit.addJavaSource(clazz2, source);
		ClassLoader loader = compiler.compile(System.class.getClassLoader(), unit);
		
		final Class<?> repo = loader.loadClass(Repository.class.getName());
		final java.lang.reflect.Method lookup = repo.getMethod("lookupClass", String.class);
		System.out.println(lookup.invoke(null, new Object[]{clazz2}));
		
	}

}
