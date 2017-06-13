/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.ikeatradfri.handler;

import static org.openhab.binding.ikeatradfri.IkeaTradfriBindingConstants.*;

import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.eclipse.smarthome.core.library.types.IncreaseDecreaseType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.PercentType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.openhab.binding.ikeatradfri.IkeaTradfriBulbConfiguration;
import org.openhab.binding.ikeatradfri.internal.IkeaTradfriCallback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSyntaxException;

/**
 * The {@link IkeaTradfriBulbHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Daniel Sundberg - Initial contribution
 * @author Kai Kreuzer - refactorings
 */
public class IkeaTradfriBulbHandler extends BaseThingHandler implements IkeaTradfriCallback {

    private static final int DELTA = 10;

    private Logger logger = LoggerFactory.getLogger(IkeaTradfriBulbHandler.class);
    private LightProperties prevProperties = null;

    private Integer id;
    private volatile boolean active = false;

    public IkeaTradfriBulbHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void initialize() {
        this.id = getConfigAs(IkeaTradfriBulbConfiguration.class).id;
        updateStatus(ThingStatus.UNKNOWN);
        active = true;
    }

    @Override
    public void dispose() {
        active = false;
        super.dispose();
    }

    @Override
    public void setStatus(ThingStatus status, ThingStatusDetail statusDetail) {
        if (active) {
            updateStatus(status, statusDetail);
        }
    }

    @Override
    public void onDataUpdate(JsonElement data) {
        updateStatus(ThingStatus.ONLINE);
        prevProperties = new LightProperties(data);
        updateStatusFromProperties(prevProperties);
    }

    private void updateStatusFromProperties(LightProperties props) {
        if (!props.getOnOffState()) {
            updateState(CHANNEL_BRIGHTNESS, PercentType.ZERO);
            logger.debug("Updating channel brightness: {}", 0);
            // if we are off, we do not want to set any value for the brightness
            return;
        }

        PercentType dimmer = props.getBrightness();
        if (dimmer != null) {
            updateState(CHANNEL_BRIGHTNESS, dimmer);
            logger.debug("Updating channel brightness: {} from {}", dimmer.toString(), props.getBrightness());
        }

        PercentType colorTemp = props.getColorTemperature();
        if (colorTemp != null) {
            updateState(CHANNEL_COLOR_TEMPERATURE, colorTemp);
            logger.debug("Updating channel color temp: {} ", colorTemp.toString());
        }
    }

    private void set(String payload) {
        logger.debug("Sending to: {} payload: {}", id, payload);
        Bridge bridge = getBridge();
        if (bridge != null) {
            IkeaTradfriGatewayHandler handler = (IkeaTradfriGatewayHandler) bridge.getHandler();
            handler.coapPUT("15001/" + id, payload, this);
        }
    }

    private void setBrightness(PercentType dim) {
        LightProperties props = new LightProperties();
        props.setBrightness(dim).setTransitionTime(3);
        set(props.getJsonString());
    }

    private void setState(boolean on) {
        LightProperties props = new LightProperties();
        props.setOnOffState(on);
        set(props.getJsonString());
    }

    private void setColorTemperature(PercentType colordim) {
        LightProperties props = new LightProperties();
        props.setColorTemperature(colordim).setTransitionTime(3);
        set(props.getJsonString());
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            if (prevProperties != null) {
                logger.debug("Refresh {}", channelUID.toString());
                updateStatusFromProperties(prevProperties);
            }
            return;
        }

