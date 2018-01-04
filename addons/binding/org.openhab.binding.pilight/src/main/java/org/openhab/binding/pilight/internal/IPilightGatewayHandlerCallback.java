package org.openhab.binding.pilight.internal;

import java.util.List;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.types.State;

public interface IPilightGatewayHandlerCallback {

    public enum GatewayStatus {
        INIT,
        ONLINE,
        OFFLINE
    };

    void onDeviceConfigReceived(List<String> deviceList);

    void onDeviceValuesReceived();

    void writeGatewayProperty(@NonNull String property, @NonNull String value);

    void writeGatewayChannel(@NonNull String channel, @NonNull State value);

    void onGatewayStatusChanged(@NonNull GatewayStatus status);

    @NonNull
    String getUID();

}
