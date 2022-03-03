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
package org.openhab.binding.pulseaudio.internal.handler;

import static org.openhab.binding.pulseaudio.internal.PulseaudioBindingConstants.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.openhab.binding.pulseaudio.internal.PulseAudioAudioSink;
import org.openhab.binding.pulseaudio.internal.PulseAudioAudioSource;
import org.openhab.binding.pulseaudio.internal.PulseaudioBindingConstants;
import org.openhab.binding.pulseaudio.internal.items.AbstractAudioDeviceConfig;
import org.openhab.binding.pulseaudio.internal.items.Sink;
import org.openhab.binding.pulseaudio.internal.items.SinkInput;
import org.openhab.binding.pulseaudio.internal.items.Source;
import org.openhab.core.audio.AudioFormat;
import org.openhab.core.audio.AudioSink;
import org.openhab.core.audio.AudioSource;
import org.openhab.core.config.core.Configuration;
import org.openhab.core.library.types.DecimalType;
import org.openhab.core.library.types.IncreaseDecreaseType;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.PercentType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.Bridge;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingTypeUID;
import org.openhab.core.thing.ThingUID;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.thing.binding.ThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.RefreshType;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link PulseaudioHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Tobias Bräutigam - Initial contribution
 * @author Miguel Álvarez - Register audio source and refactor
 */
@NonNullByDefault
public class PulseaudioHandler extends BaseThingHandler implements DeviceStatusListener {

    public static final Set<ThingTypeUID> SUPPORTED_THING_TYPES_UIDS = Collections
            .unmodifiableSet(Stream.of(SINK_THING_TYPE, COMBINED_SINK_THING_TYPE, SINK_INPUT_THING_TYPE,
                    SOURCE_THING_TYPE, SOURCE_OUTPUT_THING_TYPE).collect(Collectors.toSet()));
    private final Logger logger = LoggerFactory.getLogger(PulseaudioHandler.class);
    private final int refresh = 60; // refresh every minute as default

    private @Nullable PulseaudioBridgeHandler bridgeHandler;
    private @Nullable String name;
    private @Nullable ScheduledFuture<?> refreshJob;
    private @Nullable PulseAudioAudioSink audioSink;
    private @Nullable PulseAudioAudioSource audioSource;
    private @Nullable Integer savedVolume;

    private final Map<String, ServiceRegistration<AudioSink>> audioSinkRegistrations = new ConcurrentHashMap<>();
    private final Map<String, ServiceRegistration<AudioSource>> audioSourceRegistrations = new ConcurrentHashMap<>();

    private final BundleContext bundleContext;

    public PulseaudioHandler(Thing thing, BundleContext bundleContext) {
        super(thing);
        this.bundleContext = bundleContext;
    }

    @Override
    public void initialize() {
        Configuration config = getThing().getConfiguration();
        name = (String) config.get(DEVICE_PARAMETER_NAME);

        // until we get an update put the Thing offline
        updateStatus(ThingStatus.OFFLINE);
        deviceOnlineWatchdog();

        // if it's a SINK thing, then maybe we have to activate the audio sink
        if (PulseaudioBindingConstants.SINK_THING_TYPE.equals(thing.getThingTypeUID())) {
            // check the property to see if we it's enabled :
            Boolean sinkActivated = (Boolean) thing.getConfiguration()
                    .get(PulseaudioBindingConstants.DEVICE_PARAMETER_AUDIO_SINK_ACTIVATION);
            if (sinkActivated != null && sinkActivated) {
                audioSinkSetup();
            }
        }
        // if it's a SOURCE thing, then maybe we have to activate the audio source
        if (SOURCE_THING_TYPE.equals(thing.getThingTypeUID())) {
            // check the property to see if we it's enabled :
            Boolean sourceActivated = (Boolean) thing.getConfiguration().get(DEVICE_PARAMETER_AUDIO_SOURCE_ACTIVATION);
            if (sourceActivated != null && sourceActivated) {
                audioSourceSetup();
            }
        }
    }

