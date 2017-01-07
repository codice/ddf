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
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.LDAP_ENCRYPTION_METHODS;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.CANNOT_CONFIGURE;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.CANNOT_CONNECT;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.SUCCESSFUL_CONNECTION;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.toDescriptionMap;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.cannotBeNullFields;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.getLdapConnection;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.security.ldap.LdapConfiguration;

import com.google.common.collect.ImmutableMap;

public class ConnectTestMethod extends TestMethod<LdapConfiguration> {

    public static final String LDAP_CONNECTION_TEST_ID = "testLdapConnection";
    public static final String DESCRIPTION = "Attempts to connect to the given LDAP host";

    public static final String HOST_NAME = "hostName";
    public static final String PORT = "port";
    public static final String ENCRYPTION_METHOD = "encryptionMethod";

    // TODO: tbatie - 1/3/17 - Should we move those descriptions into configuration instead?
    public static final Map<String, String> REQUIRED_FIELDS = ImmutableMap.of(
            HOST_NAME, "Host name of the ldap url to attempt to connect to.",
            PORT, "Port of the ldap url to attempt to connect to.",
            ENCRYPTION_METHOD, "Encryption method to connect over. Must be either: " + Arrays.toString(LDAP_ENCRYPTION_METHODS.toArray()));

    public static final Map<String, String> SUCCESS_TYPES = toDescriptionMap(Arrays.asList(SUCCESSFUL_CONNECTION));
    public static final Map<String, String> FAILURE_TYPES = toDescriptionMap(Arrays.asList(CANNOT_CONFIGURE, CANNOT_CONNECT));

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
        // TODO: tbatie - 1/3/17 - Once the ldap configuration is self evaluating, this should be removed
        Map<String, Object> requiredFields = new HashMap<>();
        requiredFields.put(HOST_NAME, configuration.hostName());
        requiredFields.put(PORT, configuration.port());
        requiredFields.put(ENCRYPTION_METHOD, configuration.encryptionMethod());
        TestReport cannotBeNullFieldsTest = cannotBeNullFields(requiredFields);
        if (cannotBeNullFieldsTest.containsUnsuccessfulMessages()) {
            return cannotBeNullFieldsTest;
        }

        LdapTestingCommons.LdapConnectionAttempt connectionAttempt = getLdapConnection(configuration);
        if(connectionAttempt.connection() != null) {
            connectionAttempt.connection().close();
        }

        return createGeneralTestReport(SUCCESS_TYPES, FAILURE_TYPES, null, Arrays.asList(connectionAttempt.result().name()));


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
//                testResults.addMessage(buildMessage(WARNING,
//                        "We were unable to connect to the host with the given encryption method but we were able successfully connect using the encryption method "
//                                + testConfig.encryptionMethod()
//                                + ". If this is acceptable, please change the encryption method field and resubmit."));
//                return testResults;
//            }
//        }
//
//        testResults.addMessage(buildMessage(FAILURE,
//                "Unable to reach the specified host. We tried the other available encryption methods without success. Make sure your host and port are correct, your LDAP is running and that your network is not restricting access."));
    }
}
