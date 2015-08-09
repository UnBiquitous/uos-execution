package org.unbiquitous.driver.execution.executeAgent;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.unbiquitous.driver.execution.executeAgent.CompilationUtil.assertStream;
import static org.unbiquitous.driver.execution.executeAgent.CompilationUtil.zipEntries;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.ArgumentMatcher;
import org.unbiquitous.driver.execution.executeAgent.dummy.DummyAgent;
import org.unbiquitous.uos.core.adaptabitilyEngine.Gateway;
import org.unbiquitous.uos.core.adaptabitilyEngine.ServiceCallException;
import org.unbiquitous.uos.core.applicationManager.CallContext;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDevice;
import org.unbiquitous.uos.core.messageEngine.messages.Call;
import org.unbiquitous.uos.core.messageEngine.messages.Call.ServiceType;
import org.unbiquitous.uos.core.messageEngine.messages.Response;

import com.fasterxml.jackson.databind.ObjectMapper;

public class AgentUtilTest {

	private static final ObjectMapper mapper = new ObjectMapper();

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private static ClassToolbox box;

	private static AgentUtil agentUtil;

	@BeforeClass
	public static void init() {
		box = new ClassToolbox();
		agentUtil = new AgentUtil();
	}

	@Test
	public void movingCallsExecuteAgentOnExecutionDriver() throws Exception {
		Gateway gateway = mockGateway(new ByteArrayOutputStream(), new ByteArrayOutputStream());

		UpDevice target = new UpDevice("target");
		agentUtil.move(new MyAgent(), target, gateway);

		ArgumentCaptor<UpDevice> deviceCaptor = ArgumentCaptor.forClass(UpDevice.class);
		ArgumentCaptor<Call> callCaptor = ArgumentCaptor.forClass(Call.class);

		verify(gateway, times(2)).callService(deviceCaptor.capture(), callCaptor.capture());

		assertEquals(target, deviceCaptor.getValue());
		Call listKnownClasses = callCaptor.getAllValues().get(0);
		assertEquals("uos.ExecutionDriver", listKnownClasses.getDriver());
		assertEquals("listKnownClasses", listKnownClasses.getService());

		Call executeAgent = callCaptor.getAllValues().get(1);
		assertEquals("uos.ExecutionDriver", executeAgent.getDriver());
		assertEquals("executeAgent", executeAgent.getService());
		assertEquals("Must have 2 channels (agent and jar)", 2, executeAgent.getChannels());
		assertEquals("Service is of type stream", ServiceType.STREAM, executeAgent.getServiceType());
		assertEquals("Default type of move is jar", "true", executeAgent.getParameter("jar"));

	}

	@Test
	public void movingSendsAgentSerialized() throws Exception {
		MyAgent agent = new MyAgent();
		ByteArrayOutputStream agentSpy = new ByteArrayOutputStream();
		//Using buffered i can check if close was properly called
		Gateway gateway = mockGateway(new BufferedOutputStream(agentSpy), new ByteArrayOutputStream());

		UpDevice target = new UpDevice("target");
		agentUtil.move(agent, target, gateway);

		assertArrayEquals(serialize(agent), agentSpy.toByteArray());
	}

	@SuppressWarnings("serial")
	@Test
	public void considerRemoteKnownClassesWhenCreatingJar() throws Exception {
		MyAgent agent = new MyAgent();
		File jarSpy = File.createTempFile("uOSAUtilTmpJar", ".jar");

		Gateway gateway = mockGateway(new ByteArrayOutputStream(),
				new BufferedOutputStream(new FileOutputStream(jarSpy)));

		ArrayList<String> knownClasses = new ArrayList<String>() {
			{
				add(MyAgent.AgentSpy.class.getName());
			}
		};

		Response knownClassesResponse = new Response();
		knownClassesResponse.addParameter("classes", mapper.valueToTree(knownClasses));

		when(gateway.callService((UpDevice) any(), argThat(serviceMatcher("listKnownClasses"))))
				.thenReturn(knownClassesResponse);

		UpDevice target = new UpDevice("target");
		agentUtil.move(agent, target, gateway);

		File jar = box.packageJarFor(agent.getClass(), knownClasses);

		assertThat(zipEntries(jarSpy)).containsOnly(zipEntries(jar).toArray(new String[] {}));
	}

	@Test
	public void movingSendsAgentJar() throws Exception {
		MyAgent agent = new MyAgent();
		File jarSpy = File.createTempFile("uOSAUtilTmpJar", ".jar");
		//Using buffered i can check if close was properly called
		Gateway gateway = mockGateway(new ByteArrayOutputStream(),
				new BufferedOutputStream(new FileOutputStream(jarSpy)));

		UpDevice target = new UpDevice("target");
		agentUtil.move(agent, target, gateway);

		File jar = box.packageJarFor(agent.getClass());

		assertEquals(zipEntries(jar), zipEntries(jarSpy));
	}

