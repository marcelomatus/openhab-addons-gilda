/**
 * Copyright (c) 2014-2015 openHAB UG (haftungsbeschraenkt) and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zoneminder.handler;

import java.io.IOException;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingTypeUID;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.zoneminder.ZoneMinderConstants;
import org.openhab.binding.zoneminder.internal.ZoneMinderMonitorEventListener;
import org.openhab.binding.zoneminder.internal.config.ZoneMinderBridgeServerConfig;
import org.openhab.binding.zoneminder.internal.request.ZoneMinderMonitorRequest;
import org.openhab.binding.zoneminder.internal.request.ZoneMinderMonitorTriggerRequest;
import org.openhab.binding.zoneminder.internal.request.ZoneMinderRequestType;
import org.openhab.binding.zoneminder.internal.request.ZoneMinderServerBaseRequest;
import org.openhab.binding.zoneminder.internal.request.ZoneMinderServerRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import name.eskildsen.zoneminder.ZoneMinderServerProxy;
import name.eskildsen.zoneminder.api.ZoneMinderData;
import name.eskildsen.zoneminder.api.ZoneMinderDiskUsage;
import name.eskildsen.zoneminder.api.daemon.ZoneMinderHostDaemonStatus;
import name.eskildsen.zoneminder.api.host.ZoneMinderHostLoad;
import name.eskildsen.zoneminder.api.host.ZoneMinderHostVersion;
import name.eskildsen.zoneminder.api.monitor.ZoneMinderMonitor;

/**
 * Handler for a ZoneMinder Server.
 *
 * @author Martin S. Eskildsen
 *
 */
