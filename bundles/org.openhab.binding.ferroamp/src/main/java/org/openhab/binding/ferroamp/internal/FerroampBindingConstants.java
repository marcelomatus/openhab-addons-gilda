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
package org.openhab.binding.ferroamp.internal;

import java.util.Set;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.openhab.core.thing.ThingTypeUID;

/**
 * The {@link FerroampBindingConstants} class defines common constants, which are
 * used across the whole binding.
 *
 * @author Örjan Backsell - Initial contribution
 *
 */
@NonNullByDefault
public class FerroampBindingConstants {

    public static final String BINDING_ID = "ferroamp";

    // Broker (energyhub) port number
    static final int BROKER_PORT = 1883;

    // Broker (energyhub) status
    public static final String CONNECTED = "connected";

    // Broker (energyhub) topics
    public static final String EHUB_TOPIC = "extapi/data/ehub";
    public static final String SSO_TOPIC = "extapi/data/sso";
    public static final String ESO_TOPIC = "extapi/data/eso";
    public static final String ESM_TOPIC = "extapi/data/esm";
    public static final String REQUEST_TOPIC = "extapi/control/request";

    // Broker (energyhub) QOS level
    public static final String QOS = "2";

    // List of all Thing Type UIDs
    public static final ThingTypeUID THING_TYPE_ENERGYHUB = new ThingTypeUID(BINDING_ID, "energyhub");

