package org.unbiquitous.driver.spike;

import static org.junit.Assert.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Method;

import org.abstractmeta.toolbox.compilation.compiler.JavaSourceCompiler;
import org.abstractmeta.toolbox.compilation.compiler.impl.JavaSourceCompilerImpl;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ReflectionSpike {

	
	
	@Test public void compileAndRun() throws Exception{
		JavaSourceCompiler javaSourceCompiler = new JavaSourceCompilerImpl();
	    JavaSourceCompiler.CompilationUnit compilationUnit = javaSourceCompiler.createCompilationUnit();
	    String javaSourceCode =  "package org.unbiquitous.driver.spike;\n" +
	      "public class Foo implements org.unbiquitous.driver.spike.I {\n" +
	      "        public String m(Integer i) {\n" +
	      "            return \"Hello World \"+i;\n" +
	      "        }\n" +
	      "    }";
	    compilationUnit.addJavaSource("org.unbiquitous.driver.spike.Foo", javaSourceCode);
	    ClassLoader classLoader = javaSourceCompiler.compile(compilationUnit);
	    Class fooClass = classLoader.loadClass("org.unbiquitous.driver.spike.Foo");
	    assertEquals("Hello World 10",((I)fooClass.newInstance()).m(10));
	}
	
	
	@Test public void serializeDeserializeAndRun() throws Exception{
		I original = new nI();
		TemporaryFolder tmpFolder = new TemporaryFolder();
		File file = tmpFolder.newFile("reflection_test");
		ObjectOutputStream writer = new ObjectOutputStream(new FileOutputStream(file));
		writer.writeObject(original);
		writer.close();
		ObjectInputStream reader = new ObjectInputStream(new FileInputStream(file));
		I created = (I) reader.readObject();
		assertNotSame(original, created);
		assertEquals("my 10",created.m(10));
		assertEquals("my 5",created.m(5));
		file.delete();
	}
}

class nI implements I{
	private static final long serialVersionUID = 8095143952426379177L;
	public String m(Integer i) {	return "my "+i;	}
}