	@Test
	public void canMoveOnlyTheAgent() throws Exception {
		MyAgent agent = new MyAgent();
		ByteArrayOutputStream agentSpy = new ByteArrayOutputStream();
		File jarSpy = File.createTempFile("uOSAUtilTmpJar", ".jar");

		Gateway gateway = mockGateway(new BufferedOutputStream(agentSpy),
				new BufferedOutputStream(new FileOutputStream(jarSpy)));

		UpDevice target = new UpDevice("target");
		agentUtil.move(agent, target, gateway, false);

		assertArrayEquals(serialize(agent), agentSpy.toByteArray());
		assertEquals(0, jarSpy.length());
	}

	@Test
	public void movingSendsSpecificPackage() throws Exception {
		Agent agent = new DummyAgent();
		File jarSpy = File.createTempFile("uOSAUtilTmpJar", ".jar");

		Gateway gateway = mockGateway(new ByteArrayOutputStream(),
				new BufferedOutputStream(new FileOutputStream(jarSpy)));

		File dummyPkg = File.createTempFile("dummy", ".jar");
		FileOutputStream writer = new FileOutputStream(dummyPkg);
		writer.write("Hello Package".getBytes());
		writer.close();

		UpDevice target = new UpDevice("target");
		agentUtil.move(agent, dummyPkg, target, gateway);

		assertStream(new FileInputStream(dummyPkg), new FileInputStream(jarSpy));
	}

	@Test
	public void movingSendsDalvikJarWhenTargetIsAndroid() throws Exception {
		MyAgent agent = new MyAgent();
		File jarSpy = File.createTempFile("uOSAUtilTmpJar", ".jar");

		Gateway gateway = mockGateway(new ByteArrayOutputStream(),
				new BufferedOutputStream(new FileOutputStream(jarSpy)));

		UpDevice target = new UpDevice("target");
		target.addProperty("platform", "Dalvik");
		agentUtil.move(agent, target, gateway);

		File jar = box.packageJarFor(agent.getClass());
		File dalvik = box.convertToDalvik(folder.getRoot(), jar, System.getenv("ANDROID_HOME"));
		assertEquals(zipEntries(dalvik), zipEntries(jarSpy));
	}

	@Test(expected = RuntimeException.class)
	public void rejectsAgentsAsInnerClasses() throws Exception {
		Gateway gateway = mockGateway(new ByteArrayOutputStream(), new ByteArrayOutputStream());

		UpDevice target = new UpDevice("target");
		agentUtil.move(new Agent() {
			private static final long serialVersionUID = 2420826408820982276L;

			public void run(Gateway gateway) {
			}
		}, target, gateway);
	}

	@Test(expected = RuntimeException.class)
	public void rejectsAgentsAsNotPublicClasses() throws Exception {
		Gateway gateway = mockGateway(new ByteArrayOutputStream(), new ByteArrayOutputStream());

		UpDevice target = new UpDevice("target");
		agentUtil.move(new ShyAgent(), target, gateway);
	}

	@Test
	public void GatewayMustBeTransient() throws Exception {
		Gateway gateway = mockGateway(new ByteArrayOutputStream(), new ByteArrayOutputStream());

		UpDevice target = new UpDevice("target");
		final MyAgent agent = new MyAgent();
		agent.init(gateway);
		agentUtil.move(agent, target, gateway);
	}

	private Gateway mockGateway(final OutputStream spy, final OutputStream jarSpy) throws ServiceCallException {
		Gateway gateway = mock(Gateway.class);

		when(gateway.callService((UpDevice) any(), argThat(serviceMatcher("listKnownClasses"))))
				.thenReturn(new Response());

		ArgumentMatcher<Call> execute = serviceMatcher("executeAgent");

		when(gateway.callService(any(UpDevice.class), argThat(execute))).thenReturn(new Response() {
			@Override
			public CallContext getMessageContext() {
				return new CallContext() {
					public DataOutputStream getDataOutputStream() {
						return new DataOutputStream(spy);
					}

					public DataOutputStream getDataOutputStream(int index) {
						switch (index) {
						case 0:
							return new DataOutputStream(spy);
						case 1:
							return new DataOutputStream(jarSpy);
						default:
							return null;
						}
					}
				};
			}
		});
		return gateway;
	}

	private ArgumentMatcher<Call> serviceMatcher(final String serviceName) {
		ArgumentMatcher<Call> execute = new ArgumentMatcher<Call>() {
			public boolean matches(Object argument) {
				if (argument instanceof Call) {
					Call call = (Call) argument;
					return call != null && call.getService() != null && call.getService().equals(serviceName);
				}
				return false;
			}
		};
		return execute;
	}

	private byte[] serialize(final MyAgent agent) throws IOException {
		ByteArrayOutputStream arraySpy = new ByteArrayOutputStream();
		ObjectOutputStream objectWriter = new ObjectOutputStream(arraySpy);
		objectWriter.writeObject(agent);
		return arraySpy.toByteArray();
	}

}

class ShyAgent extends Agent {
	private static final long serialVersionUID = 1L;

	public void run(Gateway gateway) {
	}
}