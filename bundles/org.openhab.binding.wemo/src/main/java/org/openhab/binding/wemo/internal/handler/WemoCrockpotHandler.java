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
package org.openhab.binding.wemo.internal.handler;

import static org.openhab.binding.wemo.internal.WemoBindingConstants.*;
import static org.openhab.binding.wemo.internal.WemoUtil.*;

import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.wemo.internal.http.WemoHttpCall;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.io.transport.upnp.UpnpIOParticipant;
import org.openhab.core.io.transport.upnp.UpnpIOService;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link WemoCrockpotHandler} is responsible for handling commands, which are
 * sent to one of the channels and to update their states.
 *
 * @author Hans-Jörg Merk - Initial contribution;
 */
@NonNullByDefault
public class WemoCrockpotHandler extends AbstractWemoHandler implements UpnpIOParticipant {

    private final Logger logger = LoggerFactory.getLogger(WemoCrockpotHandler.class);

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_CROCKPOT);

    private final Object upnpLock = new Object();
    private final Object jobLock = new Object();

    private final String BASICEVENT = "basicevent";

    private final Map<String, String> stateMap = Collections.synchronizedMap(new HashMap<>());

    private @Nullable UpnpIOService service;

    private WemoHttpCall wemoCall;

    private String host = "";

    private Map<String, Boolean> subscriptionState = new HashMap<>();

    private @Nullable ScheduledFuture<?> pollingJob;

    public WemoCrockpotHandler(Thing thing, UpnpIOService upnpIOService, WemoHttpCall wemoHttpCaller) {
        super(thing, wemoHttpCaller);

        this.wemoCall = wemoHttpCaller;
        this.service = upnpIOService;

        logger.debug("Creating a WemoCrockpotHandler for thing '{}'", getThing().getUID());
    }

    @Override
    public void initialize() {
        Configuration configuration = getConfig();

        if (configuration.get(UDN) != null) {
            logger.debug("Initializing WemoCrockpotHandler for UDN '{}'", configuration.get(UDN));
            if (service != null) {
                service.registerParticipant(this);
            }

            pollingJob = scheduler.scheduleWithFixedDelay(this::poll, 0, DEFAULT_REFRESH_INTERVALL_SECONDS,
                    TimeUnit.SECONDS);
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "@text/config-status.error.missing-udn");
            logger.debug("Cannot initalize WemoCrockpotHandler. UDN not set.");
        }
    }

    @Override
    public void dispose() {
        logger.debug("WeMoCrockpotHandler disposed.");

        ScheduledFuture<?> job = this.pollingJob;
        if (job != null && !job.isCancelled()) {
            job.cancel(true);
        }
        this.pollingJob = null;
        removeSubscription();
        if (service != null) {
            service.unregisterParticipant(this);
        }
    }

    private void poll() {
        synchronized (jobLock) {
            if (pollingJob == null) {
                return;
            }
            try {
                logger.debug("Polling job");

                if (host.isEmpty()) {
                    if (service != null) {
                        URL descriptorURL = service.getDescriptorURL(this);
                        if (descriptorURL != null) {
                            host = descriptorURL.getHost();
                        }
                    }
                }
                // Check if the Wemo device is set in the UPnP service registry
                // If not, set the thing state to OFFLINE/CONFIG-PENDING and wait for the next poll
                if (!isUpnpDeviceRegistered()) {
                    logger.debug("UPnP device {} not yet registered", getUDN());
                    updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_PENDING,
                            "@text/config-status.pending.device-not-registered [\"" + getUDN() + "\"]");
                    synchronized (upnpLock) {
                        subscriptionState = new HashMap<>();
                    }
                    return;
                }
                updateStatus(ThingStatus.ONLINE);
                updateWemoState();
                addSubscription();
            } catch (Exception e) {
                logger.debug("Exception during poll: {}", e.getMessage(), e);
            }
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (host.isEmpty()) {
            logger.error("Failed to send command '{}' for device '{}': IP address missing", command,
                    getThing().getUID());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "@text/config-status.error.missing-ip");
            return;
        }
        String wemoURL = getWemoURL(host, BASICEVENT);
        if (wemoURL == null) {
            logger.error("Failed to send command '{}' for device '{}': URL cannot be created", command,
                    getThing().getUID());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "@text/config-status.error.missing-url");
            return;
        }
        String mode = "0";
        String time = null;

        if (command instanceof RefreshType) {
            updateWemoState();
        } else if (CHANNEL_COOKMODE.equals(channelUID.getId())) {
            String commandString = command.toString();
            switch (commandString) {
                case "OFF":
                    mode = "0";
                    time = "0";
                    break;
                case "WARM":
                    mode = "50";
                    break;
                case "LOW":
                    mode = "51";
                    break;
                case "HIGH":
                    mode = "52";
                    break;
            }
            try {
                String soapHeader = "\"urn:Belkin:service:basicevent:1#SetBinaryState\"";
                String content = "<?xml version=\"1.0\"?>"
                        + "<s:Envelope xmlns:s=\"http://schemas.xmlsoap.org/soap/envelope/\" s:encodingStyle=\"http://schemas.xmlsoap.org/soap/encoding/\">"
                        + "<s:Body>" + "<u:SetCrockpotState xmlns:u=\"urn:Belkin:service:basicevent:1\">" + "<mode>"
                        + mode + "</mode>" + "<time>" + time + "</time>" + "</u:SetCrockpotState>" + "</s:Body>"
                        + "</s:Envelope>";
                wemoCall.executeCall(wemoURL, soapHeader, content);
            } catch (RuntimeException e) {
                logger.debug("Failed to send command '{}' for device '{}':", command, getThing().getUID(), e);
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
            }
            updateStatus(ThingStatus.ONLINE);
        }
    }

    @Override
    public void onServiceSubscribed(@Nullable String service, boolean succeeded) {
        if (service != null) {
            logger.debug("WeMo {}: Subscription to service {} {}", getUDN(), service,
                    succeeded ? "succeeded" : "failed");
            subscriptionState.put(service, succeeded);
        }
    }

    @Override
    public void onValueReceived(@Nullable String variable, @Nullable String value, @Nullable String service) {
        logger.debug("Received pair '{}':'{}' (service '{}') for thing '{}'", variable, value, service,
                this.getThing().getUID());

        updateStatus(ThingStatus.ONLINE);
        if (variable != null && value != null) {
            this.stateMap.put(variable, value);
        }
    }

    private synchronized void addSubscription() {
        synchronized (upnpLock) {
            UpnpIOService localservice = service;
            if (localservice != null) {
                if (localservice.isRegistered(this)) {
                    logger.debug("Checking WeMo GENA subscription for '{}'", this);

                    String subscription = BASICEVENT + "1";

                    if (subscriptionState.get(subscription) == null) {
                        logger.debug("Setting up GENA subscription {}: Subscribing to service {}...", getUDN(),
                                subscription);
                        localservice.addSubscription(this, subscription, SUBSCRIPTION_DURATION_SECONDS);
                        subscriptionState.put(subscription, true);
                    }
                } else {
                    logger.debug(
                            "Setting up WeMo GENA subscription for '{}' FAILED - service.isRegistered(this) is FALSE",
                            getThing().getUID());
                }
            }
        }
    }

    private synchronized void removeSubscription() {
        synchronized (upnpLock) {
            UpnpIOService localservice = service;
            if (localservice != null) {
                if (localservice.isRegistered(this)) {
                    logger.debug("Removing WeMo GENA subscription for '{}'", this);
                    String subscription = BASICEVENT + "1";

                    if (subscriptionState.get(subscription) != null) {
                        logger.debug("WeMo {}: Unsubscribing from service {}...", getUDN(), subscription);
                        localservice.removeSubscription(this, subscription);
                    }
                    subscriptionState.remove(subscription);
                    localservice.unregisterParticipant(this);
                }
            }
        }
    }

    private boolean isUpnpDeviceRegistered() {
        if (service != null) {
            return service.isRegistered(this);
        }
        return false;
    }

    @Override
    public String getUDN() {
        return (String) this.getThing().getConfiguration().get(UDN);
    }

    /**
     * The {@link updateWemoState} polls the actual state of a WeMo device and
     * calls {@link onValueReceived} to update the statemap and channels..
     *
     */
    protected void updateWemoState() {
        String actionService = BASICEVENT;
        if (host.isEmpty()) {
            logger.error("Failed to get actual state for device '{}': IP address missing", getThing().getUID());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "@text/config-status.error.missing-ip");
            return;
        }
        String wemoURL = getWemoURL(host, actionService);
        if (wemoURL == null) {
            logger.error("Failed to get actual state for device '{}': URL cannot be created", getThing().getUID());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "@text/config-status.error.missing-url");
            return;
        }
        try {
            String action = "GetCrockpotState";
            String soapHeader = "\"urn:Belkin:service:" + actionService + ":1#" + action + "\"";
            String content = createStateRequestContent(action, actionService);
            String wemoCallResponse = wemoCall.executeCall(wemoURL, soapHeader, content);
            if (wemoCallResponse != null) {
                logger.trace("State response '{}' for device '{}' received", wemoCallResponse, getThing().getUID());
                String mode = substringBetween(wemoCallResponse, "<mode>", "</mode>");
                String time = substringBetween(wemoCallResponse, "<time>", "</time>");
                String coockedTime = substringBetween(wemoCallResponse, "<coockedTime>", "</coockedTime>");

                State newMode = new StringType(mode);
                State newCoockedTime = DecimalType.valueOf(coockedTime);
                switch (mode) {
                    case "0":
                        newMode = new StringType("OFF");
                        break;
                    case "50":
                        newMode = new StringType("WARM");
                        State warmTime = DecimalType.valueOf(time);
                        updateState(CHANNEL_WARMCOOKTIME, warmTime);
                        break;
                    case "51":
                        newMode = new StringType("LOW");
                        State lowTime = DecimalType.valueOf(time);
                        updateState(CHANNEL_LOWCOOKTIME, lowTime);
                        break;
                    case "52":
                        newMode = new StringType("HIGH");
                        State highTime = DecimalType.valueOf(time);
                        updateState(CHANNEL_HIGHCOOKTIME, highTime);
                        break;
                }
                updateState(CHANNEL_COOKMODE, newMode);
                updateState(CHANNEL_COOKEDTIME, newCoockedTime);
            }
        } catch (RuntimeException e) {
            logger.debug("Failed to get actual state for device '{}': {}", getThing().getUID(), e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, e.getMessage());
        }
        updateStatus(ThingStatus.ONLINE);
    }

    @Override
    public void onStatusChanged(boolean status) {
    }
}
