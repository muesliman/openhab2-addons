package org.openhab.binding.pilight.internal;

public interface IPilightDeviceHandlerCallback {

    public enum DeviceStatus {
        INIT,
        ONLINE,
        GATEWAY_OFFLINE,
        NOT_FOUND_IN_CONFIG
    }

    String getDeviceName();

    void setHandler(PilightDeviceHandler handler);

    void onDeviceStatusChanged(DeviceStatus status);

}
