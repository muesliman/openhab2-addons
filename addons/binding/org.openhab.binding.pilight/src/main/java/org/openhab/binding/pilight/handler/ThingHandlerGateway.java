/**
 * Copyright (c) 2014,2017 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.pilight.handler;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.pilight.PilightGatewayConfig;
import org.openhab.binding.pilight.internal.IPilightDeviceHandlerCallback;
import org.openhab.binding.pilight.internal.IPilightServerHandlerCallback;
import org.openhab.binding.pilight.internal.PilightServerHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link ThingHandlerGateway} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author muesliman/sja initial
 *
 */
public class ThingHandlerGateway extends BaseBridgeHandler implements IPilightServerHandlerCallback {

    private final Logger logger = LoggerFactory.getLogger(ThingHandlerGateway.class);
    private ScheduledFuture<?> pollConfigJob;
    private PilightGatewayConfig cfg;
    private PilightServerHandler pilightInstance;

    public ThingHandlerGateway(Bridge thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing PilightGatewayHandler handler.");
        cfg = getThing().getConfiguration().as(PilightGatewayConfig.class);
        logger.debug("config: " + cfg);
        pilightInstance = new PilightServerHandler();

        pilightInstance.initialize(this, cfg);
        updateStatus(ThingStatus.UNKNOWN); // init complete
        pilightInstance.start();
    }

    @Override
    public void childHandlerInitialized(ThingHandler childHandler, Thing childThing) {
        IPilightDeviceHandlerCallback devHandler = (IPilightDeviceHandlerCallback) childHandler;
        pilightInstance.initializeDevice(devHandler);
        pilightInstance.requestConfig();
    }

    @Override
    public void dispose() {
        pollConfigJob.cancel(true);
        pilightInstance.dispose();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        // if (command instanceof RefreshType) {
        // boolean success = updateWeatherData();
        // if (success) {
        // switch (channelUID.getId()) {
        // case CHANNEL_TEMPERATURE:
        // updateState(channelUID, getTemperature());
        // break;
        // case CHANNEL_HUMIDITY:
        // updateState(channelUID, getHumidity());
        // break;
        // case CHANNEL_PRESSURE:
        // updateState(channelUID, getPressure());
        // break;
        // default:
        // logger.debug("Command received for an unknown channel: {}", channelUID.getId());
        // break;
        // }
        // }
        // } else {
        // logger.debug("Command {} is not supported for channel: {}", command, channelUID.getId());
        // }
    }

    @Override
    public void handlerStatusChanged(HandlerStatus status) {
        if (status == IPilightServerHandlerCallback.HandlerStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
            startConfigPolling();
        } else {
            stopConfigPolling();
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public void writeProperty(@NonNull String property, @NonNull String value) {
        getThing().setProperty(property, value);
    }

    @Override
    public void writeChannel(@NonNull String channel, @NonNull State value) {
        updateState(new ChannelUID(getThing().getUID(), channel), value);
    }

    private void stopConfigPolling() {
        if (pollConfigJob != null && !pollConfigJob.isDone()) {
            pollConfigJob.cancel(true);
        }
    }

    private void startConfigPolling() {
        stopConfigPolling();

        pollConfigJob = scheduler.scheduleWithFixedDelay(() -> {
            pilightInstance.requestConfig();
        }, 0, cfg.configUpdadeInverval, TimeUnit.MINUTES);
    }

    @Override
    public String getUID() {
        return getThing().getUID().toString();
    }

}