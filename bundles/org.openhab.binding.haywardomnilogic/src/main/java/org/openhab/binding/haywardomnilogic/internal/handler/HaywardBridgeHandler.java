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
package org.openhab.binding.haywardomnilogic.internal.handler;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.api.Request;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.http.HttpHeader;
import org.eclipse.jetty.http.HttpMethod;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.smarthome.core.library.types.OnOffType;
import org.eclipse.smarthome.core.thing.Bridge;
import org.eclipse.smarthome.core.thing.ChannelUID;
import org.eclipse.smarthome.core.thing.Thing;
import org.eclipse.smarthome.core.thing.ThingStatus;
import org.eclipse.smarthome.core.thing.ThingStatusDetail;
import org.eclipse.smarthome.core.thing.binding.BaseBridgeHandler;
import org.eclipse.smarthome.core.thing.binding.ThingHandlerService;
import org.eclipse.smarthome.core.types.Command;
import org.openhab.binding.haywardomnilogic.internal.HaywardBindingConstants;
import org.openhab.binding.haywardomnilogic.internal.config.HaywardConfig;
import org.openhab.binding.haywardomnilogic.internal.discovery.HaywardDiscoveryService;
import org.openhab.binding.haywardomnilogic.internal.hayward.HaywardThingHandler;
import org.openhab.binding.haywardomnilogic.internal.hayward.HaywardTypeToRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * The {@link HaywardBridgeHandler} is responsible for handling commands, which are
 * sent to one of the channels.
 *
 * @author Matt Myers - Initial contribution
 */

@NonNullByDefault
public class HaywardBridgeHandler extends BaseBridgeHandler {
    private final Logger logger = LoggerFactory.getLogger(HaywardBridgeHandler.class);
    private final HttpClient httpClient;
    private @Nullable ScheduledFuture<?> initializeFuture;
    private @Nullable ScheduledFuture<?> pollTelemetryFuture;
    private @Nullable ScheduledFuture<?> pollAlarmsFuture;
    private int commFailureCount;
    private @Nullable HaywardDiscoveryService haywardDiscoveryService;

    public HaywardConfig config = getConfig().as(HaywardConfig.class);

    @Override
    public Collection<Class<? extends ThingHandlerService>> getServices() {
        return Collections.singleton(HaywardDiscoveryService.class);
    }

    public HaywardBridgeHandler(Bridge bridge, HttpClient httpClient) {
        super(bridge);
        this.httpClient = httpClient;
    }

    /**
     * Called by the zone discovery service to let this handler have a reference.
     */
    public void setHaywardDiscoveryService(HaywardDiscoveryService s) {
        this.haywardDiscoveryService = s;
    }

    @Override
    public void handleCommand(ChannelUID channelUID, Command command) {
    }

    @Override
    public void dispose() {
        clearPolling(initializeFuture);
        clearPolling(pollTelemetryFuture);
        clearPolling(pollAlarmsFuture);
        logger.trace("Hayward polling cancelled");
        super.dispose();
    }

    @Override
    public void initialize() {
        updateStatus(ThingStatus.UNKNOWN);
        initializeFuture = scheduler.schedule(this::scheduledInitialize, 1, TimeUnit.SECONDS);
        return;
    }

    public void scheduledInitialize() {
        config = getConfigAs(HaywardConfig.class);

        try {
            clearPolling(pollTelemetryFuture);
            clearPolling(pollAlarmsFuture);

            if (!(login())) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Unable to Login to Hayward's server");
                clearPolling(pollTelemetryFuture);
                clearPolling(pollAlarmsFuture);
                commFailureCount = 50;
                initPolling(60);
                return;
            }

            if (!(getSiteList())) {
                updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                        "Unable to getMSP from Hayward's server");
                clearPolling(pollTelemetryFuture);
                clearPolling(pollAlarmsFuture);
                commFailureCount = 50;
                initPolling(60);
                return;
            }

            updateStatus(ThingStatus.ONLINE);
            logger.trace("Succesfully opened connection to Hayward's server: {} Username:{}", config.hostname,
                    config.username);

            initPolling(0);
            logger.trace("Hayward Telemetry polling scheduled");

