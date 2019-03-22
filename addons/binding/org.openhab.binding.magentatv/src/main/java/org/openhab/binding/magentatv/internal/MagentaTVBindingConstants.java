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
package org.openhab.binding.magentatv.internal;

import java.util.Collections;
import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.smarthome.core.thing.ThingTypeUID;

/**
 * The {@link MagentaTVBindingConstants} class defines common constants, which
 * are used across the whole binding.
 *
 * @author Markus Michels - Initial contribution
 */
@NonNullByDefault
public class MagentaTVBindingConstants {

    public static final String BINDING_ID = "magentatv";
    public static final String VENDOR = "Deutsche Telekom";
    public static final String OEM_VENDOR = "HUAWEI";
    public static final String MODEL_MR400 = "DMS_TPB";
    public static final String MODEL_MR401B = "MR401B";
    public static final String MODEL_MR201 = "MR201";
    public static final String MODEL_AUTO = "AUTO";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_RECEIVER = new ThingTypeUID(BINDING_ID, "receiver");
    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES = Collections.singleton(THING_TYPE_RECEIVER);

    /**
     * Property names for config/status properties
     */
    public static final String PROPERTY_UDN = "udn";
    public static final String PROPERTY_FRIENDLYNAME = "friendlyName";
    public static final String PROPERTY_MODEL_NUMBER = "modelRev";
    public static final String PROPERTY_HOST = "host";
    public static final String PROPERTY_IP = "ipAddress";
    public static final String PROPERTY_PORT = "port";
    public static final String PROPERTY_DESC_URL = "descriptionUrl";
    public static final String PROPERTY_PAIRINGCODE = "pairingCode";
    public static final String PROPERTY_VERIFICATIONCODE = "verificationCode";
    public static final String PROPERTY_ACCT_NAME = "accountName";
    public static final String PROPERTY_ACCT_PWD = "accountPassword";
    public static final String PROPERTY_USERID = "userId";
    public static final String PROPERTY_LOCAL_IP = "localIP";
    public static final String PROPERTY_LOCAL_MAC = "localMAC";
    public static final String PROPERTY_TERMINALID = "terminalID";
    public static final String PROPERTY_WAKEONLAN = "wakeOnLAN";
    public static final String PROPERTY_SERVERAGENT = "serverAgent";
    public static final String PROPERTY_EPGHTTPSURL = "epgurl";

    /**
     * Channel names
     */
    public static final String CHGROUP_CONTROL = "control";
    public static final String CHANNEL_POWER = CHGROUP_CONTROL + "#" + "power";
    public static final String CHANNEL_CHUP = CHGROUP_CONTROL + "#" + "channelUp";
    public static final String CHANNEL_CHDOWN = CHGROUP_CONTROL + "#" + "channelDown";
    public static final String CHANNEL_VOLUP = CHGROUP_CONTROL + "#" + "volumeUp";
    public static final String CHANNEL_VOLDOWN = CHGROUP_CONTROL + "#" + "volumeDown";
    public static final String CHANNEL_KEY = CHGROUP_CONTROL + "#" + "key";

    public static final String CHGROUP_PROGRAM = "program";
    public static final String CHANNEL_PROG_TITLE = CHGROUP_PROGRAM + "#" + "programTitle";
    public static final String CHANNEL_PROG_TEXT = CHGROUP_PROGRAM + "#" + "programText";
    public static final String CHANNEL_PROG_START = CHGROUP_PROGRAM + "#" + "programStart";
    public static final String CHANNEL_PROG_DURATION = CHGROUP_PROGRAM + "#" + "programDuration";
    public static final String CHANNEL_PROG_POS = CHGROUP_PROGRAM + "#" + "programPosition";

    public static final String CHGROUP_STATUS = "status";
    public static final String CHANNEL_CHANNEL = CHGROUP_STATUS + "#" + "channel";
    public static final String CHANNEL_CHANNEL_CODE = CHGROUP_STATUS + "#" + "channelCode";
    public static final String CHANNEL_RUN_STATUS = CHGROUP_STATUS + "#" + "runStatus";
    public static final String CHANNEL_PLAY_MODE = CHGROUP_STATUS + "#" + "playMode";

