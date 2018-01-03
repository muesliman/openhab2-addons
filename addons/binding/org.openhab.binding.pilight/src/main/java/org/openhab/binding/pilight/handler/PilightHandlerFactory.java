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

import java.util.Hashtable;

import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandlerFactory;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.openhab.binding.pilight.PilightBindingConstants;
import org.openhab.binding.pilight.discovery.PilightDiscoveryService;

/**
 * The {@link PilightHandlerFactory} is responsible for creating things and thing
 * handlers.
 *
 * @author muesliman/sja - Initial contribution
 */
public class PilightHandlerFactory extends BaseThingHandlerFactory {

    @Override
    public boolean supportsThingType(ThingTypeUID thingTypeUID) {
        return PilightBindingConstants.SUPPORTED_THING_TYPES_UIDS.contains(thingTypeUID);
    }

    @Override
    protected ThingHandler createHandler(Thing thing) {
        ThingTypeUID thingTypeUID = thing.getThingTypeUID();

        if (thingTypeUID.equals(PilightBindingConstants.THING_TYPE_GATEWAY)) {
            PilightGatewayHandler gwHandler = new PilightGatewayHandler((Bridge) thing);
            getBundleContext().registerService(DiscoveryService.class.getName(), new PilightDiscoveryService(gwHandler),
                    new Hashtable<String, Object>());
            return gwHandler;

        } else if (thingTypeUID.equals(PilightBindingConstants.THING_TYPE_SWITCH)) {
            return new PilightSwitchHandler(thing);
        }

        return null;
    }
}
