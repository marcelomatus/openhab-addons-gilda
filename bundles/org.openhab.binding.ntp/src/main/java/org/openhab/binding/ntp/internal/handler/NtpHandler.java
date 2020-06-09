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
package org.openhab.binding.ntp.internal.handler;

import static org.openhab.binding.ntp.internal.NtpBindingConstants.*;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.DateTimeException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.apache.commons.net.ntp.NTPUDPClient;
import org.apache.commons.net.ntp.TimeInfo;
import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.i18n.TimeZoneProvider;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.ntp.internal.config.NtpStringChannelConfiguration;
import org.openhab.binding.ntp.internal.config.NtpThingConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * The NTP Refresh Service polls the configured timeserver with a configurable
 * interval and posts a new event of type ({@link DateTimeType}.
 *
 * The {@link NtpHandler} is responsible for handling commands, which are sent
 * to one of the channels.
 *
 * @author Marcel Verpaalen - Initial contribution OH2 ntp binding
 * @author Thomas.Eichstaedt-Engelen - OH1 ntp binding (getTime routine)
 * @author Markus Rathgeb - Add locale provider
 * @author Erdoan Hadzhiyusein - Adapted the class to work with the new DateTimeType
 * @author Laurent Garnier - null annotations, TimeZoneProvider, configuration settings cleanup
 */
@NonNullByDefault
public class NtpHandler extends BaseThingHandler {

    /** timeout for requests to the NTP server */
    private static final int NTP_TIMEOUT = 30000;

    public static final String DATE_PATTERN_WITH_TZ = "yyyy-MM-dd HH:mm:ss z";
    private static final DateTimeFormatter DATE_FORMATTER_WITH_TZ = DateTimeFormatter.ofPattern(DATE_PATTERN_WITH_TZ);

    private final Logger logger = LoggerFactory.getLogger(NtpHandler.class);

    private final TimeZoneProvider timeZoneProvider;

    /** for publish purposes */
    private DateTimeFormatter dateTimeFormat = DateTimeFormatter.ofPattern(DATE_PATTERN_WITH_TZ);

    private NtpThingConfiguration configuration = new NtpThingConfiguration();

    private @Nullable ScheduledFuture<?> refreshJob;

    private ZoneId timeZoneId;

    /** NTP refresh counter */
    private int refreshNtpCount = 0;
    /** NTP system time delta */
    private long timeOffset;

    public NtpHandler(final Thing thing, final TimeZoneProvider timeZoneProvider) {
        super(thing);
        this.timeZoneProvider = timeZoneProvider;
        this.timeZoneId = timeZoneProvider.getTimeZone();
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command == RefreshType.REFRESH) {
            logger.debug("Refreshing channel '{}' for '{}'.", channelUID.getId(), getThing().getUID());
            refreshTimeDate();
        }
    }

    @Override
    public void initialize() {
        logger.debug("Initializing NTP handler for '{}'.", getThing().getUID());

        configuration = getConfigAs(NtpThingConfiguration.class);

        refreshNtpCount = 0;

        if (configuration.timeZone != null) {
            logger.debug("{} with timezone '{}' set in configuration setting '{}'", getThing().getUID(),
                    configuration.timeZone, PROPERTY_TIMEZONE);
            try {
                timeZoneId = ZoneId.of(configuration.timeZone);
            } catch (DateTimeException e) {
                timeZoneId = timeZoneProvider.getTimeZone();
                logger.debug("{} using default timezone '{}', because configuration setting '{}' is invalid: {}",
                        getThing().getUID(), timeZoneId, PROPERTY_TIMEZONE, e.getMessage());
            }
        } else {
            timeZoneId = timeZoneProvider.getTimeZone();
            logger.debug("{} using default timezone '{}', because configuration setting '{}' is null.",
                    getThing().getUID(), timeZoneId, PROPERTY_TIMEZONE);
        }

        Channel stringChannel = getThing().getChannel(CHANNEL_STRING);
        if (stringChannel != null) {
            String dateTimeFormatString = stringChannel.getConfiguration()
                    .as(NtpStringChannelConfiguration.class).DateTimeFormat;
            if (!dateTimeFormatString.isEmpty()) {
                logger.debug("Date format set in config for channel '{}': {}", CHANNEL_STRING, dateTimeFormatString);
                try {
                    dateTimeFormat = DateTimeFormatter.ofPattern(dateTimeFormatString);
                } catch (IllegalArgumentException ex) {
                    logger.debug("Invalid date format set in config for channel '{}'. Using default format. ({})",
                            CHANNEL_STRING, ex.getMessage());
                    dateTimeFormat = DateTimeFormatter.ofPattern(DATE_PATTERN_WITH_TZ);
                }
            } else {
                logger.debug("No date format set in config for channel '{}'. Using default format.", CHANNEL_STRING);
                dateTimeFormat = DateTimeFormatter.ofPattern(DATE_PATTERN_WITH_TZ);
            }
        } else {
            logger.debug("Missing channel: '{}'", CHANNEL_STRING);
        }
        dateTimeFormat.withZone(timeZoneId);

        logger.debug(
                "Initialized NTP handler '{}' with configuration: host '{}', port {}, refresh interval {}, refresh frequency {}, timezone {}.",
                getThing().getUID(), configuration.hostname, configuration.serverPort, configuration.refreshInterval,
                configuration.refreshNtp, timeZoneId);

        refreshJob = scheduler.scheduleWithFixedDelay(() -> {
            try {
                refreshTimeDate();
            } catch (Exception e) {
                logger.debug("Exception occurred during refresh: {}", e.getMessage(), e);
            }
        }, 0, configuration.refreshInterval, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        logger.debug("Disposing NTP handler for '{}'.", getThing().getUID());
        ScheduledFuture<?> job = refreshJob;
        if (job != null) {
            job.cancel(true);
        }
        refreshJob = null;
        super.dispose();
    }

    private synchronized void refreshTimeDate() {
        long networkTimeInMillis;
        if (refreshNtpCount <= 0) {
            networkTimeInMillis = getTime(configuration.hostname, configuration.serverPort);
            timeOffset = networkTimeInMillis - System.currentTimeMillis();
            logger.debug("{} delta system time: {}", getThing().getUID(), timeOffset);
            refreshNtpCount = configuration.refreshNtp;
        } else {
            networkTimeInMillis = System.currentTimeMillis() + timeOffset;
            refreshNtpCount--;
        }

        ZonedDateTime zoned = ZonedDateTime.ofInstant(Instant.ofEpochMilli(networkTimeInMillis), timeZoneId);
        updateState(CHANNEL_DATE_TIME, new DateTimeType(zoned));
        updateState(CHANNEL_STRING, new StringType(dateTimeFormat.format(zoned)));
    }

    /**
     * Queries the given timeserver <code>hostname</code> and returns the time
     * in milliseconds.
     *
     * @param hostname the timeserver hostname to query
     * @param port the timeserver port to query
     * @return the time in milliseconds or the current time of the system if an
     *         error occurs.
     */
    private long getTime(String hostname, int port) {
        try {
            NTPUDPClient timeClient = new NTPUDPClient();
            timeClient.setDefaultTimeout(NTP_TIMEOUT);
            InetAddress inetAddress = InetAddress.getByName(hostname);
            TimeInfo timeInfo = timeClient.getTime(inetAddress, port);
            timeInfo.computeDetails();

            long serverMillis = timeInfo.getReturnTime() + timeInfo.getOffset();
            ZonedDateTime zoned = ZonedDateTime.ofInstant(Instant.ofEpochMilli(serverMillis), timeZoneId);
            logger.debug("{} Got time update from host '{}': {}.", getThing().getUID(), hostname,
                    zoned.format(DATE_FORMATTER_WITH_TZ));
            updateStatus(ThingStatus.ONLINE);
            return serverMillis;
        } catch (UnknownHostException uhe) {
            logger.debug(
                    "{} The given hostname '{}' of the timeserver is unknown -> returning current sytem time instead. ({})",
                    getThing().getUID(), hostname, uhe.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "@text/offline.comm-error-unknown-host [\"" + hostname + "\"]");
        } catch (IOException ioe) {
            logger.debug(
                    "{} Couldn't establish network connection to host '{}' -> returning current sytem time instead. ({})",
                    getThing().getUID(), hostname, ioe.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "@text/offline.comm-error-connection [\"" + hostname + "\"]");
        }

        return System.currentTimeMillis();
    }
}
