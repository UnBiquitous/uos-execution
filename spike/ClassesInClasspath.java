import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

public class ClassesInClasspath {
	public static void main(String[] args) throws Exception {
		Enumeration<URL> resources = ClassLoader.getSystemClassLoader().getResources("");
		while(resources.hasMoreElements()){
			URL u = resources.nextElement();
			System.out.println(u);
		}
		String[] classes = getClasses();
		for(String c : classes){
			System.out.println(c);
		}
		System.out.println(classes.length);
	}
	/**
	 * Scans all classes accessible from the context class loader which belong to the given package and subpackages.
	 *
	 * @param packageName The base package
	 * @return The classes
	 * @throws ClassNotFoundException
	 * @throws IOException
	 */
	private static String[] getClasses()
	        throws ClassNotFoundException, IOException {
//	    ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
	    
	    
//	    Enumeration<URL> resources = classLoader.getResources(path);
//	    List<File> dirs = new ArrayList<File>();
//	    while (resources.hasMoreElements()) {
//	        URL resource = resources.nextElement();
//	        dirs.add(new File(resource.getFile()));
//	    }
	    
	    List<File> dirs = new ArrayList<File>();
	    for (String entryPath : System.getProperty("java.class.path").split(
				System.getProperty("path.separator"))) {
	    	System.out.println(entryPath);
	        dirs.add(new File(entryPath));	    	
	    }
	    
	    ArrayList<String> classes = new ArrayList<String>();
	    for (File directory : dirs) {
	        classes.addAll(findClasses(directory, ""));
	    }
	    return classes.toArray(new String[classes.size()]);
	}

	/**
	 * Recursive method used to find all classes in a given directory and subdirs.
	 *
	 * @param directory   The base directory
	 * @param packageName The package name for classes found inside the base directory
	 * @return The classes
	 * @throws ClassNotFoundException
	 */
	private static List<String> findClasses(File directory, String packageName) throws ClassNotFoundException {
	    List<String> classes = new ArrayList<String>();
	    if (!directory.exists() || !directory.isDirectory()) {
	        return classes;
	    }
	    File[] files = directory.listFiles();
	    for (File file : files) {
	        if (file.isDirectory()) {
	            assert !file.getName().contains(".");
	            if (packageName.isEmpty()){
	            	classes.addAll(findClasses(file, file.getName()));
	            }else{
	            	classes.addAll(findClasses(file, packageName + "." + file.getName()));
	            }
	        } else if (file.getName().endsWith(".class")) {
	            String className = packageName + '.' + file.getName().substring(0, file.getName().length() - 6);
				classes.add(className);
	        }
	    }
	    return classes;
	}
}
