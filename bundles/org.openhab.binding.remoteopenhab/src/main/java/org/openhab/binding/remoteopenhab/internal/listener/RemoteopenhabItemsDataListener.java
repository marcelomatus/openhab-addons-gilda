/**
 * Copyright (c) 2010-2023 Contributors to the openHAB project
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
package org.openhab.binding.remoteopenhab.internal.listener;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.binding.remoteopenhab.internal.data.RemoteopenhabItem;
import org.openhab.binding.remoteopenhab.internal.rest.RemoteopenhabRestClient;

/**
 * Interface for listeners of events relative to items generated by the {@link RemoteopenhabRestClient}.
 *
 * @author Laurent Garnier - Initial contribution
 */
@NonNullByDefault
public interface RemoteopenhabItemsDataListener {

    /**
     * A new ItemStateEvent was published.
     */
    void onItemStateEvent(String itemName, String stateType, String state, boolean onlyIfStateChanged);

    /**
     * A new ItemAddedEvent was published.
     */
    void onItemAdded(RemoteopenhabItem item);

    /**
     * A new ItemRemovedEvent was published.
     */
    void onItemRemoved(RemoteopenhabItem item);

    /**
     * A new ItemUpdatedEvent was published.
     */
    void onItemUpdated(RemoteopenhabItem newItem, RemoteopenhabItem oldItem);

    /**
     * A new ChannelDescriptionChangedEvent with updated state options or updated command options was published.
     */
    void onItemOptionsUpdatedd(RemoteopenhabItem item);
}
