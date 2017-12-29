package org.openhab.binding.pilight.handler;

import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PilightSwitchHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(PilightGatewayHandler.class);
    private PilightGatewayHandler pilightGatewayHandler;

    public PilightSwitchHandler(Thing thing) {
        super(thing);
        this.pilightGatewayHandler = (PilightGatewayHandler) getBridge();
        logger.debug("bingo");
        // TODO Auto-generated constructor stub
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // TODO Auto-generated method stub

    }

}
