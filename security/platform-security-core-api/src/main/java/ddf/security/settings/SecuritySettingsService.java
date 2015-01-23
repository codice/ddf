/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.security.settings;

import org.apache.cxf.configuration.jsse.TLSClientParameters;

import java.security.KeyStore;
import java.util.Arrays;
import java.util.List;

/**
 * Service that can be used to obtain settings used for security purposes (SSL/TLS/Keystores...etc)
 */
public interface SecuritySettingsService {

    static final List<String> SSL_ALLOWED_ALGORITHMS = Arrays.asList(".*_WITH_AES_.*");

    static final List<String> SSL_DISALLOWED_ALGORITHMS = Arrays
            .asList(".*_WITH_NULL_.*", ".*_DH_anon_.*");

    static final String SSL_KEYSTORE_JAVA_PROPERTY = "javax.net.ssl.keyStore";

    static final String SSL_KEYSTORE_PASSWORD_JAVA_PROPERTY = "javax.net.ssl.keyStorePassword";

    static final String SSL_TRUSTSTORE_JAVA_PROPERTY = "javax.net.ssl.trustStore";

    static final String SSL_TRUSTSTORE_PASSWORD_JAVA_PROPERTY = "javax.net.ssl.trustStorePassword";

    TLSClientParameters getTLSParameters();

    KeyStore getKeystore();

    KeyStore getTruststore();

}
