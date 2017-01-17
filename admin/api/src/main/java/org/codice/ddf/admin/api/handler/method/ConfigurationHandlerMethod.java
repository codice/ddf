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

package org.codice.ddf.admin.api.handler.method;

import java.util.List;
import java.util.Map;

public abstract class ConfigurationHandlerMethod {

    //The following fields are used by the json parser although they look unused
    final String id;
    final String description;
    final List<String> requiredFields;
    final List<String> optionalFields;
    final Map<String, String> successTypes;
    final Map<String, String> failureTypes;
    final Map<String, String> warningTypes;

    public ConfigurationHandlerMethod(String id, String description,
            List<String> requiredFields, List<String> optionalFields,
            Map<String, String> successTypes, Map<String, String> failureTypes,
            Map<String, String> warningTypes) {
        this.id = id;
        this.description = description;
        this.requiredFields = requiredFields;
        this.optionalFields = optionalFields;
        this.successTypes = successTypes;
        this.failureTypes = failureTypes;
        this.warningTypes = warningTypes;
    }

    public String id() {
        return id;
    }
}
