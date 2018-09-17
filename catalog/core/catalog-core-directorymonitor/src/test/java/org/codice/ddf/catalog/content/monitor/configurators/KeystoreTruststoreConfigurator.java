/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.content.monitor.configurators;

import static org.ops4j.pax.exam.CoreOptions.composite;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFilePut;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.replaceConfigurationFile;

import java.io.File;
import org.ops4j.pax.exam.Option;

public class KeystoreTruststoreConfigurator {
  private static final String KEYSTORE_TYPE = "javax.net.ssl.keyStoreType";

  private static final String KEYSTORE_PATH = "javax.net.ssl.keyStore";

  private static final String KEYSTORE_PASSWORD = "javax.net.ssl.keyStorePassword";

  private static final String KEYSTORE_PATH_VALUE = "etc/keystores/serverKeystore.jks";

  private static final String TRUSTSTORE_TYPE = "javax.net.ssl.trustStoreType";

  private static final String TRUSTSTORE_PATH = "javax.net.ssl.trustStore";

  private static final String TRUSTSTORE_PASSWORD = "javax.net.ssl.trustStorePassword";

  private static final String TRUSTSTORE_PATH_VALUE = "etc/keystores/serverTruststore.jks";

  private static final String PASSWORD = "changeit";

  public static Option createKeystoreAndTruststore(File keystore, File truststore) {
    return composite(
        replaceConfigurationFile(KEYSTORE_PATH_VALUE, keystore),
        replaceConfigurationFile(TRUSTSTORE_PATH_VALUE, truststore),
        setupSystemProperties());
  }

  private static Option setupSystemProperties() {
    return composite(
        editConfigurationFilePut("etc/custom.system.properties", KEYSTORE_TYPE, "jks"),
        editConfigurationFilePut("etc/custom.system.properties", KEYSTORE_PASSWORD, PASSWORD),
        editConfigurationFilePut(
            "etc/custom.system.properties", KEYSTORE_PATH, KEYSTORE_PATH_VALUE),
        editConfigurationFilePut("etc/custom.system.properties", TRUSTSTORE_TYPE, "jks"),
        editConfigurationFilePut("etc/custom.system.properties", TRUSTSTORE_PASSWORD, PASSWORD),
        editConfigurationFilePut(
            "etc/custom.system.properties", TRUSTSTORE_PATH, TRUSTSTORE_PATH_VALUE));
  }
}
