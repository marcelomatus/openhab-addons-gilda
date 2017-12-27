/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.squeezebox.internal.utils;

/***
 *
 * @author Patrik Gfeller
 *
 */
public class SqueezeBoxTimeoutException extends Exception {
    public SqueezeBoxTimeoutException(String message) {
        super(message);
    }

    private static final long serialVersionUID = 4542388088266882905L;
}
