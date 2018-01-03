package org.openhab.binding.pilight.internal;

import org.openhab.binding.pilight.handler.PilightGatewayHandler;
import org.openhab.binding.pilight.internal.IPilightDeviceHandlerCallback.DeviceStatus;

public class PilightDeviceHandler {
    private String pilightDeviceName;
    private PilightGatewayHandler devHandler;
    private IPilightDeviceHandlerCallback callback;
    private DeviceStatus status = DeviceStatus.Init;

    public String getPilightDeviceName() {
        return pilightDeviceName;
    }

    public PilightDeviceHandler(String pilightDeviceName, PilightGatewayHandler devHandler,
            IPilightDeviceHandlerCallback callback) {
        this.pilightDeviceName = pilightDeviceName;
        this.devHandler = devHandler;
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
}
