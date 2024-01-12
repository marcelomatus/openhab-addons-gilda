/**
 * Copyright (c) 2010-2024 Contributors to the openHAB project
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

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;

/**
 * The {@link EhubParameters} is responsible for all parameters regarded to EHUB
 *
 * @author Örjan Backsell - Initial contribution
 * @author Joel Backsell - Defined parameter records
 *
 */

@NonNullByDefault
public class EhubParameters {
    public String jsonPostEhub;

    public EhubParameters(String jsonPostEhub) {
        this.jsonPostEhub = jsonPostEhub;
    }

    public static List<String> getChannelParametersEhub() {
        final List<String> channelParametersEhub = new ArrayList<>();
        channelParametersEhub.add(new String("wloadConsqL1"));
        channelParametersEhub.add(new String("wloadConsqL2"));
        channelParametersEhub.add(new String("wloadConsqL3"));
        channelParametersEhub.add(new String("iLoaddL1"));
        channelParametersEhub.add(new String("iLoaddL2"));
        channelParametersEhub.add(new String("iLoaddL3"));
        channelParametersEhub.add(new String("winvconsq_3p"));
        channelParametersEhub.add(new String("wextConsqL1"));
        channelParametersEhub.add(new String("wextConsqL2"));
        channelParametersEhub.add(new String("wextConsqL3"));
        channelParametersEhub.add(new String("winvprodq_3p"));
        channelParametersEhub.add(new String("wInvConsqL1"));
        channelParametersEhub.add(new String("wInvConsqL2"));
        channelParametersEhub.add(new String("wInvConsqL3"));
        channelParametersEhub.add(new String("iextL1"));
        channelParametersEhub.add(new String("iextL2"));
        channelParametersEhub.add(new String("iextL3"));
        channelParametersEhub.add(new String("iLoadqL1"));
        channelParametersEhub.add(new String("iLoadqL2"));
        channelParametersEhub.add(new String("iLoadqL3"));
        channelParametersEhub.add(new String("wloadprodq_3p"));
        channelParametersEhub.add(new String("iaceL1"));
        channelParametersEhub.add(new String("iaceL2"));
        channelParametersEhub.add(new String("iaceL3"));
        channelParametersEhub.add(new String("ploadL1"));
        channelParametersEhub.add(new String("ploadL2"));
        channelParametersEhub.add(new String("ploadL3"));
        channelParametersEhub.add(new String("pInvReactiveL1"));
        channelParametersEhub.add(new String("pInvReactiveL2"));
        channelParametersEhub.add(new String("pInvReactiveL3"));
        channelParametersEhub.add(new String("ts"));
        channelParametersEhub.add(new String("ploadReactiveL1"));
        channelParametersEhub.add(new String("ploadReactiveL2"));
        channelParametersEhub.add(new String("ploadReactiveL3"));
        channelParametersEhub.add(new String("state"));
        channelParametersEhub.add(new String("wloadProdqL1"));
        channelParametersEhub.add(new String("wloadProdqL2"));
        channelParametersEhub.add(new String("wloadProdqL3"));
        channelParametersEhub.add(new String("ppv"));
        channelParametersEhub.add(new String("pInvL1"));
        channelParametersEhub.add(new String("pInvL2"));
        channelParametersEhub.add(new String("pInvL3"));
        channelParametersEhub.add(new String("iextqL1"));
        channelParametersEhub.add(new String("iextqL2"));
        channelParametersEhub.add(new String("iextqL3"));
        channelParametersEhub.add(new String("pextL1"));
        channelParametersEhub.add(new String("pextL2"));
        channelParametersEhub.add(new String("pextL3"));
        channelParametersEhub.add(new String("wextProdqL1"));
        channelParametersEhub.add(new String("wextProdqL2"));
        channelParametersEhub.add(new String("wextProdqL3"));
        channelParametersEhub.add(new String("wpv"));
        channelParametersEhub.add(new String("pextReactiveL1"));
        channelParametersEhub.add(new String("pextReactiveL2"));
        channelParametersEhub.add(new String("pextReactiveL3"));
        channelParametersEhub.add(new String("udcPos"));
        channelParametersEhub.add(new String("udcNeg"));
        channelParametersEhub.add(new String("sext"));
        channelParametersEhub.add(new String("iexdtL1"));
        channelParametersEhub.add(new String("iexdtL2"));
        channelParametersEhub.add(new String("iexdtL3"));
        channelParametersEhub.add(new String("wextconsq_3p"));
        channelParametersEhub.add(new String("ildL1"));
        channelParametersEhub.add(new String("ildL2"));
        channelParametersEhub.add(new String("ildL3"));
        channelParametersEhub.add(new String("gridfreq"));
        channelParametersEhub.add(new String("wloadconsq_3p"));
        channelParametersEhub.add(new String("ulL1"));
        channelParametersEhub.add(new String("ulL2"));
        channelParametersEhub.add(new String("ulL3"));
        channelParametersEhub.add(new String("wextprodq_3p"));
        channelParametersEhub.add(new String("ilqL1"));
        channelParametersEhub.add(new String("ilqL2"));
        channelParametersEhub.add(new String("ilqL3"));
        channelParametersEhub.add(new String("wInvProdqL1"));
        channelParametersEhub.add(new String("wInvProdqL2"));
        channelParametersEhub.add(new String("wInvProdqL3"));
        channelParametersEhub.add(new String("ilL1"));
        channelParametersEhub.add(new String("ilL2"));
        channelParametersEhub.add(new String("ilL3"));
        channelParametersEhub.add(new String("wbatprod"));
        channelParametersEhub.add(new String("wbatcons"));
        channelParametersEhub.add(new String("soc"));
        channelParametersEhub.add(new String("soh"));
        channelParametersEhub.add(new String("pbat"));
        channelParametersEhub.add(new String("ratedcap"));
        return channelParametersEhub;
    }
}
