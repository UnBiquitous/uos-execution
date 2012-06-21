package org.unbiquitous.driver.execution;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.Field;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.LocalVariableTable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.BasicType;
import org.apache.bcel.generic.Type;

/**
 * This class is responsible for all logic regarding byte code mumblejumbo.
 * In a nutshell, it'll handle loading, finding and packaging all classes
 * for an easy mobility logic.
 * 
 * @author Fabricio Nogueira Buzeto
 *
 */
public class ClassToolbox {
	
	private Set<String> blacklist =  new HashSet<String>();
	private Map<Class<?>,InputStream> cache =  new HashMap<Class<?>, InputStream>();
	
	public void add2BlackList(String jarName) {	blacklist.add(jarName);}
	public Set<String> blacklist(){ return blacklist;};
	
	protected InputStream findClass(Class<?> clazz) throws IOException {
		
		if (cache.containsKey(clazz)) return cache.get(clazz);
		
		String className = clazz.getName().replace('.', File.separatorChar);
		for (String entryPath : System.getProperty("java.class.path")
								.split(System.getProperty("path.separator"))) {
			File entry = new File(entryPath);
			if (entry.isDirectory() && !inBlackList(entry.getPath())){
				File found = findClassFileOnDir(className, entry);
				if (found != null)	{
					final FileInputStream stream = new FileInputStream(found);
					cache.put(clazz, stream);
					return stream;
				}
			}else if (entry.getName().endsWith(".jar") && 
						!inBlacklist(entry.getName())) {
				InputStream result = findClassFileOnAJar(className, entry);
				if (result != null) {
					cache.put(clazz, result);
					return result;
				}
			}
		}
		cache.put(clazz, null);
		return null;
	}

	private boolean inBlackList(String path) {
		for (String black : blacklist)
			if (path.contains(black))	{
				return true;
			}
		return false;
	}
	private boolean inBlacklist(String jarName) {
		return blacklist.contains(jarName);
	}

	private InputStream findClassFileOnAJar(String className, File jar) throws FileNotFoundException, IOException {
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(new FileInputStream(jar)));
		ZipEntry j;
		List<Byte> bytes = new ArrayList<Byte>();
		while ((j = zis.getNextEntry()) != null) {
			final int BUFFER = 2048;
			byte data[] = new byte[BUFFER];
			int count = 0;
			while ((count = zis.read(data, 0, BUFFER)) != -1) {
				if (j.getName().equals(className + ".class")) {
					for (int i = 0; i < count; i++)
						bytes.add(data[i]);
				}
			}
			if (!bytes.isEmpty()) {
				byte[] buf = new byte[bytes.size()];
				for (int i = 0; i < bytes.size(); i++)
					buf[i] = bytes.get(i);
				zis.close();
				return new ByteArrayInputStream(buf);
			}

		}
		zis.close();
		return null;
	}

	private File findClassFileOnDir(String classname, File entry) throws IOException{
		if (entry.isDirectory()){
			for (File child : entry.listFiles()){
				File found = findClassFileOnDir(classname,child);
				if (found != null)	return found;
			}
		}else if (entry.getPath().endsWith(classname+".class")){
			return entry;
	    }
		return null;
	}
	
	//TODO: Check how to work this out when at android.
	/*
	 * In Android we must follow some steps like:
	 * 
	 * 1 : The input must be a optimized jar
	 * 2 : This Jar must be put on a temporary folder that is writable
	 * 3 : The Loader must be a DexClassLoader
	 * 		ex: new DexClassLoader(jarPath,parentFile.getPath(),null, getClassLoader());
	 */
	protected ClassLoader load(String className, InputStream clazz) throws Exception {
		File classDir = createClassFileDir(className, clazz);
		return new URLClassLoader(new URL[] { classDir.toURI().toURL() },ClassLoader.getSystemClassLoader());
//		addPathToClassLoader(classDir);
	}

