package org.openhab.binding.pilight.internal;

public interface IReadCallbacks {

	public enum Status{
		INIT,
		WAITING,
		RUNNING,
		TERMINATED
	}
	
	void handleReceivedCommand(String cmd);

	void statusChanged(Status status);

}
