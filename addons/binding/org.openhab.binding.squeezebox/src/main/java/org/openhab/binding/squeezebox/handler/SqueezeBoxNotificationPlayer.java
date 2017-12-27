/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.squeezebox.handler;

import java.io.Closeable;

import org.eclipse.smarthome.core.library.types.StringType;
import org.openhab.binding.squeezebox.internal.utils.SqueezeBoxTimeoutException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/***
 * Utility class to play a notification message. The message is added
 * to the playlist, played and the previous state of the playlist and the
 * player is restored.
 *
 * @author Mark Hilbush - Initial Contribution
 * @author Patrik Gfeller - Utility class added reduce complexity and length of SqueezeBoxPlayerHandler.java
 *
 */
class SqueezeBoxNotificationPlayer implements Closeable {
    // An exception is thrown if we do not receive an acknowledge
    // for a volume set command in the given amount of time [s].
    final int volumeCommandTimeout = 4;

    // We expect the media server to acknowledge a playlist command.
    // An exception is thrown if the playlist command was not processed
    // after the defined amount in [s]
    final int playlistCommandTimeout = 5;

    // Max length of the message in [s]. An exception is thrown if we did not
    // receive a "stop" message from the media server.
    final int notificationLengthTimeout = 90;

    private Logger logger = LoggerFactory.getLogger(SqueezeBoxNotificationPlayer.class);
    private SqueezeBoxPlayerState playerState;
    private SqueezeBoxPlayerHandler squeezeBoxPlayerHandler;
    private SqueezeBoxServerHandler squeezeBoxServerHandler;
    private StringType uri;
    private String mac;

    boolean playlistModified;

    private int notificationMessagePlaylistsIndex;

    SqueezeBoxNotificationPlayer(SqueezeBoxPlayerHandler squeezeBoxPlayerHandler,
            SqueezeBoxServerHandler squeezeBoxServerHandler, StringType uri) {
        this.squeezeBoxPlayerHandler = squeezeBoxPlayerHandler;
        this.squeezeBoxServerHandler = squeezeBoxServerHandler;
        this.mac = squeezeBoxPlayerHandler.getMac();
        this.uri = uri;
        this.playerState = new SqueezeBoxPlayerState(squeezeBoxPlayerHandler);
    }

    void play() throws InterruptedException, SqueezeBoxTimeoutException {
        if (squeezeBoxServerHandler == null) {
            logger.warn("Server handler is null");
            return;
        }

        setupPlayerForNotification();

        try {
            addNotificationMessageToPlaylist();
            playNotification();
        } finally {
            if (playlistModified) {
                // Mute the player to prevent any noise during the transition
                // to previous state.
                setVolume(0);
                removeNotificationMessageFromPlaylist();
            }
        }
    }

    @Override
    public void close() {
        restorePlayerState();
    }

    private void setupPlayerForNotification() throws InterruptedException, SqueezeBoxTimeoutException {
        logger.debug("Setting up player for notification");
        if (!playerState.isPoweredOn()) {
            logger.debug("Powering on the player");
            squeezeBoxServerHandler.powerOn(mac);
        }
        if (playerState.isShuffling()) {
            logger.debug("Turning off shuffle");
            squeezeBoxServerHandler.setShuffleMode(mac, 0);
        }
        if (playerState.isRepeating()) {
            logger.debug("Turning off repeat");
            squeezeBoxServerHandler.setRepeatMode(mac, 0);
        }
        if (playerState.isPlaying()) {
            squeezeBoxServerHandler.stop(mac);
        }

        int notificationVolume = squeezeBoxPlayerHandler.getNotificationSoundVolume().intValue();
        setVolume(notificationVolume);
    }

    /**
     * Sends a volume set command if target volume is not equal to the current volume.
     *
     * @param requestedVolume The requested volume value.
     * @throws InterruptedException Thread interrupted during while we were waiting for an answer from the media server.
     * @throws SqueezeBoxTimeoutException Volume command was not acknowledged by the media server.
     */
    private void setVolume(int requestedVolume) throws InterruptedException, SqueezeBoxTimeoutException {
        if (playerState.getVolume() == requestedVolume) {
            return;
        }

        SqueezeBoxNotificationListener listener = new SqueezeBoxNotificationListener(mac);
        listener.resetVolumeUpdated();

        squeezeBoxServerHandler.registerSqueezeBoxPlayerListener(listener);
        squeezeBoxServerHandler.setVolume(mac, requestedVolume);

        logger.trace("Waiting up to {} ms for volume to be updated...", volumeCommandTimeout * 1000);

        try {
            int timeoutCount = 0;

            while (!listener.isVolumeUpdated(requestedVolume)) {
                Thread.sleep(100);
                if (timeoutCount++ > volumeCommandTimeout * 10) {
                    throw new SqueezeBoxTimeoutException("Unable to update volume.");
                }
            }
        } finally {
            squeezeBoxServerHandler.unregisterSqueezeBoxPlayerListener(listener);
        }
    }

