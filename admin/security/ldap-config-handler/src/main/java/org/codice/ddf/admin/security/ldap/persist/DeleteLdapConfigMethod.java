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
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.FAILED_PERSIST;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.SUCCESSFUL_PERSIST;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;

import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.report.Report;
import org.codice.ddf.admin.api.persist.ConfigReport;
import org.codice.ddf.admin.api.persist.Configurator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class DeleteLdapConfigMethod extends PersistMethod<LdapConfiguration> {

    public static final String DELETE_CONFIG_ID = "delete";
    public static final String DESCRIPTION = "Deletes the specified LDAP configuration.";

    public static final List<String> REQUIRED_FIELDS = ImmutableList.of(SERVICE_PID);
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
    public Report persist(LdapConfiguration config) {
        Report validatedReport =
                // TODO adimka Move validation to use the validate method instead of this stuff
                new Report(config.checkRequiredFields(new HashSet(REQUIRED_FIELDS)));
        if (validatedReport.containsFailureMessages()) {
            return validatedReport;
        }

        Configurator configurator = new Configurator();
        configurator.deleteManagedService(config.servicePid());
        ConfigReport report = configurator.commit();
        if (!report.getFailedResults()
                .isEmpty()) {
            return new Report(buildMessage(FAILURE,
                    FAILED_PERSIST,
                    FAILURE_TYPES.get(FAILED_PERSIST)));
        } else {
            return new Report(buildMessage(SUCCESS,
                    SUCCESSFUL_PERSIST,
                    SUCCESS_TYPES.get(SUCCESSFUL_PERSIST)));
        }
    }
}
