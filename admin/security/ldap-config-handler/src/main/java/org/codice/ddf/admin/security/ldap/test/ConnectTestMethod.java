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

import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.ENCRYPTION_METHOD;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.HOST_NAME;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.PORT;
import static org.codice.ddf.admin.api.handler.report.TestReport.createGeneralTestReport;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.CANNOT_CONFIGURE;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.CANNOT_CONNECT;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.SUCCESSFUL_CONNECTION;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.toDescriptionMap;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.getLdapConnection;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.TestReport;

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
    public TestReport test(LdapConfiguration configuration) {
        List<ConfigurationMessage> checkMessages =
                // TODO: Use the validate method, not this
                configuration.checkRequiredFields(new HashSet(REQUIRED_FIELDS));
        if (CollectionUtils.isNotEmpty(checkMessages)) {
            return new TestReport(checkMessages);
        }

        LdapTestingCommons.LdapConnectionAttempt connectionAttempt =
                getLdapConnection(configuration);
        if (connectionAttempt.connection() != null) {
            connectionAttempt.connection()
                    .close();
        }

        return createGeneralTestReport(SUCCESS_TYPES,
                FAILURE_TYPES,
                null,
                Arrays.asList(connectionAttempt.result()
                        .name()));

        //      ---  Experimental code to try connecting to ldap using all other configuration options
        //
        //        List<String> encryptionMethodsToTry = new ArrayList<>();
        //        Collections.copy(Arrays.asList(LdapConfiguration.LDAP_ENCRYPTION_METHODS),
        //                encryptionMethodsToTry);
        //        encryptionMethodsToTry.remove(configuration.encryptionMethod());
        //
        //        List<LdapConfiguration> configsToTest = new ArrayList<>();
        //
        //        encryptionMethodsToTry.stream()
        //                .forEach(encryptM -> configsToTest.add(configuration.copy()
        //                        .encryptionMethod(encryptM)));
        //
        //        for (LdapConfiguration testConfig : configsToTest) {
        //            LdapConfigurationHandler.LdapTestResult<Connection> connectionTestRetryResult = getLdapConnection(testConfig);
        //
        //            if (connectionTestRetryResult.type() == SUCCESSFUL_CONNECTION) {
        //                connectionTestRetryResult.value()
        //                        .close();
        //                testResults.messages(buildMessage(WARNING,
        //                        "We were unable to connect to the host with the given encryption method but we were able successfully connect using the encryption method "
        //                                + testConfig.encryptionMethod()
        //                                + ". If this is acceptable, please change the encryption method field and resubmit."));
        //                return testResults;
        //            }
        //        }
        //
        //        testResults.messages(buildMessage(FAILURE,
        //                "Unable to reach the specified host. We tried the other available encryption methods without success. Make sure your host and port are correct, your LDAP is running and that your network is not restricting access."));
    }

}
