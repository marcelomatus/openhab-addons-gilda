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
package org.openhab.binding.folderwatcher.internal.handler;

import static org.openhab.binding.folderwatcher.internal.FolderWatcherBindingConstants.CHANNEL_LOCALFILENAME;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.config.core.ConfigConstants;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.folderwatcher.internal.common.WatcherCommon;
import org.openhab.binding.folderwatcher.internal.config.FolderLocalWatcherConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The {@link FolderLocalWatcherHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Alexandr Salamatov - Initial contribution
 */
@NonNullByDefault
public class FolderLocalWatcherHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(FolderLocalWatcherHandler.class);
    private @Nullable FolderLocalWatcherConfiguration config;
    private @Nullable File currentLocalListingFile;
    private @Nullable ScheduledFuture<?> executionJob;
    private List<String> currentLocalListing = new ArrayList<>();
    private List<String> previousLocalListing = new ArrayList<>();

    public FolderLocalWatcherHandler(Thing thing) {
        super(thing);
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void initialize() {
        config = getConfigAs(FolderLocalWatcherConfiguration.class);
        updateStatus(ThingStatus.UNKNOWN);

        if (!Files.isDirectory(Paths.get(config.localDir))) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, "Local directory is not valid");
            return;
        }

        currentLocalListingFile = new File(ConfigConstants.getUserDataFolder() + File.separator + "FolderWatcher"
                + File.separator + thing.getUID().getAsString().replace(':', '_') + ".data");
        try {
            previousLocalListing = WatcherCommon.initStorage(currentLocalListingFile);
        } catch (IOException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
            logger.debug("Can't write file {}: {}", currentLocalListingFile, e.getMessage());
            return;
        }

        if (config.pollIntervalLocal > 0) {
            updateStatus(ThingStatus.ONLINE);
            executionJob = scheduler.scheduleWithFixedDelay(this::refreshFolderInformation, config.pollIntervalLocal,
                    config.pollIntervalLocal, TimeUnit.SECONDS);
        } else {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR,
                    "Polling interval can't be null or negative");
        }

    }

    @Override
    public void dispose() {
        if (executionJob != null) {
            executionJob.cancel(true);
        }
    }

    @SuppressWarnings("null")
    private void refreshFolderInformation() {
        final String RootDir = config.localDir;

        try {
            currentLocalListing.clear();

            Files.walkFileTree(Paths.get(RootDir), new FileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, @Nullable BasicFileAttributes attrs)
                        throws IOException {
                    if (dir.compareTo(Paths.get(RootDir)) != 0 && !config.listRecursiveLocal) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, @Nullable BasicFileAttributes attrs) throws IOException {
                    if (Files.isHidden(file) && !config.listHiddenLocal) {
                        return FileVisitResult.CONTINUE;
                    }
                    currentLocalListing.add(file.toAbsolutePath().toString());
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFileFailed(Path file, @Nullable IOException exc) throws IOException {

                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult postVisitDirectory(Path dir, @Nullable IOException exc) throws IOException {

                    return FileVisitResult.CONTINUE;
                }
            });

            List<String> diffLocalListing = new ArrayList<>(currentLocalListing);
            diffLocalListing.removeAll(previousLocalListing);
            diffLocalListing.forEach(file -> triggerChannel(CHANNEL_LOCALFILENAME, file));
            if (!diffLocalListing.isEmpty()) {
                WatcherCommon.saveNewListing(diffLocalListing, currentLocalListingFile);
            }
            previousLocalListing = new ArrayList<>(currentLocalListing);
        } catch (IOException e) {
            logger.debug("File manipulation error: {}", e.getMessage());
        }
    }

}
