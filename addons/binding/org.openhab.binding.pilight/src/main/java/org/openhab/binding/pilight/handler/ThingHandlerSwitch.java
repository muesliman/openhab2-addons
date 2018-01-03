package org.openhab.binding.pilight.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.pilight.PilightDeviceConfig;
import org.openhab.binding.pilight.internal.IPilightDeviceHandlerCallback;
import org.openhab.binding.pilight.internal.PilightDeviceHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ThingHandlerSwitch extends BaseThingHandler implements IPilightDeviceHandlerCallback {

    private static final String STATUS_DEVICE_NOT_FOUND_IN_PILIGHT_CONFIGURATION = "Device '%s' not found in Pilight configuration";
    private final Logger logger = LoggerFactory.getLogger(ThingHandlerSwitch.class);

    private PilightDeviceConfig cfg;
    private PilightDeviceHandler handler = null;

    public ThingHandlerSwitch(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        cfg = getThing().getConfiguration().as(PilightDeviceConfig.class);
        logger.debug("config: " + cfg);
        updateStatus(ThingStatus.UNKNOWN); // init complete
    }

    @Override
    public void setHandler(PilightDeviceHandler handler) {
        this.handler = handler;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // TODO Auto-generated method stub

    }

    @Override
    public void onDeviceStatusChanged(DeviceStatus status) {
        switch (status) {
            case ONLINE:
                updateStatus(ThingStatus.ONLINE);
                break;
            case GATEWAY_OFFLINE:
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
                break;
            case NOT_FOUND_IN_CONFIG:
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        String.format(STATUS_DEVICE_NOT_FOUND_IN_PILIGHT_CONFIGURATION, cfg.pilightDeviceName));
                break;
            default:
                updateStatus(ThingStatus.UNKNOWN);
                break;

        }

    }

    @Override
    public String getDeviceName() {
        return cfg.pilightDeviceName;
    }

}
