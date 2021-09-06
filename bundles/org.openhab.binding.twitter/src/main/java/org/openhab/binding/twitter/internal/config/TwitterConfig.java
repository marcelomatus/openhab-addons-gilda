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
package org.openhab.binding.twitter.internal.config;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link TwitterConfig} class contains fields mapping thing configuration parameters.
 *
 * @author Scott Hanson - Initial contribution
 */

@NonNullByDefault
public class TwitterConfig {
    public String consumerKey = "";
    public String consumerSecret = "";
    public String accessToken = "";
    public String accessTokenSecret = "";
    public int refresh = 30;
}
