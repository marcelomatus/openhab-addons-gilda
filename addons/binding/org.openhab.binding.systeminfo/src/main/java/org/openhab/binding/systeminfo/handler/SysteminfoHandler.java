/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.systeminfo.handler;

import static org.openhab.binding.systeminfo.SysteminfoBindingConstants.*;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.systeminfo.internal.model.DeviceNotFoundException;
import org.openhab.binding.systeminfo.internal.model.SysteminfoInterface;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link SysteminfoHandler} is responsible for providing real time information about the system
 * (CPU, Memory, Storage, Display and others).
 *
 * @author Svilen Valkanov - Initial contribution
 * @author Lyubomir Papzov - Separate the creation of the systeminfo object and its initialization
 */

public class SysteminfoHandler extends BaseThingHandler {
    /**
     * Refresh interval for {@link #highPriorityChannels} in seconds.
     */
    private BigDecimal refreshIntervalHighPriority;

    /**
     * Refresh interval for {@link #mediumPriorityChannels} in seconds.
     */
    private BigDecimal refreshIntervalMediumPriority;

    /**
     * Channels with priority configuration parameter set to High. They usually need frequent update of the state like
     * CPU load, or information about the free and used memory.
     * They are updated periodically at {@link #refreshIntervalHighPriority}.
     */
    private Set<ChannelUID> highPriorityChannels = new HashSet<ChannelUID>();

    /**
     * Channels with priority configuration parameter set to Medium. These channels usually need update of the
     * state not so oft like battery capacity, storage used and etc.
     * They are updated periodically at {@link #refreshIntervalMediumPriority}.
     */
    private Set<ChannelUID> mediumPriorityChannels = new HashSet<ChannelUID>();

    /**
     * Channels with priority configuration parameter set to Low. They represent static information or information
     * that is updated rare- e.g. CPU name, storage name and etc.
     * They are updated only at {@link #initialize()}.
     */
    private Set<ChannelUID> lowPriorityChannels = new HashSet<ChannelUID>();

    /**
     * Wait time for the creation of Item-Channel links in seconds. This delay is needed, because the Item-Channel
     * links have to be created before the thing state is updated, otherwise item state will not be updated.
     */
    public static final int WAIT_TIME_CHANNEL_ITEM_LINK_INIT = 1;

    private SysteminfoInterface systeminfo;

    ScheduledFuture<?> highPriorityTasks;
    ScheduledFuture<?> mediumPriorityTasks;

    private Logger logger = LoggerFactory.getLogger(SysteminfoHandler.class);

    public SysteminfoHandler(@NonNull Thing thing, SysteminfoInterface systeminfo) {
        super(thing);
        if (systeminfo != null) {
            this.systeminfo = systeminfo;
        } else {
            throw new IllegalArgumentException("No systeminfo service was provided");
        }
    }

