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
package org.codice.ddf.admin.configurator.impl;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import com.google.common.io.Files;

class ConfigValidator {
    static void validateString(String bundleSymName, String msg) {
        if (StringUtils.isBlank(bundleSymName)) {
            throw new IllegalArgumentException(msg);
        }
    }

    static void validateMap(Map properties, String msg) {
        if (MapUtils.isEmpty(properties)) {
            throw new IllegalArgumentException(msg);
        }
    }

    static void validatePropertiesPath(Path propFile) {
        if (propFile == null) {
            throw new IllegalArgumentException("Null path provided");
        }

        String fileExtension = Files.getFileExtension(propFile.toString());
        if (!(fileExtension.equalsIgnoreCase("props")
                || fileExtension.equalsIgnoreCase("properties"))) {
            throw new IllegalArgumentException(String.format("%s is not a properties file",
                    propFile.toString()));
        }

        String ddfHomeProp = System.getProperty("ddf.home");
        validateString(ddfHomeProp, "No value set for system property ddf.home");

        Path ddfHomePath = Paths.get(ddfHomeProp);
        if (!propFile.startsWith(ddfHomePath)) {
            throw new IllegalArgumentException(String.format("%s is not beneath the %s root",
                    propFile.toString(),
                    ddfHomePath.toString()));
        }

        if (propFile.getParent()
                .equals(ddfHomePath)) {
            throw new IllegalArgumentException("Invalid attempt to edit file in ddf.home directory");
        }

        if (propFile.getParent()
                .endsWith("etc")) {
            throw new IllegalArgumentException(
                    "Invalid attempt to edit system file in /etc directory");
        }
    }
}
