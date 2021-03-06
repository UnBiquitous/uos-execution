package org.unbiquitous.driver.spike;

import static org.junit.Assert.*;

import java.io.File;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;

import org.abstractmeta.toolbox.compilation.compiler.JavaSourceCompiler;
import org.abstractmeta.toolbox.compilation.compiler.impl.JavaSourceCompilerImpl;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ReflectionSpike {

	@Rule
    public TemporaryFolder folder= new TemporaryFolder();
	
	
	@Test public void compileAndRun() throws Exception{
		File tmpDir = folder.getRoot();
		assertEquals(0, tmpDir.listFiles().length);
		JavaSourceCompiler javaSourceCompiler = new JavaSourceCompilerImpl();
	    JavaSourceCompiler.CompilationUnit compilationUnit = javaSourceCompiler.createCompilationUnit(tmpDir);
	    String javaSourceCode =  "package org.unbiquitous.driver.spike;\n" +
	      "public class Foo implements org.unbiquitous.driver.spike.I {\n" +
	      "        public String m(Integer i) {\n" +
	      "            return \"Hello World \"+i;\n" +
	      "        }\n" +
	      "    }";
	    compilationUnit.addJavaSource("org.unbiquitous.driver.spike.Foo", javaSourceCode);
	    ClassLoader classLoader = javaSourceCompiler.compile(compilationUnit);
	    @SuppressWarnings("rawtypes")
		Class fooClass = classLoader.loadClass("org.unbiquitous.driver.spike.Foo");
	    assertEquals("Hello World 10",((I)fooClass.newInstance()).m(10));
	    System.out.println(tmpDir.listFiles());
	    assertEquals(0, tmpDir.listFiles().length);
	    javaSourceCompiler.persistCompiledClasses(compilationUnit);
	    assertEquals(1, tmpDir.listFiles().length);
	    for(File f : tmpDir.listFiles())
	    	System.out.println(f.getPath());
	    try {
			Class.forName("org.unbiquitous.driver.spike.Foo");
			fail("Should not be on default classloader");
		} catch (Exception e) {}
	}
	
	
	@Test public void serializeDeserializeAndRun() throws Exception{
		InputStream in = new PipedInputStream();
		OutputStream out = new PipedOutputStream((PipedInputStream) in);

		I original = new nI();
		assertEquals((Integer)1,Spy.count);
		assertEquals("0 10",original.m(10));
		
		ObjectOutputStream writer = new ObjectOutputStream(out);
		writer.writeObject(original);
		writer.close();

		ObjectInputStream reader = new ObjectInputStream(in);
		I created = (I) reader.readObject();
		
		assertNotSame(original, created);
		assertEquals((Integer)1,Spy.count);
		assertEquals("10 5",created.m(5));
	}
}

class A {
	File f;
}

class Spy {
	static Integer count = 0;
}

class nI implements I{
	private static final long serialVersionUID = 8095143952426379177L;
	private Integer lastNumber = 0;
	public nI(){
		Spy.count++;
	}
	public String m(Integer i) {
		String r = lastNumber +" "+i;
		lastNumber = i;
		return r;	
	}
}