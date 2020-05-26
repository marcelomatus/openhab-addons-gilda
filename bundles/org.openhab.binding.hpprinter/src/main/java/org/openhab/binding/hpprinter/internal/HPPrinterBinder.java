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
package org.openhab.binding.hpprinter.internal;

import static org.openhab.binding.hpprinter.internal.HPPrinterBindingConstants.*;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.smarthome.core.library.CoreItemFactory;
import org.eclipse.smarthome.core.library.types.DecimalType;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.types.StringType;
import org.eclipse.smarthome.core.library.unit.MetricPrefix;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.ThingUID;
import org.eclipse.smarthome.core.thing.binding.builder.ChannelBuilder;
import org.eclipse.smarthome.core.types.State;
import org.openhab.binding.hpprinter.internal.api.HPFeatures;
import org.openhab.binding.hpprinter.internal.api.HPProperties;
import org.openhab.binding.hpprinter.internal.api.HPScannerStatus;
import org.openhab.binding.hpprinter.internal.api.HPScannerStatusFeatures;
import org.openhab.binding.hpprinter.internal.api.HPServerResult;
import org.openhab.binding.hpprinter.internal.api.HPServerResult.RequestStatus;
import org.openhab.binding.hpprinter.internal.api.HPStatus;
import org.openhab.binding.hpprinter.internal.api.HPProductUsageFeatures;
import org.openhab.binding.hpprinter.internal.api.HPUsage;
import org.openhab.binding.hpprinter.internal.api.HPWebServerClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link HPPrinterBinder} connects the binding to the Web Server Client
 * classes.
 *
 * @author Stewart Cossey - Initial contribution
 */
@NonNullByDefault
public class HPPrinterBinder {
    private final Logger logger = LoggerFactory.getLogger(HPPrinterBinder.class);

    private static final int OFFLINE_CHECK_INTERVAL = 15;

    private HPPrinterHandler handler;
    private ScheduledExecutorService scheduler;

    private final int statusCheckInterval;
    private final int usageCheckInterval;
    private final HPWebServerClient printerClient;

    private @Nullable ScheduledFuture<?> statusScheduler;
    private @Nullable ScheduledFuture<?> scannerStatusScheduler;
    private @Nullable ScheduledFuture<?> usageScheduler;
    private @Nullable ScheduledFuture<?> offlineScheduler;

    /**
     * Creates a new HP Printer Binder object
     * 
     * @param handler {HPPrinterBinderEvent} The Event handler for the binder.
     * @param httpClient {HttpClient} The HttpClient object to use to perform HTTP
     *            requests.
     * @param scheduler {ScheduledExecutorService} The scheduler service object.
     * @param config {HPPrinterConfiguration} The configuration object.
     */
    public HPPrinterBinder(HPPrinterHandler handler, HttpClient httpClient,
                    ScheduledExecutorService scheduler,
            HPPrinterConfiguration config) {
        this.handler = handler;
        this.scheduler = scheduler;
        usageCheckInterval = config.usageInterval;
        statusCheckInterval = config.statusInterval;
        String ipAddress = config.ipAddress;
        if (ipAddress == null) {
            throw new IllegalStateException("ip-address should have been validated already and may not be empty.");
        }
        printerClient = new HPWebServerClient(httpClient, ipAddress);

    }

    public void retrieveProperties() {
        HPServerResult<HPProperties> result = printerClient.getProperties();

        if (result.getStatus() == RequestStatus.SUCCESS) {
            handler.updateProperties(result.getData().getProperties());
        }
    }

    public synchronized void channelsChanged() {
        logger.trace("Channels have been changed");
        close();
        open();
    }

