package org.unbiquitous.driver.execution.remoteExecution;

import java.io.IOException;
import java.io.InputStream;

public class StringInputStream extends InputStream{
	private String data;
	private int index = 0;
	
	public StringInputStream(String data) {	this.data = data;}
	
	public int read() throws IOException {
		if (index < data.length()){
			return data.charAt(index++);
		}
		return -1;
	}
	
}