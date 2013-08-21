package org.unbiquitous.driver.execution.executeAgent;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArrayType;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.Type;

/*
 * I could use http://code.google.com/p/java-dependency-resolver/
 * but using BCEL i was able to bypass the limitations of reflections regarding
 * non-public methods and its inner properties
 */
class JarPackager {
	
	ClassToolbox toolbox;
	Set<Class<?>> processedClasses = new HashSet<Class<?>>();
	
	public JarPackager(ClassToolbox toolbox) {
		this.toolbox = toolbox;
	}

	File packageJar(Class<?> clazz, File path) throws Exception{
		packageClass(clazz, path);
		
		File jar =  File.createTempFile("uExe", System.nanoTime()+".jar");
		final ZipOutputStream zos = new ZipOutputStream( new FileOutputStream( jar ) );
		zip(path, path, zos);
		zos.close();
		return jar;
	}
	
	protected void packageClass(Class<?> clazz, File path) throws IOException,
			FileNotFoundException, ClassNotFoundException {
		if (!processedClasses.contains(clazz)) {
			final InputStream bytecode = toolbox.findClass(clazz);
			if (bytecode == null) return; //cut not found classes
			processedClasses.add(clazz);
			toolbox.writeClassFileOnPath(clazz.getName(), bytecode, path);

			packageFields(clazz, path);
			packageSuperclass(clazz, path);
			packageInnerClasses(clazz, path);
			packageInterfaces(clazz, path);
			packageMethods(clazz, path);
		}
	}

	private void packageMethods(Class<?> clazz, File path)
			throws ClassNotFoundException, IOException, FileNotFoundException {
		final JavaClass rClazz = Repository.lookupClass(clazz);
		for (Method method : rClazz.getMethods()) {
			packageMethodArguments(path, method);
			packageMethodLocalVariables(path, method);
			packageMethodExceptions(path, method);
			packageMethodReturnType(path, method);
		}
	}

	private void packageMethodReturnType(File path, Method method)
			throws IOException, FileNotFoundException, ClassNotFoundException {
		packageBCELType(method.getReturnType(), path);
	}

	private void packageMethodArguments(File path, Method method)
			throws IOException, FileNotFoundException, ClassNotFoundException {
		for (Type t : method.getArgumentTypes()) {
			packageBCELType(t, path);
		}
	}
	
	private void packageBCELType(Type t, File path)
			throws IOException, FileNotFoundException, ClassNotFoundException {
		if (!(t instanceof BasicType)) {
			Type type = t;
			while (type instanceof ArrayType)
				type = ((ArrayType)type).getElementType();
			if (!(type instanceof BasicType)) 
				packageClass(Class.forName(type.toString()), path);
		}
	}
	
	private void packageMethodExceptions(File path, Method method)
			throws IOException, FileNotFoundException, ClassNotFoundException {
		if (method.getExceptionTable() != null){
			for(String e:method.getExceptionTable().getExceptionNames()){
				packageClass(Class.forName(e), path);
			}
		}
	}

	private void packageMethodLocalVariables(File path, Method method)
			throws IOException, FileNotFoundException, ClassNotFoundException {
		if (method.getLocalVariableTable() != null){
			for (LocalVariable v : method.getLocalVariableTable()
												.getLocalVariableTable()){
				// signature is in the format Lorg/unbiquitous/driver/execution/AMethodParameter;
				String name = v.getSignature()
						.replaceAll("\\[", "") //get array root type
						.replace('/', '.'); // change dash for dot
				if (name.length() > 2 ){ // get rid of primitive types
					name = name.substring(1,name.length()-1); // remove L
					packageClass(Class.forName(name), path);
				}
			}
		}
	}

	private void packageInterfaces(Class<?> clazz, File path)
			throws IOException, FileNotFoundException, ClassNotFoundException {
		for (Class<?> i : clazz.getInterfaces()) {
			packageClass(i, path);
		}
	}

	private void packageInnerClasses(Class<?> clazz, File path)
			throws IOException, FileNotFoundException, ClassNotFoundException {
		for (Class<?> s : clazz.getDeclaredClasses()) {
			packageClass(s, path);
		}
	}

	private void packageSuperclass(Class<?> clazz, File path)
			throws IOException, FileNotFoundException, ClassNotFoundException {
		if (clazz.getSuperclass() != null) 
			packageClass(clazz.getSuperclass(), path);
	}

	private void packageFields(Class<?> clazz, File path) throws IOException,
			FileNotFoundException, ClassNotFoundException {
		for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
			Class<?> type = findRootTypeFromArrays(f); 
			packageClass(type, path);
		}
	}

	private Class<?> findRootTypeFromArrays(java.lang.reflect.Field f) {
		Class<?> type = f.getType();
		while(type.isArray())	type = type.getComponentType();
		return type;
	}

	protected void zip(File directory, File base, ZipOutputStream zos) throws IOException {
		File[] files = directory.listFiles();
		byte[] buffer = new byte[8192];
		int read = 0;
		for (int i = 0, n = files.length; i < n; i++) {
			if (files[i].isDirectory()) {
				zip(files[i], base, zos);
			} else {
				FileInputStream in = new FileInputStream(files[i]);
				ZipEntry entry = new ZipEntry(files[i].getPath().substring(
						base.getPath().length() + 1));
				zos.putNextEntry(entry);
				while (-1 != (read = in.read(buffer))) {
					zos.write(buffer, 0, read);
				}
				in.close();
			}
		}
	}
}