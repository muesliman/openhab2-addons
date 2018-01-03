package org.openhab.binding.pilight.discovery;

import org.eclipse.smarthome.config.discovery.AbstractDiscoveryService;
import org.eclipse.smarthome.config.discovery.DiscoveryResult;
import org.eclipse.smarthome.config.discovery.DiscoveryResultBuilder;
import org.eclipse.smarthome.config.discovery.DiscoveryService;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.openhab.binding.pilight.handler.PilightGatewayHandler;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component(service = DiscoveryService.class, immediate = false, configurationPid = "discovery.pilight")
public class PilightDiscoveryService extends AbstractDiscoveryService {
    private final Logger logger = LoggerFactory.getLogger(PilightDiscoveryService.class);

    private PilightGatewayHandler discoverHandler;

    public PilightDiscoveryService() {
        super(100);
    }

    public PilightDiscoveryService(PilightGatewayHandler gwHandler) {
        super(100);
        this.discoverHandler = gwHandler;

    }

    @Override
    protected void startScan() {
        // TODO Auto-generated method stub
        logger.error("Scan started");
        DiscoveryResult discoveryResult = DiscoveryResultBuilder.create(new ThingUID("sdlkijdsfklgf"))
                .withBridge(new ThingUID("s.o.")).build();
        thingDiscovered(discoveryResult);
    }

}
