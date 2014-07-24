package org.unbiquitous.driver.execution.executionUnity;

@SuppressWarnings("serial")
public class ExecutionError extends RuntimeException {

	public ExecutionError() {}

	public ExecutionError(String message) {
		super(message);
	}

	public ExecutionError(Throwable cause) {
		super(cause);
	}

	public ExecutionError(String message, Throwable cause) {
		super(message, cause);
	}
}
