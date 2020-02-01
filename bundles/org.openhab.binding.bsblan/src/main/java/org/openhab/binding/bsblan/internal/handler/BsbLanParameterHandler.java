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
package org.openhab.binding.bsblan.internal.handler;

import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.types.State;
import org.eclipse.smarthome.core.types.Command;

import org.openhab.binding.bsblan.internal.configuration.BsbLanBridgeConfiguration;
import org.openhab.binding.bsblan.internal.configuration.BsbLanParameterConfiguration;
import org.openhab.binding.bsblan.internal.api.BsbLanApiCaller;
import org.openhab.binding.bsblan.internal.api.models.BsbLanApiParameter;
import org.openhab.binding.bsblan.internal.api.models.BsbLanApiParameterQueryResponse;
import org.openhab.binding.bsblan.internal.api.models.BsbLanApiParameterSetRequest.Type;
import org.openhab.binding.bsblan.internal.BsbLanBindingConstants.Channels;
import org.openhab.binding.bsblan.internal.helper.BsbLanParameterConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link BsbLanParameterHandler} is responsible for updating the data, which are
 * sent to one of the channels.
 *
 * @author Peter Schraffl - Initial contribution
 */
public class BsbLanParameterHandler extends BsbLanBaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(BsbLanParameterHandler.class);
    private BsbLanParameterConfiguration parameterConfig;

    public BsbLanParameterHandler(Thing thing) {
        super(thing);
    }

    public Integer getParameterId() {
        return parameterConfig.id;
    }

    @Override
    protected String getDescription() {
        return "BSB-LAN Parameter";
    }

    @Override
    public void refresh(BsbLanBridgeConfiguration bridgeConfiguration) {
        updateChannels();
    }

    @Override
    public void initialize() {
        parameterConfig = getConfigAs(BsbLanParameterConfiguration.class);
        super.initialize();

        // validate 'setId' configuration -> fallback to value of 'id' if invalid or not specified
        if (parameterConfig.setId == null || parameterConfig.setId <= 0) {
            parameterConfig.setId = parameterConfig.id;
        }

        // validate 'setType' configuration -> fallback to 'SET' if invalid or not specified
        parameterConfig.setType = Type.getTypeWithFallback(parameterConfig.setType).toString();

        // it will take up to refreshInterval seconds until we receive a value and thing goes online
        // see notes in {@link BsbLanBridgeHandler#registerThing(BsbLanBaseThingHandler)}
        updateStatus(ThingStatus.UNKNOWN);
    }

    /**
     * Update the channel from the last data retrieved
     *
     * @param channelId the id identifying the channel to be updated
     */
    @Override
    protected void updateChannel(String channelId) {
        BsbLanApiParameterQueryResponse data = getBridgeHandler().getCachedParameterQueryResponse();
        updateChannel(channelId, data);
    }

    private void updateChannel(String channelId, BsbLanApiParameterQueryResponse data) {
        if (data == null) {
            logger.warn("no data available while updating channel '{}' of parameter {}", channelId, parameterConfig.id);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.BRIDGE_OFFLINE,
                "No data received from BSB-LAN device");
            return;
        }

        BsbLanApiParameter parameter = data.getOrDefault(parameterConfig.id, null);
        if (parameter == null) {
            logger.debug("parameter {} is not part of response data while updating channel '{}' ", parameterConfig.id, channelId);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                String.format("No data received for parameter %s", parameterConfig.id));
            return;
        }

        updateStatus(ThingStatus.ONLINE);

        if (!isLinked(channelId)) {
            return;
        }

        State state = BsbLanParameterConverter.getState(channelId, parameter);
        if (state == null) {
            return;
        }

        updateState(channelId, state);
    }

     /**
     * Update the channel from the last data retrieved
     *
     * @param channelId the id identifying the channel to be updated
     * @param command the value to be set
     */
    @Override
    protected void setChannel(String channelId, Command command) {
        logger.debug("Received command '{}' for channel '{}'", command, channelId);

        if (!channelId.equals(Channels.Parameter.NUMBER_VALUE)
         && !channelId.equals(Channels.Parameter.STRING_VALUE)
         && !channelId.equals(Channels.Parameter.SWITCH_VALUE)) {
            logger.debug("Channel '{}' is read only. Ignoring command", channelId);
            return;
        }

        String value = BsbLanParameterConverter.getValue(channelId, command);
        if (value == null) {
            logger.debug("Channel '{}' is read only or conversion failed. Ignoring command", channelId);
            return;
        }

        BsbLanApiCaller api = getApiCaller();

        boolean success = api.setParameter(parameterConfig.setId, value, Type.getTypeWithFallback(parameterConfig.setType));
        if (!success) {
            logger.warn("Failed to set parameter {} to '{}' for channel '{}'", parameterConfig.setId, value, channelId);
        }

        // refresh value
        BsbLanApiParameterQueryResponse queryResponse = api.queryParameter(parameterConfig.id);
        if (queryResponse == null) {
            logger.warn("Failed to refresh parameter {} after set request", parameterConfig.id);
            return;
        }

        updateChannel(channelId, queryResponse);
    }
}