    private void audioSinkSetup() {
        final PulseaudioHandler thisHandler = this;
        scheduler.submit(new Runnable() {
            @Override
            public void run() {
                // Register the sink as an audio sink in openhab
                logger.trace("Registering an audio sink for pulse audio sink thing {}", thing.getUID());
                PulseAudioAudioSink audioSink = new PulseAudioAudioSink(thisHandler, scheduler);
                setAudioSink(audioSink);
                try {
                    audioSink.connectIfNeeded();
                } catch (IOException e) {
                    logger.warn("pulseaudio binding cannot connect to the module-simple-protocol-tcp on {} ({})",
                            getHost(), e.getMessage());
                } catch (InterruptedException i) {
                    logger.info("Interrupted during sink audio connection: {}", i.getMessage());
                    return;
                } finally {
                    audioSink.scheduleDisconnect();
                }
                @SuppressWarnings("unchecked")
                ServiceRegistration<AudioSink> reg = (ServiceRegistration<AudioSink>) bundleContext
                        .registerService(AudioSink.class.getName(), audioSink, new Hashtable<>());
                audioSinkRegistrations.put(thing.getUID().toString(), reg);
            }
        });
    }

    private void audioSourceSetup() {
        final PulseaudioHandler thisHandler = this;
        scheduler.submit(new Runnable() {
            @Override
            public void run() {
                // Register the source as an audio source in openhab
                logger.trace("Registering an audio source for pulse audio source thing {}", thing.getUID());
                PulseAudioAudioSource audioSource = new PulseAudioAudioSource(thisHandler, scheduler);
                setAudioSource(audioSource);
                try {
                    audioSource.connectIfNeeded();
                } catch (IOException e) {
                    logger.warn("pulseaudio binding cannot connect to the module-simple-protocol-tcp on {} ({})",
                            getHost(), e.getMessage());
                } catch (InterruptedException i) {
                    logger.info("Interrupted during source audio connection: {}", i.getMessage());
                    return;
                } finally {
                    audioSource.scheduleDisconnect();
                }
                @SuppressWarnings("unchecked")
                ServiceRegistration<AudioSource> reg = (ServiceRegistration<AudioSource>) bundleContext
                        .registerService(AudioSource.class.getName(), audioSource, new Hashtable<>());
                audioSourceRegistrations.put(thing.getUID().toString(), reg);
            }
        });
    }

    @Override
    public void dispose() {
        if (refreshJob != null && !refreshJob.isCancelled()) {
            refreshJob.cancel(true);
            refreshJob = null;
        }
        updateStatus(ThingStatus.OFFLINE);
        if (bridgeHandler != null) {
            bridgeHandler.unregisterDeviceStatusListener(this);
            bridgeHandler = null;
        }
        logger.trace("Thing {} {} disposed.", getThing().getUID(), name);
        super.dispose();
        if (audioSink != null) {
            audioSink.disconnect();
        }
        if (audioSource != null) {
            audioSource.disconnect();
        }
        // Unregister the potential pulse audio sink's audio sink
        ServiceRegistration<AudioSink> sinkReg = audioSinkRegistrations.remove(getThing().getUID().toString());
        if (sinkReg != null) {
            logger.trace("Unregistering the audio sync service for pulse audio sink thing {}", getThing().getUID());
            sinkReg.unregister();
        }
        // Unregister the potential pulse audio source's audio sources
        ServiceRegistration<AudioSource> sourceReg = audioSourceRegistrations.remove(getThing().getUID().toString());
        if (sourceReg != null) {
            logger.trace("Unregistering the audio sync service for pulse audio source thing {}", getThing().getUID());
            sourceReg.unregister();
        }
    }

    private void deviceOnlineWatchdog() {
        Runnable runnable = () -> {
            try {
                PulseaudioBridgeHandler bridgeHandler = getPulseaudioBridgeHandler();
                if (bridgeHandler != null) {
                    if (bridgeHandler.getDevice(name) == null) {
                        updateStatus(ThingStatus.OFFLINE);
                        this.bridgeHandler = null;
                    } else {
                        updateStatus(ThingStatus.ONLINE);
                    }
                } else {
                    logger.debug("Bridge for pulseaudio device {} not found.", name);
                    updateStatus(ThingStatus.OFFLINE);
                }
            } catch (Exception e) {
                logger.debug("Exception occurred during execution: {}", e.getMessage(), e);
                this.bridgeHandler = null;
            }
        };

        refreshJob = scheduler.scheduleWithFixedDelay(runnable, 0, refresh, TimeUnit.SECONDS);
    }

