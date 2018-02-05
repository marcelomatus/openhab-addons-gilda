/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.digiplex.communication.events;

import org.openhab.binding.digiplex.communication.DigiplexResponse;

/**
 * Common ancestor for all events received from Digiplex system
 *
 * @author Robert Michalak - Initial contribution
 *
 */
public abstract class AbstractEvent implements DigiplexResponse {

    private int areaNo;

    public AbstractEvent(int areaNo) {
        this.areaNo = areaNo;
    }

    public int getAreaNo() {
        return areaNo;
    }

    public boolean isForArea(int areaNo) {
        if (this.areaNo == 0 || this.areaNo == areaNo) {
            return true;
        }
        // TODO: According to documentation: areaNo = 255 - Occurs in at least one area enabled in the system.
        // I did never encounter 255 on my system though (EVO192).
        // 15 is returned instead, which (I believe) has the same meaning.
        if (this.areaNo == 15 || this.areaNo == 255) {
            return true;
        }
        return false;
    }

}