public class ZoneMinderServerBridgeHandler extends ZoneMinderBaseBridgeHandler
        implements ZoneMinderMonitorEventListener {

    public static final int TELNET_TIMEOUT = 5000;

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Sets
            .newHashSet(ZoneMinderConstants.THING_TYPE_BRIDGE_ZONEMINDER_SERVER);

    /**
     * Logger
     */
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /**
     * Bridge configuration from OpenHAB
     */
    ZoneMinderBridgeServerConfig config = null;

    Boolean isInitialized = false;

    Map<String, ZoneMinderData> zmServerData = new HashMap<String, ZoneMinderData>();

    /**
     * ZoneMinder Server connection
     */
    private ZoneMinderServerProxy zoneMinderServerProxy = null;

    private ScheduledFuture<?> taskHighPriorityRefresh = null;
    private ScheduledFuture<?> taskLowPriorityRefresh = null;

    private Runnable refreshHighPriorityDataRunnable = new Runnable() {
        @Override
        public void run() {
            try {
                refreshHighPriorityPriorityData();
            } catch (Exception exception) {
                logger.error("monitorRunnable::run(): Exception: ", exception);
            }
        }
    };

    private Runnable refreshLowPriorityDataRunnable = new Runnable() {

        @Override
        public void run() {
            try {
                if (isConnected() == true) {
                    refreshLowPriorityPriorityData();
                }
            } catch (Exception exception) {
                logger.error("monitorRunnable::run(): Exception: ", exception);
            }
        }
    };

    /**
     * Constructor
     * 7
     *
     * @param bridge
     *            Bridge object representing a ZoneMinder Server
     */
    public ZoneMinderServerBridgeHandler(Bridge bridge) {
        super(bridge, ZoneMinderThingType.ZoneMinderServerBridge);

        logger.info("Starting ZoneMinder Server Bridge Handler (Bridge='{}')", bridge.getBridgeUID());
    }

    /**
     * Initializes the bridge.
     */
    @Override
    public void initialize() {
        logger.debug("About to initialize bridge " + ZoneMinderConstants.BRIDGE_ZONEMINDER_SERVER);
        super.initialize();
        try {
            updateStatus(ThingStatus.INITIALIZING);

            this.config = getBridgeConfig();

            logger.debug("ZoneMinder Server Bridge Handler Initialized");
            logger.debug("   HostName:           {}", config.getHostName());
            logger.debug("   Protocol:           {}", config.getProtocol());
            logger.debug("   Port (HTTP)         {}", config.getHttpPort());
            logger.debug("   Port (Telnet)       {}", config.getTelnetPort());
            logger.debug("   Server Path         {}", config.getServerBasePath());
            logger.debug("   User:               {}", config.getUserName());
            logger.debug("   Refresh interval:   {}", config.getRefreshInterval());
            logger.debug("   Low  prio. refresh: {}", config.getRefreshIntervalLowPriorityTask());

            closeConnection();

            taskHighPriorityRefresh = null;
            taskLowPriorityRefresh = null;

        } catch (Exception ex) {
            logger.error("'ZoneMinderServerBridgeHandler' failed to initialize. Exception='{}'", ex.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR);
        } finally {
            startWatchDogTask();
            isInitialized = true;
        }
    }

    /**
     */
    @Override
    public void dispose() {
        logger.debug("Stop polling of ZoneMinder Server API");
        stopWatchDogTask();
        stopTask(taskHighPriorityRefresh);
        stopTask(taskLowPriorityRefresh);
    }

    @Override
    public String getZoneMinderId() {
        return getThing().getUID().getAsString();
    }

    @Override
    public void channelLinked(ChannelUID channelUID) {
        // can be overridden by subclasses
        logger.debug("Channel '" + channelUID.getAsString() + "' was linked to '" + this.thing.getThingTypeUID() + "'");
    }

    @Override
    public void channelUnlinked(ChannelUID channelUID) {
        // can be overridden by subclasses
        logger.debug(
                "Channel '" + channelUID.getAsString() + "' was unlinked from '" + this.thing.getThingTypeUID() + "'");
    }

    protected ZoneMinderBridgeServerConfig getBridgeConfig() {
        return this.getConfigAs(ZoneMinderBridgeServerConfig.class);
    }

    /**
     * Method for polling the ZoneMinder Server.
     */
    @Override
    protected void onRefreshLowPriorityPriorityData() {
        logger.debug("Refreshing diskusage from ZoneMinder Server Task - '{}'", getThing().getUID());

        if (!isConnected()) {
            logger.error("Not Connected to the ZoneMinder Server!");
            connect();
        }

        /*
         * Fetch data for all monitors attached to this bridge
         */
        if (isConnected()) {
            ZoneMinderServerRequest request = new ZoneMinderServerRequest(
                    ZoneMinderRequestType.SERVER_LOW_PRIORITY_DATA, getZoneMinderId());
            processZoneMinderServerRequest(request);
        }
    }

    @Override
    protected void onRefreshHighPriorityPriorityData() {
        logger.trace("ZoneMinder Server: Refreshing Bridge...");

        // Refresh channels on the bridge
        this.refreshThing();

        List<Thing> things = getThing().getThings();

        for (Thing thing : things) {

            ZoneMinderBaseThingHandler handler = (ZoneMinderBaseThingHandler) thing.getHandler();

            if (handler != null) {
                logger.debug("***Checking '{}' - Status: {}, Refreshed: {}", thing.getUID(), thing.getStatus(),
                        handler.isThingRefreshed());

                handler.refreshThing();

            } else {
                logger.error("refreshBridge(): Thing handler not found!");
            }

        }

    }

    /**
     * Just logging - nothing to do.
     */
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        logger.debug("update " + channelUID.getAsString() + " with " + command.toString());
    }

    @Override
    protected void refreshThing() {

        logger.debug("refreshThing(): Thing='{}'!", getThing().getUID(), this.getThing().getUID());
        List<Thing> things = getThing().getThings();
        List<Channel> channels = getThing().getChannels();

        if (isConnected()) {

            /*
             * Fetch data for all monitors attached to this bridge
             */
            logger.debug("Sending request for refresh of Server channels....");
            ZoneMinderServerBaseRequest request = new ZoneMinderServerRequest(
                    ZoneMinderRequestType.SERVER_HIGH_PRIORITY_DATA, getZoneMinderId());
            processZoneMinderServerRequest(request);

            for (Thing thing : things) {
                try {
                    if (thing.getThingTypeUID().equals(ZoneMinderConstants.THING_TYPE_THING_ZONEMINDER_MONITOR)) {
                        Thing thingMonitor = thing;
                        ZoneMinderBaseThingHandler thingHandler = (ZoneMinderBaseThingHandler) thing.getHandler();

                        logger.debug(String.format("Sending request for refresh of channels for monitor '%s'....",
                                thingHandler.getZoneMinderId()));

                        request = new ZoneMinderMonitorRequest(ZoneMinderRequestType.MONITOR_THING,
                                thingHandler.getZoneMinderId());
                        processZoneMinderServerRequest(request);

                        logger.debug("Updated ZoneMinderApiData for Thing: {}  {}", thing.getThingTypeUID(),
                                thing.getUID());
                    }

                } catch (Exception ex) {
                    logger.error("Method 'refreshThing()' for Bridge {} failed for thing='{}' - Exception='{}'",
                            this.getZoneMinderId(), thing.getUID(), ex.getMessage());
                }
            }

        }

        /*
         * Update all channels
         */
        for (Channel channel : channels) {
            updateChannel(channel.getUID());
        }

        // this.setThingRefreshed(false);
    }

    @Override
    public void notifyZoneMinderApiDataUpdated(ThingTypeUID thingTypeUID, String zoneMinderId,
            List<ZoneMinderData> arrData) {

        if (thingTypeUID.equals(ZoneMinderConstants.THING_TYPE_BRIDGE_ZONEMINDER_SERVER)) {

            // Check data sets individually and update the local copy if they are there
            // ZoneMinderServerData updatedData = (ZoneMinderServerData) data;
            synchronized (zmServerData) {
                for (ZoneMinderData data : arrData) {
                    String dataClassKey = data.getKey();
                    if (zmServerData.containsKey(dataClassKey)) {
                        zmServerData.remove(dataClassKey);
                    }
                    zmServerData.put(dataClassKey, data);
                }
            }
        } else {
            ZoneMinderBaseThingHandler thing = getZoneMinderThingHandlerFromZoneMinderId(thingTypeUID, zoneMinderId);

            // If thing not found, then it is not to this thing that it belongs :-)
            if (thing != null) {

                thing.notifyZoneMinderApiDataUpdated(thingTypeUID, zoneMinderId, arrData);
            }
        }
    }

    @Override
    public void updateAvaliabilityStatus() {
        ThingStatus newStatus = ThingStatus.OFFLINE;
        isAlive = false;

        // Just wait until we are finished initializing
        if (isInitialized == false) {
            return;
        }

        // Check if server Bridge configuration is valid
        if (!isConfigValid()) {

            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Invalid configuration");
            setBridgeConnection(false);
            return;
        }

        // Check if we have a connection to ZoneMinder Server - else try to establish one
        if (zoneMinderServerProxy == null) {
            if (openConnection() == false) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Cannot access ZoneMinder Server. Check provided usercredentials");
                setBridgeConnection(false);
                return;
            }
        }

        if (!zoneMinderServerProxy.isConnected()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Cannot access ZoneMinder Server. Check provided usercredentials");
            setBridgeConnection(false);
            return;
        }

        if (zoneMinderServerProxy.getHostDaemonCheckState() == null) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "ZoneMinder Server Daemon state is unknown.");
            setBridgeConnection(false);
            return;
        }

        ZoneMinderHostDaemonStatus daemonStatus = zoneMinderServerProxy.getHostDaemonCheckState();
        if (!daemonStatus.isDaemonRunning()) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "ZoneMinder Server Daemon isn't running.");
            setBridgeConnection(false);
            return;
        }

        // Set Status to OFFLINE if it is OFFLINE

        // Check if server API can be accessed
        if (!isZoneMinderApiEnabled()) {

            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "ZoneMinder Server API isn't enabled. In ZoneMinder make sure option 'ZM_OPT_USE_API' is enabled");
            setBridgeConnection(false);
            return;
        }

        if (!isZoneMinderExternalTriggerEnabled()) {

            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "ZoneMinder Server External trigger isn't enabled. In ZoneMinder enabled ZM_xxx");
            setBridgeConnection(false);
            return;
        }

        // 4. Check server version
        // 5. Check server API version

        // Check if refresh jobs is running
        if (!isZoneMinderServerDaemonEnabled()) {

            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.BRIDGE_OFFLINE,
                    "ZoneMinder Server cannot be reached. Daemon appears to be stopped.");
            setBridgeConnection(false);
            return;
        }

        isAlive = true;
        if (isAlive == true) {
            newStatus = ThingStatus.ONLINE;
        } else {
            newStatus = ThingStatus.OFFLINE;
        }

        if (thing.getStatus() != newStatus) {
            updateStatus(newStatus);
        }

        /*
         * // Ask all things to update their Availability Status
         * for (Thing thing : getThing().getThings()) {
         * ZoneMinderBaseThingHandler thingHandler = (ZoneMinderBaseThingHandler) thing.getHandler();
         * if (thingHandler instanceof ZoneMinderThingMonitorHandler) {
         * try {
         * thingHandler.updateAvaliabilityStatus();
         * } catch (Exception ex) {
         * logger.debug("Failed to call 'updateAvailabilityStatus()' for '{}'",
         * thingHandler.getThing().getUID());
         * }
         * }
         *
         * }
         */
    }

    protected Boolean isConfigValid() {
        ZoneMinderBridgeServerConfig config = getBridgeConfig();

        if (config == null) {
            return false;
        }

        if (config.getProtocol() == null) {
            return false;
        }

        if (config.getHostName() == null) {
            return false;
        }

        if (config.getHttpPort() == null) {
            return false;
        }

        if (config.getTelnetPort() == null) {
            return false;
        }

        return true;
    }

    protected Boolean isZoneMinderApiEnabled() {

        /*
         * JSON Return upon Error
         * {
         * "success": false,
         * "data": {
         * "name": "API Disabled",
         * "message": "API Disabled",
         * "url": "\/zm\/api\/configs\/view\/ZM_OPT_USE_API.json",
         * "exception": {
         * "class": "UnauthorizedException",
         * "code": 401,
         * "message": "API Disabled",
         * "trace": [
         * "#0 [internal function]: AppController->beforeFilter(Object(CakeEvent))",
         * "#1 \/usr\/share\/zoneminder\/www\/api\/lib\/Cake\/Event\/CakeEventManager.php(243): call_user_func(Array, Object(CakeEvent))"
         * ,
         * "#2 \/usr\/share\/zoneminder\/www\/api\/lib\/Cake\/Controller\/Controller.php(677): CakeEventManager->dispatch(Object(CakeEvent))"
         * ,
         * "#3 \/usr\/share\/zoneminder\/www\/api\/lib\/Cake\/Routing\/Dispatcher.php(189): Controller->startupProcess()"
         * ,
         * "#4 \/usr\/share\/zoneminder\/www\/api\/lib\/Cake\/Routing\/Dispatcher.php(167): Dispatcher->_invoke(Object(ConfigsController), Object(CakeRequest))"
         * ,
         * "#5 \/usr\/share\/zoneminder\/www\/api\/app\/webroot\/index.php(108): Dispatcher->dispatch(Object(CakeRequest), Object(CakeResponse))"
         * ,
         * "#6 {main}"
         * ]
         * },
         * "queryLog": {
         * "default": {
         * "log": [
         * {
         * "query":
         * "SELECT `Config`.`Id`, `Config`.`Name`, `Config`.`Value`, `Config`.`Type`, `Config`.`DefaultValue`, `Config`.`Hint`, `Config`.`Pattern`, `Config`.`Format`, `Config`.`Prompt`, `Config`.`Help`, `Config`.`Category`, `Config`.`Readonly`, `Config`.`Requires` FROM `zm`.`Config` AS `Config`   WHERE `Config`.`Name` = 'ZM_OPT_USE_API'    LIMIT 1"
         * ,
         * "params": [],
         * "affected": 1,
         * "numRows": 1,
         * "took": 0
         * },
         * {
         * "query":
         * "SELECT `Config`.`Id`, `Config`.`Name`, `Config`.`Value`, `Config`.`Type`, `Config`.`DefaultValue`, `Config`.`Hint`, `Config`.`Pattern`, `Config`.`Format`, `Config`.`Prompt`, `Config`.`Help`, `Config`.`Category`, `Config`.`Readonly`, `Config`.`Requires` FROM `zm`.`Config` AS `Config`   WHERE `Config`.`Name` = 'ZM_OPT_USE_API'    LIMIT 1"
         * ,
         * "params": [],
         * "affected": 1,
         * "numRows": 1,
         * "took": 0
         * }
         * ],
         * "count": 2,
         * "time": 0
         * }
         * }
         * }
         * }
         */
        // TODO:: FIXME ConfigData cfg = zoneMinderServerProxy.getConfig(ConfigEnum.ZM_OPT_USE_API);
        return true;
    }

    protected Boolean isZoneMinderExternalTriggerEnabled() {
        return true;
    }

    // 4. Check server version
    // 5. Check server API version

    // Check if refresh jobs is running
    protected Boolean isZoneMinderServerDaemonEnabled() {

        return true;
    }

    @Override
    public void updateChannel(ChannelUID channel) {
        State state = null;
        try {
            switch (channel.getId()) {
                case ZoneMinderConstants.CHANNEL_IS_ALIVE:
                    super.updateChannel(channel);
                    break;

                case ZoneMinderConstants.CHANNEL_SERVER_ZM_VERSION:
                    state = getServerVersionState();
                    break;

                case ZoneMinderConstants.CHANNEL_SERVER_ZM_API_VERSION:
                    state = getServerVersionApiState();
                    break;

                case ZoneMinderConstants.CHANNEL_SERVER_DISKUSAGE:
                    state = getServerDiskUsageState();
                    break;

                case ZoneMinderConstants.CHANNEL_SERVER_CPULOAD:
                    state = getServerCpuLoadState();
                    break;

                default:
                    logger.warn("updateChannel(): Server '{}': No handler defined for channel='{}'", thing.getLabel(),
                            channel.getAsString());
                    // Ask super class to handle
                    super.updateChannel(channel);
                    break;
            }

            if (state != null) {
                updateState(channel.getId(), state);
            }
        } catch (Exception ex) {
            logger.error("Error occurred when 'updateChannel()' was called for thing='{}', channel='{}'",
                    channel.getThingUID(), channel.getId());
        }
    }

    @Override
    Boolean openConnection() {
        boolean connected = false;
        if (isConnected() == false) {
            logger.debug("Connecting Bridge to ZoneMinder Server.");

            try {
                closeConnection();

                zoneMinderServerProxy = new ZoneMinderServerProxy(getBridgeConfig().getProtocol(),
                        getBridgeConfig().getHostName(), getBridgeConfig().getServerBasePath(),
                        getBridgeConfig().getUserName(), getBridgeConfig().getPassword());

                connected = zoneMinderServerProxy.connect();

                logger.debug("openConnection(): Connecting to ZoneMinder Server (Telnet)");

                setBridgeConnection(connected);

            } catch (UnknownHostException unknownException) {
                logger.error("openConnection(): Unknown Host Exception: ", unknownException);
                setBridgeConnection(false);
            } catch (SocketException socketException) {
                logger.error("openConnection(): Socket Exception: ", socketException);
                setBridgeConnection(false);
            } catch (IOException ioException) {
                logger.error("openConnection(): IO Exception: ", ioException);
                setBridgeConnection(false);
            } catch (Exception exception) {
                logger.error("openConnection(): Exception: ", exception);
                setBridgeConnection(false);
            } finally {
                if (isConnected() == false) {
                    closeConnection();
                }
            }

        }
        return isConnected();

    }

    @Override
    synchronized void closeConnection() {
        try {
            if (zoneMinderServerProxy != null) {
                zoneMinderServerProxy.closeConnection();
                zoneMinderServerProxy = null;
            }
            logger.debug("closeConnection(): Closed HTTP Connection!");

            setBridgeConnection(false);

        } catch (Exception exception) {
            logger.error("closeConnection(): Error closing connection - " + exception.getMessage());
        }

    }

    public void activateZoneMinderMonitorTrigger(String monitorId, String text, Integer timeout) {
        ZoneMinderMonitorTriggerRequest request = new ZoneMinderMonitorTriggerRequest(
                ZoneMinderRequestType.MONITOR_TRIGGER_REQUEST, true, monitorId, 255, text, timeout);

        processZoneMinderServerRequest(request);
    }

    public void cancelZoneMinderMonitorTrigger(String monitorId) {
        ZoneMinderMonitorTriggerRequest request = new ZoneMinderMonitorTriggerRequest(
                ZoneMinderRequestType.MONITOR_TRIGGER_REQUEST, false, monitorId, 255, "", 0);

        processZoneMinderServerRequest(request);
    }

    private boolean processZoneMinderServerRequest(ZoneMinderServerBaseRequest request) {
        List<ZoneMinderData> arrData = new ArrayList<ZoneMinderData>();
        ThingTypeUID thingTypeUID = request.getThingTypeUID();
        String zoneMinderId = request.getId();
        Boolean doNotify = false;
        switch (request.getRequestType()) {

            case MONITOR_THING:
                arrData.add(zoneMinderServerProxy.getMonitor(zoneMinderId));
                arrData.add(zoneMinderServerProxy.getMonitorCaptureDaemonStatus(zoneMinderId));
                arrData.add(zoneMinderServerProxy.getMonitorAnalysisDaemonStatus(zoneMinderId));
                arrData.add(zoneMinderServerProxy.getMonitorFrameDaemonStatus(zoneMinderId));
                doNotify = true;
                break;

            case SERVER_HIGH_PRIORITY_DATA:
                arrData.add((zoneMinderServerProxy.getHostVersion()));
                arrData.add((zoneMinderServerProxy.getHostCpuLoad()));
                arrData.add((zoneMinderServerProxy.getHostDaemonCheckState()));
                doNotify = true;
                break;

            case SERVER_LOW_PRIORITY_DATA:
                arrData.add((zoneMinderServerProxy.getHostDiskUsage()));
                doNotify = true;
                break;

            case MONITOR_TRIGGER_REQUEST:
                ZoneMinderMonitorTriggerRequest triggerRequest = (ZoneMinderMonitorTriggerRequest) request;
                try {
                    if (triggerRequest.getActivatedState()) {
                        zoneMinderServerProxy.activateForceAlarm(triggerRequest.getId(), triggerRequest.getPriority(),
                                triggerRequest.getReason(), "", "", triggerRequest.getTimeout());
                    } else {
                        zoneMinderServerProxy.deactivateForceAlarm(triggerRequest.getId());
                    }
                } catch (Exception e) {

                    logger.error(e.getMessage());
                }
                break;
            default:
                logger.warn("Unhandled ZoneMinder Server request occurred (request='{}'", request.getRequestType());
        }

        if (doNotify == true) {
            notifyZoneMinderApiDataUpdated(thingTypeUID, zoneMinderId, arrData);
        }

        return false;

    }

    /*
     * // TODO:: FIXME
     *
     * @Override
     * protected boolean onHandleZoneMinderTelnetRequest(ZoneMinderRequestType requestType,
     * ZoneMinderOutgoingRequest request) {
     * boolean result = false;
     *
     * switch (requestType) {
     * case MONITOR_TRIGGER:
     * logger.debug("[TCP] Writing command '{}' to ZoneMinder Server", request.toCommandString());
     * writeTCP(request.toCommandString());
     * result = true;
     * break;
     *
     * default:
     * result = false;
     * }
     * return result;
     * }
     */

    public ArrayList<ZoneMinderMonitor> getMonitors() {
        return zoneMinderServerProxy.getMonitors();
    }

    protected State getServerVersionState() {

        State state = new StringType("-");
        synchronized (zmServerData) {
            if (zmServerData.containsKey(ZoneMinderHostVersion.class.getSimpleName())) {
                ZoneMinderHostVersion data = (ZoneMinderHostVersion) zmServerData
                        .get(ZoneMinderHostVersion.class.getSimpleName());
                state = new StringType(data.getVersion());
            }
        }

        return state;
    }

    protected State getServerVersionApiState() {

        State state = new StringType("-");
        synchronized (zmServerData) {
            if (zmServerData.containsKey(ZoneMinderHostVersion.class.getSimpleName())) {
                ZoneMinderHostVersion data = (ZoneMinderHostVersion) zmServerData
                        .get(ZoneMinderHostVersion.class.getSimpleName());
                state = new StringType(data.getApiVersion());
            }
        }

        return state;
    }

    protected State getServerCpuLoadState() {

        State state = new StringType("-");

        try {
            synchronized (zmServerData) {
                if (zmServerData.containsKey(ZoneMinderHostLoad.class.getSimpleName())) {
                    ZoneMinderHostLoad data = (ZoneMinderHostLoad) zmServerData
                            .get(ZoneMinderHostLoad.class.getSimpleName());

                    state = new StringType(data.getCpuLoad().toString());
                }
            }

        } catch (Exception ex) {
            logger.debug(ex.getMessage());
        }

        return state;
    }

    protected State getServerDiskUsageState() {

        State state = new StringType("-");

        try {
            synchronized (zmServerData) {
                if (zmServerData.containsKey(ZoneMinderDiskUsage.class.getSimpleName())) {
                    ZoneMinderDiskUsage data = (ZoneMinderDiskUsage) zmServerData
                            .get(ZoneMinderDiskUsage.class.getSimpleName());

                    state = new StringType(data.getDiskUsage());
                }
            }

        } catch (Exception ex) {
            logger.debug(ex.getMessage());
        }

        return state;
    }

    @Override
    public Boolean isOnline() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public Boolean isRunning() {
        // TODO Auto-generated method stub
        return true;
    }

    @Override
    public void onBridgeConnected(ZoneMinderBaseBridgeHandler bridge) {

        if (taskHighPriorityRefresh == null) {
            taskHighPriorityRefresh = startTask(refreshHighPriorityDataRunnable, config.getRefreshInterval(),
                    TimeUnit.SECONDS);
        }

        if (taskLowPriorityRefresh == null) {
            taskLowPriorityRefresh = startTask(refreshLowPriorityDataRunnable,
                    config.getRefreshIntervalLowPriorityTask(), TimeUnit.MINUTES);
        }

    }

    @Override
    public void onBridgeDisconnected(ZoneMinderBaseBridgeHandler bridge) {
        // TODO:: FIXME zoneMinderServerProxy.close();
        /*
         * if (taskHighPriorityRefresh != null) {
         * stopTask(taskHighPriorityRefresh);
         * taskHighPriorityRefresh = null;
         * }
         */
        if (taskLowPriorityRefresh != null) {
            stopTask(taskLowPriorityRefresh);
            taskLowPriorityRefresh = null;
        }
        synchronized (zmServerData) {
            zmServerData.clear();
        }

    }
}
