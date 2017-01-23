/**
 * Copyright (c) 2014-2016 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */

package org.openhab.binding.tankerkoenig.internal.config;

/***
 * The {@link TankerkoenigListResult} class is the representing java model for the json result of the tankerkoenig.de
 * api
 *
 * @author Dennis Dollinger
 *
 */
public class TankerkoenigListResult {

    private boolean ok;
    private Prices prices;

    public boolean isOk() {
        return ok;
    }

    public void setOk(boolean ok) {
        this.ok = ok;
    }

    public Prices getPrices() {
        return prices;
    }

    public void setPrices(Prices prices) {
        this.prices = prices;
    }

}
