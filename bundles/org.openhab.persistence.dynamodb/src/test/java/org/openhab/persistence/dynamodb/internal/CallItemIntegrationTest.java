/**
 * Copyright (c) 2010-2020 Contributors to the openHAB project
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
package org.openhab.persistence.dynamodb.internal;

import static org.junit.Assert.assertEquals;

import java.util.Date;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.junit.BeforeClass;
import org.openhab.core.library.items.CallItem;
import org.openhab.core.library.types.StringListType;
import org.openhab.core.types.State;

/**
 *
 * @author Sami Salonen - Initial contribution
 *
 */
@NonNullByDefault
public class CallItemIntegrationTest extends AbstractTwoItemIntegrationTest {

    private static final String NAME = "call";
    // values are encoded as dest##orig, ordering goes wrt strings
    private static final StringListType STATE1 = new StringListType("orig1", "dest1");
    private static final StringListType STATE2 = new StringListType("orig1", "dest3");
    private static final StringListType STATE_BETWEEN = new StringListType("orig2", "dest2");

    @BeforeClass
    public static void storeData() throws InterruptedException {
        CallItem item = (CallItem) ITEMS.get(NAME);
        item.setState(STATE1);
        beforeStore = new Date();
        Thread.sleep(10);
        service.store(item);
        afterStore1 = new Date();
        Thread.sleep(10);
        item.setState(STATE2);
        service.store(item);
        Thread.sleep(10);
        afterStore2 = new Date();

        LOGGER.info("Created item between {} and {}", AbstractDynamoDBItem.DATEFORMATTER.format(beforeStore),
                AbstractDynamoDBItem.DATEFORMATTER.format(afterStore1));
    }

    @Override
    protected String getItemName() {
        return NAME;
    }

    @Override
    protected State getFirstItemState() {
        return STATE1;
    }

    @Override
    protected State getSecondItemState() {
        return STATE2;
    }

    @Override
    protected @Nullable State getQueryItemStateBetween() {
        return STATE_BETWEEN;
    }

    @Override
    protected void assertStateEquals(State expected, State actual) {
        // Since CallType.equals is broken, toString is used as workaround
        assertEquals(expected.toString(), actual.toString());
    }

}
