package org.unbiquitous.driver.execution.executeAgent;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

class ClassFinder {

	protected Set<String> blacklist = new HashSet<String>();
	protected Set<String> blacklistedClasses = new HashSet<String>();

	protected Map<String,File> classToPath = new HashMap<String,File>();
	
	public void add2BlackList(String jarName) {	
		blacklist.add(jarName);
		for (String entryPath : System.getProperty("java.class.path").split(
				System.getProperty("path.separator"))) {
			if (inBlackList(entryPath)) {
				initPath(entryPath, false);
			}
		}
	}
	
	@SuppressWarnings("serial")
	public List<String> knownClasses(){
		return new ArrayList<String>(){{
			addAll(classToPath.keySet());
		}};
	}
	
	protected void init(){
		for (String entryPath : System.getProperty("java.class.path").split(
				System.getProperty("path.separator"))) {
			initPath(entryPath, true);
		}
	}

	private void initPath(String entryPath, boolean include){
		try {
			File entry = new File(entryPath);
			if (entry.isDirectory()) {
				populaterFileOnDir(entry.getPath(),entry,include);
			} else if (entry.getName().endsWith(".jar")) {
				populateClassesOnAJar(entry.getPath(), entry, include);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	private void populaterFileOnDir(String parentPath, File entry, boolean include)
			throws IOException {
		if (entry.isDirectory()) {
			for (File child : entry.listFiles()) {
				populaterFileOnDir(parentPath,child, include);
			}
		} else if (entry.getPath().endsWith(".class")) {
			String path = entry.getPath()
					.replace(parentPath, "")
					.replace(".class", "")
					.replace("/", ".")
					;
			if(path.startsWith(".")){
				path = path.substring(1);
			}
			if (include){
				classToPath.put(path, entry);
			}else{
				blacklistedClasses.add(path);
			}
		}
	}
	
	private void populateClassesOnAJar(String parentPath, File jar, 
												boolean include)
			throws FileNotFoundException, IOException {
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(
				new FileInputStream(jar)));
		ZipEntry j;
		while ((j = zis.getNextEntry()) != null) {
			final int BUFFER = 2048;
			byte data[] = new byte[BUFFER];
			while ((zis.read(data, 0, BUFFER)) != -1) {
				if (j.getName().endsWith(".class")) {
					String path = j.getName()
							.replace(parentPath, "")
							.replace(".class", "")
							.replace("/", ".")
							;
					if (include){
						classToPath.put(path, jar);
					}else{
						blacklistedClasses.add(path);
					}
				}
			}
		}
		zis.close();
	}
	
	protected InputStream findClass(Class<?> clazz, List<String> extraBlacklist) 
			throws IOException {
		
		if (extraBlacklist != null && extraBlacklist.contains(clazz.getName())){
			return null;
		}
		
		if(blacklistedClasses.contains(clazz.getName())){
			return null;
		}
		
		if (classToPath.containsKey(clazz.getName())){
			File file = classToPath.get(clazz.getName());
			if(file.getPath().endsWith(".class")){
				return new FileInputStream(file);
			}else if (file.getPath().endsWith(".jar")){
				String className = clazz.getName().replace('.', File.separatorChar);
				return findClassFileOnAJar(className, file);
			}
		}
		
		return null;
	}

	private boolean inBlackList(String path) {
		for (String black : blacklist){
			if (path.contains(black)) {
				return true;
			}
		}
		return false;
	}

	private InputStream findClassFileOnAJar(String className, File jar)
			throws FileNotFoundException, IOException {
		ZipInputStream zis = new ZipInputStream(new BufferedInputStream(
				new FileInputStream(jar)));
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
}