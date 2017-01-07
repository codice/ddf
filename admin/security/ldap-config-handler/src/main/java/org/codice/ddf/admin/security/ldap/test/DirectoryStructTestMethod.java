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

import static org.codice.ddf.admin.api.handler.report.TestReport.createGeneralTestReport;
import static org.codice.ddf.admin.security.ldap.test.BindUserTestMethod.BIND_METHOD;
import static org.codice.ddf.admin.security.ldap.test.BindUserTestMethod.BIND_USER_DN;
import static org.codice.ddf.admin.security.ldap.test.BindUserTestMethod.BIND_USER_PASSWORD;
import static org.codice.ddf.admin.security.ldap.test.ConnectTestMethod.ENCRYPTION_METHOD;
import static org.codice.ddf.admin.security.ldap.test.ConnectTestMethod.HOST_NAME;
import static org.codice.ddf.admin.security.ldap.test.ConnectTestMethod.PORT;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionAttempt;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.BASE_GROUP_DN_NOT_FOUND;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.BASE_USER_DN_NOT_FOUND;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.CANNOT_BIND;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.CANNOT_CONFIGURE;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.CANNOT_CONNECT;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.FOUND_BASE_GROUP_DN;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.FOUND_BASE_USER_DN;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.FOUND_USER_NAME_ATTRIBUTE;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.NO_GROUPS_IN_BASE_GROUP_DN;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.NO_USERS_IN_BASE_USER_DN;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.SUCCESSFUL_BIND;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.USER_NAME_ATTRIBUTE_NOT_FOUND;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.toDescriptionMap;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.bindUserToLdapConnection;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.cannotBeNullFields;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.getLdapQueryResults;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.security.ldap.LdapConfiguration;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;

import com.google.common.collect.ImmutableMap;


public class DirectoryStructTestMethod extends TestMethod<LdapConfiguration> {

    public static final String LDAP_DIRECTORY_STRUCT_TEST_ID = "testLdapDirStruct";
    public static final String DESCRIPTION = "Verifies that the specified directory structure, enteries and required attributes to configure LDAP exist.";

    public static final String BASE_USER_DN = "baseUserDn";
    public static final String BASE_GROUP_DN = "baseGroupDn";
    public static final String USER_NAME_ATTRIBUTE = "userNameAttribute";

    public static final Map<String, String> REQUIRED_FIELDS = new ImmutableMap.Builder<String, String>()
            .putAll(BindUserTestMethod.REQUIRED_FIELDS)
            .put(BASE_USER_DN, "The DN containing users that will be used for LDAP authentication")
            .put(BASE_GROUP_DN, "The DN containing groups associated to the users for LDAP authentication")
            .put(USER_NAME_ATTRIBUTE, "The attribute of the users that should be as a display name.")
            .build();

    public static final Map<String, String> SUCCESS_TYPES = toDescriptionMap(Arrays.asList(
            FOUND_BASE_USER_DN,
            FOUND_BASE_GROUP_DN,
            FOUND_USER_NAME_ATTRIBUTE));

    public static final Map<String, String> FAILURE_TYPES = toDescriptionMap(Arrays.asList(
            CANNOT_CONFIGURE,
            CANNOT_CONNECT,
            CANNOT_BIND,
            BASE_USER_DN_NOT_FOUND,
            BASE_GROUP_DN_NOT_FOUND, USER_NAME_ATTRIBUTE_NOT_FOUND));

    public static final Map<String, String> WARNING_TYPES = toDescriptionMap(Arrays.asList(
            NO_USERS_IN_BASE_USER_DN,
            NO_GROUPS_IN_BASE_GROUP_DN));

    public DirectoryStructTestMethod() {
        super(LDAP_DIRECTORY_STRUCT_TEST_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                null,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                WARNING_TYPES);
    }

    @Override
    public TestReport test(LdapConfiguration configuration) {
        // TODO: tbatie - 1/3/17 - Once the ldap configuration is self evaluating, this should be removed
        Map<String, Object> requiredFields = new HashMap<>();
        requiredFields.put(HOST_NAME, configuration.hostName());
        requiredFields.put(PORT, configuration.port());
        requiredFields.put(ENCRYPTION_METHOD, configuration.encryptionMethod());
        requiredFields.put(BIND_USER_DN, configuration.bindUserDn());
        requiredFields.put(BIND_USER_PASSWORD, configuration.bindUserPassword());
        requiredFields.put(BIND_METHOD, configuration.bindUserMethod());
        requiredFields.put(BASE_USER_DN, configuration.baseUserDn());
        requiredFields.put(BASE_GROUP_DN, configuration.baseGroupDn());
        requiredFields.put(USER_NAME_ATTRIBUTE, configuration.userNameAttribute());
        TestReport dirFieldsResults = cannotBeNullFields(requiredFields);
        if (dirFieldsResults.containsUnsuccessfulMessages()) {
            return dirFieldsResults;
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

        return createGeneralTestReport(SUCCESS_TYPES, FAILURE_TYPES, WARNING_TYPES, resultsWithConfigIds);
    }
}
