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

package ddf.security.sts;

import org.apache.cxf.sts.StaticSTSProperties;
import org.apache.cxf.sts.token.provider.DefaultConditionsProvider;

import java.util.Map;

/**
 * property-placeholder misbehaves when set to reload, causing the bundle to bounce
 * for several minutes after a config change. This class replaces property-placeholder
 * for the beans that need to be updated with config changes.
 */
public class PropertyPlaceholderWrapper {

    private static final String STS_LIFETIME = "lifetime";

    private static final String STS_SIGNATURE_USERNAME = "signatureUsername";

    private static final String STS_ISSUER = "issuer";

    private static final String STS_ENCRYPTION_USERNAME = "encryptionUsername";

    private static final int DEFAULT_LIFETIME = 1800;

    private static final String DEFAULT_NAME = "localhost";

    private DefaultConditionsProvider samlConditionsProvider;

    private StaticSTSProperties stsProperties;

    public PropertyPlaceholderWrapper(DefaultConditionsProvider conditionsProvider,
            StaticSTSProperties properties) {
        samlConditionsProvider = conditionsProvider;
        stsProperties = properties;
        // set the default values in case there is no configuration
        samlConditionsProvider.setLifetime(DEFAULT_LIFETIME);
        stsProperties.setSignatureUsername(DEFAULT_NAME);
        stsProperties.setIssuer(DEFAULT_NAME);
        stsProperties.setEncryptionUsername(DEFAULT_NAME);
    }

    public void setStsMap(Map<String, Object> map) {
        samlConditionsProvider.setLifetime((Long) map.get(STS_LIFETIME));
        stsProperties.setSignatureUsername(String.valueOf(map.get(STS_SIGNATURE_USERNAME)));
        stsProperties.setIssuer(String.valueOf(map.get(STS_ISSUER)));
        stsProperties.setEncryptionUsername(String.valueOf(map.get(STS_ENCRYPTION_USERNAME)));
    }
}
