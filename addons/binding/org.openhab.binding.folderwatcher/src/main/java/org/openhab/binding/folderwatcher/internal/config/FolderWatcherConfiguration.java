/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.folderwatcher.internal.config;

/**
 * The {@link FolderWatcherConfiguration} class contains fields mapping thing configuration parameters.
 *
 * @author Alexandr Salamatov - Initial contribution
 */
public class FolderWatcherConfiguration {

    /**
     * Sample configuration parameter. Replace with your own.
     */
    public String ftpAddress;
    public Integer ftpPort;
    public String ftpUsername;
    public String ftpPassword;
    public String ftpDir;
    public Integer pollInterval;
    public Integer connectionTimeout;
    public boolean listHidden;
    public Integer diffHours;
    public boolean listRecursiveFtp;
    public String secureMode;

}
