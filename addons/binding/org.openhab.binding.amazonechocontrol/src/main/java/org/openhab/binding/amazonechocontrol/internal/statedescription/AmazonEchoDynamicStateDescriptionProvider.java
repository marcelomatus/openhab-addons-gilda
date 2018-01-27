/**
 * Copyright (c) 2010-2018 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.amazonechocontrol.internal.statedescription;

import static org.openhab.binding.amazonechocontrol.AmazonEchoControlBindingConstants.*;

import java.util.ArrayList;
import java.util.Locale;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.type.DynamicStateDescriptionProvider;
import org.eclipse.smarthome.core.types.StateDescription;
import org.eclipse.smarthome.core.types.StateOption;
import org.openhab.binding.amazonechocontrol.handler.EchoHandler;
import org.openhab.binding.amazonechocontrol.internal.Connection;
import org.openhab.binding.amazonechocontrol.internal.jsons.JsonBluetoothStates.BluetoothState;
import org.openhab.binding.amazonechocontrol.internal.jsons.JsonBluetoothStates.PairedDevice;
import org.openhab.binding.amazonechocontrol.internal.jsons.JsonDevices.Device;
import org.openhab.binding.amazonechocontrol.internal.jsons.JsonNotificationSound;
import org.openhab.binding.amazonechocontrol.internal.jsons.JsonPlaylists;
import org.openhab.binding.amazonechocontrol.internal.jsons.JsonPlaylists.PlayList;
import org.osgi.service.component.annotations.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dynamic channel state description provider.
 * Overrides the state description for the controls, which receive its configuration in the runtime.
 *
 * @author Michael Geramb - Initial contribution
 */
@NonNullByDefault
@Component(service = { DynamicStateDescriptionProvider.class,
        AmazonEchoDynamicStateDescriptionProvider.class }, immediate = true)
public class AmazonEchoDynamicStateDescriptionProvider implements DynamicStateDescriptionProvider {

    private final Logger logger = LoggerFactory.getLogger(EchoHandler.class);

    public AmazonEchoDynamicStateDescriptionProvider() {

    }

    @Override
    public @Nullable StateDescription getStateDescription(Channel channel,
            @Nullable StateDescription originalStateDescription, @Nullable Locale locale) {

        if (originalStateDescription == null) {
            return null;
        }
        if (CHANNEL_TYPE_BLUETHOOTH_ID_SELECTION.equals(channel.getChannelTypeUID())) {

            EchoHandler handler = EchoHandler.find(channel.getUID().getThingUID());
            if (handler == null) {
                return originalStateDescription;
            }

            BluetoothState bluetoothState = handler.findBluetoothState();
            if (bluetoothState == null) {
                return originalStateDescription;
            }

            if (bluetoothState.pairedDeviceList == null) {
                return originalStateDescription;
            }

            ArrayList<StateOption> options = new ArrayList<StateOption>();
            options.add(new StateOption("", ""));
            for (PairedDevice device : bluetoothState.pairedDeviceList) {
                if (device.address != null && device.friendlyName != null) {
                    options.add(new StateOption(device.address, device.friendlyName));
                }
            }
            StateDescription result = new StateDescription(originalStateDescription.getMinimum(),
                    originalStateDescription.getMaximum(), originalStateDescription.getStep(),
                    originalStateDescription.getPattern(), originalStateDescription.isReadOnly(), options);
            return result;

        } else if (CHANNEL_TYPE_AMAZON_MUSIC_PLAY_LIST_ID.equals(channel.getChannelTypeUID())) {

            EchoHandler handler = EchoHandler.find(channel.getUID().getThingUID());
            if (handler == null) {
                return originalStateDescription;
            }
            Connection connection = handler.findConnection();
            if (connection == null) {
                return originalStateDescription;
            }
            Device device = handler.findDevice();
            if (device == null) {
                return originalStateDescription;
            }
            JsonPlaylists playLists;
            try {
                playLists = connection.getPlaylists(device);
            } catch (Exception e) {
                logger.warn("Get playlist failed: {}", e);
                return originalStateDescription;
            }
            ArrayList<StateOption> options = new ArrayList<StateOption>();
            options.add(new StateOption("", ""));
            if (playLists.playlists != null) {
                for (PlayList[] innerLists : playLists.playlists.values()) {
                    if (innerLists.length > 0) {
                        PlayList playList = innerLists[0];
                        if (playList.playlistId != null && playList.title != null) {
                            options.add(new StateOption(playList.playlistId,
                                    String.format("%s [%d]", playList.title, playList.trackCount)));
                        }
                    }
                }
            }
            StateDescription result = new StateDescription(originalStateDescription.getMinimum(),
                    originalStateDescription.getMaximum(), originalStateDescription.getStep(),
                    originalStateDescription.getPattern(), originalStateDescription.isReadOnly(), options);
            return result;
        } else if (CHANNEL_TYPE_PLAY_ALARM_SOUND.equals(channel.getChannelTypeUID())) {

            EchoHandler handler = EchoHandler.find(channel.getUID().getThingUID());
            if (handler == null) {
                return originalStateDescription;
            }
            Connection connection = handler.findConnection();
            if (connection == null) {
                return originalStateDescription;
            }
            Device device = handler.findDevice();
            if (device == null) {
                return originalStateDescription;
            }

            JsonNotificationSound[] notificationSounds;
            try {
                notificationSounds = connection.getNotificationSounds(device);
            } catch (Exception e) {
                logger.warn("Get notification sounds failed: {}", e);
                return originalStateDescription;
            }
            ArrayList<StateOption> options = new ArrayList<StateOption>();
            options.add(new StateOption("", ""));
            if (notificationSounds != null) {
                for (JsonNotificationSound notificationSound : notificationSounds) {

                    if (notificationSound.folder == null && notificationSound.providerId != null
                            && notificationSound.id != null && notificationSound.displayName != null) {
                        String providerSoundId = notificationSound.providerId + ":" + notificationSound.id;
                        options.add(new StateOption(providerSoundId,
                                String.format("%s [%s]", notificationSound.displayName, providerSoundId)));

                    }
                }
            }
            StateDescription result = new StateDescription(originalStateDescription.getMinimum(),
                    originalStateDescription.getMaximum(), originalStateDescription.getStep(),
                    originalStateDescription.getPattern(), originalStateDescription.isReadOnly(), options);
            return result;
        }
        return originalStateDescription;
    }

}
