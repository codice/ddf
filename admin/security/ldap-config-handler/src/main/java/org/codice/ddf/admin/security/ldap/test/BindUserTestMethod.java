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
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.CANNOT_BIND;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.CANNOT_CONFIGURE;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.CANNOT_CONNECT;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.SUCCESSFUL_BIND;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.toDescriptionMap;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.bindUserToLdapConnection;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.cannotBeNullFields;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.testConditionalBindFields;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.security.ldap.LdapConfiguration;

import com.google.common.collect.ImmutableMap;

public class BindUserTestMethod extends TestMethod<LdapConfiguration> {

    public static final String LDAP_BIND_TEST_ID = "testLdapBind";
    public static final String DESCRIPTION = "Attempts to bind the specified user to specified ldap connection";

    public static final String BIND_USER_DN = "bindUserDn";
    public static final String BIND_USER_PASSWORD = "bindUserPassword";
    public static final String BIND_METHOD = "bindMethod";

    public static final Map<String, String> REQUIRED_FIELDS = new ImmutableMap.Builder<String, String>()
            .putAll(ConnectTestMethod.REQUIRED_FIELDS)
            .put(BIND_USER_DN, "DN of the user that will be bound to the LDAP connection.")
            .put(BIND_USER_PASSWORD, "Password of the user to be bound to the LDAP connection.")
            // TODO: tbatie - 1/3/17 - Put the bind methods into the LdapConfiguration then fix this
            .put(BIND_METHOD, "The type of method to bind the user with. Must be either: " + null)
            .build();

    public static final Map<String, String> SUCCESS_TYPES = toDescriptionMap(Arrays.asList(
            SUCCESSFUL_BIND));

    public static final Map<String, String> FAILURE_TYPES = toDescriptionMap(Arrays.asList(
            CANNOT_CONFIGURE,
            CANNOT_CONNECT,
            CANNOT_BIND));

    public BindUserTestMethod() {
        super(LDAP_BIND_TEST_ID, DESCRIPTION, REQUIRED_FIELDS, null, SUCCESS_TYPES, FAILURE_TYPES, null);
    }

    @Override
    public TestReport test(LdapConfiguration configuration) {
        // TODO: tbatie - 1/3/17 - Once the ldap configuration is self evaluating, this should be removed
        Map<String, Object> bindRequiredFields = new HashMap<>();
        bindRequiredFields.put(ConnectTestMethod.HOST_NAME, configuration.hostName());
        bindRequiredFields.put(ConnectTestMethod.PORT, configuration.port());
        bindRequiredFields.put(ConnectTestMethod.ENCRYPTION_METHOD, configuration.encryptionMethod());
        bindRequiredFields.put(BIND_USER_DN, configuration.bindUserDn());
        bindRequiredFields.put(BIND_USER_PASSWORD, configuration.bindUserPassword());
        bindRequiredFields.put(BIND_METHOD, configuration.bindUserMethod());

        TestReport bindFieldsResults = cannotBeNullFields(bindRequiredFields);
        if (bindFieldsResults.containsUnsuccessfulMessages()) {
            return bindFieldsResults;
        }
        bindFieldsResults = testConditionalBindFields(configuration);
        if (bindFieldsResults.containsUnsuccessfulMessages()) {
            return bindFieldsResults;
        }
        // -----

        LdapTestingCommons.LdapConnectionAttempt bindConnectionAttempt = bindUserToLdapConnection(configuration);

        if(bindConnectionAttempt.result() == SUCCESSFUL_BIND) {
            bindConnectionAttempt.connection().close();
        }

        return createGeneralTestReport(SUCCESS_TYPES, FAILURE_TYPES, null, Arrays.asList(bindConnectionAttempt.result().name()));
    }
}
