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
package org.openhab.binding.pushover.internal.actions;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.pushover.internal.connection.PushoverMessageBuilder;
import org.openhab.binding.pushover.internal.handler.PushoverAccountHandler;
import org.openhab.core.automation.annotation.ActionInput;
import org.openhab.core.automation.annotation.ActionOutput;
import org.openhab.core.automation.annotation.RuleAction;
import org.openhab.core.thing.binding.ThingActions;
import org.openhab.core.thing.binding.ThingActionsScope;
import org.openhab.core.thing.binding.ThingHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Some automation actions to be used with a {@link PushoverAccountHandler}.
 *
 * @author Christoph Weitkamp - Initial contribution
 */
@ThingActionsScope(name = "pushover")
@NonNullByDefault
public class PushoverThingActions implements ThingActions {

    private final Logger logger = LoggerFactory.getLogger(PushoverThingActions.class);

    private @NonNullByDefault({}) PushoverAccountHandler accountHandler;

    @RuleAction(label = "@text/sendMessageActionLabel", description = "@text/sendMessageActionDescription")
    public @ActionOutput(name = "sent", label = "@text/sendMessageActionOutputLabel", description = "@text/sendMessageActionOutputDescription", type = "java.lang.Boolean") Boolean sendMessage(
            @ActionInput(name = "message", label = "@text/sendMessageActionInputMessageLabel", description = "@text/sendMessageActionInputMessageDescription", type = "java.lang.String", required = true) String message,
            @ActionInput(name = "title", label = "@text/sendMessageActionInputTitleLabel", description = "@text/sendMessageActionInputTitleDescription", type = "java.lang.String") @Nullable String title) {
        logger.trace("ThingAction 'sendMessage' called with value(s): message='{}', title='{}'", message, title);
        return send(getDefaultPushoverMessageBuilder(message), title);
    }

    public static Boolean sendMessage(@Nullable ThingActions actions, String message, @Nullable String title) {
        if (actions instanceof PushoverThingActions) {
            return ((PushoverThingActions) actions).sendMessage(message, title);
        } else {
            throw new IllegalArgumentException("Instance is not a PushoverThingActions class.");
        }
    }

    @RuleAction(label = "@text/sendHTMLMessageActionLabel", description = "@text/sendHTMLMessageActionDescription")
    public @ActionOutput(name = "sent", label = "@text/sendMessageActionOutputLabel", description = "@text/sendMessageActionOutputDescription", type = "java.lang.Boolean") Boolean sendHtmlMessage(
            @ActionInput(name = "message", label = "@text/sendMessageActionInputMessageLabel", description = "@text/sendMessageActionInputMessageDescription", type = "java.lang.String", required = true) String message,
            @ActionInput(name = "title", label = "@text/sendMessageActionInputTitleLabel", description = "@text/sendMessageActionInputTitleDescription", type = "java.lang.String") @Nullable String title) {
        logger.trace("ThingAction 'sendHtmlMessage' called with value(s): message='{}', title='{}'", message, title);
        return send(getDefaultPushoverMessageBuilder(message).withHtmlFormatting(), title);
    }

    public static Boolean sendHtmlMessage(@Nullable ThingActions actions, String message, @Nullable String title) {
        if (actions instanceof PushoverThingActions) {
            return ((PushoverThingActions) actions).sendHtmlMessage(message, title);
        } else {
            throw new IllegalArgumentException("Instance is not a PushoverThingActions class.");
        }
    }

    @RuleAction(label = "@text/sendMonospaceMessageActionLabel", description = "@text/sendMonospaceMessageActionDescription")
    public @ActionOutput(name = "sent", label = "@text/sendMessageActionOutputLabel", description = "@text/sendMessageActionOutputDescription", type = "java.lang.Boolean") Boolean sendMonospaceMessage(
            @ActionInput(name = "message", label = "@text/sendMessageActionInputMessageLabel", description = "@text/sendMessageActionInputMessageDescription", type = "java.lang.String", required = true) String message,
            @ActionInput(name = "title", label = "@text/sendMessageActionInputTitleLabel", description = "@text/sendMessageActionInputTitleDescription", type = "java.lang.String") @Nullable String title) {
        logger.trace("ThingAction 'sendMonospaceMessage' called with value(s): message='{}', title='{}'", message,
                title);
        return send(getDefaultPushoverMessageBuilder(message).withMonospaceFormatting(), title);
    }

