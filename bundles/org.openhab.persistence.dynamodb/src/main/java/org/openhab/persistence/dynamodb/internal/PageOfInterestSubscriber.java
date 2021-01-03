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
package org.openhab.persistence.dynamodb.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * Subscriber that subscribes the page of interest
 *
 * @author Sami Salonen - Initial contribution
 */
@NonNullByDefault
public class PageOfInterestSubscriber<T> implements Subscriber<T> {

    private AtomicInteger skipped = new AtomicInteger();
    private int skip;
    private @NonNullByDefault({}) Subscription subscription;
    private int pageIndex;
    private int pageSize;
    private List<T> page;
    private CompletableFuture<List<T>> future;

    /**
     * Create new PageOfInterestSubscriber
     *
     * @param subscriber subscriber to get the page of interest
     * @param pageIndex page index that we want subscribe
     * @param pageSize page size
     */
    protected PageOfInterestSubscriber(CompletableFuture<List<T>> future, int pageIndex, int pageSize) {
        this.future = future;
        this.pageIndex = pageIndex;
        this.pageSize = pageSize;
        this.page = new ArrayList<>();
        this.skip = pageIndex * pageSize;
    }

    @Override
    public void onSubscribe(@NonNullByDefault({}) Subscription subscription) {
        this.subscription = subscription;
        subscription.request(pageSize * (pageIndex + 1));
    }

    @Override
    public void onNext(T t) {
        if (future.isCancelled()) {
            subscription.cancel();
            onError(new InterruptedException());
        } else if (skipped.getAndIncrement() >= skip && page.size() < pageSize) {
            // We have skipped enough, start accumulating
            page.add(t);
            if (page.size() == pageSize) {
                // We have the full page read
                subscription.cancel();
                onComplete();
            }
        }
    }

    @Override
    public void onError(@NonNullByDefault({}) Throwable t) {
        if (!future.isDone()) {
            future.completeExceptionally(t);
        }
    }

    @Override
    public void onComplete() {
        if (!future.isDone()) {
            future.complete(page);
        }
    }
}
