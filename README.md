Execution Driver
================

This Project is part of the uOS:http://ubiquitos.googlecode.com middleware project.
It provides ways to explore the CPU of a device as a open resource.
For such any application on the Smart Space can send pieces of code that
wants to be executed remotely.
The purpose of this project is to provide methods for:

* Remote Execution using Lua Scripts
* Agent Execution using Java Agents
* Driver and Application Downloads using OSGi bundles

The driver name is *"uos.ExecutionDriver"*

Execution Unity:
-----------------

This allows to create small personalized serializable excution unities that carry with them their state of execution.

Ex:

```Java
StringBuffer script = new StringBuffer();
script.append("value = 0 \n");
script.append("function addTwo() \n");
script.append("		value = value + 2 \n");
script.append("		return value \n");
script.append("end\n");
ExecutionUnity ex = new ExecutionUnity(script.toString());
System.out.println("Value: "+ex.call("addTwo"));
System.out.println("Value: "+ex.call("addTwo"));
```
This will produce:

```logtalk
Value: 2
Value: 4
```

Global state can be changed (and stored) using `setState` method.

Helper methods can be created using the `ExecutionUnity.ExecutionHelper` interface.


Aditionally, an `ExecutionUnity` can be serialized and deserialized using `JSON` format, preserving the informed state and code.


Execute Agent:
-----------------

To use the agent service just call the service named *"executeAgent"*.
Two types of agents are accepted:

* Explicit agents implements the *org.unbiquitous.driver.execution.executeAgent.Agent* interface and have full access to the Smart Space through the *Gateway* object.
* Implicit agents implements only a run(Map) method and has limited access to the Smart Space using the informed map.

To move an agent the best way is to use the AgentUtil methods.

Ex:
```Java
	AgentUtil.move(new MyAgent(),targetDevice,gateway);
```


	
If you want to call the service directly, keep in mind that itd demands a code stream to be trasmited. This stream contains the jar of the transmited code.

Remote Execution:
----------------
	
To use the remote execution service just call the service named *"remoteExecution"*.

*Parameters:*

* *"code"*: Lua code that is going to be executed.
 * Any other parameter informed will be available to the script during execution.

In the lua script two methods provides a way to interact with the service call:

* *get(_key_)*: provide a way to access the parameters informed during the call.
* *set(_key, value_)*: set the values that will be returned after execution is finished.

The following example sends a code that sums two integers and returns the result in the response.

```Java
StringBuffer script = new StringBuffer();
script.append("a = Uos.get(UOS_ID,'a') \n");
script.append("b = Uos.get(UOS_ID,'b') \n");
script.append("Uos.set(UOS_ID,'sum',a+b) \n");

Call call = new Call("uos.ExecutionDriver","remoteExecution");
call.addParameter("a",2);
call.addParameter("b",3);
Response r = gateway.callService(call);
System.out.println("Value: "+r.getResponseParam("sum"));
```

And will produce:

```logtalk
Value: 5
```
