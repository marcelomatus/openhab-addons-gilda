/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.unifi.internal.api.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.unifi.internal.api.model.HasId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link UniFiCache} is a specialised lookup table that stores objects using multiple keys in the form
 * <code>prefix:suffix</code>. Each implementation is responsible for providing a list of supported prefixes and must
 * implement {@link #getSuffix(Object, String)} to provide a value specific suffix derived from the prefix.
 *
 * Objects are then retrieved simply by using the <code>suffix</code> key component and all combinations of
 * <code>prefix:suffix</code> are searched in the order of their priority.
 *
 * @author Matthew Bowman - Initial contribution
 * @author Hilbrand Bouwkamp - Moved generic code into this class
 */
@NonNullByDefault
abstract class UniFiCache<T extends @Nullable HasId> {

    private static final String SEPARATOR = ":";

    public static final String PREFIX_ALIAS = "alias";
    public static final String PREFIX_DESC = "desc";
    public static final String PREFIX_HOSTNAME = "hostname";
    public static final String PREFIX_ID = "id";
    public static final String PREFIX_IP = "ip";
    public static final String PREFIX_MAC = "mac";
    public static final String PREFIX_NAME = "name";

    private final Logger logger = LoggerFactory.getLogger(getClass());
    // Map of cid keys to the id.
    private final Map<String, String> mapToId = new HashMap<>();
    // Map of id to data object
    private final Map<String, T> map = new HashMap<>();
    private final String[] prefixes;
    private final String type;

    protected UniFiCache(final String type, final String... prefixes) {
        this.type = type;
        this.prefixes = prefixes;
    }

    public void clear() {
        map.clear();
    }

    public final @Nullable T get(final @Nullable String cid) {
        final @Nullable T value;

        if (cid != null && !cid.isBlank()) {
            synchronized (this) {
                final String id = getId(cid);

                if (id == null) {
                    logger.debug("Could not find a matching {} for cid = '{}'", type, cid);
                    value = null;
                } else {
                    value = map.get(id);
                }
            }
        } else {
            value = null;
        }
        return value;
    }

    public @Nullable String getId(final String cid) {
        String value = null;
        for (final String prefix : prefixes) {
            final String key = key(prefix, cid);

            if (mapToId.containsKey(key)) {
                value = mapToId.get(key);
                logger.trace("Cache HIT : '{}' -> {}", key, value);
                break;
            } else {
                logger.trace("Cache MISS : '{}'", key);
            }
        }
        return value;
    }

    public final void putAll(final T @Nullable [] values) {
        if (values != null) {
            logger.debug("Found {} UniFi {} (s): {}", type, values.length, lazyFormatAsList(values));
            for (final T value : values) {
                put(value.getId(), value);
            }
        }
    }

    public final void put(final String id, final T value) {
        for (final String prefix : prefixes) {
            final String suffix = getSuffix(value, prefix);

            if (suffix != null && !suffix.isBlank()) {
                mapToId.put(key(prefix, suffix), id);
            }
        }
        map.put(id, value);
    }

    private String key(final String prefix, final String suffix) {
        return (prefix + SEPARATOR + suffix).toLowerCase(Locale.ROOT);
    }

    public final Collection<T> values() {
        return map.values().stream().distinct().collect(Collectors.toList());
    }

    protected abstract @Nullable String getSuffix(T value, String prefix);

    private static Object lazyFormatAsList(final Object[] arr) {
        return new Object() {

            @Override
            public String toString() {
                String value = "";
                for (final Object o : arr) {
                    value += "\n - " + o.toString();
                }
                return value;
            }
        };
    }
}
