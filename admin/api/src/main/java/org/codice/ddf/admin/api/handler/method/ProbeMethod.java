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

import java.util.Map;

import org.codice.ddf.admin.api.config.Configuration;
import org.codice.ddf.admin.api.handler.report.ProbeReport;

public abstract class ProbeMethod<S extends Configuration> extends ConfigurationHandlerMethod {

    private Map<String, String> returnTypes;

    // TODO: tbatie - 1/12/17 - Remove this constructor and enforce the returnTypes field
    public ProbeMethod(String id, String description, Map<String, String> requiredFields,
            Map<String, String> optionalFields, Map<String, String> successTypes,
            Map<String, String> failureTypes, Map<String, String> warningTypes) {
        super(id,
                description,
                requiredFields,
                optionalFields,
                successTypes,
                failureTypes,
                warningTypes);
    }

    public ProbeMethod(String id, String description, Map<String, String> requiredFields,
            Map<String, String> optionalFields, Map<String, String> successTypes,
            Map<String, String> failureTypes, Map<String, String> warningTypes, Map<String, String> returnTypes) {
        super(id,
                description,
                requiredFields,
                optionalFields,
                successTypes,
                failureTypes,
                warningTypes);

        this.returnTypes = returnTypes;
    }

    public abstract ProbeReport probe(S configuration);

    public Map<String, String> getReturnTypes() {
        return this.returnTypes;
    }
}
