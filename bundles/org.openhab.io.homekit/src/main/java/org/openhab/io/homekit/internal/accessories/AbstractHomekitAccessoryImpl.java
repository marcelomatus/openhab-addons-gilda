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

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import javax.measure.Quantity;
import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.items.GenericItem;
import org.eclipse.smarthome.core.items.Item;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.unit.ImperialUnits;
import org.eclipse.smarthome.core.library.unit.SIUnits;
import org.eclipse.smarthome.core.types.State;
import org.openhab.io.homekit.internal.HomekitAccessoryUpdater;
import org.openhab.io.homekit.internal.HomekitCharacteristicType;
import org.openhab.io.homekit.internal.HomekitSettings;
import org.openhab.io.homekit.internal.HomekitTaggedItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.hapjava.accessories.HomekitAccessory;
import io.github.hapjava.characteristics.HomekitCharacteristicChangeCallback;
import io.github.hapjava.services.Service;

/**
 * Abstract class for Homekit Accessory implementations, this provides the
 * accessory metadata using information from the underlying Item.
 *
 * @author Andy Lintner - Initial contribution
 */
abstract class AbstractHomekitAccessoryImpl implements HomekitAccessory {
    private final Logger logger = LoggerFactory.getLogger(AbstractHomekitAccessoryImpl.class);
    private final List<HomekitTaggedItem> characteristics;
    private final HomekitTaggedItem accessory;
    private final HomekitAccessoryUpdater updater;
    private final HomekitSettings settings;
    private final List<Service> services;

    public AbstractHomekitAccessoryImpl(HomekitTaggedItem accessory, List<HomekitTaggedItem> characteristics,
            HomekitAccessoryUpdater updater, HomekitSettings settings) throws IncompleteAccessoryException {
        this.characteristics = characteristics;
        this.accessory = accessory;
        this.updater = updater;
        this.services = new ArrayList<>();
        this.settings = settings;
    }

    protected Optional<HomekitTaggedItem> getCharacteristic(HomekitCharacteristicType type) {
        return characteristics.stream().filter(c -> c.getCharacteristicType() == type).findAny();
    }

    @Override
    public int getId() {
        return accessory.getId();
    }

    @Override
    public CompletableFuture<String> getName() {
        return CompletableFuture.completedFuture(accessory.getItem().getLabel());
    }

    @Override
    public CompletableFuture<String> getManufacturer() {
        return CompletableFuture.completedFuture("none");
    }

    @Override
    public CompletableFuture<String> getModel() {
        return CompletableFuture.completedFuture("none");
    }

    @Override
    public CompletableFuture<String> getSerialNumber() {
        return CompletableFuture.completedFuture("none");
    }

    @Override
    public CompletableFuture<String> getFirmwareRevision() {
        return CompletableFuture.completedFuture("none");
    }

    @Override
    public void identify() {
        // We're not going to support this for now
    }

    public HomekitTaggedItem getRootAccessory() {
        return accessory;
    }

    public Collection<Service> getServices() {
        return this.services;
    }

    protected HomekitAccessoryUpdater getUpdater() {
        return updater;
    }

    protected HomekitSettings getSettings() {
        return settings;
    }

    protected void subscribe(HomekitCharacteristicType characteristicType,
            HomekitCharacteristicChangeCallback callback) {
        final Optional<HomekitTaggedItem> characteristic = getCharacteristic(characteristicType);
        if (characteristic.isPresent()) {
            getUpdater().subscribe((GenericItem) characteristic.get().getItem(), characteristicType.getTag(), callback);
        } else {
            logger.warn("Missing mandatory characteristic {}", characteristicType);
        }
    }

    protected void unsubscribe(HomekitCharacteristicType characteristicType) {
        final Optional<HomekitTaggedItem> characteristic = getCharacteristic(characteristicType);
        if (characteristic.isPresent()) {
            getUpdater().unsubscribe((GenericItem) characteristic.get().getItem(), characteristicType.getTag());
        } else {
            logger.warn("Missing mandatory characteristic {}", characteristicType);
        }
    }

    protected @Nullable <T extends State> T getStateAs(HomekitCharacteristicType characteristic, Class<T> type) {
        final Optional<HomekitTaggedItem> taggedItem = getCharacteristic(characteristic);
        if (taggedItem.isPresent()) {
            final State state = taggedItem.get().getItem().getStateAs(type);
            if (state != null) {
                return (T) state.as(type);
            }
        }
        logger.debug("State for characteristic {} at accessory {} cannot be retrieved.", characteristic,
                accessory.getName());
        return null;
    }

