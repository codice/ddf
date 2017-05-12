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
package org.codice.ddf.platform.util.uuidgenerator.impl;

import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.lang3.StringUtils;
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator;

public class UuidGeneratorImpl implements UuidGenerator {

    private Pattern hexPattern = Pattern.compile("^[0-9A-Fa-f]+$");

    private Pattern hexPatternWithHypens = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    private Boolean removeHyphens = true;

    public void setRemoveHyphens(Boolean removeHyphens) {
        this.removeHyphens = removeHyphens;
    }

    @Override
    public String generateUuid() {
        String uuid = UUID.randomUUID()
                .toString();
        if (removeHyphens) {
            uuid = uuid.replaceAll("-", "");
        }
        return uuid;
    }

    @Override
    public boolean validateUuid(String uuid) {
        if (StringUtils.isEmpty(uuid)) {
            return false;
        }

        if (removeHyphens && uuid.length() == 32) {
            return hexPattern.matcher(uuid)
                    .matches();
        } else if (!removeHyphens && uuid.length() == 36) {
            return hexPatternWithHypens.matcher(uuid)
                    .matches();
        }
        return false;
    }
}
