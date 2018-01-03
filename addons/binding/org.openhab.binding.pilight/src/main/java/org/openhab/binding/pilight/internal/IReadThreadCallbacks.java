package org.openhab.binding.pilight.internal;

public interface IReadThreadCallbacks {

    public enum ReadThreadStatus {
        INIT,
        WAITING,
        RUNNING,
        TERMINATED
    }

    void handleReceivedCommand(String cmd);

    void readThreadStatusChanged(ReadThreadStatus status);

}
