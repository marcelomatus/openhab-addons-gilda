/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.nest.internal;

/**
 * Interface for uniquely identifiable Nest objects (device or a structure).
 *
 * @author Wouter Born - Simplify working with deviceId and structureId
 */
public interface NestIdentifiable {

    /**
     * Returns the identifier that uniquely identifies the Nest object (deviceId or structureId).
     */
    String getId();
}
