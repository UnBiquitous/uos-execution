package org.unbiquitous.driver.execution.executeAgent;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;


/**
 * This class is responsible for all logic regarding byte code mumblejumbo.
 * In a nutshell, it'll handle loading, finding and packaging all classes
 * for an easy mobility logic.
 * 
 * @author Fabricio Nogueira Buzeto
 *
 */
public class ClassToolbox {
	private static Logger logger = Logger.getLogger(ClassToolbox.class.getName());
	
	private ClassFinder finder = new ClassFinder();
	private Map<Class<?>,File> package_cache = new HashMap<Class<?>, File>();  
	
	public ClassToolbox() {
		finder.init();
	}
	
	public void add2BlackList(String jarName) {	
		finder.add2BlackList(jarName);
	}
	public Set<String> blacklist(){ return finder.blacklist;};

	public void setPackageFor(Class<?> clazz, File jar) {
		package_cache.put(clazz, jar);
	}
	
	protected InputStream findClass(Class<?> clazz) throws IOException{
		return findClass(clazz, null);
	}
	
	
	protected InputStream findClass(Class<?> clazz, List<String> extraBlacklist)
				throws IOException {
		return finder.findClass(clazz, extraBlacklist);
	}

	public File packageJarFor(Class<?> clazz) throws Exception {
		return packageJarFor(clazz, null);
	}
	
	public File packageJarFor(Class<?> clazz, List<String> extraBlacklist)
			throws Exception {
		if (package_cache.containsKey(clazz)) return package_cache.get(clazz);
		return new JarPackager(this)
					.packageJar(clazz, platform.createTempDir(), extraBlacklist);
	}

	public File packageDalvikFor(Class<?> clazz) throws Exception {
		return packageDalvikFor(clazz, null);
	}
	
	public File packageDalvikFor(Class<?> clazz, List<String> extraBlacklist) throws Exception {
		if (package_cache.containsKey(clazz)) return package_cache.get(clazz);
		File dir = platform.createTempDir();
		File jar = new JarPackager(this).packageJar(clazz, dir,extraBlacklist);
		String ANDROID_HOME = System.getenv("ANDROID_HOME");
		return convertToDalvik(dir, jar, ANDROID_HOME);
	}
	
	public ClassLoader load(String className, InputStream clazz) throws Exception {
		File classDir = createClassFileDir(className, clazz);
		return platform.createClassLoader(classDir);
	}

	public ClassLoader load(InputStream jar) throws Exception{
		File tempJar = File.createTempFile("uExeTmp.jar", ""+System.nanoTime());
		writeOnFile(jar, tempJar);
		return platform.createClassLoader(tempJar);
	}
	
	private File createClassFileDir(String className, InputStream clazz)throws Exception {
		File tempDir = platform.createTempDir();
		writeClassFileOnPath(className, clazz, tempDir);
		return tempDir;
	}
	
	protected void writeClassFileOnPath(String className, InputStream clazzByteCode,
			File path) throws IOException, FileNotFoundException {
		File classFile = new File(path.getPath()+"/"+className.replace('.', '/')+".class");
		classFile.getParentFile().mkdirs();
		classFile.createNewFile();
		writeOnFile(clazzByteCode, classFile);
	}
	
	private void writeOnFile(InputStream clazzByteCode, File classFile)
			throws FileNotFoundException, IOException {
		FileOutputStream writer = new FileOutputStream(classFile);
		int b = 0;
		while((b = clazzByteCode.read()) != -1) {
			writer.write(b);
		}
		writer.close();
	}
	
	protected File convertToDalvik(File dir, File jar, String ANDROID_HOME)
			throws IOException {
		if (ANDROID_HOME == null){
			String msg = "Not possible to convert file "+jar+
					" for dalvik since system variable ANDROID_HOME is not defined.";
			throw new RuntimeException(msg);
		}
		
		String dx_path = findDXTool(ANDROID_HOME);
		
		String DEX_CMD=dx_path +	// Command per se
				" --dex" +						// To DEX format
				" --keep-classes" +				// keep original classes
				" --no-optimize";				// don't mess with it plz
		String script = DEX_CMD +				// The command
				" --output="+					// specify output file
				dir.getPath()+"/dalvik.jar"+	// our file path
				" "+jar.getPath();				// the jar to convert
		Process result = Runtime.getRuntime().exec(script);
		logger.fine(stream2String(result.getInputStream()));
		final String erroMsg = stream2String(result.getErrorStream());
		if (erroMsg != null){
			logger.severe(erroMsg);
			throw new RuntimeException("msg");
		}
		return new File(dir.getPath()+"/dalvik.jar");
	}
	private String findDXTool(String ANDROID_HOME) {
		String dx_path = ANDROID_HOME+"/platform-tools/dx";
		if(!new File(dx_path).exists()){
			dx_path = ANDROID_HOME+"/build-tools/dx";
			if(!new File(dx_path).exists()){
				dx_path = null;
				for(File file : new File(ANDROID_HOME+"/build-tools/").listFiles()){
					String pathname = file.getPath()+"/dx";
					if(new File(pathname).exists()){
						dx_path = pathname;
						break;
					}
				}
				if (dx_path == null){
					throw new RuntimeException("dx tool not found.");
				}
			}
		}
		return dx_path;
	}
	private static String stream2String(InputStream stream) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));
		StringBuffer msg = new StringBuffer();
		String line = null;
		while ((line = br.readLine()) != null)	msg.append(line);
		return msg.toString().isEmpty() ? null : msg.toString();
	}
	
	public List<String> listKnownClasses() {
		return finder.knownClasses();
	}
	
	/**
	 * Like in: http://lsd.luminis.nl/osgi-on-google-android-using-apache-felix/
	 * We adjust to the configurations of Android based on reflections
	 * 
	 */
	public static Platform platform = new Platform() {
		protected ClassLoader createClassLoader(File input) throws Exception {
			return new URLClassLoader(new URL[] { input.toURI().toURL() },
										ClassLoader.getSystemClassLoader());
		}
		protected File createTempDir() throws IOException {
			File tempDir = File.createTempFile("uExeTmp", ""+System.nanoTime());
			tempDir.delete(); // Delete temp file
			tempDir.mkdir();  // and transform it to a directory
			return tempDir;
		}
	}; 
	
	static {
		try {
			@SuppressWarnings("rawtypes")
			final Class dexClass = Class.forName("dalvik.system.DexClassLoader");
			platform = new Platform() {
				protected ClassLoader createClassLoader(File input)
						throws Exception {
					File folder = input.getParentFile();
					ClassLoader parent = ClassLoader.getSystemClassLoader().getParent();
					@SuppressWarnings({ "unchecked", "rawtypes" })
					Constructor constructor = dexClass.getConstructor(String.class, String.class, String.class, ClassLoader.class);
					return (ClassLoader) constructor.newInstance(input.getPath(),folder.getPath(),null, parent);
				}

				protected File createTempDir() throws Exception {
					File tempDir = File.createTempFile("uExeTmp", ""+System.nanoTime());
					tempDir.delete(); // Delete temp file
					tempDir.mkdir();  // and transform it to a directory
					return tempDir;
				}
				
			};
		} catch (ClassNotFoundException e) {
			// If it's not found keep the way it is.
		}
	}
	
	public abstract static class Platform{
		protected abstract ClassLoader createClassLoader(File input) throws Exception;
		protected abstract File createTempDir() throws Exception;
	}
	
}
