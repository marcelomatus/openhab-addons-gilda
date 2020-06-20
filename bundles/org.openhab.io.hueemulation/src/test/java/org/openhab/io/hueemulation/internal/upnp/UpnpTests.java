/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.io.hueemulation.internal.upnp;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.ws.rs.core.Response;

import org.glassfish.grizzly.osgi.httpservice.HttpServiceImpl;
import org.glassfish.grizzly.osgi.httpservice.OSGiMainHandler;
import org.glassfish.grizzly.osgi.httpservice.util.Logger;
import org.glassfish.jersey.server.ResourceConfig;
import org.hamcrest.CoreMatchers;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.openhab.io.hueemulation.internal.rest.CommonSetup;
import org.openhab.io.hueemulation.internal.rest.LightsAndGroups;
import org.osgi.framework.Bundle;
import org.osgi.util.tracker.ServiceTracker;

/**
 * Tests the upnp server part if the description.xml is available and if the udp thread comes online
 *
 * @author David Graeff - Initial contribution
 */
public class UpnpTests {
    protected static CommonSetup commonSetup = null;
    protected UpnpServer subject;
    protected static OSGiMainHandler mainHttpHandler;
    private static HttpServiceImpl httpServiceImpl;
    private static String descriptionPath;

    LightsAndGroups lightsAndGroups = new LightsAndGroups();

    @BeforeClass
    public static void setupHttpParts() throws IOException {
        commonSetup = new CommonSetup(true);
        commonSetup.start(new ResourceConfig());

        descriptionPath = commonSetup.basePath.replace("/api", "/description.xml");

        Logger logger = new org.glassfish.grizzly.osgi.httpservice.util.Logger(mock(ServiceTracker.class));

        mainHttpHandler = new OSGiMainHandler(logger, mock(Bundle.class));
        commonSetup.server.getServerConfiguration().addHttpHandler(mainHttpHandler, "/");

        httpServiceImpl = new HttpServiceImpl(mock(Bundle.class), logger);
    }

    @Before
    public void setup() {
        Executor executor = mock(Executor.class);
        Mockito.doAnswer(a -> {
            ((Runnable) a.getArgument(0)).run();
            return null;
        }).when(executor).execute(ArgumentMatchers.any());
        subject = new UpnpServer(executor);
        subject.httpService = httpServiceImpl;
        subject.cs = commonSetup.cs;
        subject.overwriteReadyToFalse = true;
        subject.activate(); // don't execute handleEvent()
        subject.overwriteReadyToFalse = false;
    }

    @After
    public void tearDown() {
        subject.deactivate();
    }

    @AfterClass
    public static void tearDownHttp() {
        mainHttpHandler.unregisterAll();
        commonSetup.dispose();
    }

    @Test
    public void descriptionWithoutAddress() {
        Response response = commonSetup.client.target(descriptionPath).request().get();
        assertEquals(404, response.getStatus());
    }

    @Test
    public void descriptionWithAddress()
            throws InterruptedException, ExecutionException, TimeoutException, IOException {
        HueEmulationConfigWithRuntime r = subject.createConfiguration(null);
        r = subject.performAddressTest(r);
        subject.applyConfiguration(r);
        Response response = commonSetup.client.target(descriptionPath).request().get();
        assertEquals(200, response.getStatus());
        String body = response.readEntity(String.class);
        assertThat(body, is(subject.xmlDocWithAddress));

        if (r == null) {
            throw new IllegalStateException();
        }

        // UDP thread started?
        r.startNow(r).get(5, TimeUnit.SECONDS);
        assertThat(subject.upnpAnnouncementThreadRunning(), is(true));

        // Send M-SEARCH UPNP "packet" and check if the result contains our bridge ID
        try (DatagramSocket sendSocket = new DatagramSocket()) {
            sendSocket.setSoTimeout(700);
            byte[] bytes = "M-SEARCH".getBytes();
            sendSocket.send(new DatagramPacket(bytes, bytes.length, subject.MULTI_ADDR_IPV4, UpnpServer.UPNP_PORT));
            byte[] buffer = new byte[1000];
            DatagramPacket p = new DatagramPacket(buffer, buffer.length);
            sendSocket.receive(p);
            String received = new String(buffer);
            assertThat(received, CoreMatchers.startsWith("HTTP/1.1 200 OK"));
            assertThat(received, CoreMatchers.containsString("hue-bridgeid: DEMOUUID"));
        }

        r.dispose();
        assertThat(subject.upnpAnnouncementThreadRunning(), is(false));
    }

    @Test
    public void handEventTest() throws InterruptedException, ExecutionException, TimeoutException {
        subject.handleEvent(null);
        subject.configChangeFuture.get(5, TimeUnit.SECONDS);
        assertThat(subject.upnpAnnouncementThreadRunning(), is(true));

        subject.deactivate();
        assertThat(subject.upnpAnnouncementThreadRunning(), is(false));
    }
}
