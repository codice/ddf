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
package org.codice.ddf.admin.api.validation;

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.createInvalidFieldMsg;
import static org.codice.ddf.admin.api.services.CswServiceProperties.CSW_FACTORY_PIDS;
import static org.codice.ddf.admin.api.services.OpensearchServiceProperties.OPENSEARCH_FACTORY_PID;
import static org.codice.ddf.admin.api.services.WfsServiceProperties.WFS_FACTORY_PIDS;
import static org.codice.ddf.admin.api.validation.ValidationUtils.validateString;

import java.util.List;

import org.codice.ddf.admin.api.handler.ConfigurationMessage;

public class SourceValidationUtils {

    public static List<ConfigurationMessage> validateWfsFactoryPid(String factoryPid,
            String configId) {
        List<ConfigurationMessage> errors = validateString(factoryPid, configId);
        if (errors.isEmpty() && !WFS_FACTORY_PIDS.contains(factoryPid)) {
            errors.add(createInvalidFieldMsg("Unknown factory PID type \"" + factoryPid
                            + "\". Wfs factory pid must be one of: " + String.join(",", WFS_FACTORY_PIDS),
                    configId));
        }
        return errors;
    }

    public static List<ConfigurationMessage> validateCswFactoryPid(String factoryPid, String configId) {
        List<ConfigurationMessage> errors = validateString(factoryPid, configId);
        if (errors.isEmpty() && !CSW_FACTORY_PIDS.contains(factoryPid)) {
            errors.add(createInvalidFieldMsg("Unknown factory PID type \"" + factoryPid + "\". CSW factory pid must be one of: " + String.join(",", CSW_FACTORY_PIDS), configId));
        }
        return errors;
    }

    public static List<ConfigurationMessage> validateOpensearchFactoryPid(String factoryPid, String configId) {
        List<ConfigurationMessage> errors = validateString(factoryPid, configId);
        if (errors.isEmpty() && !OPENSEARCH_FACTORY_PID.equals(factoryPid)) {
            errors.add(createInvalidFieldMsg("Unknown factory PID type \"" + factoryPid + "\". OpenSearch factory pid must be " + OPENSEARCH_FACTORY_PID, configId));
        }
        return errors;
    }

    public static List<ConfigurationMessage> validateCswOutputSchema(String outputSchema, String configId) {
        // TODO: tbatie - 1/17/17 - Validate the output schema
        return validateString(outputSchema, configId);
    }
}