    private synchronized @Nullable PulseaudioBridgeHandler getPulseaudioBridgeHandler() {
        if (this.bridgeHandler == null) {
            Bridge bridge = getBridge();
            if (bridge == null) {
                logger.debug("Required bridge not defined for device {}.", name);
                return null;
            }
            ThingHandler handler = bridge.getHandler();
            if (handler instanceof PulseaudioBridgeHandler) {
                this.bridgeHandler = (PulseaudioBridgeHandler) handler;
                this.bridgeHandler.registerDeviceStatusListener(this);
            } else {
                logger.debug("No available bridge handler found for device {} bridge {} .", name, bridge.getUID());
                return null;
            }
        }
        return this.bridgeHandler;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        PulseaudioBridgeHandler bridge = getPulseaudioBridgeHandler();
        if (bridge == null) {
            logger.warn("pulseaudio server bridge handler not found. Cannot handle command without bridge.");
            return;
        }
        if (command instanceof RefreshType) {
            bridge.handleCommand(channelUID, command);
            return;
        }

        AbstractAudioDeviceConfig device = bridge.getDevice(name);
        if (device == null) {
            logger.warn("device {} not found", name);
            updateStatus(ThingStatus.OFFLINE);
            bridgeHandler = null;
            return;
        } else {
            State updateState = UnDefType.UNDEF;
            if (channelUID.getId().equals(VOLUME_CHANNEL)) {
                if (command instanceof IncreaseDecreaseType) {
                    // refresh to get the current volume level
                    bridge.getClient().update();
                    device = bridge.getDevice(name);
                    int oldVolume = device.getVolume();
                    int newVolume = oldVolume;
                    if (command.equals(IncreaseDecreaseType.INCREASE)) {
                        newVolume = Math.min(100, oldVolume + 5);
                    }
                    if (command.equals(IncreaseDecreaseType.DECREASE)) {
                        newVolume = Math.max(0, oldVolume - 5);
                    }
                    bridge.getClient().setVolumePercent(device, newVolume);
                    updateState = new PercentType(newVolume);
                    savedVolume = newVolume;
                } else if (command instanceof PercentType) {
                    DecimalType volume = (DecimalType) command;
                    bridge.getClient().setVolumePercent(device, volume.intValue());
                    updateState = (PercentType) command;
                    savedVolume = volume.intValue();
                } else if (command instanceof DecimalType) {
                    // set volume
                    DecimalType volume = (DecimalType) command;
                    bridge.getClient().setVolume(device, volume.intValue());
                    updateState = (DecimalType) command;
                    savedVolume = volume.intValue();
                }
            } else if (channelUID.getId().equals(MUTE_CHANNEL)) {
                if (command instanceof OnOffType) {
                    bridge.getClient().setMute(device, OnOffType.ON.equals(command));
                    updateState = (OnOffType) command;
                }
            } else if (channelUID.getId().equals(SLAVES_CHANNEL)) {
                if (device instanceof Sink && ((Sink) device).isCombinedSink()) {
                    if (command instanceof StringType) {
                        List<Sink> slaves = new ArrayList<>();
                        for (String slaveName : command.toString().split(",")) {
                            Sink slave = bridge.getClient().getSink(slaveName.trim());
                            if (slave != null) {
                                slaves.add(slave);
                            }
                        }
                        if (!slaves.isEmpty()) {
                            bridge.getClient().setCombinedSinkSlaves(((Sink) device), slaves);
                        }
                    }
                } else {
                    logger.error("{} is no combined sink", device);
                }
            } else if (channelUID.getId().equals(ROUTE_TO_SINK_CHANNEL)) {
                if (device instanceof SinkInput) {
                    Sink newSink = null;
                    if (command instanceof DecimalType) {
                        newSink = bridge.getClient().getSink(((DecimalType) command).intValue());
                    } else {
                        newSink = bridge.getClient().getSink(command.toString());
                    }
                    if (newSink != null) {
                        logger.debug("rerouting {} to {}", device, newSink);
                        bridge.getClient().moveSinkInput(((SinkInput) device), newSink);
                        updateState = new StringType(newSink.getPaName());
                    } else {
                        logger.error("no sink {} found", command.toString());
                    }
                }
            }
            logger.trace("updating {} to {}", channelUID, updateState);
            if (!updateState.equals(UnDefType.UNDEF)) {
                updateState(channelUID, updateState);
            }
        }
    }

