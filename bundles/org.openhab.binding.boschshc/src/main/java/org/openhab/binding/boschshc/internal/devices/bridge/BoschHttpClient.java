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
package org.openhab.binding.boschshc.internal.devices.bridge;

import static org.eclipse.jetty.http.HttpMethod.GET;

import java.nio.charset.StandardCharsets;
import java.security.KeyStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.client.api.ContentResponse;
import org.eclipse.jetty.client.util.StringContentProvider;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.openhab.binding.boschshc.internal.exceptions.PairingFailedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;

/**
 * HTTP client using own context with private & Bosch Certs
 * to pair and connect to the Bosch Smart Home Controller.
 *
 * @author Gerd Zanker - Initial contribution
 */
@NonNullByDefault
public class BoschHttpClient extends HttpClient {

    private static final int MAX_PAIR_TRIES = 12;

    private final Logger logger = LoggerFactory.getLogger(BoschHttpClient.class);

    private String ipAddress;
    private String systempassword;

    public BoschHttpClient(String ipAddress, String systempassword, SslContextFactory sslContextFactory) {
        super(sslContextFactory);
        this.ipAddress = ipAddress;
        this.systempassword = systempassword;
    }

    private String getCertFromSslContextFactory() throws KeyStoreException, CertificateEncodingException {
        Certificate cert = this.getSslContextFactory().getKeyStore().getCertificate(BoschSslUtil.getBoschSHCId());
        return Base64.getEncoder().encodeToString(cert.getEncoded());
    }

    public void checkAccessAndPairIfNecessary() throws InterruptedException, PairingFailedException {
        // test if pairing is needed
        int counter = 0;
        boolean accessPossible = isAccessPossible();
        while (!accessPossible) {
            // sleep some seconds after every try, except the first one
            if (counter > 0) {
                logger.trace("Last Pairing failed, starting retry number {}/{} in few seconds", counter,
                        MAX_PAIR_TRIES);
                Thread.sleep(15000);
            } else {
                logger.debug("Pairing needed, because access to Bosch SmartHomeController not possible.");
            }
            // Timeout after max tries with an exception that pairing failed
            if (counter >= MAX_PAIR_TRIES) {
                // all tries during the last MAX_PAIR_TRIES*1500 milliseconds failed
                throw new PairingFailedException("Pairing aborted, because it failed too many times!");
            }
            // try to pair if no access is possible
            counter++;
            doPairing();
            // check access
            accessPossible = isAccessPossible();
        }
    }

    public boolean isAccessPossible() {
        try {
            ContentResponse contentResponse = newRequest("https://" + ipAddress + ":8444/smarthome/devices")
                    .header("Content-Type", "application/json").header("Accept", "application/json").method(GET).send();
            String content = contentResponse.getContentAsString();
            logger.debug("Access check response complete: {} - return code: {}", content, contentResponse.getStatus());
            return true;
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            logger.debug("Access check response failed because of {}!", e.getMessage());
            return false;
        }
    }

    public boolean doPairing() {
        logger.debug("Starting pairing openHAB Client with Bosch SmartHomeController!");
        logger.debug("Please press the Bosch SHC button until LED starts blinking");

        ContentResponse contentResponse;
        try {
            String publicCert = getCertFromSslContextFactory();
            logger.debug("Pairing this Client '{}' with SHC {} using Cert {}", BoschSslUtil.getBoschSHCId(), ipAddress,
                    publicCert);

            // JSON Rest content
            Map<String, String> items = new HashMap<>();
            items.put("@type", "client");
            items.put("id", BoschSslUtil.getBoschSHCId()); // Client Id contains the unique OpenHab instance Id
            items.put("name", "oss_OpenHAB_Binding"); // Client name according to
                                                      // https://github.com/BoschSmartHome/bosch-shc-api-docs#terms-and-conditions
            items.put("primaryRole", "ROLE_RESTRICTED_CLIENT");
            items.put("certificate", "-----BEGIN CERTIFICATE-----\r" + publicCert + "\r-----END CERTIFICATE-----");

            contentResponse = this.POST("https://" + ipAddress + ":8443/smarthome/clients")
                    .header("Systempassword",
                            Base64.getEncoder().encodeToString(systempassword.getBytes(StandardCharsets.UTF_8)))
                    .header("Content-Type", "application/json").header("Accept", "application/json")
                    .content(new StringContentProvider(new Gson().toJson(items))).send();

            logger.debug("Pairing response complete: {} - return code: {}", contentResponse.getContentAsString(),
                    contentResponse.getStatus());
            if (201 == contentResponse.getStatus()) {
                logger.info("Pairing successful.");
                return true;
            } else {
                logger.info("Pairing failed with response status {}.", contentResponse.getStatus());
                return false;
            }

        } catch (InterruptedException | TimeoutException | CertificateEncodingException | KeyStoreException e) {
            logger.warn("Pairing failed with exception {}", e.getMessage());
            return false;
        } catch (ExecutionException e) {
            // javax.net.ssl.SSLHandshakeException: General SSLEngine problem
            // => usually the pairing failed, because hardware button was not pressed.
            logger.warn("Pairing failed with exception {}, was the Bosch SHC button pressed?", e.getMessage());
            return false;
        }
    }
}
