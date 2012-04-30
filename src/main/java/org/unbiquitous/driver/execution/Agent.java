package org.unbiquitous.driver.execution;

import java.io.ObjectOutputStream;
import java.io.Serializable;

import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.Gateway;
import br.unb.unbiquitous.ubiquitos.uos.adaptabitilyEngine.ServiceCallException;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.dataType.UpDevice;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceCall.ServiceType;
import br.unb.unbiquitous.ubiquitos.uos.messageEngine.messages.ServiceResponse;

public abstract class Agent implements Serializable{
	private static final long serialVersionUID = 8386943272111021563L;

	private Gateway gateway;
	
	public abstract void run(Gateway gateway);
	
	public void init(Gateway gateway){	this.gateway = gateway;	}
	
	//FIXME: This is a huge spike
	protected void moveTo(UpDevice to){
		
		ServiceCall move = new ServiceCall("uos.ExecutionDriver", "executeAgent");
		move.setChannels(1);
		move.setServiceType(ServiceType.STREAM);
		
		try {
			ServiceResponse r = gateway.callService(to, move);
			ObjectOutputStream writer = new ObjectOutputStream(r.getMessageContext().getDataOutputStream());
			writer.writeObject(this);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
}
