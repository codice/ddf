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
package org.codice.ddf.admin.security.ldap.embedded.persist;

import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.CREDENTIAL_STORE;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.LDAP_USE_CASE;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.LOGIN;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.LOGIN_AND_CREDENTIAL_STORE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.FAILED_PERSIST;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.SUCCESSFUL_PERSIST;

import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.config.security.ldap.EmbeddedLdapConfiguration;
import org.codice.ddf.admin.api.configurator.Configurator;
import org.codice.ddf.admin.api.configurator.OperationReport;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.report.Report;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class DefaultEmbeddedLdapPersistMethod extends PersistMethod<EmbeddedLdapConfiguration> {

    public static final String DEFAULT_CONFIGURATIONS_ID = "defaults";

    public static final String DESCRIPTION =
            "Starts up the Opendj Embedded App and installs default realm and/or attribute store configurations.";

    public static final List<String> REQUIRED_FIELDS = ImmutableList.of(LDAP_USE_CASE);

    public static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(SUCCESSFUL_PERSIST,
            "Successfully started and saved Embedded LDAP configurations.");

    public static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(FAILED_PERSIST,
            "Failed to start Embedded LDAP or install a default configuration file.");

    public DefaultEmbeddedLdapPersistMethod() {
        super(DEFAULT_CONFIGURATIONS_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                null,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                null);
    }

    @Override
    public Report persist(EmbeddedLdapConfiguration configuration) {
        Configurator configurator = new Configurator();
        Report testReport = new Report(configuration.validate(REQUIRED_FIELDS));
        if(testReport.containsFailureMessages()) {
            return testReport;
        }

        configurator.startFeature("opendj-embedded");
        // TODO: tbatie - 1/12/17 - Installing default configs should have a feature req on the features with the configs they intend to start
        switch (configuration.ldapUseCase()) {
        case LOGIN:
            configurator.startFeature("security-sts-ldaplogin");
            configurator.startFeature("ldap-embedded-default-stslogin-config");
            break;
        case CREDENTIAL_STORE:
            configurator.startFeature("security-sts-ldapclaimshandler");
            configurator.startFeature("ldap-embedded-default-claimshandler-config");
            break;
        case LOGIN_AND_CREDENTIAL_STORE:
            configurator.startFeature("security-sts-ldaplogin");
            configurator.startFeature("security-sts-ldapclaimshandler");
            configurator.startFeature("ldap-embedded-default-configs");
            break;
        }
        OperationReport report = configurator.commit();

        if (report.containsFailedResults()) {
            return new Report(new ConfigurationMessage(FAILURE,
                    FAILED_PERSIST,
                    FAILURE_TYPES.get(FAILED_PERSIST)));
        }

        return new Report(new ConfigurationMessage(SUCCESS,
                SUCCESSFUL_PERSIST,
                SUCCESS_TYPES.get(SUCCESSFUL_PERSIST)));
    }
}