    private void addNotificationMessageToPlaylist() throws InterruptedException, SqueezeBoxTimeoutException {
        SqueezeBoxNotificationListener listener = new SqueezeBoxNotificationListener(mac);
        listener.resetPlaylistUpdated();

        squeezeBoxServerHandler.registerSqueezeBoxPlayerListener(listener);
        squeezeBoxServerHandler.addPlaylistItem(mac, uri.toString());

        try {
            waitForPlaylistUpdate(listener);
        } finally {
            squeezeBoxServerHandler.unregisterSqueezeBoxPlayerListener(listener);
        }
    }

    private void removeNotificationMessageFromPlaylist() throws InterruptedException, SqueezeBoxTimeoutException {
        SqueezeBoxNotificationListener listener = new SqueezeBoxNotificationListener(mac);
        listener.resetPlaylistUpdated();

        squeezeBoxServerHandler.registerSqueezeBoxPlayerListener(listener);
        squeezeBoxServerHandler.deletePlaylistItem(mac, notificationMessagePlaylistsIndex);

        try {
            waitForPlaylistUpdate(listener);
        } finally {
            squeezeBoxServerHandler.unregisterSqueezeBoxPlayerListener(listener);
        }
    }

    /**
     * Monitor the number of playlist entries. When it changes, then we know the playlist
     * has been updated with the notification URL. There's probably an edge case here where
     * someone is updating the playlist at the same time, but that should be rare.
     *
     * @param listener
     * @throws InterruptedException
     * @throws SqueezeBoxTimeoutException
     */
    private void waitForPlaylistUpdate(SqueezeBoxNotificationListener listener)
            throws InterruptedException, SqueezeBoxTimeoutException {
        logger.trace("Waiting up to {} ms for playlist to be updated...", playlistCommandTimeout * 1000);

        int timeoutCount = 0;

        while (!listener.isPlaylistUpdated()) {
            Thread.sleep(100);
            if (timeoutCount++ > playlistCommandTimeout * 10) {
                throw new SqueezeBoxTimeoutException("Unable to update playlist.");
            }
        }

        this.playlistModified = true;
    }

    private void playNotification() throws InterruptedException, SqueezeBoxTimeoutException {
        logger.debug("Playing notification");

        notificationMessagePlaylistsIndex = squeezeBoxPlayerHandler.currentNumberPlaylistTracks() - 1;
        SqueezeBoxNotificationListener listener = new SqueezeBoxNotificationListener(mac);
        listener.resetStopped();

        squeezeBoxServerHandler.registerSqueezeBoxPlayerListener(listener);
        squeezeBoxServerHandler.playPlaylistItem(mac, notificationMessagePlaylistsIndex);

        logger.trace("Waiting up to {} ms for stop...", notificationLengthTimeout * 1000);

        try {
            int timeoutCount = 0;

            while (!listener.isStopped()) {
                Thread.sleep(100);
                if (timeoutCount++ > notificationLengthTimeout * 10) {
                    throw new SqueezeBoxTimeoutException("Unable to play message.");
                }
            }
        } finally {
            squeezeBoxServerHandler.unregisterSqueezeBoxPlayerListener(listener);
        }
    }

    private void restorePlayerState() {
        logger.debug("Restoring player state");

        // Resume playing save playlist item if player wasn't stopped.
        // Note that setting the time doesn't work for remote streams.
        if (!playerState.isStopped()) {
            logger.debug("Resuming last item playing");
            squeezeBoxServerHandler.playPlaylistItem(mac, playerState.getPlaylistIndex());
            squeezeBoxServerHandler.setPlayingTime(mac, playerState.getPlayingTime());
        } else if (!playerState.isPlaying()) {
            logger.debug("Pausing the player");
            squeezeBoxServerHandler.pause(mac);
        } else {
            logger.debug("Stopping the player");
            squeezeBoxServerHandler.stop(mac);
        }

        // We do not wait for the volume acknowledge to avoid exceptions during
        // clean up. If we are not able to set the volume there´s nothing we can
        // do about it.
        squeezeBoxServerHandler.setVolume(mac, playerState.getVolume());

        if (playerState.isShuffling()) {
            logger.debug("Restoring shuffle mode");
            squeezeBoxServerHandler.setShuffleMode(mac, playerState.getShuffle());
        }
        if (playerState.isRepeating()) {
            logger.debug("Restoring repeat mode");
            squeezeBoxServerHandler.setRepeatMode(mac, playerState.getRepeat());
        }
        if (playerState.isMuted()) {
            logger.debug("Re-muting the player");
            squeezeBoxServerHandler.mute(mac);
        }
        if (!playerState.isPoweredOn()) {
            logger.debug("Powering off the player");
            squeezeBoxServerHandler.powerOff(mac);
        }
    }
}