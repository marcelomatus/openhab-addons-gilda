/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.homematic.handler;

/**
 * Exception if the BridgeHandler is not available.
 *
 * @author Gerhard Riegler - Initial contribution
 */
public class BridgeHandlerNotAvailableException extends Exception {
    private static final long serialVersionUID = 95628391238530L;

    public BridgeHandlerNotAvailableException(String message) {
        super(message);
    }

}
