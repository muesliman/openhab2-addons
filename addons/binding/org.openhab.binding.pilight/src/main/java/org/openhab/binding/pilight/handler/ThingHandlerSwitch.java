package org.openhab.binding.pilight.handler;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusInfo;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.pilight.PilightBindingConstants;
import org.openhab.binding.pilight.PilightDeviceConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//ConfigStatusThingHandler
public class ThingHandlerSwitch extends BaseThingHandler implements ThingHandlerDevice {

    private final Logger logger = LoggerFactory.getLogger(ThingHandlerSwitch.class);

    private PilightDeviceConfig cfg;

    public ThingHandlerSwitch(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        cfg = getThing().getConfiguration().as(PilightDeviceConfig.class);
        logger.debug("config: " + cfg);
        updateStatus(ThingStatus.OFFLINE); // init complete
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // TODO Auto-generated method stub
    }

    @Override
    protected void updateConfiguration(Configuration configuration) {
        super.updateConfiguration(configuration);
        // postCommand(new ChannelUID(getThing().getBridgeUID(), PilightBindingConstants.CHANNEL_CONFIG_TRIGGER),
        // RefreshType.REFRESH);
        triggerChannel(new ChannelUID(getThing().getBridgeUID(), PilightBindingConstants.CHANNEL_CONFIG_TRIGGER));

    }

    @Override
    public void bridgeStatusChanged(ThingStatusInfo bridgeStatusInfo) {

    }

    @Override
    public String getDeviceName() {
        return cfg.pilightDeviceName;
    }

}
