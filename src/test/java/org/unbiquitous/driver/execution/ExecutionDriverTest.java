package org.unbiquitous.driver.execution;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.unbiquitous.driver.execution.executeAgent.ClassToolbox;
import org.unbiquitous.uos.core.driverManager.UosDriver;
import org.unbiquitous.uos.core.messageEngine.dataType.UpDriver;
import org.unbiquitous.uos.core.messageEngine.dataType.UpService;
import org.unbiquitous.uos.core.messageEngine.dataType.UpService.ParameterType;
import org.unbiquitous.uos.core.messageEngine.messages.Response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ExecutionDriverTest {

	private static final ObjectMapper mapper = new ObjectMapper();

	private ExecutionDriver driver;

	@Before
	public void setUp() {
		driver = new ExecutionDriver();
	}

	@Test
	public void uPDriverInterfaceIsConsistent() {
		assertTrue(driver instanceof UosDriver);
		UpDriver uDriver = driver.getDriver();
		assertNotNull(uDriver);
		assertEquals("uos.ExecutionDriver", uDriver.getName());

		//assert services
		assertNotNull(uDriver.getServices());
		assertEquals(3, uDriver.getServices().size());

		// Assert remoteExecution
		UpService remoteExecution = uDriver.getServices().get(0);
		assertNotNull(remoteExecution);
		assertEquals("remoteExecution", remoteExecution.getName());
		assertNotNull(remoteExecution.getParameters());
		assertEquals(1, remoteExecution.getParameters().size());
		assertTrue(remoteExecution.getParameters().containsKey("code"));
		assertEquals(ParameterType.MANDATORY, remoteExecution.getParameters().get("code"));

		// Assert remoteExecution
		UpService executeAgent = uDriver.getServices().get(1);
		assertNotNull(executeAgent);
		assertEquals("executeAgent", executeAgent.getName());
		//		assertNull(executeAgent.getParameters());
		//		assertEquals(executeAgent.) tem que ser stream?

		// List Known Classes
		UpService listKnownClasses = uDriver.getServices().get(2);
		assertNotNull(listKnownClasses);
		assertEquals("listKnownClasses", listKnownClasses.getName());
	}

	@Test
	public void shouldUseInformedToolbox() {
		final ClassToolbox myBox = new ClassToolbox();
		driver = new ExecutionDriver(myBox);
		assertSame(myBox, driver.toolbox());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void listKnownClasses() throws IOException {
		Response response = new Response();
		driver.listKnownClasses(null, response, null);
		assertThat(response.getResponseData("classes")).isEqualTo(driver.toolbox().listKnownClasses());
		ObjectNode json = mapper.valueToTree(response);
		ArrayNode jsonArray = (ArrayNode) json.get("responseData").get("classes");
		assertThat((List<String>) mapper.treeToValue(jsonArray, List.class)).isEqualTo(driver.toolbox().listKnownClasses());

	}

}