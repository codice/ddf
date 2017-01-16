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

    private String id;
    private String description;
    private List<String> requiredFields;
    private List<String> optionalFields;
    private Map<String, String> successTypes;
    private Map<String, String> failureTypes;
    private Map<String, String> warningTypes;

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

    public String description() {
        return description;
    }

    public List<String> requireFields() {
        return requiredFields;
    }

    public List<String> optionalFields() {
        return optionalFields;
    }

    public Map<String, String> successTypes() {
        return successTypes;
    }

    public Map<String, String> failureTypes() {
        return failureTypes;
    }

    public Map<String, String> warningTypes() {
        return warningTypes;
    }
}
