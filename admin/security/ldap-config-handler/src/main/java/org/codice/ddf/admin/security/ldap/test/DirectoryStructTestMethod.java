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

import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.BASE_GROUP_DN;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.BASE_USER_DN;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.BIND_KDC;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.BIND_METHOD;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.BIND_REALM;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.BIND_USER_DN;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.BIND_USER_PASSWORD;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.ENCRYPTION_METHOD;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.HOST_NAME;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.PORT;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.USER_NAME_ATTRIBUTE;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.buildFieldMap;
import static org.codice.ddf.admin.api.handler.report.TestReport.createGeneralTestReport;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.BASE_GROUP_DN_NOT_FOUND;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.BASE_USER_DN_NOT_FOUND;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.CANNOT_BIND;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.CANNOT_CONFIGURE;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.CANNOT_CONNECT;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.FOUND_BASE_GROUP_DN;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.FOUND_BASE_USER_DN;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.FOUND_USER_NAME_ATTRIBUTE;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.NO_GROUPS_IN_BASE_GROUP_DN;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.NO_USERS_IN_BASE_USER_DN;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.SUCCESSFUL_BIND;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.USER_NAME_ATTRIBUTE_NOT_FOUND;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.toDescriptionMap;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionAttempt;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.bindUserToLdapConnection;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.getLdapQueryResults;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;

public class DirectoryStructTestMethod extends TestMethod<LdapConfiguration> {
    private static final String LDAP_DIRECTORY_STRUCT_TEST_ID = "testLdapDirStruct";

    private static final String DESCRIPTION =
            "Verifies that the specified directory structure, entries and required attributes to configure LDAP exist.";

    private static final Map<String, String> REQUIRED_FIELDS = buildFieldMap(HOST_NAME,
            PORT,
            ENCRYPTION_METHOD,
            BIND_USER_DN,
            BIND_USER_PASSWORD,
            BIND_METHOD,
            BASE_USER_DN,
            BASE_GROUP_DN,
            USER_NAME_ATTRIBUTE);

    private static final Map<String, String> OPTIONAL_FIELDS = buildFieldMap(
            BIND_REALM,
            BIND_KDC);

    private static final Map<String, String> SUCCESS_TYPES = toDescriptionMap(Arrays.asList(
            FOUND_BASE_USER_DN,
            FOUND_BASE_GROUP_DN,
            FOUND_USER_NAME_ATTRIBUTE));

    private static final Map<String, String> FAILURE_TYPES = toDescriptionMap(Arrays.asList(
            CANNOT_CONFIGURE,
            CANNOT_CONNECT,
            CANNOT_BIND,
            BASE_USER_DN_NOT_FOUND,
            BASE_GROUP_DN_NOT_FOUND,
            USER_NAME_ATTRIBUTE_NOT_FOUND));

    private static final Map<String, String> WARNING_TYPES = toDescriptionMap(Arrays.asList(
            NO_USERS_IN_BASE_USER_DN,
            NO_GROUPS_IN_BASE_GROUP_DN));

    public DirectoryStructTestMethod() {
        super(LDAP_DIRECTORY_STRUCT_TEST_ID,
                DESCRIPTION, REQUIRED_FIELDS, OPTIONAL_FIELDS,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                WARNING_TYPES);
    }

    @Override
    public TestReport test(LdapConfiguration configuration) {
        List<ConfigurationMessage> checkMessages =
                configuration.checkRequiredFields(REQUIRED_FIELDS.keySet());

        if (CollectionUtils.isNotEmpty(checkMessages)) {
            return new ProbeReport(checkMessages);
        }

        checkMessages = configuration.testConditionalBindFields();
        if (CollectionUtils.isNotEmpty(checkMessages)) {
            return new ProbeReport(checkMessages);
        }

        LdapConnectionAttempt connectionAttempt = bindUserToLdapConnection(configuration);

        if (connectionAttempt.result() != SUCCESSFUL_BIND) {
            return createGeneralTestReport(SUCCESS_TYPES,
                    FAILURE_TYPES,
                    WARNING_TYPES,
                    Arrays.asList(connectionAttempt.result()
                            .name()));
        }

        Connection ldapConnection = connectionAttempt.connection();
        List<SearchResultEntry> baseUsersResults = getLdapQueryResults(ldapConnection,
                "objectClass=*",
                configuration.baseUserDn());

        Map<String, String> resultsWithConfigIds = new HashMap<>();

        if (baseUsersResults.isEmpty()) {
            resultsWithConfigIds.put(BASE_USER_DN_NOT_FOUND.name(), BASE_USER_DN);
        } else if (baseUsersResults.size() <= 1) {
            resultsWithConfigIds.put(NO_USERS_IN_BASE_USER_DN.name(), BASE_USER_DN);
        } else {
            resultsWithConfigIds.put(FOUND_BASE_USER_DN.name(), BASE_USER_DN);
            List<SearchResultEntry> userNameAttribute = getLdapQueryResults(ldapConnection,
                    configuration.userNameAttribute() + "=*",
                    configuration.baseUserDn());
            if (userNameAttribute.isEmpty()) {
                resultsWithConfigIds.put(USER_NAME_ATTRIBUTE_NOT_FOUND.name(), USER_NAME_ATTRIBUTE);
            } else {
                resultsWithConfigIds.put(FOUND_USER_NAME_ATTRIBUTE.name(), USER_NAME_ATTRIBUTE);
            }
        }

        List<SearchResultEntry> baseGroupResults = getLdapQueryResults(ldapConnection,
                "objectClass=*",
                configuration.baseGroupDn());

        if (baseGroupResults.isEmpty()) {
            resultsWithConfigIds.put(BASE_GROUP_DN_NOT_FOUND.name(), BASE_GROUP_DN);
        } else if (baseGroupResults.size() <= 1) {
            resultsWithConfigIds.put(NO_GROUPS_IN_BASE_GROUP_DN.name(), BASE_GROUP_DN);
        } else {
            resultsWithConfigIds.put(FOUND_BASE_GROUP_DN.name(), BASE_GROUP_DN);
        }
        ldapConnection.close();

        return createGeneralTestReport(SUCCESS_TYPES,
                FAILURE_TYPES,
                WARNING_TYPES,
                resultsWithConfigIds);
    }
}
