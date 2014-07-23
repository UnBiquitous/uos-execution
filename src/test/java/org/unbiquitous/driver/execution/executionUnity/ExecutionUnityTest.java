package org.unbiquitous.driver.execution.executionUnity;

import static org.fest.assertions.api.Assertions.*;

import org.junit.Test;

public class ExecutionUnityTest {

	@Test
	public void mustExecuteCode() {
		StringBuffer script = new StringBuffer();
		script.append("function run() \n");
		script.append("		count = 3 + 1\n");
		script.append("		return count \n");
		script.append("end\n");
		ExecutionUnity ex = new ExecutionUnity(script.toString());
		assertThat(ex.call("run")).isEqualTo("4");
	}

	@Test
	public void executedCodeMantainsGlobalState() {
		StringBuffer script = new StringBuffer();
		script.append("count  = 0 \n");
		script.append("function inc() \n");
		script.append("		count = count +1\n");
		script.append("		return count \n");
		script.append("end\n");
		ExecutionUnity ex = new ExecutionUnity(script.toString());
		assertThat(ex.call("inc")).isEqualTo("1");
		assertThat(ex.call("inc")).isEqualTo("2");
	}

	@Test
	public void dontMixExecutionStateFromDifferentUnities() {
		StringBuffer script = new StringBuffer();
		script.append("count  = 0 \n");
		script.append("function inc() \n");
		script.append("		count = count +1\n");
		script.append("		return count \n");
		script.append("end\n");
		ExecutionUnity ex1 = new ExecutionUnity(script.toString());
		ExecutionUnity ex2 = new ExecutionUnity(script.toString());
		assertThat(ex1.call("inc")).isEqualTo("1");
		assertThat(ex2.call("inc")).isEqualTo("1");
		assertThat(ex1.call("inc")).isEqualTo("2");
		assertThat(ex1.call("inc")).isEqualTo("3");
		assertThat(ex2.call("inc")).isEqualTo("2");
	}

	@Test
	public void allowToRegisterHelperMethods() {
		StringBuffer script = new StringBuffer();
		script.append("function myMethod() \n");
		script.append("		value = help()\n");
		script.append("		return value \n");
		script.append("end\n");
		ExecutionUnity ex = new ExecutionUnity(script.toString(),
				new ExecutionUnity.ExecutionHelper() {
					public String invoke(String... args) {
						return "42";
					}
					public String name() {
						return null;
					}
				});
		assertThat(ex.call("myMethod")).isEqualTo("42");
	}

	@Test
	public void helperMethodsCanHaveMultipleArgs() {
		StringBuffer script = new StringBuffer();
		script.append("function m1() \n");
		script.append("		return help(1,2) \n");
		script.append("end\n");
		script.append("function m2() \n");
		script.append("		return help(1,2,3) \n");
		script.append("end\n");
		ExecutionUnity ex = new ExecutionUnity(script.toString(),
				new ExecutionUnity.ExecutionHelper() {

					public String invoke(String... args) {
						int ints[] = new int[args.length];
						for (int i = 0; i < args.length; i++) {
							ints[i] = Integer.parseInt(args[i]);
						}
						if (args.length > 2) {
							return "" + (ints[0] + ints[1] + ints[2]);
						}
						return "" + ((float) ints[0] / ints[1]);
					}

					public String name() {
						return null;
					}
				});
		assertThat(ex.call("m1")).isEqualTo("0.5");
		assertThat(ex.call("m2")).isEqualTo("6");
	}

	@Test
	public void helperMethodsCanHaveOtherNames() {
		StringBuffer script = new StringBuffer();
		script.append("function myMethod() \n");
		script.append("		value = luckyNumber()\n");
		script.append("		return value \n");
		script.append("end\n");
		ExecutionUnity ex = new ExecutionUnity(script.toString(),
				new ExecutionUnity.ExecutionHelper() {
					public String name() {
						return "luckyNumber";
					}

					public String invoke(String... args) {
						return "13";
					}
				});
		assertThat(ex.call("myMethod")).isEqualTo("13");
	}
	
	@Test
	public void multipleHelpersAreAccepted() {
		StringBuffer script = new StringBuffer();
		script.append("function theMethod() \n");
		script.append("		value = numberOne() + doubleThat(5)\n");
		script.append("		return value \n");
		script.append("end\n");
		ExecutionUnity ex = new ExecutionUnity(script.toString());
		ex.addHelper(new ExecutionUnity.ExecutionHelper() {
					public String name() {
						return "numberOne";
					}
					public String invoke(String... args) {
						return "1";
					}
				});
		ex.addHelper(new ExecutionUnity.ExecutionHelper() {
			public String name() {
				return "doubleThat";
			}
			public String invoke(String... args) {
				return ""+(2*Integer.parseInt(args[0]));
			}
		});
		assertThat(ex.call("theMethod")).isEqualTo("11");
	}
	
	//TODO: inner state
	//TODO: external state
	//TODO: serializable (JSON)
}
