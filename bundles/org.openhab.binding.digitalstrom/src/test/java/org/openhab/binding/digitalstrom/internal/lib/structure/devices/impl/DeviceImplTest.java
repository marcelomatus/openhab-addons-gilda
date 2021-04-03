package org.openhab.binding.digitalstrom.internal.lib.structure.devices.impl;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.openhab.binding.digitalstrom.internal.lib.structure.devices.deviceparameters.constants.OutputChannelEnum;
import org.openhab.binding.digitalstrom.internal.lib.structure.devices.deviceparameters.constants.OutputModeEnum;
import org.openhab.binding.digitalstrom.internal.lib.util.JsonModel;
import org.openhab.binding.digitalstrom.internal.lib.util.OutputChannel;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

@ExtendWith(MockitoExtension.class)
@NonNullByDefault
class DeviceImplTest {

    private static final List<OutputChannel> EMPTY_CHANNEL = new ArrayList<>();

    private static final List<OutputChannel> SHADE_ANGLE_CHANNELS = Arrays.asList(
            new OutputChannel(OutputChannelEnum.SHADE_OPENING_ANGLE_OUTSIDE),
            new OutputChannel(OutputChannelEnum.SHADE_OPENING_ANGLE_INDOOR));

    private static final List<OutputChannel> SHADE_POSITION_CHANNELS = Arrays.asList(
            new OutputChannel(OutputChannelEnum.SHADE_POSITION_INDOOR),
            new OutputChannel(OutputChannelEnum.SHADE_POSITION_OUTSIDE));

    private static final List<OutputChannel> NON_SHADE_CHANNEL = Arrays
            .asList(new OutputChannel(OutputChannelEnum.BRIGHTNESS));

    private static final List<OutputChannel> MIXED_SHADE_CHANNEL = Arrays.asList(
            new OutputChannel(OutputChannelEnum.BRIGHTNESS),
            new OutputChannel(OutputChannelEnum.SHADE_OPENING_ANGLE_OUTSIDE));

    @Test
    void isBlind_Switch_ShadeChannel() {
        JsonObject jsonObject = createJsonObject(OutputModeEnum.SINGLE_SWITCH, SHADE_ANGLE_CHANNELS);
        DeviceImpl deviceImpl = new DeviceImpl(jsonObject);
        assertThat(deviceImpl.isBlind()).isFalse();
    }

    @Test
    void isBlind_Switch_NoShadeChannel() {
        JsonObject jsonObject = createJsonObject(OutputModeEnum.SINGLE_SWITCH, NON_SHADE_CHANNEL);
        DeviceImpl deviceImpl = new DeviceImpl(jsonObject);
        assertThat(deviceImpl.isBlind()).isFalse();
    }

    @Test
    void isBlind_Switch_MixedShadeChannel() {
        JsonObject jsonObject = createJsonObject(OutputModeEnum.SINGLE_SWITCH, MIXED_SHADE_CHANNEL);
        DeviceImpl deviceImpl = new DeviceImpl(jsonObject);
        assertThat(deviceImpl.isBlind()).isFalse();
    }

    @Test
    void isBlind_PositionConUs_NoChannel() {
        JsonObject jsonObject = createJsonObject(OutputModeEnum.POSITION_CON_US, EMPTY_CHANNEL);
        DeviceImpl deviceImpl = new DeviceImpl(jsonObject);
        assertThat(deviceImpl.isBlind()).isFalse();
    }

    @Test
    void isBlind_PositionConUs_NonShadeChannel() {
        JsonObject jsonObject = createJsonObject(OutputModeEnum.POSITION_CON_US, NON_SHADE_CHANNEL);
        DeviceImpl deviceImpl = new DeviceImpl(jsonObject);
        assertThat(deviceImpl.isBlind()).isFalse();
    }

    @Test
    void isBlind_PositionConUs_ShadePositionChannels() {
        JsonObject jsonObject = createJsonObject(OutputModeEnum.POSITION_CON_US, SHADE_POSITION_CHANNELS);
        DeviceImpl deviceImpl = new DeviceImpl(jsonObject);
        assertThat(deviceImpl.isBlind()).isFalse();
    }

    @Test
    void isBlind_PositionConUs_ShadeAngleChannels() {
        JsonObject jsonObject = createJsonObject(OutputModeEnum.POSITION_CON_US, SHADE_ANGLE_CHANNELS);
        DeviceImpl deviceImpl = new DeviceImpl(jsonObject);
        assertThat(deviceImpl.isBlind()).isTrue();
    }

    @Test
    void isBlind_PositionConUs_MixedChannels() {
        JsonObject jsonObject = createJsonObject(OutputModeEnum.POSITION_CON_US, MIXED_SHADE_CHANNEL);
        DeviceImpl deviceImpl = new DeviceImpl(jsonObject);
        assertThat(deviceImpl.isBlind()).isTrue();
    }

    @Test
    void isBlind_PositionCon_NoChannel() {
        JsonObject jsonObject = createJsonObject(OutputModeEnum.POSITION_CON, EMPTY_CHANNEL);
        DeviceImpl deviceImpl = new DeviceImpl(jsonObject);
        assertThat(deviceImpl.isBlind()).isFalse();
    }

    @Test
    void isBlind_PositionCon_NonShadeChannel() {
        JsonObject jsonObject = createJsonObject(OutputModeEnum.POSITION_CON, NON_SHADE_CHANNEL);
        DeviceImpl deviceImpl = new DeviceImpl(jsonObject);
        assertThat(deviceImpl.isBlind()).isFalse();
    }

    @Test
    void isBlind_PositionCon_ShadePositionChannels() {
        JsonObject jsonObject = createJsonObject(OutputModeEnum.POSITION_CON, SHADE_POSITION_CHANNELS);
        DeviceImpl deviceImpl = new DeviceImpl(jsonObject);
        assertThat(deviceImpl.isBlind()).isFalse();
    }

    @Test
    void isBlind_PositionCon_ShadeAngleChannels() {
        JsonObject jsonObject = createJsonObject(OutputModeEnum.POSITION_CON, SHADE_ANGLE_CHANNELS);
        DeviceImpl deviceImpl = new DeviceImpl(jsonObject);
        assertThat(deviceImpl.isBlind()).isTrue();
    }

    @Test
    void isBlind_PositionCon_MixedChannels() {
        JsonObject jsonObject = createJsonObject(OutputModeEnum.POSITION_CON, MIXED_SHADE_CHANNEL);
        DeviceImpl deviceImpl = new DeviceImpl(jsonObject);
        assertThat(deviceImpl.isBlind()).isTrue();
    }

    private static JsonObject createJsonObject(OutputModeEnum outputMode, List<OutputChannel> channels) {
        JsonModel model = new JsonModel(outputMode.getMode(), channels);

        Gson gson = new Gson();
        String json = gson.toJson(model);

        return JsonParser.parseString(json).getAsJsonObject();
    }
}
