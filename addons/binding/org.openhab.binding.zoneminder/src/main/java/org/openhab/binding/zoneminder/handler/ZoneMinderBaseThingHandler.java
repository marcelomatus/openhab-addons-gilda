/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zoneminder.handler;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.util.EventObject;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.zoneminder.ZoneMinderConstants;
import org.openhab.binding.zoneminder.internal.DataRefreshPriorityEnum;
import org.openhab.binding.zoneminder.internal.config.ZoneMinderThingConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import name.eskildsen.zoneminder.IZoneMinderConnectionInfo;
import name.eskildsen.zoneminder.IZoneMinderSession;
import name.eskildsen.zoneminder.ZoneMinderFactory;
import name.eskildsen.zoneminder.exception.ZoneMinderUrlNotFoundException;

/**
 * The {@link ZoneMinderBaseThingHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Martin S. Eskildsen - Initial contribution
 */
public abstract class ZoneMinderBaseThingHandler extends BaseThingHandler implements ZoneMinderHandler {

    /** Logger for the Thing. */
    private Logger logger = LoggerFactory.getLogger(ZoneMinderBaseThingHandler.class);

    /** Bridge Handler for the Thing. */
    public ZoneMinderServerBridgeHandler zoneMinderBridgeHandler = null;

    /** This refresh status. */
    private boolean thingRefreshed = false;

    /** Unique Id of the thing in zoneminder. */
    private String zoneMinderId;

    /** ZoneMidner ConnectionInfo */
    private IZoneMinderConnectionInfo zoneMinderConnection = null;

    private Lock lockSession = new ReentrantLock();
    private IZoneMinderSession zoneMinderSession = null;

    /** Configuration from OpenHAB */
    protected ZoneMinderThingConfig configuration;

    private DataRefreshPriorityEnum _refreshPriority = DataRefreshPriorityEnum.SCHEDULED;

    protected boolean isOnline() {

        if (zoneMinderSession == null) {
            return false;
        }

        if (!zoneMinderSession.isConnected()) {
            return false;
        }

        return true;
    }

    public DataRefreshPriorityEnum getRefreshPriority() {
        return _refreshPriority;
    }

    public ZoneMinderBaseThingHandler(Thing thing) {
        super(thing);
    }

    /**
     * Initializes the monitor.
     *
     * @author Martin S. Eskildsen
     *
     */
    @Override
    public void initialize() {

        super.initialize();
        try {

        } catch (Exception ex) {
            logger.error("[MONITOR-{}]: 'ZoneMinderServerBridgeHandler' failed to initialize. Exception='{}'",
                    getZoneMinderId(), ex.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR);
        } finally {
        }
    }

    protected boolean isConnected() {
        if (zoneMinderSession == null) {
            return false;
        }
        return zoneMinderSession.isConnected();
    }

    protected IZoneMinderSession aquireSession() {
        lockSession.lock();
        return zoneMinderSession;
    }

    protected void releaseSession() {
        lockSession.unlock();
    }

    /**
     * Method to start a priority data refresh task.
     */

    protected boolean startPriorityRefresh() {

        logger.info("[MONITOR-{}]: Starting High Priority Refresh", getZoneMinderId());
        _refreshPriority = DataRefreshPriorityEnum.HIGH_PRIORITY;
        return true;
    }

    /**
     * Method to stop the data Refresh task.
     */
    protected void stopPriorityRefresh() {
        logger.info("[MONITOR-{}]: Stopping Priority Refresh for Monitor", getZoneMinderId());
        _refreshPriority = DataRefreshPriorityEnum.SCHEDULED;
    }

    @Override
    public void dispose() {

    }

    /**
     * Helper method for getting ChannelUID from ChannelId.
     *
     */
    public ChannelUID getChannelUIDFromChannelId(String id) {
        Channel ch = thing.getChannel(id);
        return ch.getUID();
    }

    protected abstract void onFetchData();

    /**
     * Method to Refresh Thing Handler.
     */
    public synchronized final void refreshThing(IZoneMinderSession session, DataRefreshPriorityEnum refreshPriority) {

        if ((refreshPriority != getRefreshPriority()) && (!isConnected())) {
            return;
        }

        if (refreshPriority == DataRefreshPriorityEnum.HIGH_PRIORITY) {
            logger.debug("[MONITOR-{}]: Performing HIGH PRIORITY refresh", getZoneMinderId());
        } else {
            logger.debug("[MONITOR-{}]: Performing refresh", getZoneMinderId());
        }

        if (getZoneMinderBridgeHandler() != null) {
            if (isConnected()) {

                logger.debug("[MONITOR-{}]: refreshThing(): Bridge '{}' Found for Thing '{}'!", getZoneMinderId(),
                        getThing().getUID(), this.getThing().getUID());

                onFetchData();
            }
        }

        Thing thing = getThing();
        List<Channel> channels = thing.getChannels();
        logger.debug("[MONITOR-{}]: refreshThing(): Refreshing Thing - {}", getZoneMinderId(), thing.getUID());

        for (Channel channel : channels) {
            updateChannel(channel.getUID());
        }

        this.setThingRefreshed(true);
        logger.debug("MONITOR-{}: refreshThing(): Thing Refreshed - {}", getZoneMinderId(), thing.getUID());

    }

