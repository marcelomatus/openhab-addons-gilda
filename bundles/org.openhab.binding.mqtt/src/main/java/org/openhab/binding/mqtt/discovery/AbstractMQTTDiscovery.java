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
package org.openhab.binding.mqtt.discovery;

import java.util.Date;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.core.config.discovery.AbstractDiscoveryService;
import org.openhab.core.thing.ThingTypeUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base MQTT discovery class. Responsible for connecting to the {@link MQTTTopicDiscoveryService}.
 *
 * Implement MQTT discovery services on top of this. You still need to reference
 * the MQTTTopicDiscoveryService like in:
 *
 * <pre>
 * &#64;NonNullByDefault({})
 * &#64;Reference
 * protected MQTTTopicDiscoveryService mqttTopicDiscovery;
 * </pre>
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public abstract class AbstractMQTTDiscovery extends AbstractDiscoveryService implements MQTTTopicDiscoveryParticipant {
    private final Logger logger = LoggerFactory.getLogger(AbstractMQTTDiscovery.class);

    protected final String subscribeTopic;

    private int timeout;

    private @Nullable ScheduledFuture<?> scheduledStop;

    private boolean isSubscribed;

    public AbstractMQTTDiscovery(@Nullable Set<ThingTypeUID> supportedThingTypes, int timeout,
            boolean backgroundDiscoveryEnabledByDefault, String baseTopic) {
        super(supportedThingTypes, 0, backgroundDiscoveryEnabledByDefault);
        this.subscribeTopic = baseTopic;
        this.timeout = timeout;
        isSubscribed = false;
    }

    /**
     * Only subscribe if we were not already subscribed
     */
    private void subscribe() {
        if (!isSubscribed) {
            getDiscoveryService().subscribe(this, subscribeTopic);
            isSubscribed = true;
        }
    }

    /**
     * Only unsubscribe if we were already subscribed
     */
    private void unSubscribe() {
        if (isSubscribed) {
            getDiscoveryService().unsubscribe(this);
            isSubscribed = false;
        }
    }

    /**
     * Return the topic discovery service.
     */
    protected abstract MQTTTopicDiscoveryService getDiscoveryService();

    private synchronized void stopTimeout() {
        if (scheduledStop != null) {
            scheduledStop.cancel(false);
            scheduledStop = null;
        }
    }

    protected synchronized void resetTimeout() {
        stopTimeout();

        // schedule an automatic call of stopScan when timeout is reached
        if (timeout > 0) {
            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    try {
                        stopScan();
                    } catch (Exception e) {
                        logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
                    }
                }
            };

            scheduledStop = scheduler.schedule(runnable, timeout, TimeUnit.SECONDS);
        }
    }

    @Override
    protected void startScan() {
        if (isBackgroundDiscoveryEnabled()) {
            super.stopScan();
            return;
        }
        resetTimeout();
        subscribe();
    }

    @Override
    protected synchronized void stopScan() {
        if (isBackgroundDiscoveryEnabled()) {
            super.stopScan();
            return;
        }
        stopTimeout();
        unSubscribe();
        super.stopScan();
    }

    @Override
    public synchronized void abortScan() {
        stopTimeout();
        super.abortScan();
    }

    @Override
    protected void startBackgroundDiscovery() {
        // Remove results that are restored after a restart
        removeOlderResults(new Date().getTime());
        subscribe();
    }

    @Override
    protected void stopBackgroundDiscovery() {
        unSubscribe();
    }
}
