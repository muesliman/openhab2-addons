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

import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.thing.binding.builder.ThingStatusInfoBuilder;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.pilight.PilightBindingConstants;
import org.openhab.binding.pilight.PilightGatewayConfig;
import org.openhab.binding.pilight.internal.IPilightGatewayHandlerCallback;
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
public class ThingHandlerGateway extends BaseBridgeHandler implements IPilightGatewayHandlerCallback {

    private final Logger logger = LoggerFactory.getLogger(ThingHandlerGateway.class);
    private static final String STATUS_DEVICE_NOT_FOUND_IN_PILIGHT_CONFIGURATION = "Device '%s' not found in Pilight configuration";

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
        requestSubThingStatusRefresh();
    }

    @Override
    public void dispose() {
        pollConfigJob.cancel(true);
        pilightInstance.dispose();
    }

    @Override
    public void handleUpdate(ChannelUID channelUID, State newState) {
        switch (channelUID.getId()) {
            case PilightBindingConstants.CHANNEL_CONFIG_TRIGGER:

                requestSubThingStatusRefresh();

                break;
        }

    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {

        // if (command instanceof RefreshType) {
        // boolean success = updateWeatherData();
        // if (success) {
        switch (channelUID.getId()) {
            case PilightBindingConstants.CHANNEL_CONFIG_TRIGGER:
                if (command instanceof RefreshType) {
                    requestSubThingStatusRefresh();
                }
                break;
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
        }
    }

    @Override
    public void onGatewayStatusChanged(GatewayStatus status) {
        if (status == IPilightGatewayHandlerCallback.GatewayStatus.ONLINE) {
            updateStatus(ThingStatus.ONLINE);
            for (Thing subItems : this.getThing().getThings()) {
                subItems.setStatusInfo(ThingStatusInfoBuilder.create(ThingStatus.UNKNOWN).build());
            }
            startConfigPolling();
        } else {
            stopConfigPolling();
            updateStatus(ThingStatus.OFFLINE);
            for (Thing subItems : this.getThing().getThings()) {
                subItems.setStatusInfo(
                        ThingStatusInfoBuilder.create(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE).build());
            }
        }
    }

    @Override
    public void writeGatewayProperty(@NonNull String property, @NonNull String value) {
        getThing().setProperty(property, value);
    }

    @Override
    public void writeGatewayChannel(@NonNull String channel, @NonNull State value) {
        updateState(new ChannelUID(getThing().getUID(), channel), value);
    }

    @Override
    public void onDeviceConfigReceived(List<String> devList) {
        // switch (status) {
        // case GATEWAY_OFFLINE:
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE);
        // break;
        // case GATEWAY_ONLINE_CONFIG_PENGING:
        // updateStatus(ThingStatus.UNKNOWN);
        // break;
        // case GATEWAY_ONLINE_FOUND_IN_CONFIG:
        // updateStatus(ThingStatus.ONLINE);
        // break;
        // case GATEWAY_ONLINE_NOT_FOUND_IN_CONFIG:
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
        // String.format(STATUS_DEVICE_NOT_FOUND_IN_PILIGHT_CONFIGURATION, cfg.pilightDeviceName));
        // break;
        // default:
        // updateStatus(ThingStatus.UNKNOWN);
        // break;
        // }
        for (Thing subItems : this.getThing().getThings()) {

            if (subItems.getHandler() instanceof ThingHandlerDevice) {
                ThingHandlerDevice dev = (ThingHandlerDevice) subItems.getHandler();
                if (devList.contains(dev.getDeviceName())) {
                    subItems.setStatusInfo(ThingStatusInfoBuilder.create(ThingStatus.ONLINE).build());
                }
            }
        }

    }

    @Override
    public void onDeviceValuesReceived() {

    }

    private void stopConfigPolling() {
        if (pollConfigJob != null && !pollConfigJob.isDone()) {
            pollConfigJob.cancel(true);
        }
    }

    private void startConfigPolling() {
        stopConfigPolling();

        pollConfigJob = scheduler.scheduleWithFixedDelay(() -> {
            requestSubThingStatusRefresh();
        }, 0, cfg.configUpdadeInverval, TimeUnit.MINUTES);
    }

    public void requestSubThingStatusRefresh() {
        pilightInstance.requestConfig();
    }

    @Override
    public String getUID() {
        return getThing().getUID().toString();
    }

    public static <T> T as(Class<T> clazz, Object o) {
        if (clazz.isInstance(o)) {
            return clazz.cast(o);
        }
        return null;
    }

}
