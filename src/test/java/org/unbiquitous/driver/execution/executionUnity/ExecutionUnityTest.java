package org.unbiquitous.driver.execution.executionUnity;

import static org.fest.assertions.api.Assertions.assertThat;

import java.util.HashMap;

import org.junit.Test;

public class ExecutionUnityTest {

	@Test public void mustExecuteCode() {
		StringBuffer script = new StringBuffer();
		script.append("function run() \n");
		script.append("		count = 3 + 1\n");
		script.append("		return count \n");
		script.append("end\n");
		ExecutionUnity ex = new ExecutionUnity(script.toString());
		assertThat(ex.call("run")).isEqualTo("4");
	}
	
	@Test public void notProblemWithVoidFunctions() {
		StringBuffer script = new StringBuffer();
		script.append("function run() \n");
		script.append("		a = 1+1 \n");
		script.append("end\n");
		ExecutionUnity ex = new ExecutionUnity(script.toString());
		assertThat(ex.call("run")).isEqualTo("nil");
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked", "serial" })
	@Test public void canSendParametersToCode() {
		StringBuffer script = new StringBuffer();
		script.append("function add2(value) \n");
		script.append("		return value+2 \n");
		script.append("end\n");
		script.append("function concatBar(value) \n");
		script.append("		return value..'bar' \n");
		script.append("end\n");
		script.append("function sumMap(value) \n");
		script.append("		return value['x']+value['y'] \n");
		script.append("end\n");
		ExecutionUnity ex = new ExecutionUnity(script.toString());
		assertThat(ex.call("add2",3)).isEqualTo("5");
		assertThat(ex.call("concatBar","foo")).isEqualTo("foobar");
		assertThat(ex.call("sumMap",new HashMap(){{
			put("x",7);
			put("y",11);
		}})).isEqualTo("18");
	}

	@Test public void executedCodeMantainsGlobalState() {
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

	@Test public void dontMixExecutionStateFromDifferentUnities() {
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

	@Test public void allowToRegisterHelperMethods() {
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
	
	@Test public void helperMethodCanReturnNull() {
		StringBuffer script = new StringBuffer();
		script.append("function myMethod() \n");
		script.append("		return help()\n");
		script.append("end\n");
		ExecutionUnity ex = new ExecutionUnity(script.toString(),
				new ExecutionUnity.ExecutionHelper() {
					public String invoke(String... args) {
						return null;
					}
					public String name() {
						return null;
					}
				});
		assertThat(ex.call("myMethod")).isEqualTo("nil");
	}

	@Test public void helperMethodsCanHaveMultipleArgs() {
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

	@Test public void helperMethodsCanHaveOtherNames() {
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
	
	@Test public void multipleHelpersAreAccepted() {
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
	
	@Test public void stateMustBeAvailableAsVariablesToMethods() {
		StringBuffer script = new StringBuffer();
		script.append("function sum3() \n");
		script.append("		return value+3 \n");
		script.append("end\n");
		script.append("function concat3() \n");
		script.append("		return value..'3' \n");
		script.append("end\n");
		script.append("function justTheValue() \n");
		script.append("		return value \n");
		script.append("end\n");
		ExecutionUnity ex = new ExecutionUnity(script.toString());
		ex.setState("value",2);
		assertThat(ex.call("sum3")).isEqualTo("5");
		assertThat(ex.call("justTheValue")).isEqualTo("2");
		ex.setState("value","abacate");
		assertThat(ex.call("concat3")).isEqualTo("abacate3");
		assertThat(ex.call("justTheValue")).isEqualTo("abacate");
		ex.setState("value",null);
		assertThat(ex.call("justTheValue")).isEqualTo("nil");
	}
	
	@Test public void stateAreModifiable() {
		StringBuffer script = new StringBuffer();
		script.append("function inc() \n");
		script.append("		value = value + 3 \n");
		script.append("		return value \n");
		script.append("end\n");
		ExecutionUnity ex = new ExecutionUnity(script.toString());
		ex.setState("value",2);
		assertThat(ex.call("inc")).isEqualTo("5");
		assertThat(ex.call("inc")).isEqualTo("8");
	}
	
	//TODO: set Inner state based on a map
	
	@Test public void unitsAreSerializableToJSON() {
		StringBuffer script = new StringBuffer();
		script.append("function two() \n");
		script.append("		return 2 \n");
		script.append("end\n");
		ExecutionUnity ex = new ExecutionUnity(script.toString());
		
		ExecutionUnity deserialized = ExecutionUnity.fromJSON(ex.toJSON());
		assertThat(ex.call("two")).isEqualTo("2");
		assertThat(deserialized.call("two")).isEqualTo("2");
	}
	
	@Test public void statesAreAlsoSerialized() {
		StringBuffer script = new StringBuffer();
		script.append("function addTwo() \n");
		script.append("		value = value + 2 \n");
		script.append("		return value \n");
		script.append("end\n");
		ExecutionUnity ex = new ExecutionUnity(script.toString());
		ex.setState("value", 2);
		assertThat(ex.call("addTwo")).isEqualTo("4");

		ExecutionUnity deserialized = ExecutionUnity.fromJSON(ex.toJSON());
		assertThat(deserialized.call("addTwo")).isEqualTo("6");
	}
	
	@Test public void twoJSONSerializationsAreTheSame() {
		StringBuffer script = new StringBuffer();
		script.append("function two() \n");
		script.append("		return 2 \n");
		script.append("end\n");
		ExecutionUnity ex1 = new ExecutionUnity(script.toString());
		ExecutionUnity ex2 = new ExecutionUnity(script.toString());
		assertThat(ex1.toJSON()).isEqualTo(ex2.toJSON());
	}
}
