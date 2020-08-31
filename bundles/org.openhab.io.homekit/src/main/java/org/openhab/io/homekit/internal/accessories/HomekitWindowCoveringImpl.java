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
package org.openhab.io.homekit.internal.accessories;

import static org.openhab.io.homekit.internal.HomekitCharacteristicType.CURRENT_POSITION;
import static org.openhab.io.homekit.internal.HomekitCharacteristicType.TARGET_POSITION;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.eclipse.smarthome.core.library.items.RollershutterItem;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.openhab.io.homekit.internal.HomekitAccessoryUpdater;
import org.openhab.io.homekit.internal.HomekitSettings;
import org.openhab.io.homekit.internal.HomekitTaggedItem;

import io.github.hapjava.accessories.WindowCoveringAccessory;
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback;
import io.github.hapjava.characteristics.impl.windowcovering.PositionStateEnum;
import io.github.hapjava.services.impl.WindowCoveringService;

/**
 *
 * @author epike - Initial contribution
 */
public class HomekitWindowCoveringImpl extends AbstractHomekitAccessoryImpl implements WindowCoveringAccessory {

    public HomekitWindowCoveringImpl(HomekitTaggedItem taggedItem, List<HomekitTaggedItem> mandatoryCharacteristics,
            HomekitAccessoryUpdater updater, HomekitSettings settings) {
        super(taggedItem, mandatoryCharacteristics, updater, settings);
        this.getServices().add(new WindowCoveringService(this));
    }

    @Override
    public CompletableFuture<Integer> getCurrentPosition() {
        PercentType value = getStateAs(CURRENT_POSITION, PercentType.class);
        return CompletableFuture.completedFuture(value != null ? 100 - value.intValue() : 100);
    }

    @Override
    public CompletableFuture<PositionStateEnum> getPositionState() {
        return CompletableFuture.completedFuture(PositionStateEnum.STOPPED);
    }

    @Override
    public CompletableFuture<Integer> getTargetPosition() {
        return getCurrentPosition();
    }

    @Override
    public CompletableFuture<Void> setTargetPosition(int value) {
        getItem(TARGET_POSITION, RollershutterItem.class).ifPresent(item -> item.send(new PercentType(100 - value)));
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public void subscribeCurrentPosition(HomekitCharacteristicChangeCallback callback) {
        subscribe(CURRENT_POSITION, callback);
    }

    @Override
    public void subscribePositionState(HomekitCharacteristicChangeCallback callback) {
        // Not implemented
    }

    @Override
    public void subscribeTargetPosition(HomekitCharacteristicChangeCallback callback) {
        subscribe(TARGET_POSITION, callback);
    }

    @Override
    public void unsubscribeCurrentPosition() {
        unsubscribe(CURRENT_POSITION);
    }

    @Override
    public void unsubscribePositionState() {
        // Not implemented
    }

    @Override
    public void unsubscribeTargetPosition() {
        unsubscribe(CURRENT_POSITION);
    }
}
