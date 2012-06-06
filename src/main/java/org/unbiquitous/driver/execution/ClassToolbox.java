package org.unbiquitous.driver.execution;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class ClassToolbox {
	protected InputStream findClass(Class clazz) throws IOException {
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
	
	@SuppressWarnings("rawtypes")
	//TODO: Should return the ClassLoader
	protected void load(String className, InputStream clazz) throws Exception {
		File classDir = createClassFileDir(className, clazz);
		
		addPathToClassLoader(classDir);
	}

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void addPathToClassLoader(File classDir)
			throws NoSuchMethodException, IllegalAccessException,
			InvocationTargetException, MalformedURLException {
		URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		Class sysclass = URLClassLoader.class;
		Method method = sysclass.getDeclaredMethod("addURL", URL.class);
		method.setAccessible(true);
		method.invoke(sysloader, new Object[] { classDir.toURI().toURL() });
	}

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
