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

import ddf.security.PropertiesLoader;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.common.ext.WSPasswordCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Callback handler for signature and encryption properties files.
 */
public class PropertyCallbackHandler implements CallbackHandler {

    private static final String FILE_PREFIX = "file:";

    private String signatureProperties;

    private String encryptionProperties;

    private Map<String, String> signaturePropertiesMap = new HashMap<String, String>();

    private Map<String, String> encryptionPropertiesMap = new HashMap<String, String>();

    private void setPropertyMap(Map<String, String> map, String propertyLocation) {
        if (!map.isEmpty()) {
            map.clear();
        }

        Properties properties = PropertiesLoader.loadProperties(propertyLocation);

        map.putAll(PropertiesLoader.<String, String>toMap(properties));
    }

    @Override
    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof WSPasswordCallback) {
                WSPasswordCallback passwordCallback = (WSPasswordCallback) callback;
                if (WSPasswordCallback.DECRYPT == passwordCallback.getUsage()) {
                    String alias = encryptionPropertiesMap.get(Merlin.PREFIX + Merlin.KEYSTORE_ALIAS);
                    if (alias == null) {
                        alias = encryptionPropertiesMap.get(Merlin.OLD_PREFIX + Merlin.KEYSTORE_ALIAS);
                    }
                    if (alias != null && alias.equals(passwordCallback.getIdentifier())) {
                        passwordCallback.setPassword(encryptionPropertiesMap.get(Merlin.KEYSTORE_PASSWORD));
                    }
                } else if (WSPasswordCallback.SIGNATURE == passwordCallback.getUsage()) {
                    String alias = signaturePropertiesMap.get(Merlin.PREFIX + Merlin.KEYSTORE_ALIAS);
                    if (alias == null) {
                        alias = signaturePropertiesMap.get(Merlin.OLD_PREFIX + Merlin.KEYSTORE_ALIAS);
                    }
                    if (alias != null && alias.equals(passwordCallback.getIdentifier())) {
                        passwordCallback.setPassword(signaturePropertiesMap.get(Merlin.KEYSTORE_PASSWORD));
                    }
                }
            }
        }
    }

    public String getSignatureProperties() {
        return signatureProperties;
    }

    public void setSignatureProperties(String signatureProperties) {
        if (signatureProperties.startsWith(FILE_PREFIX)) {
            signatureProperties = signatureProperties.substring(FILE_PREFIX.length());
        }
        this.signatureProperties = signatureProperties;
        setPropertyMap(signaturePropertiesMap, signatureProperties);
    }

    public String getEncryptionProperties() {
        return encryptionProperties;
    }

    public void setEncryptionProperties(String encryptionProperties) {
        if (encryptionProperties.startsWith(FILE_PREFIX)) {
            encryptionProperties = encryptionProperties.substring(FILE_PREFIX.length());
        }
        this.encryptionProperties = encryptionProperties;
        setPropertyMap(encryptionPropertiesMap, encryptionProperties);
    }
}