    @Override
    public void initialize() {
        logger.debug("Start initializing!");

        if (instantiateSysteminfoLibrary() && isConfigurationValid() && updateProperties()) {
            groupChannelsByPriority();
            scheduleUpdates();
            logger.debug("Thing is successfully initialized!");
            updateStatus(ThingStatus.ONLINE);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
                    "Thing can not be initialized!");
        }

    }

    private boolean instantiateSysteminfoLibrary() {
        try {
            systeminfo.initializeSysteminfo();
            logger.debug("Systeminfo implementation is instantiated!");
            return true;
        } catch (Exception e) {
            logger.error("Can not instantate Systeminfo object!", e);
            return false;
        }
    }

    private boolean isConfigurationValid() {
        logger.debug("Start reading Thing configuration.");
        try {
            refreshIntervalMediumPriority = (BigDecimal) this.thing.getConfiguration()
                    .get(MEDIUM_PRIORITY_REFRESH_TIME);
            refreshIntervalHighPriority = (BigDecimal) this.thing.getConfiguration().get(HIGH_PRIORITY_REFRESH_TIME);

            if (refreshIntervalHighPriority.intValue() <= 0 || refreshIntervalMediumPriority.intValue() <= 0) {
                throw new IllegalArgumentException("Refresh time must be positive number!");
            }
            logger.debug("Refresh time for medium priority channels set to {} s", refreshIntervalMediumPriority);
            logger.debug("Refresh time for high priority channels set to {} s", refreshIntervalHighPriority);
            return true;
        } catch (IllegalArgumentException e) {
            logger.error("Refresh time value is invalid! Please change the thing configuration!", e);
            return false;
        } catch (ClassCastException e) {
            logger.error("Channel configuration can not be read !");
            return false;
        }
    }

    private boolean updateProperties() {
        Map<String, String> properties = editProperties();
        try {
            properties.put(PROPERTY_CPU_LOGICAL_CORES, systeminfo.getCpuLogicalCores().toString());
            properties.put(PROPERTY_CPU_PHYSICAL_CORES, systeminfo.getCpuPhysicalCores().toString());
            properties.put(PROPERTY_OS_FAMILY, systeminfo.getOsFamily().toString());
            properties.put(PROPERTY_OS_MANUFACTURER, systeminfo.getOsManufacturer().toString());
            properties.put(PROPERTY_OS_VERSION, systeminfo.getOsVersion().toString());
            updateProperties(properties);
            logger.debug("Properties updated!");
            return true;
        } catch (Exception e) {
            logger.debug("Can not get system properties! Please try to restart the binding.", e);
            return false;
        }

    }

    private void groupChannelsByPriority() {
        logger.trace("Grouping channels by priority.");
        List<Channel> channels = this.thing.getChannels();

        for (Channel channel : channels) {
            Configuration properties = channel.getConfiguration();
            String priority = (String) properties.get(PRIOIRITY_PARAM);
            if (priority == null) {
                logger.debug("Channel with id {} will not be updated. The channel has no priority set !",
                        channel.getUID());
                break;
            }
            switch (priority) {
                case "High":
                    highPriorityChannels.add(channel.getUID());
                    break;
                case "Medium":
                    mediumPriorityChannels.add(channel.getUID());
                    break;
                case "Low":
                    lowPriorityChannels.add(channel.getUID());
                    break;
                default:
                    logger.error("Invalid priority configuration parameter. Channel will not be updated !");
            }
        }
    }

    private void changeChannelPriority(ChannelUID channelUID, String priority) {
        switch (priority) {
            case "High":
                mediumPriorityChannels.remove(channelUID);
                lowPriorityChannels.remove(channelUID);
                highPriorityChannels.add(channelUID);
                break;
            case "Medium":
                lowPriorityChannels.remove(channelUID);
                highPriorityChannels.remove(channelUID);
                mediumPriorityChannels.add(channelUID);
                break;
            case "Low":
                highPriorityChannels.remove(channelUID);
                mediumPriorityChannels.remove(channelUID);
                lowPriorityChannels.add(channelUID);
                break;
            default:
                logger.error("Invalid priority configuration parameter. Channel will not be updated !");
        }
    }

    private void scheduleUpdates() {
        logger.debug("Schedule high priority tasks at fixed rate {} s.", refreshIntervalHighPriority);
        highPriorityTasks = scheduler.scheduleWithFixedDelay(() -> {
            publishData(highPriorityChannels);
        }, WAIT_TIME_CHANNEL_ITEM_LINK_INIT, refreshIntervalHighPriority.intValue(), TimeUnit.SECONDS);

        logger.debug("Schedule medium priority tasks at fixed rate {} s.", refreshIntervalMediumPriority);
        mediumPriorityTasks = scheduler.scheduleWithFixedDelay(() -> {
            publishData(mediumPriorityChannels);
        }, WAIT_TIME_CHANNEL_ITEM_LINK_INIT, refreshIntervalMediumPriority.intValue(), TimeUnit.SECONDS);

        logger.debug("Schedule one time update for low priority tasks.");
        scheduler.schedule(() -> {
            publishData(lowPriorityChannels);
        }, WAIT_TIME_CHANNEL_ITEM_LINK_INIT, TimeUnit.SECONDS);

    }

    private void publishData(Set<ChannelUID> channels) {
        if (channels != null) {
            Iterator<ChannelUID> iter = channels.iterator();
            while (iter.hasNext()) {
                ChannelUID channeUID = iter.next();
                if (isLinked(channeUID.getId())) {
                    publishDataForChannel(channeUID);
                }
            }
        }
    }

    private void publishDataForChannel(ChannelUID channelUID) {
        State state = getInfoForChannel(channelUID);
        String channelID = channelUID.getId();
        if (state != null) {
            updateState(channelID, state);
        } else {
            logger.warn("Channel with ID {} can not be updated! No information available for the selected device.",
                    channelID);
        }
    }

    public Set<ChannelUID> getHighPriorityChannels() {
        return highPriorityChannels;
    }

    public Set<ChannelUID> getMediumPriorityChannels() {
        return mediumPriorityChannels;
    }

    public Set<ChannelUID> getLowPriorityChannels() {
        return lowPriorityChannels;
    }

    /**
     * This method gets the information for specific channel through the {@link SysteminfoInterface}. It uses the
     * channel ID to call the correct method from the {@link SysteminfoInterface} with deviceIndex parameter (in case of
     * multiple devices, for reference see {@link #getDeviceIndex(String)}})
     *
     * @param channelUID the UID of the channel
     * @return State object or null, if there is no information for the device with this index
     */
    private State getInfoForChannel(ChannelUID channelUID) {
        State state = null;
        String channelID = channelUID.getId();
        String channelIDWithoutGroup = channelUID.getIdWithoutGroup();
        String channelGroupID = channelUID.getGroupId();

        int deviceIndex = getDeviceIndex(channelUID);

        // The channelGroup may contain deviceIndex. It must be deleted from the channelID, because otherwise the
        // switch will not find the correct method below.
        // All digits are deleted from the ID
        channelID = channelGroupID.replaceAll("\\d+", "") + "#" + channelIDWithoutGroup;

        try {
            switch (channelID) {
                case CHANNEL_DISPLAY_INFORMATION:
                    state = systeminfo.getDisplayInformation(deviceIndex);
                    break;
                case CHANNEL_BATTERY_NAME:
                    state = systeminfo.getBatteryName(deviceIndex);
                    break;
                case CHANNEL_BATTERY_REMAINING_CAPACITY:
                    state = systeminfo.getBatteryRemainingCapacity(deviceIndex);
                    break;
                case CHANNEL_BATTERY_REMAINING_TIME:
                    state = systeminfo.getBatteryRemainingTime(deviceIndex);
                    break;
                case CHANNEL_SENSORS_CPU_TEMPERATURE:
                    state = systeminfo.getSensorsCpuTemperature();
                    break;
                case CHANNEL_SENOSRS_CPU_VOLTAGE:
                    state = systeminfo.getSensorsCpuVoltage();
                    break;
                case CHANNEL_SENSORS_FAN_SPEED:
                    state = systeminfo.getSensorsFanSpeed(deviceIndex);
                    break;
                case CHANNEL_CPU_LOAD:
                    state = systeminfo.getCpuLoad();
                    break;
                case CHANNEL_CPU_LOAD_1:
                    state = systeminfo.getCpuLoad1();
                    break;
                case CHANNEL_CPU_LOAD_5:
                    state = systeminfo.getCpuLoad5();
                    break;
                case CHANNEL_CPU_LOAD_15:
                    state = systeminfo.getCpuLoad15();
                    break;
                case CHANNEL_CPU_UPTIME:
                    state = systeminfo.getCpuUptime();
                    break;
                case CHANNEL_CPU_THREADS:
                    state = systeminfo.getCpuThreads();
                    break;
                case CHANNEL_CPU_DESCRIPTION:
                    state = systeminfo.getCpuDescription();
                    break;
                case CHANNEL_CPU_NAME:
                    state = systeminfo.getCpuName();
                    break;
                case CHANNEL_MEMORY_AVAILABLE:
                    state = systeminfo.getMemoryAvailable();
                    break;
                case CHANNEL_MEMORY_USED:
                    state = systeminfo.getMemoryUsed();
                    break;
                case CHANNEL_MEMORY_TOTAL:
                    state = systeminfo.getMemoryTotal();
                    break;
                case CHANNEL_MEMORY_AVAILABLE_PERCENT:
                    state = systeminfo.getMemoryAvailablePercent();
                    break;
                case CHANNEL_MEMORY_USED_PERCENT:
                    state = systeminfo.getMemoryUsedPercent();
                    break;
                case CHANNEL_SWAP_AVAILABLE:
                    state = systeminfo.getSwapAvailable();
                    break;
                case CHANNEL_SWAP_USED:
                    state = systeminfo.getSwapUsed();
                    break;
                case CHANNEL_SWAP_TOTAL:
                    state = systeminfo.getSwapTotal();
                    break;
                case CHANNEL_SWAP_AVAILABLE_PERCENT:
                    state = systeminfo.getSwapAvailablePercent();
                    break;
                case CHANNEL_SWAP_USED_PERCENT:
                    state = systeminfo.getSwapUsedPercent();
                    break;
                case CHANNEL_DRIVE_MODEL:
                    state = systeminfo.getDriveModel(deviceIndex);
                    break;
                case CHANNEL_DRIVE_SERIAL:
                    state = systeminfo.getDriveSerialNumber(deviceIndex);
                    break;
                case CHANNEL_DRIVE_NAME:
                    state = systeminfo.getDriveName(deviceIndex);
                    break;
                case CHANNEL_STORAGE_NAME:
                    state = systeminfo.getStorageName(deviceIndex);
                    break;
                case CHANNEL_STORAGE_DESCRIPTION:
                    state = systeminfo.getStorageDescription(deviceIndex);
                    break;
                case CHANNEL_STORAGE_AVAILABLE:
                    state = systeminfo.getStorageAvailable(deviceIndex);
                    break;
                case CHANNEL_STORAGE_USED:
                    state = systeminfo.getStorageUsed(deviceIndex);
                    break;
                case CHANNEL_STORAGE_TOTAL:
                    state = systeminfo.getStorageTotal(deviceIndex);
                    break;
                case CHANNEL_STORAGE_TYPE:
                    state = systeminfo.getStorageType(deviceIndex);
                    break;
                case CHANNEL_STORAGE_AVAILABLE_PERCENT:
                    state = systeminfo.getStorageAvailablePercent(deviceIndex);
                    break;
                case CHANNEL_STORAGE_USED_PERCENT:
                    state = systeminfo.getStorageUsedPercent(deviceIndex);
                    break;
                case CHANNEL_NETWORK_IP:
                    state = systeminfo.getNetworkIp(deviceIndex);
                    break;
                case CHANNEL_NETWORK_ADAPTER_NAME:
                    state = systeminfo.getNetworkDisplayName(deviceIndex);
                    break;
                case CHANNEL_NETWORK_NAME:
                    state = systeminfo.getNetworkName(deviceIndex);
                    break;
                case CHANNEL_NETWORK_MAC:
                    state = systeminfo.getNetworkMac(deviceIndex);
                    break;
                case CHANNEL_NETWORK_DATA_SENT:
                    state = systeminfo.getNetworkDataSent(deviceIndex);
                    break;
                case CHANNEL_NETWORK_DATA_RECEIVED:
                    state = systeminfo.getNetworkDataReceived(deviceIndex);
                    break;
                case CHANNEL_NETWORK_PACKETS_RECEIVED:
                    state = systeminfo.getNetworkPacketsReceived(deviceIndex);
                    break;
                case CHANNEL_NETWORK_PACKETS_SENT:
                    state = systeminfo.getNetworkPacketsSent(deviceIndex);
                    break;
                case CHANNEL_PROCESS_LOAD:
                    state = systeminfo.getProcessCpuUsage(deviceIndex);
                    break;
                case CHANNEL_PROCESS_MEMORY:
                    state = systeminfo.getProcessMemoryUsage(deviceIndex);
                    break;
                case CHANNEL_PROCESS_NAME:
                    state = systeminfo.getProcessName(deviceIndex);
                    break;
                case CHANNEL_PROCESS_PATH:
                    state = systeminfo.getProcessPath(deviceIndex);
                    break;
                case CHANNEL_PROCESS_THREADS:
                    state = systeminfo.getProcessThreads(deviceIndex);
                    break;
                default:
                    logger.error("Channel with unknown ID: {} !", channelID);
            }
        } catch (DeviceNotFoundException e) {
            logger.error("No information for channel {} with device intex {} :", channelID, deviceIndex, e);
        } catch (Exception e) {
            logger.error("Unexpected error occurred while getting system information!", e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Can not get system info as result of unexpected error. Please try to restart the binding (remove and re-add the thing)!");
        }
        return state != null ? state : UnDefType.UNDEF;
    }

    /**
     * The device index is an optional part of the channelID - the last characters of the groupID. It is used to
     * identify unique device, when more than one devices are available (e.g. local disks with names C:\, D:\, E"\ - the
     * first will have deviceIndex=0, the second deviceIndex=1 ant etc).
     * When no device index is specified, default value of 0 (first device in the list) is returned.
     *
     * @param channelID the ID of the channel
     * @return natural number (number >=0)
     */
    private int getDeviceIndex(ChannelUID channelUID) {
        int deviceIndex = 0;
        if (channelUID.getGroupId().contains(CHANNEL_GROUP_PROCESS)) {
            // Only in this case the deviceIndex is part of the channel configuration - PID (Process Identifier)
            int pid = getPID(channelUID);
            deviceIndex = pid;
            logger.debug("Channel with UID {} tracks process with PID: {}", channelUID.getAsString(), pid);
        } else {
            String channelGroupID = channelUID.getGroupId();
            char lastChar = channelGroupID.charAt(channelGroupID.length() - 1);
            if (Character.isDigit(lastChar)) {
                // All non-digits are deleted from the ID
                String deviceIndexPart = channelGroupID.replaceAll("\\D+", "");
                deviceIndex = Integer.parseInt(deviceIndexPart);
            }
        }
        return deviceIndex;
    }

    /**
     * This method gets the process identifier (PID) for specific process
     *
     * @param channelUID channel unique identifier
     * @return natural number
     */
    private int getPID(ChannelUID channelUID) {
        int pid = 0;
        try {
            Configuration channelProperties = this.thing.getChannel(channelUID.getId()).getConfiguration();
            BigDecimal pidValue = (BigDecimal) channelProperties.get(PID_PARAM);
            if (pidValue.intValue() < 0) {
                throw new IllegalArgumentException("Invalid value for Process Identifier.");
            } else {
                pid = pidValue.intValue();
            }

        } catch (ClassCastException e) {
            logger.debug("Channel configuraiton can not be read ! Fall back to default value.", e);
        } catch (IllegalArgumentException e) {
            logger.debug("PID (Process Identifier) must be positive number. Fall back to default value. ", e);
        }
        return pid;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (thing.getStatus().equals(ThingStatus.ONLINE)) {
            if (command instanceof RefreshType) {
                logger.debug("Refresh command received for channel {}!", channelUID);
                publishDataForChannel(channelUID);
            } else {
                logger.debug("Unsupported command {}! Supported commands: REFRESH", command);
            }
        } else {
            logger.debug("Cannot handle command. Thing is not ONLINE.");
        }
    }

    private boolean isConfigurationKeyChanged(Configuration currentConfig, Configuration newConfig, String key) {
        if (currentConfig != null && newConfig != null) {
            Object currentValue = currentConfig.get(key);
            Object newValue = newConfig.get(key);

            if (currentValue == null) {
                return (newValue != null);
            }

            return !currentValue.equals(newValue);
        }
        return true;
    }

    @Override
    public void thingUpdated(Thing thing) {
        logger.trace("About to update thing.");
        boolean isChannelConfigChanged = false;
        List<Channel> channels = thing.getChannels();

        for (Channel channel : channels) {
            ChannelUID channelUID = channel.getUID();
            Configuration newChannelConfig = channel.getConfiguration();
            Channel oldChannel = this.thing.getChannel(channelUID.getId());

            if (oldChannel == null) {
                logger.warn("Channel with UID : {} can not be updated, as it can not be found !",
                        channelUID.getAsString());
                continue;
            }
            Configuration currentChannelConfig = oldChannel.getConfiguration();

            if (isConfigurationKeyChanged(currentChannelConfig, newChannelConfig, PRIOIRITY_PARAM)) {
                isChannelConfigChanged = true;

                handleChannelConfigurationChange(oldChannel, newChannelConfig, PRIOIRITY_PARAM);

                String newPriority = (String) newChannelConfig.get(PRIOIRITY_PARAM);
                changeChannelPriority(channelUID, newPriority);
            }

            if (isConfigurationKeyChanged(currentChannelConfig, newChannelConfig, PID_PARAM)) {
                isChannelConfigChanged = true;
                handleChannelConfigurationChange(oldChannel, newChannelConfig, PID_PARAM);
            }
        }

        if (!(isInitialized() && isChannelConfigChanged)) {
            super.thingUpdated(thing);
        }
    }

    private void handleChannelConfigurationChange(Channel channel, Configuration newConfig, String parameter) {
        Configuration configuration = channel.getConfiguration();
        Object oldValue = configuration.get(parameter);

        configuration.put(parameter, newConfig.get(parameter));

        Object newValue = newConfig.get(parameter);
        logger.debug("Channel with UID : {} has changed its {} from {} to {}", channel.getUID(), parameter, oldValue,
                newValue);
        publishDataForChannel(channel.getUID());
    }

    private void stopScheduledUpdates() {
        if (highPriorityTasks != null) {
            logger.debug("High prioriy tasks will not be run anymore !");
            highPriorityTasks.cancel(true);
        }
        if (mediumPriorityTasks != null) {
            logger.debug("Medium prioriy tasks will not be run anymore !");
            mediumPriorityTasks.cancel(true);
        }
    }

    @Override
    public void dispose() {
        stopScheduledUpdates();
    }

}
