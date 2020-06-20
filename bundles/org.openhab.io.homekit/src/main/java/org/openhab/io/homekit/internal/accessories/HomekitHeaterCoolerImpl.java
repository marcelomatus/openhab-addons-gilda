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

import static org.openhab.io.homekit.internal.HomekitCharacteristicType.CURRENT_HEATER_COOLER_STATE;
import static org.openhab.io.homekit.internal.HomekitCharacteristicType.TARGET_HEATER_COOLER_STATE;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.library.items.StringItem;
import org.eclipse.smarthome.core.library.items.SwitchItem;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.OpenClosedType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.types.State;
import org.openhab.io.homekit.internal.HomekitAccessoryUpdater;
import org.openhab.io.homekit.internal.HomekitCharacteristicType;
import org.openhab.io.homekit.internal.HomekitSettings;
import org.openhab.io.homekit.internal.HomekitTaggedItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.hapjava.accessories.HeaterCoolerAccessory;
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback;
import io.github.hapjava.characteristics.impl.heatercooler.CurrentHeaterCoolerStateEnum;
import io.github.hapjava.characteristics.impl.heatercooler.TargetHeaterCoolerStateEnum;
import io.github.hapjava.characteristics.impl.thermostat.TemperatureDisplayUnitCharacteristic;
import io.github.hapjava.characteristics.impl.thermostat.TemperatureDisplayUnitEnum;
import io.github.hapjava.services.impl.HeaterCoolerService;

/**
 * Implements Heater Cooler
 *
 * @author Eugen Freiter - Initial contribution
 */

public class HomekitHeaterCoolerImpl extends AbstractHomekitAccessoryImpl implements HeaterCoolerAccessory {
    private final Logger logger = LoggerFactory.getLogger(HomekitHeaterCoolerImpl.class);
    private final BooleanItemReader activeReader;
    private final Map<CurrentHeaterCoolerStateEnum, String> currentStateMapping = new EnumMap(
            CurrentHeaterCoolerStateEnum.class) {
        {
            put(CurrentHeaterCoolerStateEnum.INACTIVE, "INACTIVE");
            put(CurrentHeaterCoolerStateEnum.IDLE, "IDLE");
            put(CurrentHeaterCoolerStateEnum.HEATING, "HEATING");
            put(CurrentHeaterCoolerStateEnum.COOLING, "COOLING");

        }
    };
    private final Map<TargetHeaterCoolerStateEnum, String> targetStateMapping = new EnumMap(
            TargetHeaterCoolerStateEnum.class) {
        {
            put(TargetHeaterCoolerStateEnum.AUTO, "AUTO");
            put(TargetHeaterCoolerStateEnum.HEAT, "HEAT");
            put(TargetHeaterCoolerStateEnum.COOL, "COOL");
        }
    };

    public HomekitHeaterCoolerImpl(HomekitTaggedItem taggedItem, List<HomekitTaggedItem> mandatoryCharacteristics,
            HomekitAccessoryUpdater updater, HomekitSettings settings) throws IncompleteAccessoryException {
        super(taggedItem, mandatoryCharacteristics, updater, settings);
        activeReader = new BooleanItemReader(getItem(HomekitCharacteristicType.ACTIVE_STATUS, GenericItem.class),
                OnOffType.ON, OpenClosedType.OPEN);
        updateMapping(CURRENT_HEATER_COOLER_STATE, currentStateMapping);
        updateMapping(TARGET_HEATER_COOLER_STATE, targetStateMapping);
        final HeaterCoolerService service = new HeaterCoolerService(this);
        service.addOptionalCharacteristic(new TemperatureDisplayUnitCharacteristic(() -> getTemperatureDisplayUnit(),
                (value) -> setTemperatureDisplayUnit(value), (callback) -> subscribeTemperatureDisplayUnit(callback),
                () -> unsubscribeTemperatureDisplayUnit()));
        getServices().add(service);
    }

    @Override
    public CompletableFuture<Double> getCurrentTemperature() {
        @Nullable
        final DecimalType state = getStateAs(HomekitCharacteristicType.CURRENT_TEMPERATURE, DecimalType.class);
        return CompletableFuture.completedFuture(state != null ? convertToCelsius(state.doubleValue()) : 0.0);
    }

