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
package org.openhab.binding.folderwatcher.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link FolderWatcherBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Alexandr Salamatov - Initial contribution
 */
@NonNullByDefault
public class FolderWatcherBindingConstants {

    private static final String BINDING_ID = "folderwatcher";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_SAMPLE = new ThingTypeUID(BINDING_ID, "ftpfolder");

    // List of all Channel ids
    public static final String CHANNEL_1 = "newfile";
}
