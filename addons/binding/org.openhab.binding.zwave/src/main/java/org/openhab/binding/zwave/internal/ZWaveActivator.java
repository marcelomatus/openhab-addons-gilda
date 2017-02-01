/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.zwave.internal;

import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Bundle activator to register the zwve service providers
 * 
 * @author Chris Jackson
 */
public final class ZWaveActivator implements BundleActivator {

    private static Logger logger = LoggerFactory.getLogger(ZWaveActivator.class);

    private static BundleContext context;

    // private ZWaveConfigProvider configProvider;

    /**
     * Called whenever the OSGi framework starts our bundle
     * 
     * @param bc the bundle's execution context within the framework
     */
    public void start(BundleContext bc) throws Exception {
        context = bc;
        logger.debug("ZWave binding started. Version {}", ZWaveActivator.getVersion());

        // configProvider = new ZWaveConfigProvider();
        // bc.registerService(ConfigDescriptionProvider.class.getName(), configProvider, new Hashtable<String,
        // Object>());
    }

    /**
     * Called whenever the OSGi framework stops our bundle
     * 
     * @param bc the bundle's execution context within the framework
     */
    public void stop(BundleContext bc) throws Exception {
        context = null;
        logger.debug("ZWave binding stopped.");
    }

    /**
     * Returns the bundle context of this bundle
     * 
     * @return the bundle context
     */
    public static BundleContext getContext() {
        return context;
    }

    /**
     * Returns the current version of the bundle.
     * 
     * @return the current version of the bundle.
     */
    public static Version getVersion() {
        return context.getBundle().getVersion();
    }

}
