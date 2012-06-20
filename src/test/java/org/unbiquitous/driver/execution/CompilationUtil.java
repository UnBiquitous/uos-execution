package org.unbiquitous.driver.execution;

import static org.junit.Assert.assertEquals;

import java.io.File;

import org.abstractmeta.toolbox.compilation.compiler.JavaSourceCompiler;
import org.abstractmeta.toolbox.compilation.compiler.impl.JavaSourceCompilerImpl;

public class CompilationUtil {
	public static File compileToFile(String source, String clazz, File tempDir) {
		// create origin folder
		File origin = new File(tempDir.getPath() + "/origin/");
		origin.mkdir();
		// compile
		JavaSourceCompiler compiler = new JavaSourceCompilerImpl();
		JavaSourceCompiler.CompilationUnit unit = compiler.createCompilationUnit(origin);
		unit.addJavaSource(clazz, source);
		compiler.compile(unit);
		assertEquals(0, origin.listFiles().length);
		compiler.persistCompiledClasses(unit);
		assertEquals(1, origin.listFiles().length);
		File f = origin;
		while (!f.isFile()) {
			f = f.listFiles()[0];
		}
		return f;
	}
	
	public static Class<?> compileToClass(String source, String clazz) throws ClassNotFoundException {
		JavaSourceCompiler compiler = new JavaSourceCompilerImpl();
		JavaSourceCompiler.CompilationUnit unit = compiler.createCompilationUnit();
		unit.addJavaSource(clazz, source);
		ClassLoader loader= compiler.compile(unit);
		return loader.loadClass(clazz);
	}
}
