/**
 * Copyright (c) 2010-2022 Contributors to the openHAB project
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
package org.openhab.binding.easee.internal.command;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.api.Request;
import org.openhab.binding.easee.internal.command.charger.ChangeConfiguration;
import org.openhab.binding.easee.internal.handler.ChannelUtil;
import org.openhab.binding.easee.internal.handler.EaseeHandler;
import org.openhab.binding.easee.internal.model.account.exception.ValidationException;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.Channel;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * base class for all write commands. common logic should be implemented here
 *
 * @author Alexander Friese - initial contribution
 */
@NonNullByDefault
public abstract class AbstractWriteCommand extends AbstractCommand implements EaseeCommand {
    private final Logger logger = LoggerFactory.getLogger(ChangeConfiguration.class);

    protected final Channel channel;
    protected Command command;

    /**
     * the constructor
     *
     * @param config
     */
    public AbstractWriteCommand(EaseeHandler handler, Channel channel, Command command, boolean retryOnFailure,
            boolean updateHandlerOnFailure) {
        super(handler, retryOnFailure, updateHandlerOnFailure);
        this.channel = channel;
        this.command = command;
    }

    /**
     * helper method for write commands that extracts value from command.
     *
     * @return value as String without unit.
     */
    protected String getCommandValue() {
        if (command instanceof QuantityType<?>) {
            // this is necessary because we must not send the unit to the backend
            return String.valueOf(((QuantityType<?>) command).doubleValue());
        } else if (command instanceof OnOffType) {
            // this is necessary because we must send booleans and not ON/OFF to the backend
            return String.valueOf(command.equals(OnOffType.ON));
        } else {
            return command.toString();
        }
    }

    /**
     * helper that transforms channelId + commandvalue in a JSON string that can be added as content to a POST request.
     *
     * @return converted JSON string
     */
    protected String getJsonContent() {
        Map<String, String> content = new HashMap<String, String>(1);
        content.put(channel.getUID().getIdWithoutGroup(), getCommandValue());

        return gson.toJson(content);
    }

    @Override
    protected Request prepareRequest(Request requestToPrepare) {
        String channelId = channel.getUID().getIdWithoutGroup();
        String expr = ChannelUtil.getValidationExpression(channel);
        String value = getCommandValue();

        // quantity types are transformed to double and thus we might have decimals which could cause validation error.
        // So we will shorten here in case no decimals are needed.
        if (value.endsWith(".0")) {
            value = value.substring(0, value.length() - 2);
        }

        if (value.matches(expr)) {
            return prepareWriteRequest(requestToPrepare);
        } else {
            logger.info("channel '{}' does not allow value '{}' - validation rule '{}'", channelId, value, expr);
            throw new ValidationException("channel (" + channelId + ") could not be updated due to a validation error");
        }
    }

    @Override
    protected String getChannelGroup() {
        // this is a pure write command, thus no channel group needed.
        return "";
    }

    /**
     * concrete implementation has to prepare the write requests with additional parameters, etc
     *
     * @param requestToPrepare the request to prepare
     * @return prepared Request object
     */
    protected abstract Request prepareWriteRequest(Request requestToPrepare);
}
