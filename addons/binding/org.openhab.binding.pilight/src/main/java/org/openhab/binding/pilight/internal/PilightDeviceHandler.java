package org.openhab.binding.pilight.internal;

import org.openhab.binding.pilight.internal.IPilightDeviceHandlerCallback.DeviceStatus;

public class PilightDeviceHandler {
    private String pilightDeviceName;
    private PilightServerHandler pilightInstance;
    private IPilightDeviceHandlerCallback callback;
    private DeviceStatus status = DeviceStatus.INIT;

    public String getPilightDeviceName() {
        return pilightDeviceName;
    }

    public PilightDeviceHandler(String pilightDeviceName, PilightServerHandler pilightInstance,
            IPilightDeviceHandlerCallback callback) {
        this.pilightDeviceName = pilightDeviceName;
        this.pilightInstance = pilightInstance;
        this.callback = callback;
    }

    public void notifyStatus(DeviceStatus status) {
        if (this.status != status) {
            this.status = status;
            if (callback != null) {
                callback.onDeviceStatusChanged(this.status);
            }
        }
    }

    public PilightServerHandler getPilightInstance() {
        return pilightInstance;
    }

}
