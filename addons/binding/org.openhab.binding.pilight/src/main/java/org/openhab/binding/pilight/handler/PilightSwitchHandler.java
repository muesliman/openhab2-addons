package org.openhab.binding.pilight.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.pilight.PilightDeviceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PilightSwitchHandler extends BaseThingHandler implements IPilightDeviceHandlerCallback {
    private static final String STATUS_DEVICE_NOT_FOUND_IN_PILIGHT_CONFIGURATION = "Device '%s' not found in Pilight configuration";

    private final Logger logger = LoggerFactory.getLogger(PilightSwitchHandler.class);
    private PilightDeviceConfig cfg;
    private PilightDeviceHandler handler = null;

    public PilightSwitchHandler(Thing thing) {
        super(thing);
        // TODO Auto-generated constructor stub
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);
        cfg = getThing().getConfiguration().as(PilightDeviceConfig.class);
        logger.debug("config: " + cfg);
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
            case FoundInConfig:
                updateStatus(ThingStatus.ONLINE);
                break;
            case NotFoundInConfig:
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                        String.format(STATUS_DEVICE_NOT_FOUND_IN_PILIGHT_CONFIGURATION, cfg.pilightDeviceName));
                break;
            default:
                updateStatus(ThingStatus.OFFLINE);
                break;

        }

    }

    @Override
    public String getDeviceName() {
        return cfg.pilightDeviceName;
    }

}