    @Override
    public CompletableFuture<Boolean> isActive() {
        final @Nullable State state = getStateAs(HomekitCharacteristicType.ACTIVE_STATUS, OnOffType.class);
        return CompletableFuture.completedFuture(state == OnOffType.ON);
    }

    @Override
    public CompletableFuture<Void> setActive(final boolean state) {
        final @Nullable SwitchItem item = getItem(HomekitCharacteristicType.ACTIVE_STATUS, SwitchItem.class);
        if (item != null) {
            item.send(OnOffType.from(state));
        }
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public CompletableFuture<CurrentHeaterCoolerStateEnum> getCurrentHeaterCoolerState() {
        return CompletableFuture.completedFuture(getKeyFromMapping(CURRENT_HEATER_COOLER_STATE, currentStateMapping,
                CurrentHeaterCoolerStateEnum.INACTIVE));
    }

    @Override
    public CompletableFuture<TargetHeaterCoolerStateEnum> getTargetHeaterCoolerState() {
        return CompletableFuture.completedFuture(
                getKeyFromMapping(TARGET_HEATER_COOLER_STATE, targetStateMapping, TargetHeaterCoolerStateEnum.AUTO));
    }

    @Override
    public CompletableFuture<Void> setTargetHeaterCoolerState(final TargetHeaterCoolerStateEnum state) {
        final Optional<HomekitTaggedItem> characteristic = getCharacteristic(
                HomekitCharacteristicType.TARGET_HEATER_COOLER_STATE);
        if (characteristic.isPresent()) {
            ((StringItem) characteristic.get().getItem()).send(new StringType(targetStateMapping.get(state)));
        } else {
            logger.warn("Missing mandatory characteristic {}",
                    HomekitCharacteristicType.TARGET_HEATING_COOLING_STATE.getTag());
        }
        return CompletableFuture.completedFuture(null);
    }

    public CompletableFuture<TemperatureDisplayUnitEnum> getTemperatureDisplayUnit() {
        return CompletableFuture
                .completedFuture(getSettings().useFahrenheitTemperature ? TemperatureDisplayUnitEnum.FAHRENHEIT
                        : TemperatureDisplayUnitEnum.CELSIUS);
    }

    public void setTemperatureDisplayUnit(final TemperatureDisplayUnitEnum value) throws Exception {
        // temperature unit set globally via binding setting and cannot be changed at item level.
        // this method is intentionally empty.
    }

    @Override
    public void subscribeCurrentHeaterCoolerState(final HomekitCharacteristicChangeCallback callback) {
        subscribe(HomekitCharacteristicType.CURRENT_HEATER_COOLER_STATE, callback);
    }

    @Override
    public void unsubscribeCurrentHeaterCoolerState() {
        unsubscribe(HomekitCharacteristicType.CURRENT_HEATER_COOLER_STATE);
    }

    @Override
    public void subscribeTargetHeaterCoolerState(final HomekitCharacteristicChangeCallback callback) {
        subscribe(HomekitCharacteristicType.TARGET_HEATER_COOLER_STATE, callback);
    }

    @Override
    public void unsubscribeTargetHeaterCoolerState() {
        unsubscribe(HomekitCharacteristicType.TARGET_HEATER_COOLER_STATE);
    }

    @Override
    public void subscribeActive(final HomekitCharacteristicChangeCallback callback) {
        subscribe(HomekitCharacteristicType.ACTIVE_STATUS, callback);
    }

    @Override
    public void unsubscribeActive() {
        unsubscribe(HomekitCharacteristicType.ACTIVE_STATUS);
    }

    @Override
    public void subscribeCurrentTemperature(final HomekitCharacteristicChangeCallback callback) {
        subscribe(HomekitCharacteristicType.CURRENT_TEMPERATURE, callback);
    }

    @Override
    public void unsubscribeCurrentTemperature() {
        unsubscribe(HomekitCharacteristicType.CURRENT_TEMPERATURE);
    }

    public void subscribeTemperatureDisplayUnit(final HomekitCharacteristicChangeCallback callback) {
        // temperature unit set globally via binding setting and cannot be changed at item level.
        // this method is intentionally empty
    }

    public void unsubscribeTemperatureDisplayUnit() {
        // temperature unit set globally via binding setting and cannot be changed at item level.
        // this method is intentionally empty
    }
}
