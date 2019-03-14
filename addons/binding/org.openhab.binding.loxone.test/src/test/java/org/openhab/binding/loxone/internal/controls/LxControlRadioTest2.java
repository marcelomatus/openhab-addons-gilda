/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.loxone.internal.controls;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.types.StateOption;
import org.junit.Before;
import org.junit.Test;

public class LxControlRadioTest2 extends LxControlRadioTest {
    @Override
    @Before
    public void setup() {
        setupControl("1255054f-0355-af47-ffff403fb0c34b9e", "11d68cf4-0080-7697-ffff403fb0c34b9e",
                "0fe650c2-0004-d446-ffff504f9410790f", "Sprinkler 2");
    }

    @Override
    @Test
    public void testChannels() {
        List<StateOption> opts = new ArrayList<>();
        for (Integer i = 1; i <= 6; i++) {
            opts.add(new StateOption(i.toString(), "Sprinkler " + i.toString()));
        }
        opts.add(new StateOption("0", "All Off"));
        testChannel("Number", null, BigDecimal.ZERO, new BigDecimal(16), BigDecimal.ONE, null, false, opts);
    }

    @Override
    @Test
    public void testLoxoneZeroIndexChanges() {
        changeLoxoneState("activeoutput", 0.0);
        testChannelState(new DecimalType(0));
    }

    @Override
    @Test
    public void testZeroIndexCommands() {
        executeCommand(DecimalType.ZERO);
        testAction("reset");
        executeCommand(OnOffType.OFF);
        testAction("reset");
    }
}