    /**
     * Dynamically add channels to the Thing based on the Embedded Web Server Usage
     * Feed
     */
    public void dynamicallyAddChannels(ThingUID thingUid) {
        logger.debug("Building dynamic channels based on printer");

        HPServerResult<HPFeatures> featureResult = printerClient.getProductUsageFeatures();
        if (featureResult.getStatus() == RequestStatus.SUCCESS) {
            final List<Channel> channels = new ArrayList<>();

            HPFeatures features = featureResult.getData();

            if (features.getProductStatusSupported()) {
                channels.add(ChannelBuilder
                    .create(new ChannelUID(thingUid, CGROUP_STATUS, CHANNEL_STATUS), CoreItemFactory.STRING)
                    .withLabel("Status").withDescription("Printer status")
                    .withType(chanTypeStatus).build());

                channels.add(ChannelBuilder
                    .create(new ChannelUID(thingUid, CGROUP_STATUS, CHANNEL_TRAYEMPTYOROPEN), CoreItemFactory.SWITCH)
                    .withLabel("Tray Empty/Open").withDescription("The tray is empty or open")
                    .withType(chanTypeReadSwitch).build());  
            }

            if (features.getScannerStatusSupported()) {
                HPServerResult<HPScannerStatusFeatures> result = printerClient.getScannerFeatures();
                if (result.getStatus() == RequestStatus.SUCCESS) {
                    HPScannerStatusFeatures feature = result.getData();

                    if (feature.hasStatus()) {
                        channels.add(ChannelBuilder
                            .create(new ChannelUID(thingUid, CGROUP_STATUS, CHANNEL_SCANNER_STATUS), CoreItemFactory.STRING)
                            .withLabel("Scanner").withDescription("Scanner status")
                            .withType(chanTypeStatus).build());
                    }

                    if (feature.hasAdf()) {
                        channels.add(ChannelBuilder
                            .create(new ChannelUID(thingUid, CGROUP_STATUS, CHANNEL_SCANNER_ADFLOADED), CoreItemFactory.SWITCH)
                            .withLabel("ADF Loaded").withDescription("The automatic document feeder is loaded")
                            .withType(chanTypeReadSwitch).build());   
                    }
                }
            }

            if (features.getProductUsageSupported()) {
                HPServerResult<HPProductUsageFeatures> result = printerClient.getProductFeatures();
                if (result.getStatus() == RequestStatus.SUCCESS) {
                    HPProductUsageFeatures feature = result.getData();
        
                    if (feature.hasCumulativeMarking()) {
                        channels.add(ChannelBuilder
                                .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_BLACK_MARKING), HPPrinterBindingConstants.ITEM_TYPE_CUMLMARK)
                                .withLabel("Black Marking Used").withDescription("The amount of Black Marking used")
                                .withType(chanTypeMarking).build());
                    }
        
                    switch (feature.getType()) {
                        case SINGLECOLOR:
                            if (feature.hasCumulativeMarking()) {
                                channels.add(ChannelBuilder
                                        .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_COLOR_MARKING),
                                        HPPrinterBindingConstants.ITEM_TYPE_CUMLMARK)
                                        .withLabel("Colour Marking Used").withDescription("The amount of Colour Marking used")
                                        .withType(chanTypeMarking).build());
                            }
        
                            channels.add(ChannelBuilder
                                    .create(new ChannelUID(thingUid, CGROUP_INK, CHANNEL_COLOR_LEVEL), HPPrinterBindingConstants.ITEM_TYPE_INK)
                                    .withLabel("Color Level").withDescription("Shows the amount of Colour Ink/Toner remaining")
                                    .withType(chanTypeInkLevel).build());
        
                            channels.add(ChannelBuilder
                                    .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_TOTAL_COLORPAGES),
                                            CoreItemFactory.NUMBER)
                                    .withLabel("Total Colour Pages").withDescription("The amount of colour pages printed")
                                    .withType(chanTypeTotals).build());
        
                            channels.add(ChannelBuilder
                                    .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_TOTAL_MONOPAGES),
                                            CoreItemFactory.NUMBER)
                                    .withLabel("Total Monochrome Pages")
                                    .withDescription("The amount of monochrome pages printed")
                                    .withType(chanTypeTotals).build());
        

                            if (feature.hasPagesRemaining()) {
                                channels.add(ChannelBuilder
                                        .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_BLACK_PAGES_REMAINING),
                                                CoreItemFactory.NUMBER)
                                        .withLabel("Black Pages Remaining")
                                        .withDescription("Estimated Black pages remaining")
                                        .withType(chanTypeTotalsAdvanced).build());

                                channels.add(ChannelBuilder
                                        .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_COLOR_PAGES_REMAINING),
                                                CoreItemFactory.NUMBER)
                                        .withLabel("Colour Pages Remaining")
                                        .withDescription("Estimated Colour pages remaining")
                                        .withType(chanTypeTotalsAdvanced).build());
                            }

                            break;
        
                        case MULTICOLOR:
                            if (feature.hasCumulativeMarking()) {
                                channels.add(ChannelBuilder
                                        .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_CYAN_MARKING),
                                        HPPrinterBindingConstants.ITEM_TYPE_CUMLMARK)
                                        .withLabel("Cyan Marking Used").withDescription("The amount of Cyan Marking used")
                                        .withType(chanTypeMarking).build());
        
                                channels.add(ChannelBuilder
                                        .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_MAGENTA_MARKING),
                                        HPPrinterBindingConstants.ITEM_TYPE_CUMLMARK)
                                        .withLabel("Magenta Marking Used").withDescription("The amount of Magenta Marking used")
                                        .withType(chanTypeMarking).build());
        
                                channels.add(ChannelBuilder
                                        .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_YELLOW_MARKING),
                                        HPPrinterBindingConstants.ITEM_TYPE_CUMLMARK)
                                        .withLabel("Yellow Marking Used").withDescription("The amount of Yellow Marking used")
                                        .withType(chanTypeMarking).build());
                            }

                            if (feature.hasPagesRemaining()) {
                                channels.add(ChannelBuilder
                                        .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_BLACK_PAGES_REMAINING),
                                                CoreItemFactory.NUMBER)
                                        .withLabel("Black Pages Remaining")
                                        .withDescription("Estimated Black pages remaining")
                                        .withType(chanTypeTotalsAdvanced).build());

                                channels.add(ChannelBuilder
                                        .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_CYAN_PAGES_REMAINING),
                                                CoreItemFactory.NUMBER)
                                        .withLabel("Cyan Pages Remaining")
                                        .withDescription("Estimated Cyan pages remaining")
                                        .withType(chanTypeTotalsAdvanced).build());

                                channels.add(ChannelBuilder
                                        .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_MAGENTA_PAGES_REMAINING),
                                                CoreItemFactory.NUMBER)
                                        .withLabel("Magenta Pages Remaining")
                                        .withDescription("Estimated Magenta pages remaining")
                                        .withType(chanTypeTotalsAdvanced).build());
                                        
                                channels.add(ChannelBuilder
                                        .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_YELLOW_PAGES_REMAINING),
                                                CoreItemFactory.NUMBER)
                                        .withLabel("Yellow Pages Remaining")
                                        .withDescription("Estimated Yellow pages remaining")
                                        .withType(chanTypeTotalsAdvanced).build());
                            }
        
                            channels.add(ChannelBuilder
                                    .create(new ChannelUID(thingUid, CGROUP_INK, CHANNEL_CYAN_LEVEL), HPPrinterBindingConstants.ITEM_TYPE_INK)
                                    .withLabel("Cyan Level").withDescription("Shows the amount of Cyan Ink/Toner remaining")
                                    .withType(chanTypeInkLevel).build());
        
                            channels.add(ChannelBuilder
                                    .create(new ChannelUID(thingUid, CGROUP_INK, CHANNEL_MAGENTA_LEVEL), HPPrinterBindingConstants.ITEM_TYPE_INK)
                                    .withLabel("Magenta Level")
                                    .withDescription("Shows the amount of Magenta Ink/Toner remaining")
                                    .withType(chanTypeInkLevel).build());
        
                            channels.add(ChannelBuilder
                                    .create(new ChannelUID(thingUid, CGROUP_INK, CHANNEL_YELLOW_LEVEL), HPPrinterBindingConstants.ITEM_TYPE_INK)
                                    .withLabel("Yellow Level").withDescription("Shows the amount of Yellow Ink/Toner remaining")
                                    .withType(chanTypeInkLevel).build());
        
                            channels.add(ChannelBuilder
                                    .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_TOTAL_COLORPAGES),
                                            CoreItemFactory.NUMBER)
                                    .withLabel("Total Colour Pages").withDescription("The amount of colour pages printed")
                                    .withType(chanTypeTotals).build());
        
                            channels.add(ChannelBuilder
                                    .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_TOTAL_MONOPAGES),
                                            CoreItemFactory.NUMBER)
                                    .withLabel("Total Monochrome Pages")
                                    .withDescription("The amount of monochrome pages printed")
                                    .withType(chanTypeTotals).build());
        
                            break;
        
                        default:
                            if (feature.hasPagesRemaining()) {
                                channels.add(ChannelBuilder
                                        .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_BLACK_PAGES_REMAINING),
                                                CoreItemFactory.NUMBER)
                                        .withLabel("Black Pages Remaining")
                                        .withDescription("Estimated Black pages remaining")
                                        .withType(chanTypeTotalsAdvanced).build());
                            }
                    }
        
                    if (feature.hasJamEvents()) {
                        channels.add(ChannelBuilder
                                .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_JAM_EVENTS), CoreItemFactory.NUMBER)
                                .withLabel("Paper Jams").withDescription("The amount of times the paper has jammed")
                                .withType(chanTypeTotals).build());
                    }
        
                    if (feature.hasMispickEvents()) {
                        channels.add(ChannelBuilder
                                .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_MISPICK_EVENTS), CoreItemFactory.NUMBER)
                                .withLabel("Mispicks")
                                .withDescription("The amount of times the paper failed to feed into the printer")
                                .withType(chanTypeTotalsAdvanced).build());
                    }
        
                    if (feature.hasSubscriptionCount()) {
                        channels.add(ChannelBuilder
                                .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_SUBSCRIPTION), CoreItemFactory.NUMBER)
                                .withLabel("Subscription Count")
                                .withDescription("The amount of times an item has been printed in subscription")
                                .withType(chanTypeTotalsAdvanced).build());
                    }
        
                    if (feature.hasTotalFrontPanelCancelPresses()) {
                        channels.add(ChannelBuilder
                                .create(new ChannelUID(thingUid, CGROUP_USAGE, CHANNEL_FRONT_PANEL_CANCEL),
                                        CoreItemFactory.NUMBER)
                                .withLabel("Front Panel Cancel Count")
                                .withDescription("The amount of times a print has been cancelled from the Front Panel")
                                .withType(chanTypeTotalsAdvanced).build());
                    }
        
                    //Scanner
                    if (feature.hasScannerADF()) {
                        channels.add(ChannelBuilder
                                .create(new ChannelUID(thingUid, CGROUP_SCANNER, CHANNEL_TOTAL_ADF), CoreItemFactory.NUMBER)
                                .withLabel("Document Feeder Count").withDescription("Times scanned via the Document Feeder")
                                .withType(chanTypeTotals).build());
                    }
        
                    if (feature.hasScannerFlatbed()) {
                        channels.add(ChannelBuilder
                                .create(new ChannelUID(thingUid, CGROUP_SCANNER, CHANNEL_TOTAL_FLATBED), CoreItemFactory.NUMBER)
                                .withLabel("Flatbed Count").withDescription("Times scanned via the Flatbed/Glass")
                                .withType(chanTypeTotals).build());
                    }
        
                    if (feature.hasScanToEmail()) {
                        channels.add(ChannelBuilder
                                .create(new ChannelUID(thingUid, CGROUP_SCANNER, CHANNEL_TOTAL_TOEMAIL), CoreItemFactory.NUMBER)
                                .withLabel("Scan to Email Count").withDescription("Times scanned using Scan to Email")
                                .withType(chanTypeTotalsAdvanced).build());
                    }
        
                    if (feature.hasScanToFolder()) {
                        channels.add(ChannelBuilder
                                .create(new ChannelUID(thingUid, CGROUP_SCANNER, CHANNEL_TOTAL_TOFOLDER), CoreItemFactory.NUMBER)
                                .withLabel("Scan to Folder Count").withDescription("Times scanned using Scan to Folder")
                                .withType(chanTypeTotalsAdvanced).build());
                    }
        
                    if (feature.hasScanToHost()) {
                        channels.add(ChannelBuilder
                                .create(new ChannelUID(thingUid, CGROUP_SCANNER, CHANNEL_TOTAL_TOHOST), CoreItemFactory.NUMBER)
                                .withLabel("Scan to Host Count").withDescription("Times scanned using Scan to Host Device")
                                .withType(chanTypeTotalsAdvanced).build());
                    }
        
                    //App Usage
                    if (feature.hasPrintApplication()) {
                        channels.add(ChannelBuilder
                                .create(new ChannelUID(thingUid, CGROUP_APP, CHANNEL_TOTAL_WIN), CoreItemFactory.NUMBER)
                                .withLabel("Windows")
                                .withType(chanTypeTotalsAdvanced).build());
        
                        channels.add(ChannelBuilder
                                .create(new ChannelUID(thingUid, CGROUP_APP, CHANNEL_TOTAL_ANDROID), CoreItemFactory.NUMBER)
                                .withLabel("Android")
                                .withType(chanTypeTotalsAdvanced).build());
        
                        channels.add(ChannelBuilder
                                .create(new ChannelUID(thingUid, CGROUP_APP, CHANNEL_TOTAL_IOS), CoreItemFactory.NUMBER)
                                .withLabel("iOS")
                                .withType(chanTypeTotalsAdvanced).build());
        
                        channels.add(ChannelBuilder
                                .create(new ChannelUID(thingUid, CGROUP_APP, CHANNEL_TOTAL_OSX), CoreItemFactory.NUMBER)
                                .withLabel("OSX")
                                .withType(chanTypeTotalsAdvanced).build());
        
                        channels.add(ChannelBuilder
                                .create(new ChannelUID(thingUid, CGROUP_APP, CHANNEL_TOTAL_SAMSUNG), CoreItemFactory.NUMBER)
                                .withLabel("Samsung")
                                .withType(chanTypeTotalsAdvanced).build());
        
                        if (feature.hasPrintApplicationChrome()) {
                            channels.add(ChannelBuilder
                                    .create(new ChannelUID(thingUid, CGROUP_APP, CHANNEL_TOTAL_CHROME), CoreItemFactory.NUMBER)
                                    .withLabel("Chrome")
                                    .withType(chanTypeTotalsAdvanced).build());
                        }
                    }
        
        
        
                    
                }
            }

            handler.binderAddChannels(channels);
        }
    }

    /**
     * Opens the connection to the Embedded Web Server
     */
    public void open() {
        runOfflineScheduler();
    }

    /**
     * Close the connection to the Embedded Web Server
     */
    public void close() {
        stopBackgroundSchedules();

        if (offlineScheduler != null) {
            offlineScheduler.cancel(true);
            offlineScheduler = null;
        }
    }

    /**
     * The device has gone offline
     */
    public void goneOffline() {
        handler.updateStatus(ThingStatus.OFFLINE);

        close();
        runOfflineScheduler();
    }

    private void runOfflineScheduler() {
        offlineScheduler = scheduler.scheduleWithFixedDelay(this::checkOnline, 0, OFFLINE_CHECK_INTERVAL,
                TimeUnit.SECONDS);
    }

    private synchronized void stopBackgroundSchedules() {
        logger.debug("Stopping Interval Refreshes");
        ScheduledFuture<?> localUsageScheduler = usageScheduler;
        if (localUsageScheduler != null) {
            localUsageScheduler.cancel(true);
        }

        ScheduledFuture<?> localStatusScheduler = statusScheduler;
        if (localStatusScheduler != null) {
            localStatusScheduler.cancel(true);
        }

        ScheduledFuture<?> localScannerStatusScheduler = scannerStatusScheduler;
        if (localScannerStatusScheduler != null) {
            localScannerStatusScheduler.cancel(true);
        }
    }

    private synchronized void startBackgroundSchedules() {
        stopBackgroundSchedules();
        logger.debug("Starting Interval Refreshes");

        usageScheduler = scheduler.scheduleWithFixedDelay(this::checkUsage, 0, usageCheckInterval, TimeUnit.SECONDS);

        //Only schedule below items when there channels are linked
        if (handler.areStatusChannelsLinked(new String[] {CHANNEL_STATUS, CHANNEL_TRAYEMPTYOROPEN})) {
            statusScheduler = scheduler.scheduleWithFixedDelay(this::checkStatus, 0, statusCheckInterval, TimeUnit.SECONDS);
        }

        if (handler.areStatusChannelsLinked(new String[] {CHANNEL_SCANNER_STATUS, CHANNEL_SCANNER_ADFLOADED})) {
            scannerStatusScheduler = scheduler.scheduleWithFixedDelay(this::checkScannerStatus, 0, statusCheckInterval, TimeUnit.SECONDS);
        }
    }

    private void checkScannerStatus() {
        HPServerResult<HPScannerStatus> result = printerClient.getScannerStatus();

        if (result.getStatus() == RequestStatus.SUCCESS) {
            handler.updateState(CGROUP_STATUS, CHANNEL_SCANNER_STATUS, new StringType(result.getData().getScannerStatus()));
            handler.updateState(CGROUP_STATUS, CHANNEL_SCANNER_ADFLOADED, convertToOnOffType(result.getData().getAdfLoaded()));
        } else {
            goneOffline();
        }
    }

    private void checkStatus() {
        HPServerResult<HPStatus> result = printerClient.getStatus();

        if (result.getStatus() == RequestStatus.SUCCESS) {
            handler.updateState(CGROUP_STATUS, CHANNEL_STATUS, new StringType(result.getData().getPrinterStatus()));
            handler.updateState(CGROUP_STATUS, CHANNEL_TRAYEMPTYOROPEN, convertToOnOffType(result.getData().getTrayEmptyOrOpen()));
        } else {
            goneOffline();
        }
    }

    private static State convertToOnOffType(Boolean bool) {
        if (bool) {
            return OnOffType.ON;
        } else {
            return OnOffType.OFF;
        }
    }

    private void checkUsage() {
        HPServerResult<HPUsage> result = printerClient.getUsage();

        if (result.getStatus() == RequestStatus.SUCCESS) {
            // Inks
            handler.updateState(CGROUP_INK, CHANNEL_BLACK_LEVEL,
                    new QuantityType<>(result.getData().getInkBlack(), SmartHomeUnits.PERCENT));
            handler.updateState(CGROUP_INK, CHANNEL_COLOR_LEVEL,
                    new QuantityType<>(result.getData().getInkColor(), SmartHomeUnits.PERCENT));
            handler.updateState(CGROUP_INK, CHANNEL_CYAN_LEVEL,
                    new QuantityType<>(result.getData().getInkCyan(), SmartHomeUnits.PERCENT));
            handler.updateState(CGROUP_INK, CHANNEL_MAGENTA_LEVEL,
                    new QuantityType<>(result.getData().getInkMagenta(), SmartHomeUnits.PERCENT));
            handler.updateState(CGROUP_INK, CHANNEL_YELLOW_LEVEL,
                    new QuantityType<>(result.getData().getInkYellow(), SmartHomeUnits.PERCENT));

            handler.updateState(CGROUP_USAGE, CHANNEL_JAM_EVENTS, new DecimalType(result.getData().getJamEvents()));
            handler.updateState(CGROUP_USAGE, CHANNEL_SUBSCRIPTION,
                    new DecimalType(result.getData().getTotalSubscriptionImpressions()));
            handler.updateState(CGROUP_USAGE, CHANNEL_TOTAL_COLORPAGES,
                    new DecimalType(result.getData().getTotalColorImpressions()));
            handler.updateState(CGROUP_USAGE, CHANNEL_TOTAL_MONOPAGES,
                    new DecimalType(result.getData().getTotalMonochromeImpressions()));
            handler.updateState(CGROUP_USAGE, CHANNEL_TOTAL_PAGES,
                    new DecimalType(result.getData().getTotalImpressions()));
            handler.updateState(CGROUP_USAGE, CHANNEL_MISPICK_EVENTS,
                    new DecimalType(result.getData().getMispickEvents()));
            handler.updateState(CGROUP_USAGE, CHANNEL_FRONT_PANEL_CANCEL,
                    new DecimalType(result.getData().getFrontPanelCancelCount()));

            handler.updateState(CGROUP_USAGE, CHANNEL_BLACK_MARKING, new QuantityType<>(
                    result.getData().getInkBlackMarking(), MetricPrefix.MILLI(SmartHomeUnits.LITRE)));
            handler.updateState(CGROUP_USAGE, CHANNEL_COLOR_MARKING, new QuantityType<>(
                    result.getData().getInkColorMarking(), MetricPrefix.MILLI(SmartHomeUnits.LITRE)));
            handler.updateState(CGROUP_USAGE, CHANNEL_CYAN_MARKING,
                    new QuantityType<>(result.getData().getInkCyanMarking(), MetricPrefix.MILLI(SmartHomeUnits.LITRE)));
            handler.updateState(CGROUP_USAGE, CHANNEL_MAGENTA_MARKING, new QuantityType<>(
                    result.getData().getInkMagentaMarking(), MetricPrefix.MILLI(SmartHomeUnits.LITRE)));
            handler.updateState(CGROUP_USAGE, CHANNEL_YELLOW_MARKING, new QuantityType<>(
                    result.getData().getInkYellowMarking(), MetricPrefix.MILLI(SmartHomeUnits.LITRE)));

            handler.updateState(CGROUP_USAGE, CHANNEL_BLACK_PAGES_REMAINING, 
                    new DecimalType(result.getData().getInkBlackPagesRemaining()));

            handler.updateState(CGROUP_USAGE, CHANNEL_COLOR_PAGES_REMAINING, 
                    new DecimalType(result.getData().getInkColorPagesRemaining()));

            handler.updateState(CGROUP_USAGE, CHANNEL_CYAN_PAGES_REMAINING, 
                    new DecimalType(result.getData().getInkCyanPagesRemaining()));

            handler.updateState(CGROUP_USAGE, CHANNEL_MAGENTA_PAGES_REMAINING, 
                    new DecimalType(result.getData().getInkMagentaPagesRemaining()));

            handler.updateState(CGROUP_USAGE, CHANNEL_YELLOW_PAGES_REMAINING, 
                    new DecimalType(result.getData().getInkYellowPagesRemaining()));

            //Scanner
            handler.updateState(CGROUP_SCANNER, CHANNEL_TOTAL_ADF, new DecimalType(
                    result.getData().getScanAdfCount()));

            handler.updateState(CGROUP_SCANNER, CHANNEL_TOTAL_FLATBED, new DecimalType(
                result.getData().getScanFlatbedCount()));

            handler.updateState(CGROUP_SCANNER, CHANNEL_TOTAL_TOEMAIL, new DecimalType(
                result.getData().getScanToEmailCount()));

            handler.updateState(CGROUP_SCANNER, CHANNEL_TOTAL_TOFOLDER, new DecimalType(
                result.getData().getScanToFolderCount()));

            handler.updateState(CGROUP_SCANNER, CHANNEL_TOTAL_TOHOST, new DecimalType(
                result.getData().getScanToHostCount()));

            //App Usage
            handler.updateState(CGROUP_APP, CHANNEL_TOTAL_WIN, new DecimalType(result.getData().getAppWindowsCount()));
            handler.updateState(CGROUP_APP, CHANNEL_TOTAL_OSX, new DecimalType(result.getData().getAppOSXCount()));
            handler.updateState(CGROUP_APP, CHANNEL_TOTAL_IOS, new DecimalType(result.getData().getAppIosCount()));
            handler.updateState(CGROUP_APP, CHANNEL_TOTAL_ANDROID, new DecimalType(result.getData().getAppAndroidCount()));
            handler.updateState(CGROUP_APP, CHANNEL_TOTAL_SAMSUNG, new DecimalType(result.getData().getAppSamsungCount()));
            handler.updateState(CGROUP_APP, CHANNEL_TOTAL_CHROME, new DecimalType(result.getData().getAppChromeCount()));
        } else {
            goneOffline();
        }
    }

    private void goneOnline() {
        handler.updateStatus(ThingStatus.ONLINE);

        if (offlineScheduler != null) {
            offlineScheduler.cancel(true);
            offlineScheduler = null;
        }
        retrieveProperties();

        startBackgroundSchedules();
    }

    private void checkOnline() {
        HPServerResult<HPStatus> result = printerClient.getStatus();

        if (result.getStatus() == RequestStatus.SUCCESS) {
            goneOnline();
        } else if (result.getStatus() == RequestStatus.TIMEOUT) {
            handler.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR, result.getErrorMessage());
        } else {
            handler.updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.NONE, result.getErrorMessage());
        }
    }
}
