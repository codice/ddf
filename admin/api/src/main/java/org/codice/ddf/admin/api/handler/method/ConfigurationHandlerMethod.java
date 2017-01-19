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

import com.google.gson.annotations.Expose;

public abstract class ConfigurationHandlerMethod {

    //The following fields are used by the json parser although they look unused
    @Expose
    final String id;
    @Expose
    final String description;
    @Expose
    final List<String> requiredFields;
    @Expose
    final List<String> optionalFields;
    @Expose
    final Map<String, String> successTypes;
    @Expose
    final Map<String, String> failureTypes;
    @Expose
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