    /**
     * Get the Bridge Handler for ZoneMinder.
     *
     * @return zoneMinderBridgeHandler
     */
    public synchronized ZoneMinderServerBridgeHandler getZoneMinderBridgeHandler() {

        if (this.zoneMinderBridgeHandler == null) {

            Bridge bridge = getBridge();

            if (bridge == null) {
                logger.debug("[MONITOR-{}]: getZoneMinderBridgeHandler(): Unable to get bridge!", getZoneMinderId());
                return null;
            }

            logger.debug("[MONITOR-{}]: getZoneMinderBridgeHandler(): Bridge for '{}' - '{}'", getZoneMinderId(),
                    getThing().getUID(), bridge.getUID());
            ThingHandler handler = null;
            try {
                handler = bridge.getHandler();
            } catch (Exception ex) {
                logger.debug(String.format("[MONITOR-{}]: Exception in 'getZoneMinderBridgeHandler()': {}",
                        getZoneMinderId(), ex.getMessage()));
            }

            if (handler instanceof ZoneMinderServerBridgeHandler) {
                this.zoneMinderBridgeHandler = (ZoneMinderServerBridgeHandler) handler;
            } else {
                logger.debug("[MONITOR-{}]: getZoneMinderBridgeHandler(): Unable to get bridge handler!",
                        getZoneMinderId());
            }
        }

        return this.zoneMinderBridgeHandler;
    }

    /**
     * Method to Update a Channel
     *
     * @param channel
     */
    @Override
    public void updateChannel(ChannelUID channel) {
        OnOffType onOffType;

        switch (channel.getId()) {
            case ZoneMinderConstants.CHANNEL_ONLINE:
                updateState(channel, getChannelBoolAsOnOffState(isOnline()));
                break;
            default:
                logger.error(
                        "[MONITOR-{}]: updateChannel() in base class, called for an unknown channel '{}', this channel must be handled in super class.",
                        getZoneMinderId(), channel.getId());
        }
    }

    /**
     * Method to Update Device Properties.
     *
     * @param channelUID
     * @param state
     * @param description
     */
    public abstract void updateProperties(ChannelUID channelUID, int state, String description);

    /**
     * Receives ZoneMinder Events from the bridge.
     *
     * @param event.
     * @param thing
     */
    public abstract void ZoneMinderEventReceived(EventObject event, Thing thing);

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void onBridgeConnected(ZoneMinderServerBridgeHandler bridge, IZoneMinderConnectionInfo connection)
            throws IllegalArgumentException, GeneralSecurityException, IOException, ZoneMinderUrlNotFoundException {
        lockSession.lock();
        try {
            zoneMinderSession = ZoneMinderFactory.CreateSession(connection);

        } finally {
            lockSession.unlock();
        }
    }

    @Override
    public void onBridgeDisconnected(ZoneMinderServerBridgeHandler bridge) {

        if (bridge.getThing().getUID().equals(getThing().getBridgeUID())) {

            this.setThingRefreshed(false);
        }

        lockSession.lock();
        try {
            zoneMinderSession = null;

        } finally {
            lockSession.unlock();
        }

    }

    /**
     * Get Channel by ChannelUID.
     *
     * @param {ChannelUID} channelUID Identifier of Channel
     */
    public Channel getChannel(ChannelUID channelUID) {
        Channel channel = null;

        List<Channel> channels = getThing().getChannels();

        for (Channel ch : channels) {
            if (channelUID == ch.getUID()) {
                channel = ch;
                break;
            }
        }

        return channel;
    }

    /**
     * Get Thing Handler refresh status.
     *
     * @return thingRefresh
     */
    public boolean isThingRefreshed() {
        return thingRefreshed;
    }

    /**
     * Set Thing Handler refresh status.
     *
     * @param {boolean} refreshed Sets status refreshed of thing
     */
    public void setThingRefreshed(boolean refreshed) {
        this.thingRefreshed = refreshed;
    }

    protected abstract String getZoneMinderThingType();

    private Object getConfigValue(String configKey) {
        return getThing().getConfiguration().getProperties().get(configKey);
    }

    /*
     * Helper to get a value from configuration as a String
     *
     * @author Martin S. Eskildsen
     *
     */
    protected String getConfigValueAsString(String configKey) {
        return (String) getConfigValue(configKey);
    }

    /*
     * Helper to get a value from configuration as a Integer
     *
     * @author Martin S. Eskildsen
     *
     */
    protected Integer getConfigValueAsInteger(String configKey) {
        return (Integer) getConfigValue(configKey);
    }

    protected BigDecimal getConfigValueAsBigDecimal(String configKey) {
        return (BigDecimal) getConfigValue(configKey);
    }

    protected State getChannelStringAsStringState(String channelValue) {
        State state = UnDefType.UNDEF;

        try {
            if (isConnected()) {
                state = new StringType(channelValue);
            }

        } catch (Exception ex) {
            logger.error(ex.getMessage());
        }

        return state;

    }

    protected State getChannelBoolAsOnOffState(boolean value) {
        State state = UnDefType.UNDEF;

        try {
            if (isConnected()) {
                state = value ? OnOffType.ON : OnOffType.OFF;
            }

        } catch (Exception ex) {
            logger.error("[MONITOR-{}]: Exception occurred in 'getChannelBoolAsOnOffState()' (Exception='{}')",
                    getZoneMinderId(), ex.getMessage());
        }

        return state;
    }

}
