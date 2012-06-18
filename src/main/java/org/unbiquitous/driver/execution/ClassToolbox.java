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
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * This class is responsible for all logic regarding byte code mumblejumbo.
 * In a nutshell, it'll handle loading, finding and packaging all classes
 * for an easy mobility logic.
 * 
 * @author Fabricio Nogueira Buzeto
 *
 */
public class ClassToolbox {
	protected InputStream findClass(Class<?> clazz) throws IOException {
		String className = clazz.getName().replace('.', File.separatorChar);
		for (String entryPath : System.getProperty("java.class.path").split(System.getProperty("path.separator"))) {
			File entry = new File(entryPath);
			if (entry.isDirectory()){
				File found = findClassFileOnDir(className, entry);
				if (found != null)	return new FileInputStream(found);
			}else if (entry.getName().endsWith(".jar")) {
				InputStream result = findClassFileOnAJar(className, entry);
				if (result != null) {
					return result;
				}
			}
		}
		return null;
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
				if (j.getName().endsWith(className + ".class")) {
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

	// TODO: should tempdir be unique for the driver?
	private File createClassFileDir(String className, InputStream clazz)
			throws IOException, FileNotFoundException {
		File tempDir = File.createTempFile("uExeTmp", ""+System.nanoTime());
		tempDir.delete(); // Delete temp file
		tempDir.mkdir();  // and transform it to a directory
		
		File classFile = new File(tempDir.getPath()+"/"+className.replace('.', '/')+".class");
		classFile.getParentFile().mkdirs();
		classFile.createNewFile();
		FileOutputStream writer = new FileOutputStream(classFile);
		int b = 0;
		while((b = clazz.read()) != -1) writer.write(b);
		writer.close();
		return tempDir;
	}
}
