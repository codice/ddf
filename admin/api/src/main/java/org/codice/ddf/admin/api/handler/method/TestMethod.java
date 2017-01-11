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

import org.codice.ddf.admin.api.handler.Configuration;
import org.codice.ddf.admin.api.handler.report.TestReport;

public abstract class TestMethod<S extends Configuration> extends ConfigurationHandlerMethod {

    public TestMethod(String id, String description, Map<String, String> requiredFields,
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

    public abstract TestReport test(S configuration);
}
