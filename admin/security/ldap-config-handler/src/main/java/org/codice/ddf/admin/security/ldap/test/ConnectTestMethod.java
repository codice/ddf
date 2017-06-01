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

package org.codice.ddf.admin.security.ldap.test;

import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.ENCRYPTION_METHOD;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.HOST_NAME;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.PORT;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.CANNOT_CONFIGURE;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.CANNOT_CONNECT;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.SUCCESSFUL_CONNECTION;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.toDescriptionMap;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.getLdapConnection;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.codice.ddf.admin.api.config.ldap.LdapConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.Report;

import com.google.common.collect.ImmutableList;

public class ConnectTestMethod extends TestMethod<LdapConfiguration> {

    private static final String LDAP_CONNECTION_TEST_ID = "connection";

    private static final String DESCRIPTION = "Attempts to connect to the given LDAP host";

    private static final List<String> REQUIRED_FIELDS = ImmutableList.of(
            HOST_NAME,
            PORT,
            ENCRYPTION_METHOD);

    private static final Map<String, String> SUCCESS_TYPES =
            toDescriptionMap(Collections.singletonList(SUCCESSFUL_CONNECTION));

    private static final Map<String, String> FAILURE_TYPES = toDescriptionMap(ImmutableList.of(
            CANNOT_CONFIGURE,
            CANNOT_CONNECT));

    public ConnectTestMethod() {
        super(LDAP_CONNECTION_TEST_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                null,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                null);
    }

    @Override
    public Report test(LdapConfiguration configuration) {
        List<ConfigurationMessage> checkMessages = configuration.validate(REQUIRED_FIELDS);
        if (CollectionUtils.isNotEmpty(checkMessages)) {
            return new Report(checkMessages);
        }

        LdapTestingCommons.LdapConnectionAttempt connectionAttempt =
                getLdapConnection(configuration);
        if (connectionAttempt.connection() != null) {
            connectionAttempt.connection()
                    .close();
        }

        return Report.createReport(SUCCESS_TYPES,
                FAILURE_TYPES,
                null,
                Arrays.asList(connectionAttempt.result()
                        .name()));
    }

}
