package org.openhab.binding.mystrom.internal;

import static org.openhab.binding.mystrom.internal.MyStromBindingConstants.*;
import static org.openhab.core.library.unit.SIUnits.CELSIUS;
import static org.openhab.core.library.unit.Units.PERCENT;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpMethod;
import org.openhab.core.library.types.OnOffType;
import org.openhab.core.library.types.QuantityType;
import org.openhab.core.thing.ChannelUID;
import org.openhab.core.thing.Thing;
import org.openhab.core.thing.ThingStatus;
import org.openhab.core.thing.ThingStatusDetail;
import org.openhab.core.types.Command;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author Stefan Navratil - Initial Contribution
 *
 */

@NonNullByDefault
public class MyStromPIRHandler extends AbstractMyStromHandler {

    private final Logger logger = LoggerFactory.getLogger(MyStromPIRHandler.class);
    private MyStromConfiguration config;

    private static class MyStromReport {

        public float light;
        public boolean motion;
        public float temperature;
    }

    public MyStromPIRHandler(Thing thing, HttpClient httpClient) {
        super(thing, httpClient);
        config = getConfigAs(MyStromConfiguration.class);
        try {
            sendHttpRequest(HttpMethod.POST, "/api/v1/settings/pir",
                    "{\"backoff_time\":" + config.getBackoffTime() + ",\"led_enable\":" + config.getLedEnable() + "}");
        } catch (MyStromException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    protected void pollDevice() {
        MyStromReport report = getReport();
        if (report != null) {
            updateState(CHANNEL_MOTION, report.motion ? OnOffType.ON : OnOffType.OFF);
            updateState(CHANNEL_TEMPERATURE, QuantityType.valueOf(report.temperature, CELSIUS));
            // The Default Light thresholds are from 30 to 300.
            updateState(CHANNEL_LIGHT, QuantityType.valueOf(report.light / 3, PERCENT));

        }
    }

    private @Nullable MyStromReport getReport() {
        try {
            String json = sendHttpRequest(HttpMethod.GET, "/api/v1/sensors", null);
            MyStromReport x = gson.fromJson(json, MyStromReport.class);
            updateStatus(ThingStatus.ONLINE);
            return x;
        } catch (MyStromException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
            return null;
        }
    }
}
