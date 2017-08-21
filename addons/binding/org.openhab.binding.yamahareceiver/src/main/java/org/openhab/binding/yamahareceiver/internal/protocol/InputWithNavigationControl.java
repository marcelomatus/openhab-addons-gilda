/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.yamahareceiver.internal.protocol;

import java.io.IOException;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * The navigation control protocol interface
 *
 * @author David Graeff - Initial contribution
 * @author Tomasz Maruszak - refactoring
 */

public interface InputWithNavigationControl extends IStateUpdatable {
    /**
     * List all inputs that are compatible with this kind of control
     */
    Set<String> SUPPORTED_INPUTS = Sets.newHashSet("NET_RADIO", "USB", "DOCK", "iPOD_USB", "PC", "Napster",
            "Pandora", "SIRIUS", "Rhapsody", "iPod", "HD_RADIO");

    /**
     * Navigate back
     *
     * @throws ReceivedMessageParseException, IOException
     */
    void goBack() throws ReceivedMessageParseException, IOException;

    /**
     * Navigate up
     *
     * @throws ReceivedMessageParseException, IOException
     */
    void goUp() throws IOException, ReceivedMessageParseException;

    /**
     * Navigate down
     *
     * @throws ReceivedMessageParseException, IOException
     */
    void goDown() throws IOException, ReceivedMessageParseException;

    /**
     * Navigate left. Not for all zones or functions available.
     *
     * @throws ReceivedMessageParseException, IOException
     */
    void goLeft() throws IOException, ReceivedMessageParseException;

    /**
     * Navigate right. Not for all zones or functions available.
     *
     * @throws ReceivedMessageParseException, IOException
     */
    void goRight() throws IOException, ReceivedMessageParseException;

    /**
     * Select current item. Not for all zones or functions available.
     *
     * @throws ReceivedMessageParseException, IOException
     */
    void selectCurrentItem() throws IOException, ReceivedMessageParseException;

    /**
     * Navigate to root menu
     *
     * @throws ReceivedMessageParseException, IOException
     */
    boolean goToRoot() throws IOException, ReceivedMessageParseException;

    /**
     * Navigate to the given page. The Yamaha protocol separates list of items into pages.
     *
     * @param page The page, starting with 1.
     *
     * @throws IOException
     * @throws ReceivedMessageParseException
     */
    void goToPage(int page) throws IOException, ReceivedMessageParseException;

    /**
     * Provide a full path to the menu and menu item
     *
     * @param fullPath
     * @throws IOException
     * @throws ReceivedMessageParseException
     */
    void selectItemFullPath(String fullPath) throws IOException, ReceivedMessageParseException;
}