    /**
     * Use last checked volume for faster access
     *
     * @return
     */
    public int getLastVolume() {
        if (savedVolume == null) {
            PulseaudioBridgeHandler bridge = getPulseaudioBridgeHandler();
            AbstractAudioDeviceConfig device = bridge.getDevice(name);
            // refresh to get the current volume level
            bridge.getClient().update();
            device = bridge.getDevice(name);
            savedVolume = device.getVolume();
        }
        return savedVolume == null ? 50 : savedVolume;
    }

    public void setVolume(int volume) {
        PulseaudioBridgeHandler bridge = getPulseaudioBridgeHandler();
        AbstractAudioDeviceConfig device = bridge.getDevice(name);
        bridge.getClient().setVolumePercent(device, volume);
        updateState(VOLUME_CHANNEL, new PercentType(volume));
        savedVolume = volume;
    }

    @Override
    public void onDeviceStateChanged(ThingUID bridge, AbstractAudioDeviceConfig device) {
        if (device.getPaName().equals(name)) {
            updateStatus(ThingStatus.ONLINE);
            logger.debug("Updating states of {} id: {}", device, VOLUME_CHANNEL);
            savedVolume = device.getVolume();
            updateState(VOLUME_CHANNEL, new PercentType(savedVolume));
            updateState(MUTE_CHANNEL, device.isMuted() ? OnOffType.ON : OnOffType.OFF);
            updateState(STATE_CHANNEL,
                    device.getState() != null ? new StringType(device.getState().toString()) : new StringType("-"));
            if (device instanceof SinkInput) {
                updateState(ROUTE_TO_SINK_CHANNEL,
                        ((SinkInput) device).getSink() != null
                                ? new StringType(((SinkInput) device).getSink().getPaName())
                                : new StringType("-"));
            }
            if (device instanceof Sink && ((Sink) device).isCombinedSink()) {
                updateState(SLAVES_CHANNEL, new StringType(String.join(",", ((Sink) device).getCombinedSinkNames())));
            }
        }
    }

    public String getHost() {
        Bridge bridge = getBridge();
        if (bridge != null) {
            return (String) bridge.getConfiguration().get(PulseaudioBindingConstants.BRIDGE_PARAMETER_HOST);
        } else {
            logger.error("A bridge must be configured for this pulseaudio thing");
            return "null";
        }
    }

    /**
     * This method will scan the pulseaudio server to find the port on which the module/sink is listening
     * If no module is listening, then it will command the module to load on the pulse audio server,
     *
     * @return the port on which the pulseaudio server is listening for this sink
     * @throws InterruptedException when interrupted during the loading module wait
     */
    public int getSimpleTcpPort() throws InterruptedException {
        var bridgeHandler = getPulseaudioBridgeHandler();
        AbstractAudioDeviceConfig device = bridgeHandler.getDevice(name);
        String simpleTcpPortPrefName = (device instanceof Source) ? DEVICE_PARAMETER_AUDIO_SOURCE_PORT
                : DEVICE_PARAMETER_AUDIO_SINK_PORT;
        BigDecimal simpleTcpPortPref = ((BigDecimal) getThing().getConfiguration().get(simpleTcpPortPrefName));
        int simpleTcpPort = simpleTcpPortPref != null ? simpleTcpPortPref.intValue()
                : MODULE_SIMPLE_PROTOCOL_TCP_DEFAULT_PORT;
        String simpleFormat = ((String) getThing().getConfiguration().get(DEVICE_PARAMETER_AUDIO_SOURCE_FORMAT));
        BigDecimal simpleRate = (BigDecimal) getThing().getConfiguration().get(DEVICE_PARAMETER_AUDIO_SOURCE_RATE);
        BigDecimal simpleChannels = (BigDecimal) getThing().getConfiguration()
                .get(DEVICE_PARAMETER_AUDIO_SOURCE_CHANNELS);
        return getPulseaudioBridgeHandler().getClient()
                .loadModuleSimpleProtocolTcpIfNeeded(device, simpleTcpPort, simpleFormat, simpleRate, simpleChannels)
                .orElse(simpleTcpPort);
    }

