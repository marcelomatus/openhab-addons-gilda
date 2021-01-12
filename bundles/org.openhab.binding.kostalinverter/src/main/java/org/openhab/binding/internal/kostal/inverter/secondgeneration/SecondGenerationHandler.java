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

package org.openhab.binding.internal.kostal.inverter.secondgeneration;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.measure.Unit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.library.types.StringType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.thing.binding.BaseThingHandler;
import org.openhab.core.types.Command;
import org.openhab.core.types.State;
import org.openhab.core.types.UnDefType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

/**
 * The {@link SecondGenerationHandler} is responsible for handling commands, which are
 * sent to one of the channels, and initiation and refreshing regarded to second generation part of the binding.
 *
 *
 * @author Christian Schneider - Initial contribution
 * @author Christoph Weitkamp - Incorporated new QuantityType (Units of Measurement)
 * @author Örjan Backsell - Redesigned regarding Piko1020, Piko New Generation
 */
@NonNullByDefault
public class SecondGenerationHandler extends BaseThingHandler {

    private final Logger logger = LoggerFactory.getLogger(SecondGenerationHandler.class);

    private @Nullable ScheduledFuture<?> secondGenerationPoller;

    private final HttpClient httpClient;

    private List<SecondGenerationChannelConfiguration> channelConfigs = new ArrayList<>();
    private List<SecondGenerationChannelConfiguration> channelConfigsExt = new ArrayList<>();
    private List<SecondGenerationChannelConfiguration> channelConfigsExtExt = new ArrayList<>();
    private List<SecondGenerationChannelConfiguration> channelConfigsConfigurable = new ArrayList<>();
    private List<SecondGenerationChannelConfiguration> channelConfigsAll = new ArrayList<>();

    private @Nullable SecondGenerationInverterConfig inverterConfig;

    private @Nullable Gson gson;

    public SecondGenerationHandler(Thing thing, HttpClient httpClient) {
        super(thing);
        this.httpClient = httpClient;
    }

    @SuppressWarnings("null")
    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        String url = inverterConfig.url;
        String username = inverterConfig.username;
        String password = inverterConfig.password;
        String valueConfiguration = "";
        String dxsEntriesConf = "";

