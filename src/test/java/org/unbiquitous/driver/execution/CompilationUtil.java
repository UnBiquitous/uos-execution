package org.unbiquitous.driver.execution;

import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.abstractmeta.toolbox.compilation.compiler.JavaSourceCompiler;
import org.abstractmeta.toolbox.compilation.compiler.impl.JavaSourceCompilerImpl;

public class CompilationUtil {
	public static File compileToFile(String source, String clazz, File tempDir) {
		File f = compileToPath(new String[]{source},new String[]{clazz},tempDir);
		while (!f.isFile()) {
			f = f.listFiles()[0];
		}
		return f;
	}
	
	public static File compileToPath(String[] sources, String[] clazzes, File tempDir) {
		// create origin folder
		File origin = new File(tempDir.getPath() + "/origin/");
		origin.mkdir();
		// compile
		JavaSourceCompiler compiler = new JavaSourceCompilerImpl();
		JavaSourceCompiler.CompilationUnit unit = compiler.createCompilationUnit(origin);
		for (int i = 0 ; i < Math.min(sources.length, clazzes.length); i++)
			unit.addJavaSource(clazzes[i], sources[i]);
		compiler.compile(unit);
		compiler.persistCompiledClasses(unit);
		return origin;
	}
	
	public static Class<?> compileToClass(String source, String clazz) throws ClassNotFoundException {
		JavaSourceCompiler compiler = new JavaSourceCompilerImpl();
		JavaSourceCompiler.CompilationUnit unit = compiler.createCompilationUnit();
		unit.addJavaSource(clazz, source);
		ClassLoader loader= compiler.compile(unit);
		return loader.loadClass(clazz);
	}
	
	@SuppressWarnings("unchecked")
	public static Set<String> zipEntries(File jar) throws ZipException, IOException {
		Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) 
													new ZipFile(jar).entries();
		Set<String> set = new HashSet<String>();
		
		while(entries.hasMoreElements()){
			ZipEntry entry = entries.nextElement();
			set.add(entry.getName());
		}
		return set;
	}
}