    public static Boolean sendMonospaceMessage(@Nullable ThingActions actions, String message, @Nullable String title) {
        if (actions instanceof PushoverThingActions) {
            return ((PushoverThingActions) actions).sendMonospaceMessage(message, title);
        } else {
            throw new IllegalArgumentException("Instance is not a PushoverThingActions class.");
        }
    }

    @RuleAction(label = "@text/sendAttachmentMessageActionLabel", description = "@text/sendAttachmentMessageActionDescription")
    public @ActionOutput(name = "sent", label = "@text/sendMessageActionOutputLabel", description = "@text/sendMessageActionOutputDescription", type = "java.lang.Boolean") Boolean sendAttachmentMessage(
            @ActionInput(name = "message", label = "@text/sendMessageActionInputMessageLabel", description = "@text/sendMessageActionInputMessageDescription", type = "java.lang.String", required = true) String message,
            @ActionInput(name = "title", label = "@text/sendMessageActionInputTitleLabel", description = "@text/sendMessageActionInputTitleDescription", type = "java.lang.String") @Nullable String title,
            @ActionInput(name = "attachment", label = "@text/sendMessageActionInputAttachmentLabel", description = "@text/sendMessageActionInputAttachmentDescription", type = "java.lang.String", required = true) String attachment,
            @ActionInput(name = "contentType", label = "@text/sendMessageActionInputContentTypeLabel", description = "@text/sendMessageActionInputContentTypeDescription", type = "java.lang.String") @Nullable String contentType) {
        logger.trace(
                "ThingAction 'sendAttachmentMessage' called with value(s): message='{}', title='{}', attachment='{}', contentType='{}'",
                message, title, attachment, contentType);
        if (attachment == null) {
            throw new IllegalArgumentException("Skip sending message as 'attachment' is null.");
        }

        PushoverMessageBuilder builder = getDefaultPushoverMessageBuilder(message).withAttachment(attachment);
        if (contentType != null) {
            builder.withContentType(contentType);
        }
        return send(builder, title);
    }

    public static Boolean sendAttachmentMessage(@Nullable ThingActions actions, String message, @Nullable String title,
            String attachment, @Nullable String contentType) {
        if (actions instanceof PushoverThingActions) {
            return ((PushoverThingActions) actions).sendAttachmentMessage(message, title, attachment, contentType);
        } else {
            throw new IllegalArgumentException("Instance is not a PushoverThingActions class.");
        }
    }

    @RuleAction(label = "@text/sendPriorityMessageActionLabel", description = "@text/sendPriorityMessageActionDescription")
    public @ActionOutput(name = "receipt", label = "@text/sendPriorityMessageActionOutputLabel", description = "@text/sendPriorityMessageActionOutputDescription", type = "java.lang.String") String sendPriorityMessage(
            @ActionInput(name = "message", label = "@text/sendMessageActionInputMessageLabel", description = "@text/sendMessageActionInputMessageDescription", type = "java.lang.String", required = true) String message,
            @ActionInput(name = "title", label = "@text/sendMessageActionInputTitleLabel", description = "@text/sendMessageActionInputTitleDescription", type = "java.lang.String") @Nullable String title,
            @ActionInput(name = "priority", label = "@text/sendMessageActionInputPriorityLabel", description = "@text/sendMessageActionInputPriorityDescription") int priority) {
        logger.trace("ThingAction 'sendPriorityMessage' called with value(s): message='{}', title='{}', priority='{}'",
                message, title, priority);
        if (accountHandler == null) {
            throw new RuntimeException("PushoverAccountHandler is null!");
        }

        PushoverMessageBuilder builder = getDefaultPushoverMessageBuilder(message).withPriority(priority);
        if (title != null) {
            builder.withTitle(title);
        }
        return accountHandler.sendPriorityMessage(builder);
    }

