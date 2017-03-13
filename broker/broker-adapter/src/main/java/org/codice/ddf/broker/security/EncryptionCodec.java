/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.broker.security;

import java.util.Map;

import org.apache.activemq.artemis.utils.SensitiveDataCodec;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;

import ddf.security.encryption.EncryptionService;

public class EncryptionCodec implements SensitiveDataCodec<String> {

    EncryptionService encryptionService;

    @Override
    public String decode(Object o) throws Exception {
        if (getEncryptionService() == null) {
            return o.toString();
        }
        return getEncryptionService().decryptValue(o.toString());
    }

    @Override
    public String encode(Object o) throws Exception {
        if (getEncryptionService() == null) {
            return o.toString();
        }
        return getEncryptionService().encrypt(o.toString());
    }

    @Override
    public void init(Map<String, String> map) throws Exception {
        encryptionService = getEncryptionService();
    }

    private EncryptionService getEncryptionService() {
        if (encryptionService != null) {
            return encryptionService;
        }

        BundleContext context = getBundleContext();
        ServiceReference<EncryptionService> securityManagerRef = context.getServiceReference(
                EncryptionService.class);
        return context.getService(securityManagerRef);
    }

    private BundleContext getBundleContext() {
        Bundle bundle = FrameworkUtil.getBundle(EncryptionService.class);
        return bundle.getBundleContext();
    }
}