    // List of EHUB Channel ids
    public static final String CHANNEL_EHUBWLOADCONSQL1 = "ehubWLoadConsqL1";
    public static final String CHANNEL_EHUBWLOADCONSQL2 = "ehubWLoadConsqL2";
    public static final String CHANNEL_EHUBWLOADCONSQL3 = "ehubWLoadConsqL3";
    public static final String CHANNEL_EHUBILOADDL1 = "ehubILoaddL1";
    public static final String CHANNEL_EHUBILOADDL2 = "ehubILoaddL2";
    public static final String CHANNEL_EHUBILOADDL3 = "ehubILoaddL3";
    public static final String CHANNEL_EHUBWINVCONSQ_3P = "ehubWInvConsq_3p";
    public static final String CHANNEL_EHUBWEXTCONSQL1 = "ehubWExtConsqL1";
    public static final String CHANNEL_EHUBWEXTCONSQL2 = "ehubWExtConsqL2";
    public static final String CHANNEL_EHUBWEXTCONSQL3 = "ehubWExtConsqL3";
    public static final String CHANNEL_EHUBWINVPRODQ_3P = "ehubWInvProdq_3p";
    public static final String CHANNEL_EHUBWINVCONSQL1 = "ehubWInvConsqL1";
    public static final String CHANNEL_EHUBWINVCONSQL2 = "ehubWInvConsqL2";
    public static final String CHANNEL_EHUBWINVCONSQL3 = "ehubWInvConsqL3";
    public static final String CHANNEL_EHUBIEXTL1 = "ehubIExtL1";
    public static final String CHANNEL_EHUBIEXTL2 = "ehubIExtL2";
    public static final String CHANNEL_EHUBIEXTL3 = "ehubIExtL3";
    public static final String CHANNEL_EHUBILOADQL1 = "ehubILoadqL1";
    public static final String CHANNEL_EHUBILOADQL2 = "ehubILoadqL2";
    public static final String CHANNEL_EHUBILOADQL3 = "ehubILoadqL3";
    public static final String CHANNEL_EHUBWLOADPRODQ_3P = "ehubWLoadProdq_3p";
    public static final String CHANNEL_EHUBIACEL1 = "ehubIaceL1";
    public static final String CHANNEL_EHUBIACEL2 = "ehubIaceL2";
    public static final String CHANNEL_EHUBIACEL3 = "ehubIaceL3";
    public static final String CHANNEL_EHUBPLOADL1 = "ehubPLoadL1";
    public static final String CHANNEL_EHUBPLOADL2 = "ehubPLoadL2";
    public static final String CHANNEL_EHUBPLOADL3 = "ehubPLoadL3";
    public static final String CHANNEL_EHUBPINVREACTIVEL1 = "ehubPInvReactiveL1";
    public static final String CHANNEL_EHUBPINVREACTIVEL2 = "ehubPInvReactiveL2";
    public static final String CHANNEL_EHUBPINVREACTIVEL3 = "ehubPInvReactiveL3";
    public static final String CHANNEL_EHUBTS = "ehubTs";
    public static final String CHANNEL_EHUBPLOADREACTIVEL1 = "ehubPLoadReactiveL1";
    public static final String CHANNEL_EHUBPLOADREACTIVEL2 = "ehubPLoadReactiveL2";
    public static final String CHANNEL_EHUBPLOADREACTIVEL3 = "ehubPLoadReactiveL3";
    public static final String CHANNEL_EHUBSTATE = "ehubState";
    public static final String CHANNEL_EHUBWLOADPRODQL1 = "ehubWLoadProdqL1";
    public static final String CHANNEL_EHUBWLOADPRODQL2 = "ehubWLoadProdqL2";
    public static final String CHANNEL_EHUBWLOADPRODQL3 = "ehubWLoadProdqL3";
    public static final String CHANNEL_EHUBPPV = "ehubPPv";
    public static final String CHANNEL_EHUBPINVL1 = "ehubPInvL1";
    public static final String CHANNEL_EHUBPINVL2 = "ehubPInvL2";
    public static final String CHANNEL_EHUBPINVL3 = "ehubPInvL3";
    public static final String CHANNEL_EHUBIEXTQL1 = "ehubIExtqL1";
    public static final String CHANNEL_EHUBIEXTQL2 = "ehubIExtqL2";
    public static final String CHANNEL_EHUBIEXTQL3 = "ehubIExtqL3";
    public static final String CHANNEL_EHUBPEXTL1 = "ehubPExtL1";
    public static final String CHANNEL_EHUBPEXTL2 = "ehubPExtL2";
    public static final String CHANNEL_EHUBPEXTL3 = "ehubPExtL3";
    public static final String CHANNEL_EHUBWEXTPRODQL1 = "ehubWExtProdqL1";
    public static final String CHANNEL_EHUBWEXTPRODQL2 = "ehubWExtProdqL2";
    public static final String CHANNEL_EHUBWEXTPRODQL3 = "ehubWExtProdqL3";
    public static final String CHANNEL_EHUBWPV = "ehubWPv";
    public static final String CHANNEL_EHUBPEXTREACTIVEL1 = "ehubPExtReactiveL1";
    public static final String CHANNEL_EHUBPEXTREACTIVEL2 = "ehubPExtReactiveL2";
    public static final String CHANNEL_EHUBPEXTREACTIVEL3 = "ehubPExtReactiveL3";
    public static final String CHANNEL_EHUBUDCPOS = "ehubUDcPos";
    public static final String CHANNEL_EHUBUDCNEG = "ehubUDcNeg";
    public static final String CHANNEL_EHUBSEXT = "ehubSExt";
    public static final String CHANNEL_EHUBIEXTDL1 = "ehubIExtdL1";
    public static final String CHANNEL_EHUBIEXTDL2 = "ehubIExtdL2";
    public static final String CHANNEL_EHUBIEXTDL3 = "ehubIExtdL3";
    public static final String CHANNEL_EHUBWEXTCONSQ_3P = "ehubWExtConsq_3p";
    public static final String CHANNEL_EHUBILDL1 = "ehubIldL1";
    public static final String CHANNEL_EHUBILDL2 = "ehubIldL2";
    public static final String CHANNEL_EHUBILDL3 = "ehubIldL3";
    public static final String CHANNEL_EHUBGRIDFREQ = "ehubGridFreq";
    public static final String CHANNEL_EHUBWLOADCONSQ_3P = "ehubWLoadConsq_3p";
    public static final String CHANNEL_EHUBULL1 = "ehubUlL1";
    public static final String CHANNEL_EHUBULL2 = "ehubUlL2";
    public static final String CHANNEL_EHUBULL3 = "ehubUlL3";
    public static final String CHANNEL_EHUBWEXTPRODQ_3P = "ehubWExtProdq_3p";
    public static final String CHANNEL_EHUBILQL1 = "ehubIlqL1";
    public static final String CHANNEL_EHUBILQL2 = "ehubIlqL2";
    public static final String CHANNEL_EHUBILQL3 = "ehubIlqL3";
    public static final String CHANNEL_EHUBWINVPRODQL1 = "ehubWInvProdqL1";
    public static final String CHANNEL_EHUBWINVPRODQL2 = "ehubWInvProdqL2";
    public static final String CHANNEL_EHUBWINVPRODQL3 = "ehubWInvProdqL3";
    public static final String CHANNEL_EHUBILL1 = "ehubIlL1";
    public static final String CHANNEL_EHUBILL2 = "ehubIlL2";
    public static final String CHANNEL_EHUBILL3 = "ehubIlL3";

    // List of battery setup Channel ids
    public static final String CHANNEL_EHUBWBATPROD = "ehubWBatProd";
    public static final String CHANNEL_EHUBWBATCONS = "ehubWBatCons";
    public static final String CHANNEL_EHUBSOC = "ehubSoc";
    public static final String CHANNEL_EHUBSOH = "ehubSoh";
    public static final String CHANNEL_EHUBPBAT = "ehubPBat";
    public static final String CHANNEL_EHUBRATEDCAP = "ehubRatedCap";

