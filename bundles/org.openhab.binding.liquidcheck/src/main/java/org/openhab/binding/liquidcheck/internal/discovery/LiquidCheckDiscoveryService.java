/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.liquidcheck.internal.discovery;

import static org.openhab.binding.liquidcheck.internal.LiquidCheckBindingConstants.*;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.openhab.binding.liquidcheck.internal.json.Response;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.config.discovery.DiscoveryResult;
import org.openhab.core.config.discovery.DiscoveryResultBuilder;
import org.openhab.core.config.discovery.DiscoveryService;
import org.openhab.core.thing.ThingUID;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * The {@link LiquidCheckDiscoveryService} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Marcel Goerentz - Initial contribution
 */
@NonNullByDefault
@Component(service = DiscoveryService.class, immediate = true, configurationPid = "discovery.liquidcheck")
public class LiquidCheckDiscoveryService extends AbstractDiscoveryService {

    private static final int DISCOVER_TIMEOUT_SECONDS = 60;
    private static final int BACKGROUND_DISCOVERY_AFTER_MINUTES = 60;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    private @Nullable ScheduledFuture<?> liquidCheckBackgroundDiscoveryJob;

    public LiquidCheckDiscoveryService() {
        super(SUPPORTED_THING_TYPES_UIDS, DISCOVER_TIMEOUT_SECONDS);
    }

<<<<<<< HEAD
=======
    /**
     * This method starts the scan
     */
>>>>>>> eac3c23fa09d0130ae16dbdc99ddb83d1743b51d
    @Override
    protected void startScan() {
        try {
            List<InetAddress> addresses = getIPv4Adresses();
            List<InetAddress> hosts = findActiveHosts(addresses);
            HttpClient client = new HttpClient();
            client.start();
            for (InetAddress host : hosts) {
                Request request = client.newRequest("http://" + host.getHostAddress() + "/infos.json");
                request.followRedirects(false);
                ContentResponse response = request.send();
                if (response.getStatus() == 200) {
                    Response json = new Gson().fromJson(response.getContentAsString(), Response.class);
                    if (null != json) {
                        buildDiscoveryResult(json);
                    } else {
                        logger.debug("Response Object is null!");
                    }
                }
            }
        } catch (SocketException e) {
            logger.debug("Socket Exception {}", e.toString());
        } catch (UnknownHostException e) {
            logger.debug("UnknownHostException {}", e.toString());
        } catch (IOException e) {
            logger.debug("IOException {}", e.toString());
        } catch (Exception e) {
            logger.debug("Exception {}", e.toString());
        }
    }

    /**
     * Method to stop the scan
     */
    @Override
    protected synchronized void stopScan() {
        super.stopScan();
        removeOlderResults(getTimestampOfLastScan());
    }

    /**
     * Method for starting the background discovery
     */
    @Override
    protected void startBackgroundDiscovery() {
        ScheduledFuture<?> avoidNullException = this.liquidCheckBackgroundDiscoveryJob;
        if (null == avoidNullException || avoidNullException.isCancelled()) {
            this.liquidCheckBackgroundDiscoveryJob = scheduler.scheduleWithFixedDelay(liquidCheckDiscoveryRunnable(), 0,
                    BACKGROUND_DISCOVERY_AFTER_MINUTES, TimeUnit.MINUTES);
        }
    }

    /**
     * Method for stopping the background discovery
     */
    @Override
    protected void stopBackgroundDiscovery() {
        ScheduledFuture<?> avoidNullException = this.liquidCheckBackgroundDiscoveryJob;
        if (null != avoidNullException && !avoidNullException.isCancelled()) {
            avoidNullException.cancel(true);
            this.liquidCheckBackgroundDiscoveryJob = null;
        }
    }

