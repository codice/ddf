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
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.BIND_KDC;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.BIND_METHOD;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.BIND_REALM;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.BIND_USER_DN;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.BIND_USER_PASSWORD;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.ENCRYPTION_METHOD;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.HOST_NAME;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.PORT;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.CANNOT_BIND;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.CANNOT_CONFIGURE;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.CANNOT_CONNECT;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.SUCCESSFUL_BIND;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.toDescriptionMap;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.bindUserToLdapConnection;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.security.ldap.commons.LdapConfiguration;

public class BindUserTestMethod extends TestMethod<LdapConfiguration> {

    public static final String LDAP_BIND_TEST_ID = "testLdapBind";

    public static final String DESCRIPTION =
            "Attempts to bind the specified user to specified ldap connection";

    private static final Map<String, String> REQUIRED_FIELDS = LdapConfiguration.buildFieldMap(
            HOST_NAME,
            PORT,
            ENCRYPTION_METHOD,
            BIND_USER_DN,
            BIND_USER_PASSWORD,
            BIND_METHOD);

    private static final Map<String, String> OPTIONAL_FIELDS = LdapConfiguration.buildFieldMap(
            BIND_REALM,
            BIND_KDC);

    public static final Map<String, String> SUCCESS_TYPES =
            toDescriptionMap(Collections.singletonList(SUCCESSFUL_BIND));

    public static final Map<String, String> FAILURE_TYPES = toDescriptionMap(Arrays.asList(
            CANNOT_CONFIGURE,
            CANNOT_CONNECT,
            CANNOT_BIND));

    public BindUserTestMethod() {
        super(LDAP_BIND_TEST_ID,
                DESCRIPTION,
                REQUIRED_FIELDS,
                OPTIONAL_FIELDS,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                null);
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

        LdapTestingCommons.LdapConnectionAttempt bindConnectionAttempt = bindUserToLdapConnection(
                configuration);

        if (bindConnectionAttempt.result() == SUCCESSFUL_BIND) {
            bindConnectionAttempt.connection()
                    .close();
        }

        return createGeneralTestReport(SUCCESS_TYPES,
                FAILURE_TYPES,
                null,
                Arrays.asList(bindConnectionAttempt.result()
                        .name()));
    }
}
