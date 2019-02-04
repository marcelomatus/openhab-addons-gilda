/**
 * Copyright (c) 2010-2019 Contributors to the openHAB project
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
package org.openhab.binding.mqtt.internal.ssl;

import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.net.ssl.X509TrustManager;

import org.eclipse.jdt.annotation.NonNullByDefault;
import org.eclipse.jdt.annotation.Nullable;

/**
 * This is a custom {@link X509TrustManager}. {@link Pin} objects can be added and will
 * be used in the checkServerTrusted() method to determine if a connection can be trusted.
 *
 * @author David Graeff - Initial contribution
 */
@NonNullByDefault
public class PinTrustManager implements X509TrustManager {
    List<Pin> pins = new ArrayList<>();
    protected @Nullable PinnedCallback callback;

    /**
     * Adds a pin (certificate key, public key) to the trust manager. If a connections has assigned pins,
     * it will not accept any other certificates or public keys anymore!
     *
     * @param pin The pin
     */
    public void addPinning(Pin pin) {
        pins.add(pin);
    }

    public void setCallback(PinnedCallback callback) {
        this.callback = callback;
    }

    @Override
    public X509Certificate[] getAcceptedIssuers() {
        return new X509Certificate[0];
    }

    @Override
    public void checkClientTrusted(X509Certificate @Nullable [] certs, @Nullable String authType)
            throws CertificateException {
        if ((certs == null) || (certs.length == 0)) {
            throw new IllegalArgumentException();
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public void checkServerTrusted(X509Certificate @Nullable [] certs, @Nullable String authType)
            throws CertificateException {
        if ((certs == null) || (certs.length == 0)) {
            throw new IllegalArgumentException();
        }

        final PinMessageDigest digestForSigAlg = getMessageDigestForSigAlg(certs[0].getSigAlgName());
        final PinnedCallback callback = this.callback;

        // All pins have to accept the connection
        for (Pin pin : pins) {
            byte[] origData = getEncoded(pin.getType(), certs[0]);

            // If in learning mode: Learn new signature algorithm and hash and notify listeners
            if (pin.isLearning()) {
                pin.setCheckMode(digestForSigAlg, digestForSigAlg.digest(origData));
                if (callback != null) {
                    callback.pinnedLearnedHash(pin);
                }
                continue;
            } else {
                final PinMessageDigest hashDigest = pin.hashDigest;
                if (hashDigest == null) {
                    throw new CertificateException("No hashDigest given!");
                }

                // Check if hash is equal
                final byte[] digestData = hashDigest.digest(origData);
                if (pin.isEqual(digestData)) {
                    continue;
                }
                // This pin does not accept the connection
                if (callback != null) {
                    callback.pinnedConnectionDenied(pin);
                }
                throw new CertificateException(pin.getType().name() + " pinning denied access. Destination pin is "
                        + hashDigest.toHexString(digestData) + "' but expected: " + pin.toString());
            }
        }
        // All pin instances passed, the connection is accepted
        if (callback != null) {
            callback.pinnedConnectionAccepted();
        }
    }

    protected byte[] getEncoded(PinType type, X509Certificate cert) throws CertificateEncodingException {
        switch (type) {
            case CERTIFICATE_TYPE:
                return cert.getEncoded();
            case PUBLIC_KEY_TYPE:
                return cert.getPublicKey().getEncoded();
        }
        throw new CertificateEncodingException("Type unknown");
    }

    /**
     * A signature name depends on the security provider but usually follows
     * https://docs.oracle.com/javase/8/docs/technotes/guides/security/StandardNames.html#Signature.
     * E.g.: "SHA256withRSA". We need "SHA" and "256" to initialize a {@link PinMessageDigest}.
     */
    PinMessageDigest getMessageDigestForSigAlg(String sigAlg) throws CertificateException {
        final Matcher matcher = Pattern.compile("(\\D*)(\\d+)").matcher(sigAlg);
        matcher.find();
        try {
            return new PinMessageDigest(matcher.group(1) + "-" + matcher.group(2));
        } catch (NoSuchAlgorithmException e) {
            throw new CertificateException(e);
        }
    }

}
