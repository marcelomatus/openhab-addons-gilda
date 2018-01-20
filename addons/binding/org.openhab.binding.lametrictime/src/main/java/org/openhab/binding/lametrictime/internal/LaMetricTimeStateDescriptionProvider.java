package org.openhab.binding.lametrictime.internal;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.jdt.annotation.NonNull;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.thing.Channel;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.type.DynamicStateDescriptionProvider;
import org.eclipse.smarthome.core.types.StateDescription;
import org.osgi.service.component.annotations.Component;

/**
 * Dynamic state description provider for the LaMetric Time binding.
 *
 * @author Gregory Moyer - Initial contribution
 */
@Component(service = { DynamicStateDescriptionProvider.class,
        LaMetricTimeStateDescriptionProvider.class }, immediate = true)
public class LaMetricTimeStateDescriptionProvider implements DynamicStateDescriptionProvider {
    private final Map<ChannelUID, StateDescription> stateDescriptions = new ConcurrentHashMap<>();

    public void setStateDescription(ChannelUID channelUID, StateDescription stateDescription) {
        stateDescriptions.put(channelUID, stateDescription);
    }

    @Override
    public @Nullable StateDescription getStateDescription(@NonNull Channel channel,
            @Nullable StateDescription originalStateDescription, @Nullable Locale locale) {
        return stateDescriptions.get(channel.getUID());
    }
}
