package org.openhab.binding.pilight.internal;

public interface IPilightDeviceHandlerCallback {

    public enum DeviceStatus {
        INIT,
        GATEWAY_OFFLINE,
        GATEWAY_ONLINE_CONFIG_PENGING,
        GATEWAY_ONLINE_FOUND_IN_CONFIG,
        GATEWAY_ONLINE_NOT_FOUND_IN_CONFIG
    }

    String getDeviceName();

    void setHandler(PilightDeviceHandler handler);

    void onDeviceStatusChanged(DeviceStatus status);

}
