/**
 * Copyright (c) 2010-2017 by the respective copyright holders.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.mihome.internal.ssl;

import java.io.InputStream;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A helper class for building a {@link SSLContext}
 *
 * @author Svilen Valkanov
 */

public class SSLContextBuilder {

    private static final String X_509_CERTIFICATE = "X.509";
    private static final String JKS_KEYSTORE_TYPE = "JKS";
    private static final String SSL_PROTOCOL = "SSL";

    private final Logger logger = LoggerFactory.getLogger(SSLContextBuilder.class);

    private BundleContext bundleContext;
    private TrustManager[] trustedManagers;
    private KeyManager[] keyManagers;
    private SecureRandom randomGenerator;

    private SSLContextBuilder(BundleContext bundleContext) {
        this.bundleContext = bundleContext;
    }

    /**
     * Creates a SSLContextBuilder that can load key stores and certificates from a bundle
     *
     * @param bundleContext - used to load files from a bundle
     * @return SSLContextBuilder
     */

    public static SSLContextBuilder create(BundleContext bundleContext) {
        return new SSLContextBuilder(bundleContext);
    }

    /**
     * Initializes the {@link SSLContext} using the appended {@link TrustManager}s and {@link KeyManager}s and
     * {@link SecureRandom} generator
     *
     * @return SSLContext or null if no {@link TrustManager}s or {@link KeyManager}s are set
     */
    public SSLContext build() {
        try {
            if (trustedManagers != null || keyManagers != null) {
                SSLContext sslContext = SSLContext.getInstance(SSL_PROTOCOL);
                sslContext.init(keyManagers, trustedManagers, randomGenerator);
                return sslContext;
            }
        } catch (KeyManagementException e) {
            logger.error("A KeyManagementException occurred", e);
        } catch (NoSuchAlgorithmException e) {
            logger.error("A NoSuchAlgorithmException occurred", e);
        }
        return null;
    }

    /**
     * Loads a {@link KeyStore} from the bundle, creates {@link TrustManager}s from it and sets them.
     * Accepted key store type is jks
     *
     * @param directory - directory path relative to the bundle base dir
     * @param keyStoreName - the name of the key store
     * @param keyStorePass - the key store password
     * @return SSLContextBuilder
     */
    public SSLContextBuilder withTrustManagers(String directory, String keyStoreName, String keyStorePass) {
        URL url = getFileURL(bundleContext, directory, keyStoreName);
        if (url != null) {
            try {
                KeyStore keyStore = KeyStore.getInstance(JKS_KEYSTORE_TYPE);
                InputStream is = url.openStream();
                keyStore.load(is, keyStorePass.toCharArray());
                is.close();

                TrustManagerFactory trustManagerFactory = TrustManagerFactory
                        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(keyStore);

                this.trustedManagers = trustManagerFactory.getTrustManagers();
                logger.info("KeyStore {} loaded from directory {}", keyStoreName, directory);
            } catch (Exception e) {
                logger.error("Error occured while loading keys store {} : ", keyStoreName, e);
            }
        }
        return this;
    }

    /**
     * Loads a {@link KeyStore} from the bundle, creates a {@link KeyManager}s from it and sets them.
     * Accepted key store type is jks
     *
     * @param directory - directory path relative to the bundle base dir
     * @param keyStoreName - the name of the key store
     * @param keyStorePass - the key store password
     * @return SSLContextBuilder
     */
    public SSLContextBuilder withKeyManagers(String directory, String keyStoreName, String keyStorePass) {
        URL url = getFileURL(bundleContext, directory, keyStoreName);
        if (url != null) {
            try {
                KeyStore keyStore = KeyStore.getInstance(JKS_KEYSTORE_TYPE);
                InputStream is = url.openStream();
                keyStore.load(is, keyStorePass.toCharArray());
                is.close();

                KeyManagerFactory keyManagerFactory = KeyManagerFactory
                        .getInstance(KeyManagerFactory.getDefaultAlgorithm());
                keyManagerFactory.init(keyStore, keyStorePass.toCharArray());
                this.keyManagers = keyManagerFactory.getKeyManagers();
                logger.info("KeyStore {} loaded from directory {}", keyStoreName, directory);
            } catch (Exception e) {
                logger.error("Error occured while loading keys from store {} : ", keyStoreName, e);
            }

        }
        return this;
    }

    public SSLContextBuilder withSecure(SecureRandom secureRandom) {
        this.randomGenerator = secureRandom;
        return this;
    }

    /**
     * Loads a {@link Certificate} from the bundle, creates {@link TrustManager} from it and sets it.
     * Accepted certificate type is X.509
     *
     * @param directory - directory path relative to the bundle base dir
     * @param certificateName - the name of the key certificate file
     * @return SSLContextBuilder
     */
    public SSLContextBuilder withTrustedCertificate(String directory, String certificateName) {
        URL url = getFileURL(bundleContext, directory, certificateName);
        if (url != null) {
            try {
                // The keystore has to be initialized before adding the certificate, otherwise exception will be thrown
                KeyStore keyStore = KeyStore.getInstance(JKS_KEYSTORE_TYPE);
                // It is valid to call load(null) it actually creates new InputStream and initializes the KeyStore with
                // it
                keyStore.load(null);

                InputStream is = url.openStream();
                CertificateFactory factory = CertificateFactory.getInstance(X_509_CERTIFICATE);
                X509Certificate certificate = (X509Certificate) factory.generateCertificate(is);
                keyStore.setCertificateEntry("mihome", certificate);
                is.close();

                TrustManagerFactory trustManagerFactory = TrustManagerFactory
                        .getInstance(TrustManagerFactory.getDefaultAlgorithm());
                trustManagerFactory.init(keyStore);

                this.trustedManagers = trustManagerFactory.getTrustManagers();
                logger.info("Certificate {} loaded from directory {}", certificateName, directory);
            } catch (Exception e) {
                logger.error("Error occured while loading certificate {} : ", certificateName, e);
            }
        }
        return this;
    }

    private URL getFileURL(BundleContext bundleContext, String directory, String file) {
        Enumeration<URL> certURLsEnum = bundleContext.getBundle().findEntries(directory, file, false);

        if (certURLsEnum != null) {
            List<URL> certURLs = Collections.list(certURLsEnum);
            if (certURLs.size() == 1) {
                return certURLs.get(0);
            }
        }

        logger.warn("File {}/{} not found in bundle {}", directory, file, bundleContext.getBundle().getSymbolicName());
        return null;
    }
}