    public static String sendPriorityMessage(@Nullable ThingActions actions, String message, @Nullable String title,
            int priority) {
        if (actions instanceof PushoverThingActions) {
            return ((PushoverThingActions) actions).sendPriorityMessage(message, title, priority);
        } else {
            throw new IllegalArgumentException("Instance is not a PushoverThingActions class.");
        }
    }

    @RuleAction(label = "@text/cancelPriorityMessageActionLabel", description = "@text/cancelPriorityMessageActionDescription")
    public @ActionOutput(name = "Canceled", label = "@text/cancelPriorityMessageActionOutputLabel", description = "@text/cancelPriorityMessageActionOutputDescription", type = "java.lang.Boolean") Boolean cancelPriorityMessage(
            @ActionInput(name = "receipt", label = "@text/cancelPriorityMessageActionInputReceiptLabel", description = "@text/cancelPriorityMessageActionInputReceiptDescription", type = "java.lang.String", required = true) String receipt) {
        logger.trace("ThingAction 'cancelPriorityMessage' called with value(s): '{}'", receipt);
        if (accountHandler == null) {
            throw new RuntimeException("PushoverAccountHandler is null!");
        }
        if (receipt == null) {
            throw new IllegalArgumentException("Skip canceling message as 'receipt' is null.");
        }

        return accountHandler.cancelPriorityMessage(receipt);
    }

    public static Boolean cancelPriorityMessage(@Nullable ThingActions actions, String receipt) {
        if (actions instanceof PushoverThingActions) {
            return ((PushoverThingActions) actions).cancelPriorityMessage(receipt);
        } else {
            throw new IllegalArgumentException("Instance is not a PushoverThingActions class.");
        }
    }

    @RuleAction(label = "@text/sendMessageToDeviceActionLabel", description = "@text/sendMessageToDeviceActionDescription")
    public @ActionOutput(name = "sent", label = "@text/sendMessageActionOutputLabel", description = "@text/sendMessageActionOutputDescription", type = "java.lang.Boolean") Boolean sendMessageToDevice(
            @ActionInput(name = "device", label = "@text/sendMessageActionInputDeviceLabel", description = "@text/sendMessageActionInputDeviceDescription", type = "java.lang.String", required = true) String device,
            @ActionInput(name = "message", label = "@text/sendMessageActionInputMessageLabel", description = "@text/sendMessageActionInputMessageDescription", type = "java.lang.String", required = true) String message,
            @ActionInput(name = "title", label = "@text/sendMessageActionInputTitleLabel", description = "@text/sendMessageActionInputTitleDescription", type = "java.lang.String") @Nullable String title) {
        logger.trace("ThingAction 'sendMessageToDevice' called with value(s): device='{}', message='{}', title='{}'",
                device, message, title);
        if (device == null) {
            throw new IllegalArgumentException("Skip sending message as 'device' is null.");
        }

        return send(getDefaultPushoverMessageBuilder(message).withDevice(device), title);
    }

    public static Boolean sendMessageToDevice(@Nullable ThingActions actions, String device, String message,
            @Nullable String title) {
        if (actions instanceof PushoverThingActions) {
            return ((PushoverThingActions) actions).sendMessageToDevice(device, message, title);
        } else {
            throw new IllegalArgumentException("Instance is not a PushoverThingActions class.");
        }
    }

    private PushoverMessageBuilder getDefaultPushoverMessageBuilder(String message) {
        if (message == null) {
            throw new IllegalArgumentException("Skip sending message as 'message' is null.");
        }

        return accountHandler.getDefaultPushoverMessageBuilder(message);
    }

    private Boolean send(PushoverMessageBuilder builder, @Nullable String title) {
        if (accountHandler == null) {
            throw new RuntimeException("PushoverAccountHandler is null!");
        }

        if (title != null) {
            builder.withTitle(title);
        }
        return accountHandler.sendMessage(builder);
    }

    @Override
    public void setThingHandler(@Nullable ThingHandler handler) {
        if (handler instanceof PushoverAccountHandler) {
            this.accountHandler = (PushoverAccountHandler) handler;
        }
    }

    @Override
    public @Nullable ThingHandler getThingHandler() {
        return accountHandler;
    }
}