        switch (channelUID.getId()) {
            case CHANNEL_BRIGHTNESS:
                if (command instanceof PercentType) {
                    setBrightness((PercentType) command);
                } else if (command instanceof OnOffType) {
                    setState(((OnOffType) command) == OnOffType.ON ? true : false);
                } else if (command instanceof IncreaseDecreaseType) {
                    handleIncreaseDecrease(prevProperties, command, channelUID, prev -> prev.getBrightness(),
                            newValue -> setBrightness(newValue));
                } else {
                    logger.debug("Can't handle command {} on channel {}", command, channelUID);
                }
                break;
            case CHANNEL_COLOR_TEMPERATURE:
                if (command instanceof PercentType) {
                    setColorTemperature((PercentType) command);
                } else if (command instanceof IncreaseDecreaseType) {
                    handleIncreaseDecrease(prevProperties, command, channelUID, prev -> prev.getColorTemperature(),
                            newValue -> setColorTemperature(newValue));
                } else {
                    logger.debug("Can't handle command {} on channel {}", command, channelUID);
                }
                break;
            default:
                logger.error("Unkown channel {}", channelUID);
        }
    }

    private void handleIncreaseDecrease(LightProperties prevProperties, Command command, ChannelUID channelUID,
            Function<LightProperties, PercentType> getOldValue, Consumer<PercentType> setNewValue) {
        if (prevProperties != null && getOldValue.apply(prevProperties) != null) {
            int old = getOldValue.apply(prevProperties).intValue();
            if (IncreaseDecreaseType.INCREASE.equals(command)) {
                setNewValue.accept(new PercentType(Math.min(old + DELTA, PercentType.HUNDRED.intValue())));
            } else {
                setNewValue.accept(new PercentType(Math.max(old - DELTA, PercentType.ZERO.intValue())));
            }
        } else {
            logger.debug("Can't handle inc/dec without knowing the previous state of {}", channelUID);
        }
    }

    private class LightProperties {
        JsonObject root;
        JsonObject settings;
        JsonArray array;

        public LightProperties() {
            root = new JsonObject();
            settings = new JsonObject();
            array = new JsonArray();
            array.add(settings);
            root.add(TRADFRI_LIGHT, array);
        }

        public LightProperties(JsonElement json) {
            try {
                root = json.getAsJsonObject();
                array = root.getAsJsonArray(TRADFRI_LIGHT);
                settings = array.get(0).getAsJsonObject();
            } catch (JsonSyntaxException e) {
                logger.error("JSON error: {}", e.getMessage());
            }
        }

        LightProperties setBrightness(PercentType b) {
            settings.add(TRADFRI_DIMMER, new JsonPrimitive(Math.round(b.floatValue() / 100.0f * 254)));
            return this;
        }

        PercentType getBrightness() {
            int b = settings.get(TRADFRI_DIMMER).getAsInt();
            if (b == 1) {
                return new PercentType(1);
            }
            return new PercentType((int) Math.round(b / 2.54));
        }

        LightProperties setTransitionTime(int seconds) {
            settings.add(TRADFRI_TRANSITION_TIME, new JsonPrimitive(seconds));
            return this;
        }

        @SuppressWarnings("unused")
        int getTransitionTime() {
            return settings.get(TRADFRI_TRANSITION_TIME).getAsInt();
        }

        private final List<Double> X = Arrays.asList(33137.0, 30138.0, 24933.0);
        private final List<Double> Y = Arrays.asList(27211.0, 26909.0, 24691.0);

        LightProperties setColorTemperature(PercentType c) {
            double percentValue = c.doubleValue();

            long newX, newY;
            if (percentValue < 50.0) {
                double p = percentValue / 50.0;
                newX = Math.round(X.get(0) + p * (X.get(1) - X.get(0)));
                newY = Math.round(Y.get(0) + p * (Y.get(1) - Y.get(0)));
            } else {
                double p = (percentValue - 50) / 50.0;
                newX = Math.round(X.get(1) + p * (X.get(2) - X.get(1)));
                newY = Math.round(Y.get(1) + p * (Y.get(2) - Y.get(1)));
            }
            logger.debug("Setting new color: {} {} for {}", newX, newY, percentValue);

            settings.add(TRADFRI_COLOR_X, new JsonPrimitive(newX));
            settings.add(TRADFRI_COLOR_Y, new JsonPrimitive(newY));
            return this;
        }

        PercentType getColorTemperature() {
            JsonElement colorX = settings.get(TRADFRI_COLOR_X);
            if (colorX != null) {
                double x = settings.get(TRADFRI_COLOR_X).getAsInt() / 1.0, value = 0.0;
                if (x > X.get(1)) {
                    value = (x - X.get(0)) / (X.get(1) - X.get(0)) / 2.0;
                } else {
                    value = (x - X.get(1)) / (X.get(2) - X.get(1)) / 2.0 + 0.5;
                }
                return new PercentType((int) Math.round(value * 100.0));
            } else {
                return null;
            }
        }

        LightProperties setOnOffState(boolean on) {
            settings.add(TRADFRI_ONOFF, new JsonPrimitive(on ? 1 : 0));
            return this;
        }

        boolean getOnOffState() {
            JsonElement onOff = settings.get(TRADFRI_ONOFF);
            if (onOff != null) {
                return settings.get(TRADFRI_ONOFF).getAsInt() == 1;
            } else {
                return false;
            }
        }

        String getJsonString() {
            return root.toString();
        }
    }
}
