/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.mybmw.internal.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.openhab.binding.mybmw.internal.dto.vehicle.Vehicle;
import org.openhab.binding.mybmw.internal.handler.MyBMWBridgeHandler;
import org.openhab.binding.mybmw.internal.util.FileReader;
import org.openhab.binding.mybmw.internal.utils.Converter;
import org.openhab.core.config.discovery.DiscoveryListener;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ThingUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link DiscoveryTest} Test Discovery Results
 *
 * @author Bernd Weymann - Initial contribution
 */
@NonNullByDefault
public class DiscoveryTest {
    private final Logger logger = LoggerFactory.getLogger(DiscoveryTest.class);

    @Test
    public void testDiscovery() {
        String content = FileReader.readFileInString("src/test/resources/responses/I01_REX/vehicles.json");
        MyBMWBridgeHandler bh = mock(MyBMWBridgeHandler.class);
        Bridge b = mock(Bridge.class);
        when(bh.getThing()).thenReturn(b);
        when(b.getUID()).thenReturn(new ThingUID("mybmw", "account", "abc"));
        VehicleDiscovery discovery = new VehicleDiscovery();
        discovery.setThingHandler(bh);
        DiscoveryListener listener = mock(DiscoveryListener.class);
        discovery.addDiscoveryListener(listener);
        List<Vehicle> vl = Converter.getVehicleList(content);
        assertEquals(1, vl.size(), "Vehicles found");
        ArgumentCaptor<DiscoveryResult> discoveries = ArgumentCaptor.forClass(DiscoveryResult.class);
        ArgumentCaptor<DiscoveryService> services = ArgumentCaptor.forClass(DiscoveryService.class);
        discovery.onResponse(vl);
        verify(listener, times(1)).thingDiscovered(services.capture(), discoveries.capture());
        List<DiscoveryResult> results = discoveries.getAllValues();
        assertEquals(1, results.size(), "Found Vehicles");
        DiscoveryResult result = results.get(0);
        assertEquals("mybmw:bev_rex:abc:anonymous", result.getThingUID().getAsString(), "Thing UID");
    }
}
