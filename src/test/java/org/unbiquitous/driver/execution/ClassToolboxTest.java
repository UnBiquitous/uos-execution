package org.unbiquitous.driver.execution;

import static org.junit.Assert.*;
import static org.unbiquitous.driver.execution.CompilationUtil.compileToFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.luaj.vm2.Lua;
import org.mockito.Mockito;
import org.unbiquitous.driver.execution.MyAgent.AgentSpy;

import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;

public class ClassToolboxTest {
	@Rule
    public TemporaryFolder folder= new TemporaryFolder();
	
	private ClassToolbox box;
	
	private File tempDir;
	private String currentDir;
	
	@Before public void setUp(){
		box = new ClassToolbox();
		tempDir = folder.getRoot();
		currentDir = System.getProperty("user.dir") ;
	}
	
	// TODO: handle android optimization
	// TODO: Tempfolder and stuff like that will need to be parametized for Android
	@Test
	public void findsAClassInTheSourceFolder() throws Exception {
		String pkgRoot = "org/unbiquitous/driver/execution/";
		String rootPath = currentDir + "/target/test-classes/" + pkgRoot;

		InputStream dummyClass = box.findClass(DummyAgent.class);
		InputStream myClass = box.findClass(MyAgent.class);

		FileInputStream expected_dummy = new FileInputStream(rootPath+"DummyAgent.class");
		assertStream(expected_dummy, dummyClass);

		FileInputStream expected = new FileInputStream(rootPath+ "MyAgent.class");
		assertStream(expected, myClass);
	}

	@Test
	public void MustNotConfuseHomonymsClases() throws Exception {
		String pkgRoot = "org/unbiquitous/driver/execution/";
		String rootPath = currentDir + "/target/test-classes/" + pkgRoot;

		FileInputStream expected_dummy = new FileInputStream(rootPath
				+ "DummyAgent.class");
		assertStream(expected_dummy, box.findClass(DummyAgent.class));

		FileInputStream expected_other = new FileInputStream(rootPath
				+ "dummy/DummyAgent.class");
		assertStream(
				expected_other,
				box.findClass(org.unbiquitous.driver.execution.dummy.DummyAgent.class));
	}

	@Test public void mustFindAClassInsideAJar() throws Exception {
		String rootPath = currentDir + "/target/test-classes/";

		FileInputStream expected_mockito = new FileInputStream(rootPath+"Mockito_class");
		assertStream(expected_mockito, box.findClass(Mockito.class));
	}

	@Test public void mustNotFindJDKClasses() throws Exception {
		assertStream(null, box.findClass(Integer.class));
	}

	@Test public void mustNotFindClassesOnBlacklistedJars() throws Exception{
		box.addJar2BlackList("luaj-jse-2.0.2.jar");
		assertStream(null, box.findClass(Lua.class));
	}
	
	@Test public void mustLoadAClassFromStream() throws Exception {

		String source = "package org.unbiquitous.driver.execution;"
				+ "public class Foo extends org.unbiquitous.driver.execution.MyAgent {"
				+ "public int plusOne(Integer i){" + "	return i+1;" + "}" + "}";

		String clazz = "org.unbiquitous.driver.execution.Foo";
		File origin = compileToFile(source, clazz, tempDir);

		ClassLoader loader = box.load("org.unbiquitous.driver.execution.Foo",
				new FileInputStream(origin));

		assertEquals("default ClassLoader must me URLClassLoader.",
						URLClassLoader.class, loader.getClass());
		
		Object o = loader.loadClass("org.unbiquitous.driver.execution.Foo")
				.newInstance();

		Method plusOne = o.getClass().getMethod("plusOne", Integer.class);
		assertEquals(2, plusOne.invoke(o, 1));

		Method run = o.getClass().getMethod("run", Gateway.class);
		int before = AgentSpy.count;
		run.invoke(o, new Object[] { null });
		assertEquals((Integer) (before + 1), AgentSpy.count);
	}
	
	// TODO: handle jars and classes composition
	@Test public void packageJarWithASingleAgentClass() throws Exception{
		File jar = box.packageJarFor(
				org.unbiquitous.driver.execution.dummy.DummyAgent.class);
		
		assertNotNull(jar);

		Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) new ZipFile(jar).entries();
		ZipEntry entry = entries.nextElement();
		assertEquals("org/unbiquitous/driver/execution/dummy/DummyAgent.class",
																entry.getName());
		assertFalse(entries.hasMoreElements());
		
//		assertEquals("Must go down as many levels",dummyPath.length,levels);
	}
	//TODO: Agent class cannot be inner class and must be public
	
	
	private void assertStream(InputStream expected, InputStream dummyClass)
			throws IOException {
		if (expected == dummyClass)
			return;
		if (expected == null)
			fail("Returned stream was not null.");
		if (dummyClass == null)
			fail("Returned stream was null.");
		byte b;
		long count = 0;
		while ((b = (byte) expected.read()) != -1) {
			assertEquals("Failed on byte " + count, b, (byte) dummyClass.read());
			count++;
		}
	}
}