    // List of SSO Channel ids
    public static final String CHANNEL_SSORELAYSTATUS_S0 = "ssoRelayStatusS0";
    public static final String CHANNEL_SSOTEMP_S0 = "ssoTempS0";
    public static final String CHANNEL_SSOWPV_S0 = "ssoWPvS0";
    public static final String CHANNEL_SSOTS_S0 = "ssoTsS0";
    public static final String CHANNEL_SSOUDC_S0 = "ssoUDcS0";
    public static final String CHANNEL_SSOFAULTCODE_S0 = "ssoFaultCodeS0";
    public static final String CHANNEL_SSOIPV_S0 = "ssoIPvS0";
    public static final String CHANNEL_SSOUPV_S0 = "ssoUPvS0";
    public static final String CHANNEL_SSOID_S0 = "ssoIdS0";
    public static final String CHANNEL_SSORELAYSTATUS_S1 = "ssoRelayStatusS1";
    public static final String CHANNEL_SSOTEMP_S1 = "ssoTempS1";
    public static final String CHANNEL_SSOWPV_S1 = "ssoWPvS1";
    public static final String CHANNEL_SSOTS_S1 = "ssoTsS1";
    public static final String CHANNEL_SSOUDC_S1 = "ssoUDcS1";
    public static final String CHANNEL_SSOFAULTCODE_S1 = "ssoFaultCodeS1";
    public static final String CHANNEL_SSOIPV_S1 = "ssoIPvS1";
    public static final String CHANNEL_SSOUPV_S1 = "ssoUPvS1";
    public static final String CHANNEL_SSOID_S1 = "ssoIdS1";

    public static final String CHANNEL_SSORELAYSTATUS_S2 = "ssoRelayStatusS2";
    public static final String CHANNEL_SSOTEMP_S2 = "ssoTempS2";
    public static final String CHANNEL_SSOWPV_S2 = "ssoWPvS2";
    public static final String CHANNEL_SSOTS_S2 = "ssoTsS2";
    public static final String CHANNEL_SSOUDC_S2 = "ssoUDcS2";
    public static final String CHANNEL_SSOFAULTCODE_S2 = "ssoFaultCodeS2";
    public static final String CHANNEL_SSOIPV_S2 = "ssoIPvS2";
    public static final String CHANNEL_SSOUPV_S2 = "ssoUPvS2";
    public static final String CHANNEL_SSOID_S2 = "ssoIdS2";
    public static final String CHANNEL_SSORELAYSTATUS_S3 = "ssoRelayStatusS3";
    public static final String CHANNEL_SSOTEMP_S3 = "ssoTempS3";
    public static final String CHANNEL_SSOWPV_S3 = "ssoWPvS3";
    public static final String CHANNEL_SSOTS_S3 = "ssoTsS3";
    public static final String CHANNEL_SSOUDC_S3 = "ssoUDcS3";
    public static final String CHANNEL_SSOFAULTCODE_S3 = "ssoFaultCodeS3";
    public static final String CHANNEL_SSOIPV_S3 = "ssoIPvS3";
    public static final String CHANNEL_SSOUPV_S3 = "ssoUPvS3";
    public static final String CHANNEL_SSOID_S3 = "ssoIdS3";

    // List of ESO Channel ids
    public static final String CHANNEL_ESOFAULTCODE = "esoFaultCode";
    public static final String CHANNEL_ESOID = "esoId";
    public static final String CHANNEL_ESOIBAT = "esoIBat";
    public static final String CHANNEL_ESOUBAT = "esoUBat";
    public static final String CHANNEL_ESORELAYSTATUS = "esoRelayStatus";
    public static final String CHANNEL_ESOSOC = "esoSoc";
    public static final String CHANNEL_ESOTEMP = "esoTemp";
    public static final String CHANNEL_ESOWBATCONS = "esoWBatCons";
    public static final String CHANNEL_ESOWBATPROD = "esoWBatProd";
    public static final String CHANNEL_ESOUDC = "esoUDc";
    public static final String CHANNEL_ESOTS = "esoTs";

    // List of ESM Channel ids
    public static final String CHANNEL_ESMSOH = "esmSoh";
    public static final String CHANNEL_ESMSOC = "esmSoc";
    public static final String CHANNEL_ESMRATEDCAPACITY = "esmRatedCapacity";
    public static final String CHANNEL_ESMSID = "esmId";
    public static final String CHANNEL_ESMRATEDPOWER = "esmRatedPower";
    public static final String CHANNEL_ESMSTATUS = "esmStatus";
    public static final String CHANNEL_ESMTS = "esmTs";

    // List of all Channel ids for configuration
    public static final String CHANNEL_REQUESTCHARGE = "requestCharge";
    public static final String CHANNEL_REQUESTDISCHARGE = "requestDischarge";
    public static final String CHANNEL_AUTO = "requestAuto";
    public static final String CHANNEL_REQUESTEXTAPIVERSION = "requestExtapiVersion";

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Set.of(THING_TYPE_ENERGYHUB);
}
