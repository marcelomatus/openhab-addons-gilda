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
package org.openhab.binding.fmiweather.internal;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Optional;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.library.types.DateTimeType;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.fmiweather.internal.client.Client;
import org.openhab.binding.fmiweather.internal.client.Data;
import org.openhab.binding.fmiweather.internal.client.FMIResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link AbstractWeatherHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Sami Salonen - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractWeatherHandler extends BaseThingHandler {

    private static final ZoneId UTC = ZoneId.of("UTC");
    protected static final String PROP_LONGITUDE = "longitude";
    protected static final String PROP_LATITUDE = "latitude";
    protected static final String PROP_NAME = "name";
    protected static final String PROP_REGION = "region";
    private static final long REFRESH_THROTTLE_MILLIS = 10_000;

    protected static final int TIMEOUT_MILLIS = 10_000;
    private final Logger logger = LoggerFactory.getLogger(AbstractWeatherHandler.class);

    protected volatile @NonNullByDefault({}) Client client;
    protected final AtomicReference<@Nullable ScheduledFuture<?>> futureRef = new AtomicReference<>();
    protected volatile @Nullable FMIResponse response;
    protected volatile int pollIntervalSeconds = 120; // reset by subclasses

    private volatile long lastRefreshMillis = 0;
    private final AtomicReference<@Nullable ScheduledFuture<?>> updateChannelsFutureRef = new AtomicReference<>();

    public AbstractWeatherHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (RefreshType.REFRESH == command) {
            ScheduledFuture<?> prevFuture = updateChannelsFutureRef.get();
            ScheduledFuture<?> newFuture = updateChannelsFutureRef
                    .updateAndGet(fut -> fut == null || fut.isDone() ? submitUpdateChannelsThrottled() : fut);
            assert newFuture != null; // invariant
            long delayRemainingMillis = newFuture.getDelay(TimeUnit.MILLISECONDS);
            if (delayRemainingMillis <= 0) {
                logger.trace("REFRESH received. Channels are updated");
            } else {
                logger.trace("REFRESH received. Delaying by {} ms to avoid throttle excessive REFRESH",
                        delayRemainingMillis);
            }
            if (prevFuture == newFuture) {
                logger.trace("REFRESH received. Previous refresh ongoing, will wait for it to complete in {} ms",
                        lastRefreshMillis + REFRESH_THROTTLE_MILLIS - System.currentTimeMillis());
            }
        }
    }

    @Override
    public void initialize() {
        client = new Client();
        updateStatus(ThingStatus.UNKNOWN);
        rescheduleUpdate(0, false);
    }

    /**
     * Call updateChannels asynchronously, possibly in a delayed fashion to throttle updates. This protects against a
     * situation where many channels receive REFRESH command, e.g. when openHAB is requesting to update channels
     *
     * @return scheduled future
     */
    private ScheduledFuture<?> submitUpdateChannelsThrottled() {
        long now = System.currentTimeMillis();
        long nextRefresh = lastRefreshMillis + REFRESH_THROTTLE_MILLIS;
        lastRefreshMillis = now;
        if (now > nextRefresh) {
            return scheduler.schedule(this::updateChannels, 0, TimeUnit.MILLISECONDS);
        } else {
            long delayMillis = nextRefresh - now;
            return scheduler.schedule(this::updateChannels, delayMillis, TimeUnit.MILLISECONDS);
        }
    }

    protected abstract void update(int retry);

    protected abstract void updateChannels();

    @Override
    public void dispose() {
        super.dispose();
        response = null;
        cancel(futureRef.getAndSet(null), true);
        cancel(updateChannelsFutureRef.getAndSet(null), true);
    }

    protected static int lastValidIndex(Data data) {
        if (data.values.length < 2) {
            throw new IllegalStateException("Excepted at least two data items");
        }
        if (data.values[0] == null) {
            return -1;
        }
        for (int i = 1; i < data.values.length; i++) {
            if (data.values[i] == null) {
                return i - 1;
            }
        }
        if (data.values[data.values.length - 1] == null) {
            return -1;
        }
        return data.values.length - 1;
    }

    protected static long floorToEvenMinutes(long epochSeconds, int roundMinutes) {
        long roundSecs = roundMinutes * 60;
        return (epochSeconds / roundSecs) * roundSecs;
    }

    protected static long ceilToEvenMinutes(long epochSeconds, int roundMinutes) {
        double epochDouble = epochSeconds;
        long roundSecs = roundMinutes * 60;
        double roundSecsDouble = (roundMinutes * 60);
        return (long) Math.ceil(epochDouble / roundSecsDouble) * roundSecs;
    }

    /**
     * Update QuantityType channel state
     *
     * @param channelUID channel UID
     * @param epochSecond value to update
     * @param unit unit associated with the value
     */
    protected <T extends Quantity<T>> void updateEpochSecondStateIfLinked(ChannelUID channelUID, long epochSecond) {
        if (isLinked(channelUID)) {
            updateState(channelUID, new DateTimeType(ZonedDateTime.ofInstant(Instant.ofEpochSecond(epochSecond), UTC)
                    .withZoneSameInstant(ZoneId.systemDefault())));
        }
    }

    /**
     * Update QuantityType or DecimalType channel state
     *
     * Updates UNDEF state when value is null
     *
     * @param channelUID channel UID
     * @param value value to update
     * @param unit unit associated with the value
     */
    protected void updateStateIfLinked(ChannelUID channelUID, @Nullable BigDecimal value, @Nullable Unit<?> unit) {
        if (isLinked(channelUID)) {
            if (value == null) {
                updateState(channelUID, UnDefType.UNDEF);
            } else if (unit == null) {
                updateState(channelUID, new DecimalType(value));
            } else {
                updateState(channelUID, new QuantityType<>(value, unit));
            }
        }
    }

    /**
     * Unwrap optional value and log with ERROR if value is not present
     *
     * @param optional optional to unwrap
     * @param messageIfNotPresent logging message
     * @param args arguments to logging
     * @throws IllegalStateException when value is not present
     * @return unwrapped value of the optional
     */
    protected <T> T unwrap(Optional<T> optional, String messageIfNotPresent, Object... args) {
        if (optional.isPresent()) {
            return optional.get();
        } else {
            // logger.error(messageIfNotPresent, args) avoided due to static analyzer
            String formattedMessage = String.format(messageIfNotPresent, args);
            logger.error("Unwrapping error: {}", formattedMessage);
            throw new IllegalStateException("unwrapping");
        }
    }

    protected void rescheduleUpdate(long delayMillis, boolean mayInterruptIfRunning) {
        rescheduleUpdate(delayMillis, mayInterruptIfRunning, 0);
    }

    protected void rescheduleUpdate(long delayMillis, boolean mayInterruptIfRunning, int retry) {
        cancel(futureRef.getAndSet(scheduler.schedule(() -> this.update(retry), delayMillis, TimeUnit.MILLISECONDS)),
                mayInterruptIfRunning);
    }

    private static void cancel(@Nullable ScheduledFuture<?> future, boolean mayInterruptIfRunning) {
        if (future != null) {
            future.cancel(mayInterruptIfRunning);
        }
    }
}
