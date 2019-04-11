/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.xmltv.internal.configuration;

/**
 * The {@link XmlChannelConfiguration} class contains fields mapping
 * Channel thing configuration parameters.
 *
 * @author Gaël L'hopital - Initial contribution
 */
public class XmlChannelConfiguration {
    public static final String CHANNEL_ID = "channelId";

    public String channelId;
    public Integer offset;
    public Integer refresh;
}
