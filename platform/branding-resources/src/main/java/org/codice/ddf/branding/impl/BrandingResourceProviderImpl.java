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
 */
package org.codice.ddf.branding.impl;

import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.IOUtils;
import org.apache.felix.webconsole.WebConsoleUtil;
import org.codice.ddf.branding.BrandingResourceProvider;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

public class BrandingResourceProviderImpl implements BrandingResourceProvider {

    @Override
    public byte[] getResourceAsBytes(String path) throws IOException {
        Bundle bundle = getBundle(WebConsoleUtil.class);
        if (bundle != null) {
            URL entry = bundle.getEntry(path);
            if (entry != null) {
                return IOUtils.toByteArray(entry.openConnection()
                        .getInputStream());
            }
        }
        return new byte[0];
    }

    // package-private for unit testing
    Bundle getBundle(Class<?> aClass) {
        return FrameworkUtil.getBundle(aClass);
    }
}
