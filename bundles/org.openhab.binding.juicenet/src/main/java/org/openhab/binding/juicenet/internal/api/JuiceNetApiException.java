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
package org.openhab.binding.juicenet.internal.api;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.juicenet.internal.handler.JuiceNetBridgeHandler;

/**
 * The {@link JuiceNetBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Jeff James - Initial contribution
 */
@NonNullByDefault
public class JuiceNetApiException extends Exception {

    /**
     *
     */
    private static final long serialVersionUID = 1L;

    public JuiceNetApiException(String message) {
        super(message);
    }
}
