package org.unbiquitous.driver.execution;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

// This Spike creates a relation with the AndroidSpike created to validate this one
public class DEXSpike {

	private static final String ANDROID_HOME="/opt/android-sdk-linux";
	private static final String DEX_CMD="platform-tools/dx --dex --verbose --keep-classes --no-optimize --debug --statistics ";
	
	
	// All classes must be compiled in Java1.6 bytecode
	public static void main(String[] args) throws Exception {
//		File fuiPath = findClass(Fui.class);
		
		String currentDir = System.getProperty("user.dir") ;
		
		// Create DEX File
//		String dexFile = currentDir+"/mydex.jar";
		String dexFile = currentDir+"/classes.dex";
//		String classFile = fuiPath.getPath();
		String classFilesPath = "/home/dados/unb/ubiquitos/workspace/execution/target/test-classes/";
		runDex(classFilesPath, dexFile);
		
		// copy original .class file
		
	}
	
	private static void runDex(String classFile, String dexFile) throws IOException{
		String command = ANDROID_HOME+"/"+DEX_CMD+"--output="+dexFile +" "+classFile;
//		String command = "PAPAPA=123456;echo $PAPAPA";
		System.out.println(command);
		Process p = Runtime.getRuntime().exec(command);
		
		System.out.println("[[Normal]]:");
		printStream(p.getInputStream());
		System.out.println("[[Error]]:");
		printStream(p.getErrorStream());
	}

	private static void printStream(InputStream stream) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));
		String line;
		while ((line = br.readLine()) != null)
			System.out.println(line);
	}
	
	static File findClass(Class clazz) throws IOException {
		String className = clazz.getName().replace('.', File.separatorChar);
		for (String entryPath : System.getProperty("java.class.path").split(System.getProperty("path.separator"))) {
			File entry = new File(entryPath);
			if (entry.isDirectory()){
				return findClassFileOnDir(className, entry);
			}else if (entry.getName().endsWith(".jar")) {
				//Nothing to do here
			}
		}
		return null;
	}
	
	static File findClassFileOnDir(String classname, File entry) throws IOException{
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
}