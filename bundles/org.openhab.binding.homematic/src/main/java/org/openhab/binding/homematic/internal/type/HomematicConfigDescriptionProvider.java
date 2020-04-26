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
package org.openhab.binding.homematic.internal.type;

import java.net.URI;
import java.util.Locale;

import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigDescription;
import org.eclipse.smarthome.config.core.ConfigDescriptionProvider;

/**
 * Extends the ConfigDescriptionProvider to manually add a ConfigDescription.
 *
 * @author Gerhard Riegler - Initial contribution
 */
public interface HomematicConfigDescriptionProvider extends ConfigDescriptionProvider {

    /**
     * Adds the ConfigDescription to this provider.
     */
    public void addConfigDescription(ConfigDescription configDescription);

    /**
     * Provides a {@link ConfigDescription} for the given URI.
     *
     * @param uri uri of the config description
     * @param locale locale
     *
     * @return config description or null if no config description could be found
     */
    @Override
    @Nullable
    ConfigDescription getConfigDescription(URI uri, @Nullable Locale locale);

    /**
     * Use this method to lookup a ConfigDescription which was generated by the
     * homematic binding. Other than {@link #getConfigDescription(URI, Locale)}
     * of this provider, it will return also those {@link ConfigDescription}s
     * which are excluded by {@link HomematicThingTypeExcluder}
     * 
     * @param URI config-description-uri
     *            e.g. <i>thing-type:homematic:HM-WDS40-TH-I-2</i>
     * @return ConfigDescription that was added to HomematicConfigDescriptionProvider,
     *         identified by its config-description-uri<br>
     *         <i>null</i> if no ConfigDescription with the given URI was added
     *         before
     */
    public ConfigDescription getInternalConfigDescription(URI uri);
}
