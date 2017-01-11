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
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.LDAP_USE_CASES;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.LOGIN;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.LOGIN_AND_CREDENTIAL_STORE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;

import java.util.Arrays;
import java.util.Map;

import org.codice.ddf.admin.api.config.security.ldap.EmbeddedLdapConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.api.persist.ConfigReport;
import org.codice.ddf.admin.api.persist.Configurator;

import com.google.common.collect.ImmutableMap;

public class DefaultEmbeddedLdapPersistMethod extends PersistMethod<EmbeddedLdapConfiguration> {

    public static final String DEFAULT_CONFIGURATIONS_ID = "defaultConfigs";

    public static final String DESCRIPTION =
            "Starts up the Opendj Embedded App and installs default realm and/or attribute store configurations.";

    public static final String LDAP_USE_CASE = "ldapUseCase";

    public static final String SUCCESSFUL_PERSIST = "SUCCESSFUL_PERSIST";

    public static final String FAILED_STARTING_LDAP = "FAILED_STARTING_LDAP";

    public static final Map<String, String> REQUIRED_FIELDS = ImmutableMap.of(LDAP_USE_CASE,
            "Host name of the ldap url to attempt to connect to."
                    + Arrays.toString(LDAP_USE_CASES.toArray()));

    public static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(SUCCESSFUL_PERSIST,
            "Successfully started and saved Embedded LDAP configurations.");

    public static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(FAILED_STARTING_LDAP,
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
    public TestReport persist(EmbeddedLdapConfiguration configuration) {
        Configurator configurator = new Configurator();
        configurator.startFeature("opendj-embedded");

        switch (configuration.ldapUseCase()) {
        case LOGIN:
            configurator.startFeature("ldap-embedded-default-stslogin-config");
            break;
        case CREDENTIAL_STORE:
            configurator.startFeature("ldap-embedded-default-claimshandler-config");
            break;
        case LOGIN_AND_CREDENTIAL_STORE:
            configurator.startFeature("ldap-embedded-default-configs");
            break;
        }
        ConfigReport report = configurator.commit();

        if (report.containsFailedResults()) {
            return new TestReport(new ConfigurationMessage(FAILURE,
                    FAILED_STARTING_LDAP,
                    FAILURE_TYPES.get(FAILED_STARTING_LDAP)));
        }

        return new TestReport(new ConfigurationMessage(SUCCESS,
                SUCCESSFUL_PERSIST,
                SUCCESS_TYPES.get(SUCCESSFUL_PERSIST)));
    }
}