        if (inverterConfig.invertertype.equals("withBattery")) {
            switch (channelUID.getId()) {
                case SecondGenerationBindingConstants.CHANNEL_BATTERYUSAGECONSUMPTIONSET:
                    valueConfiguration = command.toString();
                    dxsEntriesConf = "33556249";
                    preSetExecuteConfigurationChanges(httpClient, url, username, password, dxsEntriesConf,
                            valueConfiguration);
                    break;
                case SecondGenerationBindingConstants.CHANNEL_BATTERYUSAGESTRATEGYSET:
                    valueConfiguration = command.toString();
                    dxsEntriesConf = "83888896";
                    preSetExecuteConfigurationChanges(httpClient, url, username, password, dxsEntriesConf,
                            valueConfiguration);
                    break;
                case SecondGenerationBindingConstants.CHANNEL_SMARTBATTERYCONTROLSET:
                    if (command.toString().equals("ON")) {
                        valueConfiguration = "true";
                    }
                    if (command.toString().equals("OFF")) {
                        valueConfiguration = "false";
                    }
                    dxsEntriesConf = "33556484";
                    preSetExecuteConfigurationChanges(httpClient, url, username, password, dxsEntriesConf,
                            valueConfiguration);
                    break;
                case SecondGenerationBindingConstants.CHANNEL_BATTERYCHARGETIMEFROMSET:
                    valueConfiguration = command.toString();
                    String valueConfigurationFromTransformed = String.valueOf(stringToSeconds(valueConfiguration));
                    dxsEntriesConf = "33556239";
                    preSetExecuteConfigurationChanges(httpClient, url, username, password, dxsEntriesConf,
                            valueConfigurationFromTransformed);
                    break;
                case SecondGenerationBindingConstants.CHANNEL_BATTERYCHARGETIMETOSET:
                    valueConfiguration = command.toString();
                    String valueConfigurationToTransformed = String.valueOf(stringToSeconds(valueConfiguration));
                    dxsEntriesConf = "33556240";
                    preSetExecuteConfigurationChanges(httpClient, url, username, password, dxsEntriesConf,
                            valueConfigurationToTransformed);
                    break;
                case SecondGenerationBindingConstants.CHANNEL_MAXDEPTHOFDISCHARGESET:
                    valueConfiguration = command.toString();
                    dxsEntriesConf = "33556247";
                    preSetExecuteConfigurationChanges(httpClient, url, username, password, dxsEntriesConf,
                            valueConfiguration);
                    break;
                case SecondGenerationBindingConstants.CHANNEL_SHADOWMANAGEMENTSET:
                    valueConfiguration = command.toString();
                    dxsEntriesConf = "33556483";
                    preSetExecuteConfigurationChanges(httpClient, url, username, password, dxsEntriesConf,
                            valueConfiguration);
                    break;
                case SecondGenerationBindingConstants.CHANNEL_EXTERNALMODULECONTROLSET:
                    valueConfiguration = command.toString();
                    dxsEntriesConf = "33556482";
                    preSetExecuteConfigurationChanges(httpClient, url, username, password, dxsEntriesConf,
                            valueConfiguration);
                    break;
            }
        }
    }

    @Override
    public void initialize() {
        // Set channel configuration parameters
        channelConfigs = SecondGenerationChannelConfiguration.getChannelConfiguration();
        channelConfigsExt = SecondGenerationChannelConfiguration.getChannelConfigurationExt();
        channelConfigsExtExt = SecondGenerationChannelConfiguration.getChannelConfigurationExtExt();
        channelConfigsConfigurable = SecondGenerationChannelConfiguration.getChannelConfigurationConfigurable();

        // Set inverter configuration parameters
        final SecondGenerationInverterConfig inverterConfig = getConfigAs(SecondGenerationInverterConfig.class);
        this.inverterConfig = inverterConfig;

        // Initialize Gson object gson
        final Gson gson = new GsonBuilder().setPrettyPrinting().create();
        this.gson = gson;

        // Temporary value during initializing
        updateStatus(ThingStatus.UNKNOWN);

        // Start update as configured
        secondGenerationPoller = scheduler.scheduleWithFixedDelay(() -> {
            try {
                refresh();
                updateStatus(ThingStatus.ONLINE);
            } catch (RuntimeException scheduleWithFixedDelayException) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        scheduleWithFixedDelayException.getClass().getName() + ":"
                                + scheduleWithFixedDelayException.getMessage());
                logger.debug("Error when connecting to inverter, {}: {}", getThing().getUID(),
                        scheduleWithFixedDelayException.getMessage());
            } catch (InterruptedException e) {
                logger.debug("Communication with inverter interrupted, exception {}", e.getMessage());
            } catch (ExecutionException e) {
                logger.debug("Communication with inverter failed, exception {}", e.getMessage());
            } catch (TimeoutException e) {
                logger.debug("Communication with inverter timed out, exception {}", e.getMessage());
            }
        }, 0, SecondGenerationInverterConfig.REFRESHINTERVAL_SEC, TimeUnit.SECONDS);
    }

    @Override
    public void dispose() {
        final ScheduledFuture<?> secondGenerationLocalPoller = secondGenerationPoller;

        if (secondGenerationLocalPoller != null) {
            secondGenerationLocalPoller.cancel(true);
            secondGenerationPoller = null;
        }
    }

    @SuppressWarnings({ "null", "unchecked" })
    private void refresh() throws InterruptedException, ExecutionException, TimeoutException {
        // dxsEntries part
        String dxsEntriesCall = inverterConfig.url.toString() + "/api/dxs.json?dxsEntries="
                + channelConfigs.get(0).dxsEntries;
        for (int i = 1; i < channelConfigs.size(); i++) {
            dxsEntriesCall += ("&dxsEntries=" + channelConfigs.get(i).dxsEntries);
        }
        String jsonDxsEntriesResponse = callURL(dxsEntriesCall);
        SecondGenerationDxsEntriesContainerDTO dxsEntriesContainer = gson.fromJson(jsonDxsEntriesResponse,
                SecondGenerationDxsEntriesContainerDTO.class);

        String[] channelPosts = new String[23];
        int channelPostsCounter = 0;
        for (SecondGenerationDxsEntries dxsentries : dxsEntriesContainer.dxsEntries) {
            channelPosts[channelPostsCounter] = dxsentries.getName();
            channelPostsCounter++;
        }
        List<String> channelPostsTemp = new ArrayList<String>(Arrays.asList(channelPosts));

        // dxsEntriesExt part
        String dxsEntriesCallExt = inverterConfig.url.toString() + "/api/dxs.json?dxsEntries="
                + channelConfigsExt.get(0).dxsEntries;
        for (int i = 1; i < channelConfigs.size(); i++) {
            dxsEntriesCallExt += ("&dxsEntries=" + channelConfigsExt.get(i).dxsEntries);
        }
        String jsonDxsEntriesResponseExt = callURL(dxsEntriesCallExt);
        SecondGenerationDxsEntriesContainerDTO dxsEntriesContainerExt = gson.fromJson(jsonDxsEntriesResponseExt,
                SecondGenerationDxsEntriesContainerDTO.class);

        String[] channelPostsExt = new String[23];
        int channelPostsCounterExt = 0;
        for (SecondGenerationDxsEntries dxsentriesExt : dxsEntriesContainerExt.dxsEntries) {
            channelPostsExt[channelPostsCounterExt] = dxsentriesExt.getName();
            channelPostsCounterExt++;
        }
        channelPostsTemp.addAll(Arrays.asList(channelPostsExt));

        // dxsEntriesExtExt part
        String dxsEntriesCallExtExt = inverterConfig.url.toString() + "/api/dxs.json?dxsEntries="
                + channelConfigsExtExt.get(0).dxsEntries;
        for (int i = 1; i < channelConfigsExtExt.size(); i++) {
            dxsEntriesCallExtExt += ("&dxsEntries=" + channelConfigsExtExt.get(i).dxsEntries);
        }
        String jsonDxsEntriesResponseExtExt = callURL(dxsEntriesCallExtExt);
        SecondGenerationDxsEntriesContainerDTO dxsEntriesContainerExtExt = gson.fromJson(jsonDxsEntriesResponseExtExt,
                SecondGenerationDxsEntriesContainerDTO.class);

        String[] channelPostsExtExt = new String[3];
        int channelPostsCounterExtExt = 0;
        for (SecondGenerationDxsEntries dxsentriesExtExt : dxsEntriesContainerExtExt.dxsEntries) {
            channelPostsExtExt[channelPostsCounterExtExt] = dxsentriesExtExt.getName();
            channelPostsCounterExtExt++;
        }
        channelPostsTemp.addAll(Arrays.asList(channelPostsExtExt));

        // dxsEntriesConfigurable part
        String dxsEntriesCallConfigurable = inverterConfig.url.toString() + "/api/dxs.json?dxsEntries="
                + channelConfigsConfigurable.get(0).dxsEntries;
        for (int i = 1; i < channelConfigsConfigurable.size(); i++) {
            dxsEntriesCallConfigurable += ("&dxsEntries=" + channelConfigsConfigurable.get(i).dxsEntries);
        }

        String jsonDxsEntriesResponseConfigurable = callURL(dxsEntriesCallConfigurable);
        SecondGenerationDxsEntriesContainerDTO dxsEntriesContainerConfigurable = gson
                .fromJson(jsonDxsEntriesResponseConfigurable, SecondGenerationDxsEntriesContainerDTO.class);

        String[] channelPostsConfigurable = new String[5];
        int channelPostsCounterConfigurable = 0;
        for (SecondGenerationDxsEntries dxsentriesConfigurable : dxsEntriesContainerConfigurable.dxsEntries) {
            channelPostsConfigurable[channelPostsCounterConfigurable] = dxsentriesConfigurable.getName();
            channelPostsCounterConfigurable++;
        }

        // Create and update actual values for non-configurable items
        if (inverterConfig.invertertype.equals("withoutBattery")) {
            Object[] channelPostsTemp1 = channelPostsTemp.toArray();
            String[] channelPostsAll = Arrays.copyOf(channelPostsTemp1, channelPostsTemp1.length, String[].class);

            channelConfigsAll = combineChannelConfigLists(channelConfigs, channelConfigsExt, channelConfigsExtExt);

            int channelValuesCounterAll = 0;
            for (SecondGenerationChannelConfiguration cConfig : channelConfigsAll) {
                String channel = cConfig.id;
                State state = getState(channelPostsAll[channelValuesCounterAll], cConfig.unit);

                // Update the channels
                if (state != null) {
                    updateState(channel, state);
                }
                channelValuesCounterAll++;
            }
        }

        // Create and update actual values for all items
        if (inverterConfig.invertertype.equals("withBattery")) {

            // Part for non-configurable updating
            Object[] channelPostsTemp1 = channelPostsTemp.toArray();
            String[] channelPostsAll = Arrays.copyOf(channelPostsTemp1, channelPostsTemp1.length, String[].class);

            channelConfigsAll = combineChannelConfigLists(channelConfigs, channelConfigsExt, channelConfigsExtExt);
            // Update the non-configurable channels
            int channelValuesCounterAll = 0;
            for (SecondGenerationChannelConfiguration cConfig : channelConfigsAll) {
                String channel = cConfig.id;
                State state = getState(channelPostsAll[channelValuesCounterAll], cConfig.unit);

                if (state != null) {
                    updateState(channel, state);
                }
                channelValuesCounterAll++;
            }

            // Update the configurable channels
            int channelValuesCounterConfigurable = 0;
            for (SecondGenerationChannelConfiguration cConfig : channelConfigsConfigurable) {
                String channel = cConfig.id;
                String value = channelPostsConfigurable[channelValuesCounterConfigurable];
                int dxsEntriesCheckCounter = 3;

                if (cConfig.dxsEntries.equals("33556484")) {
                    dxsEntriesCheckCounter = 1;
                }
                if (cConfig.dxsEntries.equals("33556482")) {
                    dxsEntriesCheckCounter = 2;
                }
                switch (dxsEntriesCheckCounter) {
                    case 1:
                        if (value.equals("false")) {
                            updateState(channel, OnOffType.OFF);
                        }
                        if (value.equals("true")) {
                            updateState(channel, OnOffType.ON);
                        }
                        channelValuesCounterConfigurable++;
                        break;
                    case 2:
                        if (value.equals("false")) {
                            State stateFalse = new StringType("0");
                            updateState(channel, stateFalse);
                        }
                        if (value.equals("true")) {
                            State stateTrue = new StringType("1");
                            updateState(channel, stateTrue);
                        }
                        channelValuesCounterConfigurable++;
                        break;
                    case 3:
                        State stateOther = getState(channelPostsConfigurable[channelValuesCounterConfigurable],
                                cConfig.unit);
                        updateState(channel, stateOther);
                        channelValuesCounterConfigurable++;
                        break;
                }
            }
        }
    }

    // Help method of handleCommand to with SecondGenerationConfigurationHandler.executeConfigurationChanges method send
    // configuration changes.
    public final void preSetExecuteConfigurationChanges(HttpClient httpClient, String url, String username,
            String password, String dxsEntriesConf, String valueConfiguration) {
        try {
            SecondGenerationConfigurationHandler.executeConfigurationChanges(httpClient, url, username, password,
                    dxsEntriesConf, valueConfiguration);
        } catch (Exception handleCommandException) {
            logger.debug("Handle command for {} on channel {}: {}: {}: {}: {}", thing.getUID(), httpClient, url,
                    dxsEntriesConf, valueConfiguration, handleCommandException.getMessage());
        }
    }

    // Method callURL connect to inverter for value scraping
    public final String callURL(String dxsEntriesCall)
            throws InterruptedException, ExecutionException, TimeoutException {
        String jsonDxsResponse = httpClient.GET(dxsEntriesCall).getContentAsString();
        return jsonDxsResponse;
    }

    // Method getState is used for non-configurable values
    private State getState(String value, @Nullable Unit<?> unit) {
        if (unit == null) {
            return new StringType(value);
        } else {
            try {
                return new QuantityType<>(new BigDecimal(value), unit);
            } catch (NumberFormatException getStateException) {
                logger.debug("Error parsing value '{}: {}'", value, getStateException.getMessage());
                return UnDefType.UNDEF;
            }
        }
    }

    // Method stringToSeconds transform given time in 00:16 syntax to seconds syntax
    public static long stringToSeconds(String stringTime) {
        long secondsMin = Long.parseLong(stringTime.substring(3, 5)) * 60;
        long secondsHrs = Long.parseLong(stringTime.substring(0, 2)) * 3600;
        return secondsMin + secondsHrs;
    }

    // Method to concatenate channelConfigs Lists to one List
    public final List<SecondGenerationChannelConfiguration> combineChannelConfigLists(
            @SuppressWarnings("unchecked") List<SecondGenerationChannelConfiguration>... args) {
        List<SecondGenerationChannelConfiguration> combinedChannelConfigLists = new ArrayList<>();
        for (List<SecondGenerationChannelConfiguration> list : args) {
            for (SecondGenerationChannelConfiguration i : list) {
                combinedChannelConfigLists.add(i);
            }
        }
        return combinedChannelConfigLists;
    }
}