            if (config.alarmPollTime > 0) {
                initAlarmPolling(1);
                logger.trace("Hayward Alarm polling scheduled");
            } else {
                logger.trace("Hayward Alarm polling disabled");
            }
        } catch (Exception e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.HANDLER_INITIALIZING_ERROR,
                    "scheduledInitialize exception");
            logger.error("Unable to open connection to Hayward Server: {} Username: {}", config.hostname,
                    config.username, e);
            clearPolling(pollTelemetryFuture);
            clearPolling(pollAlarmsFuture);
            commFailureCount = 50;
            initPolling(60);
            return;
        }
    }

    public synchronized boolean login() throws Exception {
        String xmlResponse;
        String status;

        // *****Login to Hayward server
        String urlParameters = "<?xml version=\"1.0\" encoding=\"utf-8\"?><Request>" + "<Name>Login</Name><Parameters>"
                + "<Parameter name=\"UserName\" dataType=\"String\">" + config.username + "</Parameter>"
                + "<Parameter name=\"Password\" dataType=\"String\">" + config.password + "</Parameter>"
                + "</Parameters></Request>";

        xmlResponse = httpXmlResponse(urlParameters);

        if (xmlResponse.isEmpty()) {
            return false;
        }

        status = evaluateXPath("/Response/Parameters//Parameter[@name='Status']/text()", xmlResponse).get(0);

        if (!(status.equals("0"))) {
            logger.error("Hayward Login XML response: {}", xmlResponse);
            return false;
        }

        config.token = evaluateXPath("/Response/Parameters//Parameter[@name='Token']/text()", xmlResponse).get(0);
        config.userID = evaluateXPath("/Response/Parameters//Parameter[@name='UserID']/text()", xmlResponse).get(0);
        return true;
    }

    public synchronized boolean getApiDef() throws Exception {
        String xmlResponse;

        // *****getConfig from Hayward server
        String urlParameters = "<?xml version=\"1.0\" encoding=\"utf-8\"?><Request><Name>GetAPIDef</Name><Parameters>"
                + "<Parameter name=\"Token\" dataType=\"String\">" + config.token + "</Parameter>"
                + "<Parameter name=\"MspSystemID\" dataType=\"int\">" + config.mspSystemID + "</Parameter>;"
                + "<Parameter name=\"Version\" dataType=\"string\">0.4</Parameter >\r\n"
                + "<Parameter name=\"Language\" dataType=\"string\">en</Parameter >\r\n" + "</Parameters></Request>";

        xmlResponse = httpXmlResponse(urlParameters);

        if (xmlResponse.isEmpty()) {
            logger.error("Hayward Login XML response was null");
            return false;
        }
        return true;
    }

    public synchronized boolean getSiteList() throws Exception {
        String xmlResponse;
        String status;

        // *****Get MSP
        String urlParameters = "<?xml version=\"1.0\" encoding=\"utf-8\"?><Request><Name>GetSiteList</Name><Parameters>"
                + "<Parameter name=\"Token\" dataType=\"String\">" + config.token
                + "</Parameter><Parameter name=\"UserID\" dataType=\"String\">" + config.userID
                + "</Parameter></Parameters></Request>";

        xmlResponse = httpXmlResponse(urlParameters);

        if (xmlResponse.isEmpty()) {
            logger.error("Hayward getSiteList XML response was null");
            return false;
        }

        status = evaluateXPath("/Response/Parameters//Parameter[@name='Status']/text()", xmlResponse).get(0);

        if (!(status.equals("0"))) {
            logger.error("Hayward getSiteList XML response: {}", xmlResponse);
            return false;
        }

        config.mspSystemID = evaluateXPath("/Response/Parameters/Parameter/Item//Property[@name='MspSystemID']/text()",
                xmlResponse).get(0);
        config.backyardName = evaluateXPath(
                "/Response/Parameters/Parameter/Item//Property[@name='BackyardName']/text()", xmlResponse).get(0);
        config.address = evaluateXPath("/Response/Parameters/Parameter/Item//Property[@name='Address']/text()",
                xmlResponse).get(0);
        return true;
    }

    public synchronized String getMspConfig() throws Exception {
        // *****getMspConfig from Hayward server
        String urlParameters = "<?xml version=\"1.0\" encoding=\"utf-8\"?><Request><Name>GetMspConfigFile</Name><Parameters>"
                + "<Parameter name=\"Token\" dataType=\"String\">" + config.token + "</Parameter>"
                + "<Parameter name=\"MspSystemID\" dataType=\"int\">" + config.mspSystemID
                + "</Parameter><Parameter name=\"Version\" dataType=\"string\">0</Parameter>\r\n"
                + "</Parameters></Request>";

        String xmlResponse = httpXmlResponse(urlParameters);

        // Debug: Inject xml file for testing
        // String path =
        // "C:/Users/Controls/openhab-2-5-x/git/openhab-addons/bundles/org.openhab.binding.haywardomnilogic/getConfig.xml";
        // xmlResponse = new String(Files.readAllBytes(Paths.get(path)));

        if (xmlResponse.isEmpty()) {
            logger.error("Hayward requestConfig XML response was null");
            return "";
        }

        if (evaluateXPath("//Backyard/Name/text()", xmlResponse).isEmpty()) {
            logger.error("Hayward requestConfiguration XML response: {}", xmlResponse);
            return "";
        }
        return xmlResponse;
    }

    public synchronized boolean getTelemetryData() throws Exception {
        // *****Request Telemetry from Hayward server
        String urlParameters = "<?xml version=\"1.0\" encoding=\"utf-8\"?><Request><Name>GetTelemetryData</Name><Parameters>"
                + "<Parameter name=\"Token\" dataType=\"String\">" + config.token + "</Parameter>"
                + "<Parameter name=\"MspSystemID\" dataType=\"int\">" + config.mspSystemID
                + "</Parameter></Parameters></Request>";

        String xmlResponse = httpXmlResponse(urlParameters);

        if (xmlResponse.isEmpty()) {
            logger.error("Hayward getTelemetry XML response was null");
            return false;
        }

        if (!evaluateXPath("/Response/Parameters//Parameter[@name='StatusMessage']/text()", xmlResponse).isEmpty()) {
            logger.error("Hayward getTelemetry XML response: {}", xmlResponse);
            return false;
        }

        for (Thing thing : getThing().getThings()) {
            if (thing.getHandler() instanceof HaywardThingHandler) {
                HaywardThingHandler handler = (HaywardThingHandler) thing.getHandler();
                if (handler != null) {
                    handler.getTelemetry(xmlResponse);
                }
            }
        }
        return true;
    }

    public synchronized boolean getAlarmList() throws Exception {
        for (Thing thing : getThing().getThings()) {
            Map<String, String> properties = thing.getProperties();
            if (properties.get(HaywardBindingConstants.PROPERTY_TYPE).equals("BACKYARD")) {
                HaywardBackyardHandler handler = (HaywardBackyardHandler) thing.getHandler();
                if (handler != null) {
                    return handler.getAlarmList(properties.get(HaywardBindingConstants.PROPERTY_SYSTEM_ID));
                }
            }
        }
        return false;
    }

    private synchronized void initPolling(int initalDelay) {
        pollTelemetryFuture = scheduler.scheduleWithFixedDelay(() -> {
            try {
                if (commFailureCount >= 5) {
                    commFailureCount = 0;
                    clearPolling(pollTelemetryFuture);
                    clearPolling(pollAlarmsFuture);
                    initialize();
                    return;
                }
                if (!(getTelemetryData())) {
                    commFailureCount++;
                    return;
                }
            } catch (Exception e) {
                logger.debug("Exception during poll", e);
            }
        }, initalDelay, config.telemetryPollTime, TimeUnit.SECONDS);
        return;
    }

    private synchronized void initAlarmPolling(int initalDelay) {
        pollAlarmsFuture = scheduler.scheduleWithFixedDelay(() -> {
            try {
                getAlarmList();
            } catch (Exception e) {
                logger.debug("Exception during poll", e);
            }
        }, initalDelay, config.alarmPollTime, TimeUnit.SECONDS);
    }

    private void clearPolling(@Nullable ScheduledFuture<?> pollJob) {
        if (pollJob != null) {
            pollJob.cancel(false);
        }
    }

    @Nullable
    Thing getThingForType(HaywardTypeToRequest type, int num) {
        for (Thing thing : getThing().getThings()) {
            Map<String, String> properties = thing.getProperties();
            if (properties.get(HaywardBindingConstants.PROPERTY_SYSTEM_ID).equals(Integer.toString(num))) {
                if (properties.get(HaywardBindingConstants.PROPERTY_TYPE).equals(type.toString())) {
                    return thing;
                }
            }
        }
        return null;
    }

    public List<String> evaluateXPath(String xpathExp, String xmlResponse) throws Exception {
        List<String> values = new ArrayList<>();
        try {
            InputSource inputXML = new InputSource(new StringReader(xmlResponse));
            XPath xPath = XPathFactory.newInstance().newXPath();
            NodeList nodes = (NodeList) xPath.evaluate(xpathExp, inputXML, XPathConstants.NODESET);

            for (int i = 0; i < nodes.getLength(); i++) {
                values.add(nodes.item(i).getNodeValue());
            }
        } catch (XPathExpressionException e) {
            logger.error("XPathExpression exception:", e);
        }
        return values;
    }

    private Request sendRequestBuilder(String url, HttpMethod method) {
        return this.httpClient.newRequest(url).agent("NextGenForIPhone/16565 CFNetwork/887 Darwin/17.0.0")
                .method(method).header(HttpHeader.ACCEPT_LANGUAGE, "en-us").header(HttpHeader.ACCEPT, "*/*")
                .header(HttpHeader.ACCEPT_ENCODING, "gzip, deflate").version(HttpVersion.HTTP_1_1)
                .header(HttpHeader.CONNECTION, "keep-alive").header(HttpHeader.HOST, "www.haywardomnilogic.com:80")
                .timeout(10, TimeUnit.SECONDS);
    }

    public synchronized String httpXmlResponse(String urlParameters) throws Exception {
        String statusMessage;
        String urlParameterslength = Integer.toString(urlParameters.length());

        try {
            ContentResponse httpResponse = sendRequestBuilder(config.hostname, HttpMethod.POST)
                    .content(new StringContentProvider(urlParameters), "text/xml; charset=utf-8")
                    .header(HttpHeader.CONTENT_LENGTH, urlParameterslength).send();

            int status = httpResponse.getStatus();
            String xmlResponse = httpResponse.getContentAsString();

            if (!(evaluateXPath("/Response/Parameters//Parameter[@name='StatusMessage']/text()", xmlResponse)
                    .isEmpty())) {
                statusMessage = evaluateXPath("/Response/Parameters//Parameter[@name='StatusMessage']/text()",
                        xmlResponse).get(0);
            } else {
                statusMessage = httpResponse.getReason();
            }

            if (status == 200) {
                if (logger.isTraceEnabled()) {
                    logger.trace("{} Hayward http command: {}", getCallingMethod(), urlParameters);
                    logger.trace("{} Hayward http response: {} {}", getCallingMethod(), statusMessage, xmlResponse);
                } else if (logger.isDebugEnabled()) {
                    logger.debug("{} Hayward http response: {}", getCallingMethod(), statusMessage);
                }
                return xmlResponse;
            } else {
                if (logger.isErrorEnabled()) {
                    logger.error("{} Hayward http command: {}", getCallingMethod(), urlParameters);
                    logger.error("{} Hayward http response: {}", getCallingMethod(), status);
                }
                return "";
            }
        } catch (java.net.UnknownHostException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Unable to resolve host.  Check Hayward hostname and your internet connection.");
            return "";
        } catch (TimeoutException e) {
            updateStatus(ThingStatus.OFFLINE, ThingStatusDetail.COMMUNICATION_ERROR,
                    "Connection Timeout.  Check Hayward hostname and your internet connection.");
            return "";
        }
    }

    private String getCallingMethod() {
        StackTraceElement[] stacktrace = Thread.currentThread().getStackTrace();
        StackTraceElement e = stacktrace[3];
        return e.getMethodName();
    }

    public int convertCommand(Command command) {
        if (command == OnOffType.ON) {
            return 1;
        } else {
            return 0;
        }
    }
}
