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

import static org.openhab.binding.pilight.PilightBindingConstants.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.eclipse.smarthome.config.core.status.ConfigStatusMessage;
import org.eclipse.smarthome.core.cache.ExpiringCacheMap;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.ConfigStatusBridgeHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.pilight.PilightGatewayConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The {@link PilightGatewayHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Kai Kreuzer - Initial contribution
 * @author Stefan Bußweiler - Integrate new thing status handling
 * @author Thomas Höfer - Added config status provider
 * @author Christoph Weitkamp - Changed use of caching utils to ESH ExpiringCacheMap
 *
 */
public class PilightGatewayHandler extends ConfigStatusBridgeHandler implements IReadCallbacks, IDiscover {

    private final Logger logger = LoggerFactory.getLogger(PilightGatewayHandler.class);
    private final String setupChannel = "{\"action\": \"identify\", \"options\": { \"core\": 1, \"receiver\": 1, \"config\": 1, \"forward\": 0, \"stats\" : 1 }, \"uuid\": \"%s\", \"media\": \"all\"}";
    private final String requestConfig = "{\"action\": \"request config\"}";
    private final String requestValues = "{\"action\": \"request values\"}";

    /// OLD ///
    private static final String LOCATION_PARAM = "location";
    private static final int MAX_DATA_AGE = 3 * 60 * 60 * 1000; // 3h
    private static final int CACHE_EXPIRY = 10 * 1000; // 10s
    private static final String CACHE_KEY_CONFIG = "CONFIG_STATUS";
    private static final String CACHE_KEY_WEATHER = "WEATHER";

    private final ExpiringCacheMap<String, String> cache = new ExpiringCacheMap<>(CACHE_EXPIRY);

    private long lastUpdateTime;

    private BigDecimal location;
    private BigDecimal refresh;

    private String weatherData = null;

    ScheduledFuture<?> refreshJob;

    //// END OLD ///

    private ReaderThread readerThread;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PilightGatewayHandler(Bridge thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        logger.debug("Initializing PilightGatewayHandler handler.");

        //
        // location = (BigDecimal) config.get(LOCATION_PARAM);
        //
        // try {
        // refresh = (BigDecimal) config.get("refresh");
        // } catch (Exception e) {
        // logger.debug("Cannot set refresh parameter.", e);
        // }
        //
        // if (refresh == null) {
        // // let's go for the default
        // refresh = new BigDecimal(60);
        // }
        //
        // cache.put(CACHE_KEY_CONFIG, () -> connection.getResponseFromQuery(
        // "SELECT location FROM weather.forecast WHERE woeid = " + location.toPlainString()));
        // cache.put(CACHE_KEY_WEATHER, () -> connection.getResponseFromQuery(
        // "SELECT * FROM weather.forecast WHERE u = 'c' AND woeid = " + location.toPlainString()));

        // get ip from conig
        // validate ip + port
        //

        PilightGatewayConfig cfg = getThing().getConfiguration().as(PilightGatewayConfig.class);

        logger.debug("config: " + cfg);

        readerThread = new ReaderThread(cfg.ipAddress, cfg.port, this);
        readerThread.start();

        // startAutomaticRefresh();
    }

    @Override
    public void dispose() {
        // refreshJob.cancel(true);
        readerThread.stopReading();
    }

    private void startAutomaticRefresh() {
        refreshJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                boolean success = updateWeatherData();
                if (success) {
                    updateState(new ChannelUID(getThing().getUID(), CHANNEL_TEMPERATURE), getTemperature());
                    updateState(new ChannelUID(getThing().getUID(), CHANNEL_HUMIDITY), getHumidity());
                    updateState(new ChannelUID(getThing().getUID(), CHANNEL_PRESSURE), getPressure());
                }
            } catch (Exception e) {
                logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, e.getMessage());
            }
        }, 0, refresh.intValue(), TimeUnit.SECONDS);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (true) {
            return;
        }
        if (command instanceof RefreshType) {
            boolean success = updateWeatherData();
            if (success) {
                switch (channelUID.getId()) {
                    case CHANNEL_TEMPERATURE:
                        updateState(channelUID, getTemperature());
                        break;
                    case CHANNEL_HUMIDITY:
                        updateState(channelUID, getHumidity());
                        break;
                    case CHANNEL_PRESSURE:
                        updateState(channelUID, getPressure());
                        break;
                    default:
                        logger.debug("Command received for an unknown channel: {}", channelUID.getId());
                        break;
                }
            }
        } else {
            logger.debug("Command {} is not supported for channel: {}", command, channelUID.getId());
        }
    }

    @Override
    public Collection<ConfigStatusMessage> getConfigStatus() {
        Collection<ConfigStatusMessage> configStatus = new ArrayList<>();

        final String locationData = cache.get(CACHE_KEY_CONFIG);
        if (locationData != null) {
            String city = getValue(locationData, "location", "city");
            if (city == null) {
                configStatus.add(ConfigStatusMessage.Builder.error(LOCATION_PARAM)
                        .withMessageKeySuffix("location-not-found").withArguments(location.toPlainString()).build());
            }
        }

        return configStatus;
    }

    private synchronized boolean updateWeatherData() {
        final String data = cache.get(CACHE_KEY_WEATHER);
        if (data != null) {
            if (data.contains("\"results\":null")) {
                if (isCurrentDataExpired()) {
                    logger.trace(
                            "The Yahoo Weather API did not return any data. Omiting the old result because it became too old.");
                    weatherData = null;
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                            "@text/offline.no-data");
                    return false;
                } else {
                    // simply keep the old data
                    logger.trace("The Yahoo Weather API did not return any data. Keeping the old result.");
                    return false;
                }
            } else {
                lastUpdateTime = System.currentTimeMillis();
                weatherData = data;
            }
            updateStatus(ThingStatus.ONLINE);
            return true;
        }
        weatherData = null;
        updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR,
                "@text/offline.location [\"" + location.toPlainString() + "\"");
        return false;
    }

    private boolean isCurrentDataExpired() {
        return lastUpdateTime + MAX_DATA_AGE < System.currentTimeMillis();
    }

    private State getHumidity() {
        if (weatherData != null) {
            String humidity = getValue(weatherData, "atmosphere", "humidity");
            if (humidity != null) {
                return new DecimalType(humidity);
            }
        }
        return UnDefType.UNDEF;
    }

    private State getPressure() {
        if (weatherData != null) {
            String pressure = getValue(weatherData, "atmosphere", "pressure");
            if (pressure != null) {
                DecimalType ret = new DecimalType(pressure);
                if (ret.doubleValue() > 10000.0) {
                    // Unreasonably high, record so far was 1085,8 hPa
                    // The Yahoo API currently returns inHg values although it claims they are mbar - therefore convert
                    ret = new DecimalType(BigDecimal.valueOf((long) (ret.doubleValue() / 0.3386388158), 2));
                }
                return ret;
            }
        }
        return UnDefType.UNDEF;
    }

    private State getTemperature() {
        if (weatherData != null) {
            String temp = getValue(weatherData, "condition", "temp");
            if (temp != null) {
                return new DecimalType(temp);
            }
        }
        return UnDefType.UNDEF;
    }

    private String getValue(String data, String element, String param) {
        String tmp = StringUtils.substringAfter(data, element);
        if (tmp != null) {
            return StringUtils.substringBetween(tmp, param + "\":\"", "\"");
        }
        return null;
    }

    /**
     * Function to convert a string into a JSON Node
     *
     * @param cmd json string
     * @return null on errors
     */
    private JsonNode getNode(String cmd) {
        try {
            return objectMapper.readTree(cmd);
        } catch (IOException e) {
            logger.error("received invalid JSON: '" + cmd + "'");
        }
        return null;
    }

    @Override
    public void handleReceivedCommand(String cmd) {
        try {
            JsonNode node = getNode(cmd);
            if (node != null) {
                // only parse if is is a valid json
                parseJson(node);
            }
        } catch (JsonParseException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (JsonMappingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    /**
     * Function to parse the returned json string.
     * Internal commands are parsed here, all others are forwarded
     *
     * @param node
     * @param jsonStr
     * @throws JsonParseException
     * @throws JsonMappingException
     * @throws IOException
     */
    private void parseJson(JsonNode node) throws JsonParseException, JsonMappingException, IOException {
        JsonNode subNode = null;

        if (node != null) {

            // check if it was a 'status' json
            subNode = node.get("status");
            if (subNode != null) {
                logger.debug("status received from pilight");
                // ignore status. Do not know what to do in case of ack/nak
                // value = success or failure
                // Response response = objectMapper.readValue(node.toString(), Response.class);
                // TODO add content
            }

            // check if it was a 'config' json
            subNode = node.get("config");
            if (subNode != null) {
                logger.debug("config received from pilight");

                JsonNode configNode = subNode.get("devices");
                if (configNode != null) {
                    // todo: check for removed devices

                    // it over dev.
                    for (Thing thing : getThing().getThings()) {
                        //
                    }
                    // new device found:

                }

                configNode = subNode.get("registry");
                if (configNode != null) {
                    configNode = configNode.get("pilight");
                    configNode = configNode.get("version");
                    configNode = configNode.get("current");
                    getThing().setProperty(PROP_PILIGHT, configNode.asText());
                }
            }

            // check if it was a 'origin' json
            subNode = node.get("origin");
            if (subNode != null)

            {
                logger.debug("origin received from pilight");
                // value = core or config or sender or receiver
                String value = subNode.asText();
                if (value.equalsIgnoreCase("core")) {
                    // {"origin":"core","values":{"cpu":0.2872180289431904,"ram":0.7184862377889740},"type":-1,"uuid":"0000-74-da-38-0e47eb"}
                    /*
                     * tryReadDoubleValue(node, "cpu");
                     * tryReadDoubleValue(node, "ram");
                     * tryReadIntValue(node, "version");
                     * tryReadIntValue(node, "lpf");
                     * tryReadIntValue(node, "hpf");
                     */
                    Double val;

                    if (null != (val = tryReadDoubleValue(node, "cpu"))) {
                        updateState(new ChannelUID(getThing().getUID(), CHANNEL_CPU), new DecimalType(val));
                    }

                } else if (value.equalsIgnoreCase("sender")) {
                    // no publish to OH
                } else if (value.equalsIgnoreCase("receiver")) {
                    // no publish to OH
                } else if (value.equalsIgnoreCase("update")) {
                    // no publish to OH
                } else {
                    logger.debug("unkown origin type " + value);
                }
            }

            subNode = node.get("action");
            if (subNode != null) {
                logger.debug("action received from pilight");
            }

            subNode = node.get("settings");
            if (subNode != null) {
                logger.debug("settings received from pilight");
            }

            subNode = node.get("values");
            if (subNode != null) {
                logger.debug("values received from pilight");
            }

        }
    }

    private Double tryReadDoubleValue(JsonNode node, String valueName) {
        JsonNode valueNode = node.get("values");
        if (valueNode != null) {
            JsonNode valueSubNode = valueNode.get(valueName);
            if (valueSubNode != null) {
                if (valueSubNode.isDouble()) {
                    return valueSubNode.asDouble();
                }
            }
        }
        return null;
    }

    private String tryReadText(JsonNode node, String valueName) {
        JsonNode valueNode = node.get("values");
        if (valueNode != null) {
            JsonNode valueSubNode = valueNode.get(valueName);
            if (valueSubNode != null) {
                return valueSubNode.asText();
            }
        }
        return null;
    }

    @Override
    public void statusChanged(Status status) {
        switch (status) {
            case INIT:
                updateStatus(ThingStatus.OFFLINE);
                break;
            case RUNNING:
                updateStatus(ThingStatus.ONLINE);
                String uuid = getThing().getUID().toString();
                int first = uuid.length() - 21;
                if (first < 0) {
                    first = 0;
                    // "0000-74-da-38-123456"
                }

                readerThread.sendRequest(String.format(setupChannel, uuid.substring(first, uuid.length() - 1)));
                readerThread.sendRequest(requestConfig);
                break;
            case TERMINATED:
                updateStatus(ThingStatus.OFFLINE);
                break;
            case WAITING:
                updateStatus(ThingStatus.OFFLINE);
                break;
            default:
                updateStatus(ThingStatus.UNKNOWN);
                break;
        }
    }
}
