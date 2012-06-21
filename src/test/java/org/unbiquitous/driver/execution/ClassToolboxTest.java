package org.unbiquitous.driver.execution;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.unbiquitous.driver.execution.CompilationUtil.compileToFile;
import static org.unbiquitous.driver.execution.CompilationUtil.compileToPath;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

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
		box.add2BlackList("luaj-jse-2.0.2.jar");
		assertStream(null, box.findClass(Lua.class));
	}
	
	@Test public void mustNotFindClassesOnBlacklistedPaths() throws Exception{
		box.add2BlackList("/uos_core/target/classes");
		assertStream(null, box.findClass(Gateway.class));
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
	
	@Test public void mustLoadClassesFromJar() throws Exception {
		
		String parentSrc = 
				"package org.unbiquitous.driver.execution;"
			+	"public class Parent{"
			+	"	public int two(){return 2;}"
			+	"}";
		
		String childSrc = 
				"package org.unbiquitous.driver.execution;"
			+	"public class Child extends Parent{"
			+	"	public int three(){return two()+1;}"
			+	"}";
		
		
		File path = compileToPath(new String[]{parentSrc, childSrc}, 
									new String[]{
										"org.unbiquitous.driver.execution.Parent", 
										"org.unbiquitous.driver.execution.Child"}, 
									tempDir);
		
		File jar = new File(path.getPath()+"/temp.jar");
		JarPackager packager = new JarPackager(box);
		final ZipOutputStream zos = new ZipOutputStream( new FileOutputStream( jar ) );
		packager.zip(path, path, zos);
		zos.close();
		
		
		ClassLoader loader = box.load(new FileInputStream(jar));
		
		assertEquals("default ClassLoader must me URLClassLoader.",
				URLClassLoader.class, loader.getClass());
		
		Object o = loader.loadClass("org.unbiquitous.driver.execution.Child")
				.newInstance();
		
		Method plusOne = o.getClass().getMethod("three");
		assertEquals(3, plusOne.invoke(o, new Object[]{}));
	}
	
	@SuppressWarnings("unchecked")
	@Test public void packageJarWithASingleAgentClass() throws Exception{
		box.add2BlackList("uos-core-2.2.0_DEV.jar");
		box.add2BlackList("/uos_core/target/classes");
		File jar = box.packageJarFor(
				org.unbiquitous.driver.execution.dummy.DummyAgent.class);
		
		assertNotNull(jar);

		Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) new ZipFile(jar).entries();
		
		Set<String> expected = new HashSet<String>();
		expected.add("org/unbiquitous/driver/execution/dummy/DummyAgent.class");
		expected.add("org/unbiquitous/driver/execution/Agent.class");
		
		Set<String> received = new HashSet<String>();
		
		while(entries.hasMoreElements()){
			ZipEntry entry = entries.nextElement();
			received.add(entry.getName());
		}
		
		assertEquals(expected,received);
	}
	
	/**
	 * This is a very complex testCase since we use the {@link MyJarAgent}
	 * as the tested agent to be compared against the result, such class
	 * is composed by several rules to be complied by the jar packaging method.
	 * Such rules are:
	 * 
	 * - Must include the Atributes Classes
	 * - Must include referenced Classes recursively
	 * - Must ignore primites as classes.
	 * - Must ignore JDK classes
	 * - Must ignore blacklisted classes
	 * - Must not fall into loops (CLasses referring same classes)
	 * - Must include Superclasses
	 * - Must include Interfaces
	 * - Must include method parameters
	 * - Must include method return types
	 * - Must include constants types
	 * - Must include static fields and methods
	 * - Must include inner classes
	 * - Must include parameter variables types
	 * - Must include thrown Exceptions
	 */
	@SuppressWarnings("unchecked")
	@Test public void packageJarWithAnAgentClassAndItsObjectAttributes() throws Exception{
		box.add2BlackList("luaj-jse-2.0.2.jar");
		File jar = box.packageJarFor(MyJarAgent.class);
		
		Enumeration<ZipEntry> entries = (Enumeration<ZipEntry>) new ZipFile(jar).entries();
		
		Set<String> expected = new HashSet<String>();
		expected.add("org/unbiquitous/driver/execution/MyJarAgent.class");
		expected.add("org/unbiquitous/driver/execution/JustAnAttributeClass.class");
		expected.add("org/unbiquitous/driver/execution/AnotherAtributeClass.class");
		expected.add("org/unbiquitous/driver/execution/JustACoolSuperclass.class");
		expected.add("org/unbiquitous/driver/execution/JustANiceInterface.class");
		expected.add("org/unbiquitous/driver/execution/JustAnotherNiceInterface.class");
		expected.add("org/unbiquitous/driver/execution/AMethodParameter.class");
		expected.add("org/unbiquitous/driver/execution/AMethodReturnType.class");
		expected.add("org/unbiquitous/driver/execution/AConstantType.class");
		expected.add("org/unbiquitous/driver/execution/AStaticReturnType.class");
		expected.add("org/unbiquitous/driver/execution/MyJarAgent$Inner.class");
		expected.add("org/unbiquitous/driver/execution/AInnerMethodUsedType.class");
		expected.add("org/unbiquitous/driver/execution/AnException.class");
		
		Set<String> received = new HashSet<String>();
		
		while(entries.hasMoreElements()){
			ZipEntry entry = entries.nextElement();
			received.add(entry.getName());
		}
		
		assertEquals(expected,received);
		
	}
	
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
