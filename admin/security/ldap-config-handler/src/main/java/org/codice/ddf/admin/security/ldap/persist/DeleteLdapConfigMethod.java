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

package org.codice.ddf.admin.security.ldap.persist;

import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.SERVICE_PID;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.buildFieldMap;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.FAILED_PERSIST;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.SUCCESSFUL_PERSIST;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;

import java.util.Map;

import org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.api.persist.ConfigReport;
import org.codice.ddf.admin.api.persist.Configurator;

import com.google.common.collect.ImmutableMap;

public class DeleteLdapConfigMethod extends PersistMethod<LdapConfiguration> {

    public static final String DELETE_CONFIG_ID = "delete";
    public static final String DESCRIPTION = "Deletes the specified LDAP configuration.";

    public static final Map<String, String> REQUIRED_FIELDS = buildFieldMap(SERVICE_PID);
    public static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(SUCCESSFUL_PERSIST, "Successfully deleted configuration.");
    public static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(FAILED_PERSIST, "Unable to delete configuration.");

    public DeleteLdapConfigMethod() {
        super(DELETE_CONFIG_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                null,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                null);
    }

    @Override
    public TestReport persist(LdapConfiguration config) {
        TestReport validatedReport =
                new TestReport(config.checkRequiredFields(REQUIRED_FIELDS.keySet()));
        if (validatedReport.containsFailureMessages()) {
            return validatedReport;
        }

        Configurator configurator = new Configurator();
        configurator.deleteManagedService(config.servicePid());
        ConfigReport report = configurator.commit();
        if (!report.getFailedResults()
                .isEmpty()) {
            return new TestReport(buildMessage(FAILURE,
                    FAILED_PERSIST,
                    FAILURE_TYPES.get(FAILED_PERSIST)));
        } else {
            return new TestReport(buildMessage(SUCCESS,
                    SUCCESSFUL_PERSIST,
                    SUCCESS_TYPES.get(SUCCESSFUL_PERSIST)));
        }
    }
}
