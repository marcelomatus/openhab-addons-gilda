/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
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
package org.openhab.binding.pushsafer.internal;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link PushsaferBindingConstants} class defines common constants, which are used across the whole binding.
 *
 * @author Pushsafer.com (Kevin Siml) - Initial contribution, forked from Christoph Weitkamp
 */
@NonNullByDefault
public class PushsaferBindingConstants {

    private static final String BINDING_ID = "pushsafer";

    public static final ThingTypeUID PUSHSAFER_ACCOUNT = new ThingTypeUID(BINDING_ID, "pushsafer-account");

    public static final String CONFIG_SOUND = "sound";
	public static final String CONFIG_ICON = "icon";

    public static final String DEFAULT_SOUND = "";
	public static final String DEFAULT_ICON = "1";
	public static final String DEFAULT_COLOR = "1";
	public static final String DEFAULT_VIBRATION = "1";
	public static final String DEFAULT_CONFIRM = "0";
	public static final String DEFAULT_ANSWER = "0";
	public static final String DEFAULT_TIME2LIVE = "0";
    public static final String DEFAULT_TITLE = "openHAB";
}