    /**
     * Definitions for the control interface
     */
    public static final String CONTENT_TYPE_XML = "text/xml; charset=UTF-8";
    public static final String CHARSET_UTF8 = "utf-8";

    public static final String PAIRING_NOTIFY_URI = "/magentatv/notify";
    public static final String NOTIFY_PAIRING_CODE = "X-pairingCheck:";

    public static final String DEF_LOCAL_PORT = "8080";
    public static final String MR400_DEF_REMOTE_PORT = "49152";
    public static final String MR400_DEF_DESCRIPTION_URL = "/description.xml";
    public static final String MR401B_DEF_REMOTE_PORT = "8081";
    public static final String MR401B_DEF_DESCRIPTION_URL = "/xml/dial.xml";
    public static final String DEF_FRIENDLY_NAME = "PAD:openHAB";

    public static final Integer DEF_REFRESH_INTERVAL = 60;
    public static final int NETWORK_TIMEOUT = 3000;

    public static final String OPENHAB_HTTP_PORT = "OPENHAB_HTTP_PORT";

    public static final String HTTP_GET = "GET";
    public static final String HTTP_PUT = "PUT";
    public static final String HTTP_POST = "POST";
    public static final String HTTP_DELETE = "DELETE";

    public static final String HEADER_USER_AGENT = "USER_AGENT";
    public static final String HEADER_CONTENT_TYPE = "Content-Type";
    public static final String HEADER_HOST = "HOST";
    public static final String HEADER_ACCEPT = "Accept";
    public static final String HEADER_CACHE_CONTROL = "Cache-Control";
    public static final String HEADER_LANGUAGE = "Accept-Language";
    public static final String HEADER_SOAPACTION = "SOAPACTION";
    public static final String HEADER_CONNECTION = "CONNECTION";
    public static final String USER_AGENT = "Darwin/16.5.0 UPnP/1.0 HUAWEI_iCOS/iCOS V1R1C00 DLNADOC/1.50";
    public static final String ACCEPT_TYPE = "*/*";

    /**
     * program Info event data
     * EVENT_EIT_CHANGE: for a complete list see
     * http://support.huawei.com/hedex/pages/DOC1100366313CEH0713H/01/DOC1100366313CEH0713H/01/resources/dsv_hdx_idp/DSV/en/en-us_topic_0094619523.html
     */
    public static final Integer EV_EITCHG_RUNNING_NOT_RUNNING = 1;
    public static final Integer EV_EITCHG_RUNNING_STARTING = 2;
    public static final Integer EV_EITCHG_RUNNING_PAUSING = 3;
    public static final Integer EV_EITCHG_RUNNING_RUNNING = 4;

    /**
     * playStatus event data
     * EVENT_PLAYMODE_CHANGE: for a complete list see
     * http://support.huawei.com/hedex/pages/DOC1100366313CEH0713H/01/DOC1100366313CEH0713H/01/resources/dsv_hdx_idp/DSV/en/en-us_topic_0094619231.html
     */
    public static final Integer EV_PLAYCHG_STOP = 0; // STOP: stop status.
    public static final Integer EV_PLAYCHG_PAUSE = 1; // PAUSE: pause status.
    public static final Integer EV_PLAYCHG_PLAY = 2; // NORMAL_PLAY: normal playback status for non-live content
                                                     // (including TSTV).
    public static final Integer EV_PLAYCHG_TRICK = 3; // TRICK_MODE: trick play mode, such as fast-forward, rewind,
                                                      // slow-forward, and slow-rewind.
    public static final Integer EV_PLAYCHG_MC_PLAY = 4; // MULTICAST_CHANNEL_PLAY: live broadcast status of IPTV
                                                        // multicast channels and DVB channels.
    public static final Integer EV_PLAYCHG_UC_PLAY = 5; // UNICAST_CHANNEL_PLAY: live broadcast status of IPTV unicast
                                                        // channels and OTT channels. //
    public static final Integer EV_PLAYCHG_BUFFERING = 20; // BUFFERING: playback buffering status, including playing
                                                           // cPVR content during the recording, playing content
                                                           // during the download, playing the OTT content, and no
                                                           // data in the buffer area.
}
