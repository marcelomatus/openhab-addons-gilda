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
package org.openhab.binding.linky.internal.handler;

import static org.openhab.binding.linky.internal.LinkyBindingConstants.*;
import static org.openhab.binding.linky.internal.model.LinkyTimeScale.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoField;
import java.time.temporal.ChronoUnit;
import java.time.temporal.WeekFields;
import java.util.Base64;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.smarthome.core.i18n.LocaleProvider;
import org.eclipse.smarthome.core.library.types.QuantityType;
import org.eclipse.smarthome.core.library.unit.SmartHomeUnits;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseThingHandler;
import org.eclipse.smarthome.core.types.Command;
import org.eclipse.smarthome.core.types.RefreshType;
import org.eclipse.smarthome.core.types.UnDefType;
import org.openhab.binding.linky.internal.LinkyConfiguration;
import org.openhab.binding.linky.internal.model.LinkyConsumptionData;
import org.openhab.binding.linky.internal.model.LinkyTimeScale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import okhttp3.FormBody;
import okhttp3.FormBody.Builder;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * The {@link LinkyHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Gaël L'hopital - Initial contribution
 */

@NonNullByDefault
public class LinkyHandler extends BaseThingHandler {
    private final Logger logger = LoggerFactory.getLogger(LinkyHandler.class);
    private static final String LOGIN_BASE_URI = "https://espace-client-connexion.enedis.fr/auth/UI/Login";
    // private static final String LOGIN_BODY_BUILDER =
    // "encoded=true&gx_charset=UTF-8&SunQueryParamsString=%s&IDToken1=%s&IDToken2=%s";
    private static final String API_BASE_URI = "https://espace-client-particuliers.enedis.fr/group/espace-particuliers/suivi-de-consommation";
    // private static final String DATA_BODY_BUILDER =
    // "p_p_id=lincspartdisplaycdc_WAR_lincspartcdcportlet&p_p_lifecycle=2&p_p_resource_id=%s&_lincspartdisplaycdc_WAR_lincspartcdcportlet_dateDebut=%s&_lincspartdisplaycdc_WAR_lincspartcdcportlet_dateFin=%s";
    private static final DateTimeFormatter API_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    // private static final int HTTP_DEFAULT_TIMEOUT_MS = (int) TimeUnit.SECONDS.toMillis(5);
    private static final Builder LOGIN_BODY_BUILDER = new FormBody.Builder().add("encoded", "true")
            .add("gx_charset", "UTF-8").add("SunQueryParamsString",
                    Base64.getEncoder().encodeToString("realm=particuliers".getBytes(StandardCharsets.UTF_8)));

    private static final int REFRESH_FIRST_HOUR_OF_DAY = 3;
    private static final int REFRESH_INTERVAL_IN_MIN = 720;

    private final OkHttpClient client = new OkHttpClient.Builder().followRedirects(false)
            .cookieJar(new LinkyCookieJar()).build();
    private final Gson GSON = new Gson();

    private @NonNullByDefault({}) ScheduledFuture<?> refreshJob;
    private final WeekFields weekFields;

    public LinkyHandler(Thing thing, LocaleProvider localeProvider) {
        super(thing);
        weekFields = WeekFields.of(localeProvider.getLocale());
    }

    @Override
    public void initialize() {
        logger.debug("Initializing Linky handler.");
        LinkyConfiguration config = getConfigAs(LinkyConfiguration.class);

        scheduler.schedule(this::login, 0, TimeUnit.SECONDS);

        // Refresh data twice a day at approximatively 3am and 3pm
        final LocalDateTime now = LocalDateTime.now();
        final LocalDateTime nextDayFirstTimeUpdate = now.plusDays(1)
                .with(ChronoField.HOUR_OF_DAY, REFRESH_FIRST_HOUR_OF_DAY).truncatedTo(ChronoUnit.HOURS);
        refreshJob = scheduler.scheduleWithFixedDelay(this::updateData,
                ChronoUnit.MINUTES.between(now, nextDayFirstTimeUpdate) % REFRESH_INTERVAL_IN_MIN,
                REFRESH_INTERVAL_IN_MIN, TimeUnit.MINUTES);
    }

    private synchronized boolean login() {
        logger.debug("login");

        LinkyConfiguration config = getConfigAs(LinkyConfiguration.class);

        try {
            Request requestLogin = new Request.Builder().url(LOGIN_BASE_URI)
                    .post(LOGIN_BODY_BUILDER.add("IDToken1", config.username).add("IDToken2", config.password).build())
                    .build();
            Response response = client.newCall(requestLogin).execute();
            if (response.isRedirect()) {
                logger.debug("Response status {} {} redirects to {}", response.code(), response.message(),
                        response.header("Location"));
            } else {
                logger.debug("Response status {} {}", response.code(), response.message());
            }
            response.close();
            // String requestContent = String.format(LOGIN_BODY_BUILDER,
            // Base64.getEncoder().encodeToString("realm=particuliers".getBytes(StandardCharsets.UTF_8)),
            // URLEncoder.encode(config.username, StandardCharsets.UTF_8.name()),
            // URLEncoder.encode(config.password, StandardCharsets.UTF_8.name()));
            // InputStream stream = new ByteArrayInputStream(requestContent.getBytes(StandardCharsets.UTF_8));
            // logger.debug("POST {} requestContent {}", LOGIN_BASE_URI, requestContent);
            // HttpUtil.executeUrl("POST", LOGIN_BASE_URI, stream, "application/x-www-form-urlencoded",
            // HTTP_DEFAULT_TIMEOUT_MS);
            // stream.close();

            // Do a first call to get data; this first call will fail with code 302
            getConsumptionData(DAILY, LocalDate.now(), LocalDate.now(), false);

            updateStatus(ThingStatus.ONLINE);
            return true;
        } catch (IOException e) {
            logger.debug("Exception while trying to login: {}", e.getMessage(), e);
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.CONFIGURATION_ERROR, e.getMessage());
            return false;
        }
    }

    /**
     * Request new data and updates channels
     */
    private void updateData() {
        updateDailyData();
        updateMonthlyData();
        updateYearlyData();
    }

    /**
     * Request new dayly/weekly data and updates channels
     */
    private synchronized void updateDailyData() {
        if (!isLinked(YESTERDAY) && !isLinked(LAST_WEEK) && !isLinked(THIS_WEEK)) {
            return;
        }

        final LocalDate today = LocalDate.now();

        double lastWeek = -1;
        double thisWeek = -1;
        double yesterday = -1;
        LocalDate rangeStart = today.minusDays(13);
        LinkyConsumptionData result = getConsumptionData(DAILY, rangeStart, today, true);
        if (result != null && result.success()) {
            int jump = result.getDecalage();
            while (rangeStart.getDayOfWeek() != weekFields.getFirstDayOfWeek()) {
                rangeStart = rangeStart.plusDays(1);
                jump++;
            }

            int lastWeekNumber = rangeStart.get(weekFields.weekOfWeekBasedYear());

            lastWeek = 0;
            thisWeek = 0;
            yesterday = -1;
            while (jump < result.getData().size()) {
                double consumption = result.getData().get(jump).valeur;
                if (consumption > 0) {
                    if (rangeStart.get(weekFields.weekOfWeekBasedYear()) == lastWeekNumber) {
                        lastWeek += consumption;
                        logger.trace("Consumption at index {} added to last week: {}", jump, consumption);
                    } else {
                        thisWeek += consumption;
                        logger.trace("Consumption at index {} added to current week: {}", jump, consumption);
                    }
                    yesterday = consumption;
                }
                jump++;
                rangeStart = rangeStart.plusDays(1);
            }
        }
        updateKwhChannel(YESTERDAY, yesterday);
        updateKwhChannel(THIS_WEEK, thisWeek);
        updateKwhChannel(LAST_WEEK, lastWeek);
    }

    /**
     * Request new monthly data and updates channels
     */
    private synchronized void updateMonthlyData() {
        if (!isLinked(LAST_MONTH) && !isLinked(THIS_MONTH)) {
            return;
        }

        final LocalDate today = LocalDate.now();

        double lastMonth = -1;
        double thisMonth = -1;
        LinkyConsumptionData result = getConsumptionData(MONTHLY, today.withDayOfMonth(1).minusMonths(1), today, true);
        if (result != null && result.success()) {
            lastMonth = result.getData().stream().filter(LinkyConsumptionData.Data::isPositive).findFirst()
                    .get().valeur;
            thisMonth = result.getData().stream().filter(LinkyConsumptionData.Data::isPositive)
                    .reduce((first, second) -> second).get().valeur;
        }
        updateKwhChannel(LAST_MONTH, lastMonth);
        updateKwhChannel(THIS_MONTH, thisMonth);
    }

    /**
     * Request new yearly data and updates channels
     */
    private synchronized void updateYearlyData() {
        if (!isLinked(LAST_YEAR) && !isLinked(THIS_YEAR)) {
            return;
        }

        final LocalDate today = LocalDate.now();

        double thisYear = -1;
        double lastYear = -1;
        LinkyConsumptionData result = getConsumptionData(YEARLY, LocalDate.of(today.getYear() - 1, 1, 1), today, true);
        if (result != null && result.success()) {
            int elementQuantity = result.getData().size();
            thisYear = elementQuantity > 0 ? result.getData().get(elementQuantity - 1).valeur : -1;
            lastYear = elementQuantity > 1 ? result.getData().get(elementQuantity - 2).valeur : -1;
        }
        updateKwhChannel(LAST_YEAR, lastYear);
        updateKwhChannel(THIS_YEAR, thisYear);
    }

    private void updateKwhChannel(String channelId, double consumption) {
        logger.debug("Update channel {} with {}", channelId, consumption);
        updateState(channelId,
                consumption != -1 ? new QuantityType<>(consumption, SmartHomeUnits.KILOWATT_HOUR) : UnDefType.UNDEF);
    }

    private @Nullable LinkyConsumptionData getConsumptionData(LinkyTimeScale timeScale, LocalDate from, LocalDate to,
            boolean reLog) {
        logger.debug("getConsumptionData {}", timeScale);

        LinkyConsumptionData result = null;
        boolean tryRelog = false;

        FormBody formBody = new FormBody.Builder().add("p_p_id", "lincspartdisplaycdc_WAR_lincspartcdcportlet")
                .add("p_p_lifecycle", "2").add("p_p_resource_id", timeScale.getId())
                .add("_lincspartdisplaycdc_WAR_lincspartcdcportlet_dateDebut", from.format(API_DATE_FORMAT))
                .add("_lincspartdisplaycdc_WAR_lincspartcdcportlet_dateFin", to.format(API_DATE_FORMAT)).build();

        Request requestData = new Request.Builder().url(API_BASE_URI).post(formBody).build();
        try (Response response = client.newCall(requestData).execute()) {
            if (response.isRedirect()) {
                String location = response.header("Location");
                logger.debug("Response status {} {} redirects to {}", response.code(), response.message(), location);
                if (reLog && location != null && location.startsWith(LOGIN_BASE_URI)) {
                    tryRelog = true;
                }
            } else {
                String body = (response.body() != null) ? response.body().string() : null;
                logger.debug("Response status {} {} : {}", response.code(), response.message(), body);
                if (body != null && !body.isEmpty()) {
                    result = GSON.fromJson(body, LinkyConsumptionData.class);
                }
            }
            response.close();
        } catch (IOException e) {
            logger.debug("Exception calling API : {} - {}", e.getClass().getCanonicalName(), e.getMessage());
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, e.getMessage());
        } catch (JsonSyntaxException e) {
            logger.debug("Exception while converting JSON response : {}", e.getMessage());
        }
        if (tryRelog && login()) {
            result = getConsumptionData(timeScale, from, to, false);
        }
        // String requestContent = String.format(DATA_BODY_BUILDER, timeScale.getId(), from.format(API_DATE_FORMAT),
        // to.format(API_DATE_FORMAT));
        // InputStream stream = new ByteArrayInputStream(requestContent.getBytes(StandardCharsets.UTF_8));
        // logger.debug("POST {} requestContent {}", API_BASE_URI, requestContent);
        // try {
        // String jsonResponse = HttpUtil.executeUrl("POST", API_BASE_URI, stream, "application/x-www-form-urlencoded",
        // HTTP_DEFAULT_TIMEOUT_MS);
        // if (jsonResponse != null) {
        // result = GSON.fromJson(jsonResponse, LinkyConsumptionData.class);
        // }
        // } catch (IOException e) {
        // logger.debug("Exception calling API : {} - {}", e.getClass().getCanonicalName(), e.getMessage());
        // updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.OFFLINE.COMMUNICATION_ERROR, e.getMessage());
        // } catch (JsonSyntaxException e) {
        // logger.debug("Exception while converting JSON response : {}", e.getMessage());
        // }
        // try {
        // stream.close();
        // } catch (IOException e) {
        // }
        return result;
    }

    @Override
    public void dispose() {
        logger.debug("Disposing the Linky handler.");

        if (refreshJob != null && !refreshJob.isCancelled()) {
            refreshJob.cancel(true);
            refreshJob = null;
        }
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
        if (command instanceof RefreshType) {
            logger.debug("Refreshing channel {}", channelUID.getId());
            switch (channelUID.getId()) {
                case YESTERDAY:
                case LAST_WEEK:
                case THIS_WEEK:
                    updateDailyData();
                    break;
                case LAST_MONTH:
                case THIS_MONTH:
                    updateMonthlyData();
                    break;
                case LAST_YEAR:
                case THIS_YEAR:
                    updateYearlyData();
                    break;
                default:
                    break;
            }
        } else {
            logger.debug("The Linky binding is read-only and can not handle command {}", command);
        }
    }

}