    public @Nullable AudioFormat getSourceAudioFormat() {
        String simpleFormat = ((String) getThing().getConfiguration().get(DEVICE_PARAMETER_AUDIO_SOURCE_FORMAT));
        BigDecimal simpleRate = ((BigDecimal) getThing().getConfiguration().get(DEVICE_PARAMETER_AUDIO_SOURCE_RATE));
        BigDecimal simpleChannels = ((BigDecimal) getThing().getConfiguration()
                .get(DEVICE_PARAMETER_AUDIO_SOURCE_CHANNELS));
        if (simpleFormat == null || simpleRate == null || simpleChannels == null) {
            return null;
        }
        switch (simpleFormat) {
            case "u8":
                return new AudioFormat(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_SIGNED, null, 8, 1,
                        simpleRate.longValue(), simpleChannels.intValue());
            case "s16le":
                return new AudioFormat(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_SIGNED, false, 16, 1,
                        simpleRate.longValue(), simpleChannels.intValue());
            case "s16be":
                return new AudioFormat(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_SIGNED, true, 16, 1,
                        simpleRate.longValue(), simpleChannels.intValue());
            case "s24le":
                return new AudioFormat(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_UNSIGNED, false, 24, 1,
                        simpleRate.longValue(), simpleChannels.intValue());
            case "s24be":
                return new AudioFormat(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_UNSIGNED, true, 24, 1,
                        simpleRate.longValue(), simpleChannels.intValue());
            case "s32le":
                return new AudioFormat(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_UNSIGNED, false, 32, 1,
                        simpleRate.longValue(), simpleChannels.intValue());
            case "s32be":
                return new AudioFormat(AudioFormat.CONTAINER_WAVE, AudioFormat.CODEC_PCM_UNSIGNED, true, 32, 1,
                        simpleRate.longValue(), simpleChannels.intValue());
            default:
                logger.warn("unsupported format {}", simpleFormat);
                return null;
        }
    }

    public int getIdleTimeout() {
        var handler = getPulseaudioBridgeHandler();
        if (handler == null) {
            return 30000;
        }
        AbstractAudioDeviceConfig device = handler.getDevice(name);
        String idleTimeoutPropName = (device instanceof Source) ? DEVICE_PARAMETER_AUDIO_SOURCE_IDLE_TIMEOUT
                : DEVICE_PARAMETER_AUDIO_SINK_IDLE_TIMEOUT;
        var idleTimeout = (BigDecimal) getThing().getConfiguration().get(idleTimeoutPropName);
        return idleTimeout != null ? idleTimeout.intValue() : 30000;
    }

    public int getBasicProtocolSOTimeout() {
        var soTimeout = (BigDecimal) getThing().getConfiguration().get(DEVICE_PARAMETER_AUDIO_SOCKET_SO_TIMEOUT);
        return soTimeout != null ? soTimeout.intValue() : 500;
    }

    @Override
    public void onDeviceRemoved(PulseaudioBridgeHandler bridge, AbstractAudioDeviceConfig device) {
        if (device.getPaName().equals(name)) {
            bridgeHandler.unregisterDeviceStatusListener(this);
            bridgeHandler = null;
            audioSink.disconnect();
            audioSink = null;
            updateStatus(ThingStatus.OFFLINE);
        }
    }

    @Override
    public void onDeviceAdded(Bridge bridge, AbstractAudioDeviceConfig device) {
        logger.trace("new device discovered {} by {}", device, bridge);
    }

    public void setAudioSink(PulseAudioAudioSink audioSink) {
        this.audioSink = audioSink;
    }

    public void setAudioSource(PulseAudioAudioSource audioSource) {
        this.audioSource = audioSource;
    }
}
