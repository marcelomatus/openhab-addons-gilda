/**
 * Copyright (c) 2010-2019 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.regoheatpump.internal.rego6xx;

/**
 * The {@link WriteResponse} is responsible for parsing write responses
 * coming from the rego 6xx unit.
 *
 * @author Boris Krivonog - Initial contribution
 */
class WriteResponse extends AbstractResponseParser<Void> {
    @Override
    public int responseLength() {
        return 1;
    }

    @Override
    protected Void convert(byte[] responseBytes) {
        return null;
    }
}