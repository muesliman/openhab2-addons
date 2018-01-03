package org.openhab.binding.pilight.internal;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.types.State;

public interface IPilightServerHandlerCallback {

    public enum HandlerStatus {
        INIT,
        ONLINE,
        OFFLINE
    };

    void writeProperty(@NonNull String property, @NonNull String value);

    void writeChannel(@NonNull String channel, @NonNull State value);

    void handlerStatusChanged(@NonNull HandlerStatus status);

    @NonNull
    String getUID();

}
