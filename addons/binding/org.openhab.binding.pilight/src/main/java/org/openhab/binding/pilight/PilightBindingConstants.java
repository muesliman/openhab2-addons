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
package org.openhab.binding.pilight;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link YahooWeatherBinding} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Kai Kreuzer - Initial contribution
 */
public class PilightBindingConstants {

    public static final String BINDING_ID = "pilight";

    // List all Thing Type UIDs, related to the YahooWeather Binding
    public static final ThingTypeUID THING_TYPE_GATEWAY = new ThingTypeUID(BINDING_ID, "gateway");
    public static final ThingTypeUID THING_TYPE_SWITCH = new ThingTypeUID(BINDING_ID, "switch");
    public static final ThingTypeUID THING_TYPE_DIMMER = new ThingTypeUID(BINDING_ID, "dimmer");
    public static final ThingTypeUID THING_TYPE_WEATHER = new ThingTypeUID(BINDING_ID, "weather");

    // List all channels
    public static final String CHANNEL_TEMPERATURE = "temperature";
    public static final String CHANNEL_HUMIDITY = "humidity";
    public static final String CHANNEL_PRESSURE = "pressure";

    public static final String CHANNEL_STATE = "state";
    public static final String CHANNEL_CPU = "cpu";
    public static final String CHANNEL_RAM = "ram";
    public static final String CHANNEL_CONFIG_TRIGGER = "configTrigger";
    // List of GW Properties

    public static final String PROP_PILIGHT = "pilightVersion";
    public static final String PROP_PROTOCOL = "protocol";

    // List of GW Parameter

    public static final String CONFIG_GATEWAY_IP = "ipAddress";
    public static final String CONFIG_GATEWAY_PORT = "port";

    public static final List<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections.unmodifiableList(
            Arrays.asList(THING_TYPE_GATEWAY, THING_TYPE_SWITCH, THING_TYPE_DIMMER, THING_TYPE_WEATHER));
}
