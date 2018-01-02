package org.openhab.binding.pilight.handler;

public interface IPilightDeviceHandlerCallback {

    public enum DeviceStatus {
        Init,
        FoundInConfig,
        NotFoundInConfig
    }

    String getDeviceName();

    void setHandler(PilightDeviceHandler handler);

    void onDeviceStatusChanged(DeviceStatus status);

}
