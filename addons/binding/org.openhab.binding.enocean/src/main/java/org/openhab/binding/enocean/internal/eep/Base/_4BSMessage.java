/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.enocean.internal.eep.Base;

import org.eclipse.smarthome.config.core.Configuration;
import org.eclipse.smarthome.core.util.HexUtils;
import org.openhab.binding.enocean.internal.config.EnOceanChannelTeachInConfig;
import org.openhab.binding.enocean.internal.eep.EEP;
import org.openhab.binding.enocean.internal.eep.EEPType;
import org.openhab.binding.enocean.internal.messages.ERP1Message;

/**
 *
 * @author Daniel Weber - Initial contribution
 */
public abstract class _4BSMessage extends EEP {

    public _4BSMessage(ERP1Message packet) {
        super(packet);
    }

    public _4BSMessage() {
        super();
    }

    public static final byte TeachInBit = 0x08;
    public static final byte LRN_Type_Mask = (byte) 0x80;

    public byte getDB_0() {
        return bytes[3];
    }

    public int getDB_0Value() {
        return (getDB_0() & 0xFF);
    }

    public byte getDB_1() {
        return bytes[2];
    }

    public int getDB_1Value() {
        return (getDB_1() & 0xFF);
    }

    public byte getDB_2() {
        return bytes[1];
    }

    public int getDB_2Value() {
        return (getDB_2() & 0xFF);
    }

    public byte getDB_3() {
        return bytes[0];
    }

    public int getDB_3Value() {
        return (getDB_3() & 0xFF);
    }

    @Override
    public boolean isValid() {
        if (!super.isValid()) {
            logger.warn("Discard message because it is invalid!");
            return false;
        }

        byte db0 = getDB_0();
        boolean lrn = getBit(db0, 3);

        if (!lrn) {
            logger.info("Discard message because this is a teach-in telegram!");
            return false;
        }

        return true;
    }

    @Override
    protected void teachInQueryImpl(Configuration config) {

        if (config == null) {
            return;
        }

        EnOceanChannelTeachInConfig c = config.as(EnOceanChannelTeachInConfig.class);
        if (c.teachInMSG == null || c.teachInMSG.isEmpty()) {

            EEPType type = getEEPType();

            byte db3 = (byte) ((getEEPType().getFunc() << 2) | ((type.getType()) >>> 5));
            byte db2 = (byte) ((type.getType() << 3) & 0xff);
            byte db1 = 0;

            try {
                int manufId = (Integer.parseInt(c.manufacturerId, 16) & 0x7ff); // => 11 bit
                db2 += (manufId >>> 8);
                db1 += (manufId & 0xff);
            } catch (Exception e) {

            }

            setData(db3, db2, db1, LRN_Type_Mask);

        } else {
            try {
                byte[] msg = HexUtils.hexToBytes(c.teachInMSG);
                setData(msg);
            } catch (Exception e) {
            }
        }

    }
}
