package org.openhab.binding.pilight.internal;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.types.State;

public interface IPilightGatewayHandlerCallback {

    public enum GatewayStatus {
        INIT,
        ONLINE,
        OFFLINE
    };

    void writeProperty(@NonNull String property, @NonNull String value);

    void writeChannel(@NonNull String channel, @NonNull State value);

    void onStatusChanged(@NonNull GatewayStatus status);

    @NonNull
    String getUID();

}