    /**
<<<<<<< HEAD
     * Method for starting the scan
=======
     * Method for setting up a runnable
>>>>>>> eac3c23fa09d0130ae16dbdc99ddb83d1743b51d
     */
    protected Runnable liquidCheckDiscoveryRunnable() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                startScan();
            }
        };
        return runnable;
    }

    /**
<<<<<<< HEAD
     * This Method retrieves all IPv4 addresses of the server
=======
     * This method retrieves all IPv4 addresses of the server
>>>>>>> eac3c23fa09d0130ae16dbdc99ddb83d1743b51d
     * 
     * @return A list of all available IPv4 Adresses that are registered
     * @throws SocketException
     */
    private List<InetAddress> getIPv4Adresses() throws SocketException {
        Iterator<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces().asIterator();
        List<InetAddress> addresses = new ArrayList<>();
        // Get IPv4 addresses from all network interfaces
        if (null != networkInterfaces) {
            while (networkInterfaces.hasNext()) {
                NetworkInterface currentNetworkInterface = networkInterfaces.next();
                Iterator<InetAddress> inetAddresses = currentNetworkInterface.getInetAddresses().asIterator();
                while (inetAddresses.hasNext()) {
                    InetAddress currentAddress = inetAddresses.next();
                    if (currentAddress instanceof Inet4Address && !currentAddress.isLoopbackAddress()) {
                        addresses.add(currentAddress);
                    }
                }
            }
        }
        return addresses;
    }

<<<<<<< HEAD
=======
    /**
     * This method returns a list with the active hosts that have been found
     * 
     * @param addresses List with ip addresses
     * @return list with active Hosts
     * @throws UnknownHostException
     * @throws IOException
     */
>>>>>>> eac3c23fa09d0130ae16dbdc99ddb83d1743b51d
    private List<InetAddress> findActiveHosts(List<InetAddress> addresses) throws UnknownHostException, IOException {
        List<InetAddress> hosts = new ArrayList<>();
        for (InetAddress inetAddress : addresses) {
            String[] adresStrings = inetAddress.getHostAddress().split("[.]");
            String subnet = adresStrings[0] + "." + adresStrings[1] + "." + adresStrings[2];
            int timeout = 50;
<<<<<<< HEAD
            for (int i = 1; i < 255; i++) {
=======
            for (int i = 1; i < 255; i++) { // search for every ip in the given subnet
>>>>>>> eac3c23fa09d0130ae16dbdc99ddb83d1743b51d
                String host = subnet + "." + i;
                if (InetAddress.getByName(host).isReachable(timeout)) {
                    hosts.add(InetAddress.getByName(host));
                }
            }
        }
        return hosts;
    }

<<<<<<< HEAD
    private void buildDiscoveryResult(Response response) {
        LiquidCheckProperties lcproperties = new LiquidCheckProperties(response);
=======
    /**
     * 
     * @param response
     */
    private void buildDiscoveryResult(Response response) {
>>>>>>> eac3c23fa09d0130ae16dbdc99ddb83d1743b51d
        Map<String, Object> properties = new HashMap<>();
        properties.put(CONFIG_ID_FIRMWARE, response.payload.device.firmware);
        properties.put(CONFIG_ID_HARDWARE, response.payload.device.hardware);
        properties.put(CONFIG_ID_NAME, response.payload.device.name);
        properties.put(CONFIG_ID_MANUFACTURER, response.payload.device.manufacturer);
        properties.put(CONFIG_ID_UUID, response.payload.device.uuid);
        properties.put(CONFIG_ID_SECURITY_CODE, response.payload.device.security.code);
        properties.put(CONFIG_ID_IP, response.payload.wifi.station.ip);
        properties.put(CONFIG_ID_MAC, response.payload.wifi.station.mac);
        properties.put(CONFIG_ID_SSID, response.payload.wifi.accessPoint.ssid);
<<<<<<< HEAD
        ThingUID thingUID = new ThingUID(THING_TYPE_LIQUID_CHEK, lcproperties.uuid);
        DiscoveryResult dResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                .withLabel(lcproperties.name + "_DEBUG").build();
=======
        ThingUID thingUID = new ThingUID(THING_TYPE_LIQUID_CHECK, response.payload.device.uuid);
        DiscoveryResult dResult = DiscoveryResultBuilder.create(thingUID).withProperties(properties)
                .withLabel(response.payload.device.name + "_DEBUG").build();
>>>>>>> eac3c23fa09d0130ae16dbdc99ddb83d1743b51d
        thingDiscovered(dResult);
    }
}
