package org.openhab.io.homekit.internal;

import java.time.Clock;
import java.time.Duration;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Highly performant generic debouncer
 *
 * Note: This is not written as an Actor for performance reasons. Debounced calls are filtered synchronously, in the
 * caller thread, without the need for locks, context switches, or heap allocations. We use AtomicBoolean to resolve
 * concurrent races; the probability of contending on an AtomicBoolean transition is very low.
 *
 * @param name      The name of this debouncer
 * @param scheduler The scheduler implementation to use
 * @param delay     The time after which to invoke action; each time [[Debouncer.call]] is invoked, this delay is
 *                      reset
 * @param Clock     The source from which we get the current time. This input should use the same source. Specified
 *                      for testing purposes
 * @param action    The action to invoke
 */
class Debouncer {

    private final Clock clock;
    private final ScheduledExecutorService scheduler;
    private final Long delayMs;
    private final Runnable action;
    private final AtomicBoolean pending = new AtomicBoolean(false);
    private final AtomicInteger calls = new AtomicInteger(0);
    private final String name;

    private static Logger logger = LoggerFactory.getLogger(Debouncer.class);

    private volatile Long lastCallAttempt;

    Debouncer(String name, ScheduledExecutorService scheduler, Duration delay, Clock clock, Runnable action) {
        this.name = name;
        this.scheduler = scheduler;
        this.action = action;

        this.delayMs = delay.toMillis();
        this.clock = clock;
        this.lastCallAttempt = clock.millis();
    }

    /**
     * Register that the provided action should be called according to the debounce logic
     */
    void call() {
        lastCallAttempt = clock.millis();
        calls.incrementAndGet();
        if (pending.compareAndSet(false, true)) {
            scheduler.schedule(() -> {
                tryActionOrPostpone();
            }, delayMs, TimeUnit.MILLISECONDS);
        }
    }

    private void tryActionOrPostpone() {
        long now = clock.millis();

        boolean delaySurpassed = ((now - lastCallAttempt) >= delayMs);

        if (delaySurpassed) {
            if (pending.compareAndSet(true, false)) {
                int foldedCalls = calls.getAndSet(0);
                logger.debug("Debouncer action {} invoked after delay {}  ({} calls)", name, delayMs, foldedCalls);
                try {
                    action.run();
                } catch (Throwable ex) {
                    logger.error("Debouncer " + name + " action resulted in error", ex);
                }
            } else {
                logger.error("Invalid state in debouncer. Should not have reached here!");
            }
        } else {
            // reschedule at origLastInvocation + delayMs
            // Note: we use Math.max as there's a _very_ small chance lastCallAttempt could advance in another thread,
            // and result in a negative calculation
            long delay = Math.max(1, lastCallAttempt - now + delayMs);
            scheduler.schedule(() -> tryActionOrPostpone(), delay, TimeUnit.MILLISECONDS);
        }
    }
}