//	@SuppressWarnings({ "rawtypes", "unchecked" })
//	private void addPathToClassLoader(File classDir)
//			throws NoSuchMethodException, IllegalAccessException,
//			InvocationTargetException, MalformedURLException {
//		URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
//		Class sysclass = URLClassLoader.class;
//		Method method = sysclass.getDeclaredMethod("addURL", URL.class);
//		method.setAccessible(true);
//		method.invoke(sysloader, new Object[] { classDir.toURI().toURL() });
	
	
//	ClassLoader sysloader = ClassLoader.getSystemClassLoader();
//	Class sysclass = ClassLoader.class;
//	Method method = sysclass.getDeclaredMethod("defineClass", 
//				new Class[]{String.class,byte[].class,int.class,int.class});
//	method.setAccessible(true);
//	method.invoke(sysloader, new Object[] { "org.unbiquitous.driver.execution.Fui", 
//										codeArray, 0, codeArray.length })
//	}

	private File createClassFileDir(String className, InputStream clazz)
			throws IOException, FileNotFoundException {
		File tempDir = temporaryDir();
		writeClassFileOnPath(className, clazz, tempDir);
		return tempDir;
	}
	
	protected void writeClassFileOnPath(String className, InputStream clazzByteCode,
			File path) throws IOException, FileNotFoundException {
		File classFile = new File(path.getPath()+"/"+className.replace('.', '/')+".class");
		classFile.getParentFile().mkdirs();
		classFile.createNewFile();
		FileOutputStream writer = new FileOutputStream(classFile);
		int b = 0;
		while((b = clazzByteCode.read()) != -1) writer.write(b);
		writer.close();
	}
	
	// TODO: should tempdir be unique for the driver?
	private File temporaryDir() throws IOException {
		File tempDir = File.createTempFile("uExeTmp", ""+System.nanoTime());
		tempDir.delete(); // Delete temp file
		tempDir.mkdir();  // and transform it to a directory
		return tempDir;
	}
	
	public File packageJarFor(Class<?> clazz) throws Exception {
		return new JarPackager(this).packageJar(clazz, temporaryDir());
	}
	
	
}

class JarPackager {
	
	ClassToolbox toolbox;
	Set<Class<?>> processedClasses = new HashSet<Class<?>>();
	
	public JarPackager(ClassToolbox toolbox) {this.toolbox = toolbox;}

	File packageJar(Class<?> clazz, File path) throws Exception{
		packageClass(clazz, path);
		
		File jar =  File.createTempFile("uExeJar", ""+System.nanoTime());
		final ZipOutputStream zos = new ZipOutputStream( new FileOutputStream( jar ) );
		zip(path, path, zos);
		zos.close();
		return jar;
	}
	
	protected void packageClass(Class<?> clazz, File path) throws IOException,
			FileNotFoundException, ClassNotFoundException {
		if (!processedClasses.contains(clazz)) {
			processedClasses.add(clazz);
			toolbox.writeClassFileOnPath(clazz.getName(), toolbox.findClass(clazz), path);

			final JavaClass rClazz = Repository.lookupClass(clazz);

			for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
				if (toolbox.findClass(f.getType()) != null){ // found
					packageClass(f.getType(), path);
				}
			}

			if (clazz.getSuperclass() != null 
					&& toolbox.findClass(clazz.getSuperclass()) != null) // found
				packageClass(clazz.getSuperclass(), path);

			for (Class<?> s : clazz.getDeclaredClasses()) {
				if (toolbox.findClass(s) != null) // found
						packageClass(s, path);
			}

			for (Class<?> i : clazz.getInterfaces()) {
				if (toolbox.findClass(i) != null) // found
					packageClass(i, path);
			}

			for (Method method : rClazz.getMethods()) {
				for (Type t : method.getArgumentTypes()) {
					if (!(t instanceof BasicType)) {
						final Class<?> type = Class.forName(t.toString());
						if (toolbox.findClass(type) != null) // found
							packageClass(type, path);
					}
				}

				if (method.getLocalVariableTable() != null){
					for (LocalVariable v : method.getLocalVariableTable()
														.getLocalVariableTable()){
						// signature is in the format Lorg/unbiquitous/driver/execution/AMethodParameter;
						final String signature = v.getSignature();
						if (signature.length() > 2 ){
							String name = signature
											.substring(1,signature.length()-1)
											.replace('/', '.');
							Class<?> c = Class.forName(name);
							if (toolbox.findClass(c) != null) // found
								packageClass(c, path);
						}
					}
				}
				
				if (method.getExceptionTable() != null){
					for(String e:method.getExceptionTable().getExceptionNames()){
						Class<?> c = Class.forName(e);
						if (toolbox.findClass(c) != null) // found
							packageClass(c, path);
					}
				}
				
				if (!(method.getReturnType() instanceof BasicType)) {
					final Class<?> type = Class.forName(method.getReturnType()
							.toString());
					if (toolbox.findClass(type) != null) // found
						packageClass(type, path);
				}
			}
		}
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
