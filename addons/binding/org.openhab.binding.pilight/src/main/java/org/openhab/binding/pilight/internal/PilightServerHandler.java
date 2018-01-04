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
package org.openhab.binding.pilight.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.openhab.binding.pilight.PilightBindingConstants;
import org.openhab.binding.pilight.PilightGatewayConfig;
import org.openhab.binding.pilight.internal.IPilightGatewayHandlerCallback.GatewayStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * The {@link PilightServerHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author muesliman/sja initial
 *
 */
public class PilightServerHandler implements IReadThreadCallbacks {

    private final String setupChannel = "{\"action\": \"identify\", \"options\": { \"core\": 1, \"receiver\": 1, \"config\": 1, \"forward\": 0, \"stats\" : 1 }, \"uuid\": \"%s\", \"media\": \"all\"}";
    private final String requestConfig = "{\"action\": \"request config\"}";
    private final String requestValues = "{\"action\": \"request values\"}";

    private final Logger logger = LoggerFactory.getLogger(PilightServerHandler.class);

    private final List<String> registeredDevices = new ArrayList<String>();

    private IPilightGatewayHandlerCallback gatewayHandler;
    private PilightGatewayConfig cfg;

    private String requestUid = "oh2";
    private ReaderThread readerThread;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public void initialize(IPilightGatewayHandlerCallback pilightGatewayHandler, PilightGatewayConfig cfg) {
        logger.debug("Initializing PilightInstance.");
        this.cfg = cfg;
        this.gatewayHandler = pilightGatewayHandler;

        String uuid = pilightGatewayHandler.getUID();
        int first = uuid.length() - 21; // max len from PILIGHT
        if (first < 0) {
            first = 0;
        }
        requestUid = uuid.substring(first, uuid.length() - 1);

        readerThread = new ReaderThread(cfg.ipAddress, cfg.port, this);
    }

    public void start() {
        readerThread.start();
    }

    public void dispose() {
        readerThread.stopReading();
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
                    readPilightConfiguration(configNode);
                }

                configNode = subNode.get("registry");
                if (configNode != null) {
                    readPilightRegistry(configNode);
                }
            }

            // check if it was a 'origin' json
            subNode = node.get("origin");
            if (subNode != null) {
                logger.debug("origin received from pilight");
                // value = core or config or sender or receiver
                String value = subNode.asText();
                if (value.equalsIgnoreCase("core")) {
                    readPilightOriginCore(subNode);
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

    private void readPilightOriginCore(JsonNode node) {
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
            gatewayHandler.writeGatewayChannel(PilightBindingConstants.CHANNEL_CPU, new DecimalType(val));
        }
        if (null != (val = tryReadDoubleValue(node, "ram"))) {
            gatewayHandler.writeGatewayChannel(PilightBindingConstants.CHANNEL_RAM, new DecimalType(val));
        }
    }

    private void readPilightRegistry(@NonNull JsonNode configNode) {
        try {
            JsonNode subNode = configNode.get("pilight");
            subNode = subNode.get("version");
            subNode = subNode.get("current");
            gatewayHandler.writeGatewayProperty(PilightBindingConstants.PROP_PILIGHT, subNode.asText());
        } catch (NullPointerException e) {
            // no special handling
        }
    }

    private void readPilightConfiguration(@NonNull JsonNode configNode) {
        registeredDevices.clear();
        Iterator<Entry<String, JsonNode>> jsonDevices = configNode.fields();
        while (jsonDevices.hasNext()) {
            Map.Entry<String, JsonNode> jsonDevice = jsonDevices.next();
            registeredDevices.add(jsonDevice.getKey());
        }
        gatewayHandler.onDeviceConfigReceived(new ArrayList<String>(registeredDevices));
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
    public void readThreadStatusChanged(ReadThreadStatus status) {
        switch (status) {
            case RUNNING:
                readerThread.sendRequest(String.format(setupChannel, requestUid));
                readerThread.sendRequest(requestConfig);
                gatewayHandler.onGatewayStatusChanged(GatewayStatus.ONLINE);
                break;
            case TERMINATED:
            case WAITING:
            case INIT:
            default:
                gatewayHandler.onGatewayStatusChanged(GatewayStatus.OFFLINE);
                break;
        }
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

    public void requestConfig() {
        readerThread.sendRequest(requestConfig);
    }

    public void requestValues() {
        readerThread.sendRequest(requestValues);
    }

}
