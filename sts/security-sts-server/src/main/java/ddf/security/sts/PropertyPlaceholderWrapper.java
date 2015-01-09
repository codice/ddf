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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * property-placeholder misbehaves when set to reload, causing the bundle to bounce
 * for several minutes after a config change. This class replaces property-placeholder
 * for the beans that need to be updated with config changes.
 */
public class PropertyPlaceholderWrapper {

    public static final String STS_LIFETIME = "lifetime";

    public static final String STS_SIGNATURE_USERNAME = "signatureUsername";

    public static final String STS_ISSUER = "issuer";

    public static final String STS_ENCRYPTION_USERNAME = "encryptionUsername";

    private DefaultConditionsProvider samlConditionsProvider;

    private StaticSTSProperties stsProperties;

    public PropertyPlaceholderWrapper(DefaultConditionsProvider conditionsProvider,
            StaticSTSProperties properties) {
        samlConditionsProvider = conditionsProvider;
        stsProperties = properties;
    }

    public void setStsMap(Map<String, Object> map) {
        samlConditionsProvider.setLifetime((Long)map.get(STS_LIFETIME));
        stsProperties.setSignatureUsername(String.valueOf(map.get(STS_SIGNATURE_USERNAME)));
        stsProperties.setIssuer(String.valueOf(map.get(STS_ISSUER)));
        stsProperties.setEncryptionUsername(String.valueOf(map.get(STS_ENCRYPTION_USERNAME)));
    }
}
