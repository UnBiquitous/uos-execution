package org.unbiquitous.driver.execution.executeAgent;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.unbiquitous.driver.execution.executeAgent.CompilationUtil.assertStream;
import static org.unbiquitous.driver.execution.executeAgent.CompilationUtil.compileToFile;
import static org.unbiquitous.driver.execution.executeAgent.CompilationUtil.compileToPath;
import static org.unbiquitous.driver.execution.executeAgent.CompilationUtil.zipEntries;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.luaj.vm2.Lua;
import org.mockito.Mockito;
import org.unbiquitous.driver.execution.executeAgent.MyAgent.AgentSpy;

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
	
	// TODO: handle resources
	@Test
	public void findsAClassInTheSourceFolder() throws Exception {
		String pkgRoot = "org/unbiquitous/driver/execution/executeAgent/";
		String rootPath = currentDir + "/target/test-classes/" + pkgRoot;

		InputStream dummyClass = box.findClass(DummyAgent.class);
		InputStream myClass = box.findClass(MyAgent.class);

		FileInputStream expected_dummy = new FileInputStream(rootPath+"DummyAgent.class");
		assertStream(expected_dummy, dummyClass);

		FileInputStream expected = new FileInputStream(rootPath+ "MyAgent.class");
		assertStream(expected, myClass);
	}

	@Test
	public void mustNotConfuseHomonymsClasses() throws Exception {
		String pkgRoot = "org/unbiquitous/driver/execution/executeAgent/";
		String rootPath = currentDir + "/target/test-classes/" + pkgRoot;

		FileInputStream expected_dummy = new FileInputStream(rootPath
				+ "DummyAgent.class");
		assertStream(expected_dummy, box.findClass(DummyAgent.class));

		FileInputStream expected_other = new FileInputStream(rootPath
				+ "dummy/DummyAgent.class");
		assertStream(
				expected_other,
				box.findClass(org.unbiquitous.driver.execution.executeAgent.dummy.DummyAgent.class));
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
	
	@Test public void mustNotFindClassesOnBlacklistedAsWildCards() throws Exception{
		box.add2BlackList("luaj-jse");
		assertStream(null, box.findClass(Lua.class));
	}
	
	@Test public void mustNotFindClassesOnBlacklistedPaths() throws Exception{
		box.add2BlackList("uos-core");
		box.add2BlackList("/uos_core/target/classes");
		assertStream(null, box.findClass(Gateway.class));
	}
	
	@Test public void mustLoadAClassFromStream() throws Exception {

		String source = "package org.unbiquitous.driver.execution.executeAgent;"
				+ "public class Foo extends org.unbiquitous.driver.execution.executeAgent.MyAgent {"
				+ "public int plusOne(Integer i){" + "	return i+1;" + "}" + "}";

		String clazz = "org.unbiquitous.driver.execution.Foo";
		File origin = compileToFile(source, clazz, tempDir);

		ClassLoader loader = box.load("org.unbiquitous.driver.execution.executeAgent.Foo",
				new FileInputStream(origin));

		assertEquals("default ClassLoader must me URLClassLoader.",
						URLClassLoader.class, loader.getClass());
		
		Object o = loader.loadClass("org.unbiquitous.driver.execution.executeAgent.Foo")
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
		
		File jar = folder.newFile("temp.jar");
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
	
	@Test public void packageJarWithASingleAgentClass() throws Exception{
		box.add2BlackList("uos-core");
		box.add2BlackList("/uos_core/target/classes");
		File jar = box.packageJarFor(
				org.unbiquitous.driver.execution.executeAgent.dummy.DummyAgent.class);
		
		assertThat(zipEntries(jar)).containsOnly(
				"org/unbiquitous/driver/execution/executeAgent/dummy/DummyAgent.class",
				"org/unbiquitous/driver/execution/executeAgent/Agent.class");
	}
	
	@SuppressWarnings("serial")
	@Test public void returnInformedJar() throws Exception{
		Agent dummy_inner = new Agent() {
			public void run(Gateway gateway) {}
		};
		File dummy_jar = File.createTempFile("dummy", ".jar");
		box.setPackageFor(dummy_inner.getClass(), dummy_jar);
		assertEquals(dummy_jar, box.packageJarFor(dummy_inner.getClass()));
	}
	
	@SuppressWarnings("serial")
	@Test public void returnInformedApk() throws Exception{
		Agent dummy_inner = new Agent() {
			public void run(Gateway gateway) {}
		};
		File dummy_jar = File.createTempFile("dummy", ".apk");
		box.setPackageFor(dummy_inner.getClass(), dummy_jar);
		assertEquals(dummy_jar, box.packageDalvikFor(dummy_inner.getClass()));
	}
	
	/**
	 * This is a very complex testCase since we use the {@link MyJarAgent}
	 * as the tested agent to be compared against the result, such class
	 * is composed by several rules to be complied by the jar packaging method.
	 * Such rules are:
	 * 
	 * - Must include the Attributes Classes
	 * - Must include referenced Classes recursively
	 * - Must ignore primitives as classes.
	 * - Must ignore JDK classes
	 * - Must ignore blacklisted classes
	 * - Must not fall into loops (Classes referring same classes)
	 * - Must include Superclasses
	 * - Must include Interfaces
	 * - Must include method parameters
	 * - Must include method return types
	 * - Must include constants types
	 * - Must include static fields and methods
	 * - Must include inner classes
	 * - Must include parameter variables types
	 * - Must include thrown Exceptions
	 * - Must handle array types in Attributes
	 * - Must handle array types in parameters
	 * - Must handle array types in return 
	 * - Must handle primitive types in arrays
	 * TODO: JarPackage does not handle Generics
	 * - Must handle generic referenced types
	 */
	@Test public void packageJarWithAnAgentClassAndItsObjectAttributes() throws Exception{
		box.add2BlackList("luaj-jse-2.0.2.jar");
		File jar = box.packageJarFor(MyJarAgent.class);
		
		assertThat(zipEntries(jar)).contains(
			"org/unbiquitous/driver/execution/executeAgent/MyJarAgent.class",
			"org/unbiquitous/driver/execution/executeAgent/JustAnAttributeClass.class",
			"org/unbiquitous/driver/execution/executeAgent/AnotherAtributeClass.class",
			"org/unbiquitous/driver/execution/executeAgent/JustACoolSuperclass.class",
			"org/unbiquitous/driver/execution/executeAgent/JustANiceInterface.class",
			"org/unbiquitous/driver/execution/executeAgent/JustAnotherNiceInterface.class",
			"org/unbiquitous/driver/execution/executeAgent/AMethodParameter.class",
			"org/unbiquitous/driver/execution/executeAgent/AMethodReturnType.class",
			"org/unbiquitous/driver/execution/executeAgent/AConstantType.class",
			"org/unbiquitous/driver/execution/executeAgent/AStaticReturnType.class",
			"org/unbiquitous/driver/execution/executeAgent/MyJarAgent$Inner.class",
			"org/unbiquitous/driver/execution/executeAgent/AInnerMethodUsedType.class",
			"org/unbiquitous/driver/execution/executeAgent/AnException.class",
			"org/unbiquitous/driver/execution/executeAgent/JustAnArrayAtributeClass.class",
			"org/unbiquitous/driver/execution/executeAgent/JustAnMultiArrayAtributeClass.class",
			"org/unbiquitous/driver/execution/executeAgent/AMethodArrayParameter.class",
			"org/unbiquitous/driver/execution/executeAgent/AMethodArrayReturnType.class"
	//		"org/unbiquitous/driver/execution/JustAGenericReferencedAtributeClass.class"
		);
	}

	@Test public void packageAlwaysTheSameJar() throws Exception{
		ClassToolbox box1 = new ClassToolbox();
		box1.add2BlackList("luaj-jse-2.0.2.jar");
		File jar1 = box1.packageJarFor(MyJarAgent.class);
		ClassToolbox box2 = new ClassToolbox();
		box2.add2BlackList("luaj-jse-2.0.2.jar");
		File jar2_1 = box2.packageJarFor(MyJarAgent.class);
		File jar2_2 = box2.packageJarFor(MyJarAgent.class);
		
		Set<String> set1 = zipEntries(jar1);
		Set<String> set2_1 = zipEntries(jar2_1);
		Set<String> set2_2 = zipEntries(jar2_2);
		
		assertEquals(jar1.length(),jar2_1.length());	
		// They are different bytes, but with the same content
		assertEquals(set1,set2_1);
		assertEquals(jar2_1.length(),jar2_2.length());	
		assertEquals(set2_1,set2_2);	
	}
	
	@Test public void optimizeJarForDalvik() throws Exception{
		box.add2BlackList("luaj-jse-2.0.2.jar");
		File dalvik = box.packageDalvikFor(MyJarAgent.class);
		
		Set<String> expected = new HashSet<String>();
		expected.add("org/unbiquitous/driver/execution/executeAgent/MyJarAgent.class");
		expected.add("org/unbiquitous/driver/execution/executeAgent/JustAnAttributeClass.class");
		expected.add("org/unbiquitous/driver/execution/executeAgent/AnotherAtributeClass.class");
		expected.add("org/unbiquitous/driver/execution/executeAgent/JustACoolSuperclass.class");
		expected.add("org/unbiquitous/driver/execution/executeAgent/JustANiceInterface.class");
		expected.add("org/unbiquitous/driver/execution/executeAgent/JustAnotherNiceInterface.class");
		expected.add("org/unbiquitous/driver/execution/executeAgent/AMethodParameter.class");
		expected.add("org/unbiquitous/driver/execution/executeAgent/AMethodReturnType.class");
		expected.add("org/unbiquitous/driver/execution/executeAgent/AConstantType.class");
		expected.add("org/unbiquitous/driver/execution/executeAgent/AStaticReturnType.class");
		expected.add("org/unbiquitous/driver/execution/executeAgent/MyJarAgent$Inner.class");
		expected.add("org/unbiquitous/driver/execution/executeAgent/AInnerMethodUsedType.class");
		expected.add("org/unbiquitous/driver/execution/executeAgent/AnException.class");
		expected.add("org/unbiquitous/driver/execution/executeAgent/JustAnArrayAtributeClass.class");
		expected.add("org/unbiquitous/driver/execution/executeAgent/JustAnMultiArrayAtributeClass.class");
		expected.add("org/unbiquitous/driver/execution/executeAgent/AMethodArrayParameter.class");
		expected.add("org/unbiquitous/driver/execution/executeAgent/AMethodArrayReturnType.class");
		expected.add("classes.dex");
		expected.add("META-INF/MANIFEST.MF");
		
		assertThat(zipEntries(dalvik)).containsOnly(expected.toArray(new String[]{}));
	}
	
	@Test(expected=RuntimeException.class) 
	public void conversionToDalvikFailsWhenNoAndroidHomeIsDefined() throws Exception{
		box.convertToDalvik(tempDir, tempDir, null);
	}
	
	@Test(expected=RuntimeException.class) 
	public void conversionToDalvikFailsSoftlyWhenTheConversionDontGoAsExpected() throws Exception{
		box.convertToDalvik(tempDir, new File("doesnotexsist"), System.getenv("ANDROID_HOME"));
	}
}
