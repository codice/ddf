/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.catalog.content.monitor.configurators;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;

import org.apache.commons.io.IOUtils;
import org.ops4j.pax.exam.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class KeystoreTruststoreConfigurator {
    private static final Logger LOG = LoggerFactory.getLogger(KeystoreTruststoreConfigurator.class);

    private static final String KEYSTORE_TYPE = "javax.net.ssl.keyStoreType";

    private static final String KEYSTORE_PATH = "javax.net.ssl.keyStore";

    private static final String KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword";

    private static final String TRUSTSTORE_TYPE = "javax.net.ssl.trustStoreType";

    private static final String TRUSTSTORE_PATH = "javax.net.ssl.trustStore";

    private static final String TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";

    private static final String PASSWORD = "changeit";

    public static Option createKeystoreAndTruststore(InputStream keystore, InputStream truststore) {
        String keystorePath = copyKeystoreAndGetPath(keystore);
        String truststorePath = copyTruststoreAndGetPath(truststore);

        return setupSystemProperties(keystorePath, truststorePath);
    }

    private static Option setupSystemProperties(String keystorePath, String truststorePath) {
        return composite(systemProperty(KEYSTORE_TYPE).value("jks"),
                systemProperty(KEYSTORE_PATH).value(keystorePath),
                systemProperty(KEYSTORE_PASSWORD).value(PASSWORD),

                systemProperty(TRUSTSTORE_TYPE).value("jks"),
                systemProperty(TRUSTSTORE_PATH).value(truststorePath),
                systemProperty(TRUSTSTORE_PASSWORD).value(PASSWORD));
    }

    private static String copyKeystoreAndGetPath(InputStream keystore) {
        try {
            return createTempFile("keystore", keystore);
        } catch (IOException e) {
            LOG.error("Failed to copy and create keystore.jks", e);
        }

        return "";
    }

    private static String copyTruststoreAndGetPath(InputStream truststore) {
        try {
            return createTempFile("truststore", truststore);
        } catch (IOException e) {
            LOG.error("Failed to copy and create truststore.jks", e);
        }

        return "";
    }

    private static String createTempFile(String filename, InputStream contents) throws IOException {
        File tempFile = Files.createTempFile(filename, ".jks")
                .toFile();
        tempFile.deleteOnExit();
        FileOutputStream outputStream = new FileOutputStream(tempFile);
        IOUtils.copy(contents, outputStream);
        return tempFile.getCanonicalPath();
    }
}
