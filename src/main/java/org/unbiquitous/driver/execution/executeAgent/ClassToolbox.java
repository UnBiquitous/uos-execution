package org.unbiquitous.driver.execution.executeAgent;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Constructor;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.apache.bcel.Repository;
import org.apache.bcel.classfile.JavaClass;
import org.apache.bcel.classfile.LocalVariable;
import org.apache.bcel.classfile.Method;
import org.apache.bcel.generic.ArrayType;
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
	private static Logger logger = Logger.getLogger(ClassToolbox.class.getName());
	
	/**
	 * TODO: http://lsd.luminis.nl/osgi-on-google-android-using-apache-felix/
	 * In the case of Android (Dalvik platform) the platform must be:
	 * 
	 * <createClassLoader>
	 *  File folder = input.getParentFile();
	 *  return new DexClassLoader(input.getPath(),folder.getPath(),null, getClassLoader());
	 * 
	 * <createTempDir>
	 *  File writableDir = getApplicationContext().getDir("temp", Context.MODE_WORLD_WRITEABLE);
	 *	return File.createTempFile("uExeTmp", ""+System.nanoTime(),writableDir);
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
			final Class dexClass = Class.forName("dalvik.system.DexClassLoader");
			System.out.println("Using DEX");
			platform = new Platform() {
				protected ClassLoader createClassLoader(File input)
						throws Exception {
					File folder = input.getParentFile();
					ClassLoader parent = ClassLoader.getSystemClassLoader().getParent();
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
			System.out.println("Not Using DEX");
		}
	}
	
	public abstract static class Platform{
		protected abstract ClassLoader createClassLoader(File input) throws Exception;
		protected abstract File createTempDir() throws Exception;
	}
	
	private ClassFinder finder = new ClassFinder();
	private Map<Class<?>,File> package_cache = new HashMap<Class<?>, File>();  
	
	public void add2BlackList(String jarName) {	
		finder.blacklist.add(jarName);
	}
	public Set<String> blacklist(){ return finder.blacklist;};

	public void setPackageFor(Class<?> clazz, File jar) {
		package_cache.put(clazz, jar);
	}
	
	protected InputStream findClass(Class<?> clazz) throws IOException {
		return finder.findClass(clazz);
	}

	public File packageJarFor(Class<?> clazz) throws Exception {
		if (package_cache.containsKey(clazz)) return package_cache.get(clazz);
		return new JarPackager(this).packageJar(clazz, platform.createTempDir());
	}

	public File packageDalvikFor(Class<?> clazz) throws Exception {
		if (package_cache.containsKey(clazz)) return package_cache.get(clazz);
		File dir = platform.createTempDir();
		File jar = new JarPackager(this).packageJar(clazz, dir);
		String ANDROID_HOME = System.getenv("ANDROID_HOME");
		return convertToDalvik(dir, jar, ANDROID_HOME);
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
	public ClassLoader load(String className, InputStream clazz) throws Exception {
		File classDir = createClassFileDir(className, clazz);
		return platform.createClassLoader(classDir);
//		addPathToClassLoader(classDir);
	}

	public ClassLoader load(InputStream jar) throws Exception{
		File tempJar = File.createTempFile("uExeTmp.jar", ""+System.nanoTime());
		writeOnFile(jar, tempJar);
		return platform.createClassLoader(tempJar);
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
		String DEX_CMD="/platform-tools/dx" +	// Command per se
				" --dex" +						// To DEX format
				" --keep-classes" +				// keep original classes
				" --no-optimize";				// don't mess with it plz
		String script = ANDROID_HOME+DEX_CMD +				// The command
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
	private static String stream2String(InputStream stream) throws IOException {
		BufferedReader br = new BufferedReader(new InputStreamReader(stream));
		StringBuffer msg = new StringBuffer();
		String line = null;
		while ((line = br.readLine()) != null)	msg.append(line);
		return msg.toString().isEmpty() ? null : msg.toString();
	}
	
}

class ClassFinder {

	protected Set<String> blacklist = new HashSet<String>();
	//Caching results seems not to work on a second search, why ?
//	protected Map<Class<?>, InputStream> 
//								cache = new HashMap<Class<?>, InputStream>();
	protected Set<Class<?>> notFoundCache = new HashSet<Class<?>>();

	protected InputStream findClass(Class<?> clazz) throws IOException {

//		if (cache.containsKey(clazz)){
//			InputStream cached = cache.get(clazz);
//			try {
//				if (cached != null)		cached.reset();
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			return cached;
//		}
		if (notFoundCache.contains(clazz)) return null;

		String className = clazz.getName().replace('.', File.separatorChar);
		for (String entryPath : System.getProperty("java.class.path").split(
				System.getProperty("path.separator"))) {
			File entry = new File(entryPath);
			if (entry.isDirectory() && !inBlackList(entry.getPath())) {
				File found = findClassFileOnDir(className, entry);
				if (found != null) {
					final FileInputStream stream = new FileInputStream(found);
//					stream.mark((int)found.length());
//					cache.put(clazz, stream);
					return stream;
				}
			} else if (entry.getName().endsWith(".jar")
					&& !inBlackList(entry.getName())) {
				InputStream result = findClassFileOnAJar(className, entry);
				if (result != null) {
//					cache.put(clazz, result);
					return result;
				}
			}
		}
		notFoundCache.add(clazz);
//		cache.put(clazz, null);
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

//	private boolean inBlacklist(String jarName) {
//		return blacklist.contains(jarName);
//	}

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

	private File findClassFileOnDir(String classname, File entry)
			throws IOException {
		if (entry.isDirectory()) {
			for (File child : entry.listFiles()) {
				File found = findClassFileOnDir(classname, child);
				if (found != null)
					return found;
			}
		} else if (entry.getPath().endsWith(classname + ".class")) {
			return entry;
		}
		return null;
	}
}

/*
 * I could use http://code.google.com/p/java-dependency-resolver/
 * but using BCEL i was able to bypass the limitations of reflections regarding
 * non-public methods and its inner properties
 */
class JarPackager {
	
	ClassToolbox toolbox;
	Set<Class<?>> processedClasses = new HashSet<Class<?>>();
	
	public JarPackager(ClassToolbox toolbox) {this.toolbox = toolbox;}

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