    @SuppressWarnings("unchecked")
    protected @Nullable <T extends Item> T getItem(HomekitCharacteristicType characteristic, Class<T> type) {
        final Optional<HomekitTaggedItem> taggedItem = getCharacteristic(characteristic);
        if (taggedItem.isPresent()) {
            if (type.isInstance(taggedItem.get().getItem()))
                return (T) taggedItem.get().getItem();
            else
                logger.warn("Unsupported item type for characteristic {} at accessory {}. Expected {}, got {}",
                        characteristic, accessory.getItem().getLabel(), type, taggedItem.get().getItem().getClass());
        } else {
            logger.warn("Mandatory characteristic {} not found at accessory {}. ", characteristic,
                    accessory.getItem().getLabel());

        }
        return null;
    }

    /**
     * return configuration attached to the root accessory, e.g. groupItem.
     * Note: result will be casted to the type of the default value.
     * The type for number is BigDecimal.
     * 
     * @param key configuration key
     * @param defaultValue default value
     * @param <T> expected type
     * @return configuration value
     */
    protected <T> T getAccessoryConfiguration(@NonNull String key, @NonNull T defaultValue) {
        return accessory.getConfiguration(key, defaultValue);
    }

    /**
     * return configuration of the characteristic item, e.g. currentTemperature.
     * Note: result will be casted to the type of the default value.
     * The type for number is BigDecimal.
     * 
     * @param characteristicType characteristic type
     * @param key configuration key
     * @param defaultValue default value
     * @param <T> expected type
     * @return configuration value
     */
    protected <T> T getAccessoryConfiguration(@NonNull HomekitCharacteristicType characteristicType,
            @NonNull String key, @NonNull T defaultValue) {
        final Optional<HomekitTaggedItem> characteristic = getCharacteristic(characteristicType);
        return characteristic.isPresent() ? characteristic.get().getConfiguration(key, defaultValue) : defaultValue;
    }

    /**
     * update mapping with values from item configuration.
     * it checks for all keys from the mapping whether there is configuration at item with the same key and if yes,
     * replace the value.
     * 
     * @param characteristicType characteristicType to identify item
     * @param map mapping to update
     */
    protected void updateMapping(HomekitCharacteristicType characteristicType, Map<?, String> map) {
        getCharacteristic(characteristicType).ifPresent(c -> {
            final Map<String, Object> configuration = c.getConfiguration();
            if (configuration != null) {
                map.replaceAll((k, current_value) -> {
                    final Object new_value = configuration.get(current_value);
                    return (new_value instanceof String) ? (String) new_value : current_value;
                });
            }
        });
    }

    /**
     * takes item state as value and retrieves the key for that value from mapping.
     * e.g. used to map StringItem value to HomeKit Enum
     * 
     * @param characteristicType characteristicType to identify item
     * @param mapping mapping
     * @param defaultValue default value if nothing found in mapping
     * @param <T> type of the result derived from
     * @return key for the value
     */
    protected <T> T getKeyFromMapping(final HomekitCharacteristicType characteristicType, Map<T, String> mapping,
            final T defaultValue) {
        final Optional<HomekitTaggedItem> c = getCharacteristic(characteristicType);
        if (c.isPresent()) {
            final State state = c.get().getItem().getState();
            logger.trace("getKeyFromMapping: characteristic {}, state {}, mapping {}", characteristicType.getTag(),
                    state, mapping);
            if (state instanceof StringType) {
                return mapping.entrySet().stream().filter(entry -> state.toString().equalsIgnoreCase(entry.getValue()))
                        .findAny().map(Entry::getKey).orElseGet(() -> {
                            logger.warn(
                                    "Wrong value {} for {} characteristic of the item {}. Expected one of following {}. Returning {}.",
                                    state.toString(), characteristicType.getTag(), c.get().getName(), mapping.values(),
                                    defaultValue);
                            return defaultValue;
                        });
            }
        }
        return defaultValue;
    }

    protected void addCharacteristic(HomekitTaggedItem characteristic) {
        characteristics.add(characteristic);
    }

    private <T extends Quantity<T>> double convertAndRound(double value, Unit<T> from, Unit<T> to) {
        double rawValue = from == to ? value : from.getConverterTo(to).convert(value);
        return new BigDecimal(rawValue).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    protected double convertToCelsius(double degrees) {
        return convertAndRound(degrees,
                getSettings().useFahrenheitTemperature ? ImperialUnits.FAHRENHEIT : SIUnits.CELSIUS, SIUnits.CELSIUS);
    }

    protected double convertFromCelsius(double degrees) {
        return convertAndRound(degrees,
                getSettings().useFahrenheitTemperature ? SIUnits.CELSIUS : ImperialUnits.FAHRENHEIT,
                ImperialUnits.FAHRENHEIT);
    }
}
