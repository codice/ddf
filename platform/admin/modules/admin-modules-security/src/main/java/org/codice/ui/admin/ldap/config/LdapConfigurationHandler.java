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

package org.codice.ui.admin.ldap.config;

import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAPS;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.NONE;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.TLS;
import static org.codice.ui.admin.ldap.config.LdapConfigurationHandler.LdapTestResultType.CANNOT_BIND;
import static org.codice.ui.admin.ldap.config.LdapConfigurationHandler.LdapTestResultType.CANNOT_CONFIGURE;
import static org.codice.ui.admin.ldap.config.LdapConfigurationHandler.LdapTestResultType.CANNOT_CONNECT;
import static org.codice.ui.admin.ldap.config.LdapConfigurationHandler.LdapTestResultType.SUCCESSFUL_BIND;
import static org.codice.ui.admin.ldap.config.LdapConfigurationHandler.LdapTestResultType.SUCCESSFUL_CONNECTION;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.NO_TEST_FOUND;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.REQUIRED_FIELDS;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.WARNING;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.buildMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.apache.cxf.common.util.StringUtils;
import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.api.ConfigurationMessage;
import org.codice.ui.admin.wizard.api.ProbeReport;
import org.codice.ui.admin.wizard.api.TestReport;
import org.codice.ui.admin.wizard.config.ConfigReport;
import org.codice.ui.admin.wizard.config.Configurator;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LDAPOptions;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;

import com.google.common.collect.ImmutableMap;

public class LdapConfigurationHandler implements ConfigurationHandler<LdapConfiguration> {

    public static final String LDAP_CONFIGURATION_HANDLER_ID = "ldap";

    // Test Ids
    public static final String LDAP_CONNECTION_TEST_ID = "testLdapConnection";

    public static final String LDAP_BIND_TEST_ID = "testLdapBind";

    public static final String LDAP_DIRECTORY_STRUCT_TEST_ID = "testLdapDirStruct";

    public static final String LDAP_QUERY_RESULTS_ID = "ldapQueryResults";

    // Probe Ids
    public static final String LDAP_QUERY_PROBE_ID = "ldapQuery";

    @Override
    public String getConfigurationHandlerId() {
        return LDAP_CONFIGURATION_HANDLER_ID;
    }

    @Override
    public List<LdapConfiguration> getConfigurations() {
        LdapConfiguration sampleConfig = new LdapConfiguration();
        sampleConfig.hostName("localhost")
                .port(1389)
                .encryptionMethod(LdapConfiguration.NONE)
                .bindUserDn("Example,Bind,User,DN")
                .bindUserPassword("*******")
                .userNameAttribute("User name attribute")
                .baseGroupDn("Example,Group,DN");
        return Arrays.asList(sampleConfig);
    }

    @Override
    public ProbeReport probe(String probeId, LdapConfiguration configuration) {

        // TODO: 11/14/16 Figure out this crappy map situation
        Map<String, Object> connectionRequiredFields = new HashMap<>();
        connectionRequiredFields.put("hostName", configuration.hostName());
        connectionRequiredFields.put("port", configuration.port());
        connectionRequiredFields.put("encryptionMethod", configuration.encryptionMethod());
        connectionRequiredFields.put("bindUserDn", configuration.bindUserDn());
        connectionRequiredFields.put("bindUserPassword", configuration.bindUserPassword());
        connectionRequiredFields.put("query", configuration.query());
        connectionRequiredFields.put("queryBase", configuration.queryBase());

        TestReport nullFields = cannotBeNullFields(connectionRequiredFields);
        if (nullFields.containsUnsuccessfulMessages()) {
            return new ProbeReport(nullFields.getMessages());
        }

        switch (probeId) {
        case LDAP_QUERY_PROBE_ID:
            // TODO: 11/14/16 Do checks on the connection
            LdapTestResult<Connection> connectionResult = bindUserToLdapConnection(configuration);
            List<SearchResultEntry> searchResults = getLdapQueryResults(connectionResult.value(),
                    configuration.query(),
                    configuration.queryBase());
            List<Map<String, String>> convertedSearchResults = new ArrayList<>();

            for (SearchResultEntry entry : searchResults) {
                Map<String, String> entryMap = new HashMap<>();
                for (Attribute attri : entry.getAllAttributes()) {
                    entryMap.put("name",
                            entry.getName()
                                    .toString());
                    entryMap.put(attri.getAttributeDescriptionAsString(),
                            attri.firstValueAsString());
                }
                convertedSearchResults.add(entryMap);
            }

            return new ProbeReport(new ArrayList<>()).addProbeResult(LDAP_QUERY_RESULTS_ID,
                    convertedSearchResults);
        }
        return new ProbeReport(Arrays.asList(buildMessage(FAILURE, "UNKNOWN PROBE ID")));
    }

    @Override
    public TestReport test(String testId, LdapConfiguration ldapConfiguration) {

        // TODO: 11/21/16 make sure that a dn is valid
        Map<String, Object> connectionRequiredFields = new HashMap<>();
        connectionRequiredFields.put("hostName", ldapConfiguration.hostName());
        connectionRequiredFields.put("port", ldapConfiguration.port());
        connectionRequiredFields.put("encryptionMethod", ldapConfiguration.encryptionMethod());

        Map<String, Object> bindRequiredFields = new HashMap<>(connectionRequiredFields);
        bindRequiredFields.put("bindUserDn", ldapConfiguration.bindUserDn());
        bindRequiredFields.put("bindUserPassword", ldapConfiguration.bindUserPassword());

        Map<String, Object> dirRequiredFields = new HashMap<>(bindRequiredFields);
        dirRequiredFields.put("baseUserDn", ldapConfiguration.baseUserDn());
        dirRequiredFields.put("baseGroupDn", ldapConfiguration.baseGroupDn());
        dirRequiredFields.put("userNameAttribute", ldapConfiguration.userNameAttribute());

        switch (testId) {
        case LDAP_CONNECTION_TEST_ID:
            TestReport cannotBeNullFieldsTest = cannotBeNullFields(connectionRequiredFields);
            if (cannotBeNullFieldsTest.containsUnsuccessfulMessages()) {
                return cannotBeNullFieldsTest;
            }
            return testLdapConnection(ldapConfiguration);

        case LDAP_BIND_TEST_ID:
            TestReport bindFieldsResults = cannotBeNullFields(bindRequiredFields);
            if (bindFieldsResults.containsUnsuccessfulMessages()) {
                return bindFieldsResults;
            }
            return testLdapBind(ldapConfiguration);

        case LDAP_DIRECTORY_STRUCT_TEST_ID:
            TestReport dirFieldsResults = cannotBeNullFields(dirRequiredFields);
            if (dirFieldsResults.containsUnsuccessfulMessages()) {
                return dirFieldsResults;
            }

            return testLdapDirectoryStructure(ldapConfiguration);
        }

        return new TestReport(new ConfigurationMessage(NO_TEST_FOUND));
    }

    @Override
    public TestReport persist(LdapConfiguration config) {

        Map<String, String> ldapStsConfig = new HashMap<>();

        String ldapUrl = "";
        boolean startTls = false;
        ldapStsConfig.put("ldapBindUserDn", config.bindUserDn());
        ldapStsConfig.put("ldapBindUserPass", config.bindUserPassword());
        ldapStsConfig.put("userNameAttribute", config.userNameAttribute());
        ldapStsConfig.put("userBaseDn", config.baseUserDn());
        ldapStsConfig.put("groupBaseDn", config.baseGroupDn());

        switch (config.encryptionMethod()) {
        case LDAPS:
            ldapUrl = "ldaps://";
            break;
        case TLS:
            startTls = true;
        case NONE:
            ldapUrl = "ldap://";
            break;
        }
        ldapStsConfig.put("ldapUrl", ldapUrl + config.hostName() + ":" + config.port());
        ldapStsConfig.put("startTls", Boolean.toString(startTls));

        Configurator configurator = new Configurator();
        configurator.startFeature("security-sts-ldaplogin");
        configurator.createManagedService("Ldap_Login_Config", ldapStsConfig);
        ConfigReport configReport = configurator.commit();
        if (!configReport.getFailedResults()
                .isEmpty()) {
            return new TestReport(buildMessage(ConfigurationMessage.MessageType.FAILURE,
                    "Unable to persist changes"));
        } else {
            return new TestReport(buildMessage(ConfigurationMessage.MessageType.SUCCESS,
                    "Successfully saved LDAP settings"));
        }
    }

    public TestReport testLdapConnection(LdapConfiguration ldapConfiguration) {

        Map<String, Object> requiredFields = ImmutableMap.of("hostName",
                ldapConfiguration.hostName(),
                "port",
                ldapConfiguration.port(),
                "encryptionMethod",
                ldapConfiguration.encryptionMethod());

        TestReport testResults = cannotBeNullFields(requiredFields);

        if (testResults.containsUnsuccessfulMessages()) {
            return testResults;
        }

        LdapTestResult<Connection> connectionTestResult = getLdapConnection(ldapConfiguration);

        if (connectionTestResult.type() == SUCCESSFUL_CONNECTION) {
            connectionTestResult.value()
                    .close();
            testResults.addMessage(buildMessage(SUCCESS, "Successfully connected to LDAP"));
            return testResults;
        }

        if (connectionTestResult.type() == CANNOT_CONFIGURE) {
            testResults.addMessage(buildMessage(FAILURE,
                    "Unable to create configuration used for the LDAP connection. The system may not be properly installed to setup LDAP."));
            return testResults;
        }

        //Since the test failed, trying other encryption methods
        List<String> encryptionMethodsToTry = new ArrayList<>();
        Collections.copy(LdapConfiguration.LDAP_ENCRYPTION_METHODS, encryptionMethodsToTry);
        encryptionMethodsToTry.remove(ldapConfiguration.encryptionMethod());

        List<LdapConfiguration> configsToTest = new ArrayList<>();

        encryptionMethodsToTry.stream()
                .forEach(encryptM -> configsToTest.add(ldapConfiguration.copy()
                        .encryptionMethod(encryptM)));

        for (LdapConfiguration testConfig : configsToTest) {
            LdapTestResult<Connection> connectionTestRetryResult = getLdapConnection(testConfig);

            if (connectionTestRetryResult.type() == SUCCESSFUL_CONNECTION) {
                connectionTestRetryResult.value()
                        .close();
                testResults.addMessage(buildMessage(WARNING,
                        "We were unable to connect to the host with the given encryption method but we were able successfully connect using the encryption method "
                                + testConfig.encryptionMethod()
                                + ". If this is acceptable, please change the encryption method field and resubmit."));
                return testResults;
            }
        }

        testResults.addMessage(buildMessage(FAILURE,
                "Unable to reach the specified host. We tried the other available encryption methods without success. Make sure your host and port are correct, your LDAP is running and that your network is not restricting access."));

        return testResults;
    }

    public TestReport testLdapBind(LdapConfiguration ldapConfiguration) {
        LdapTestResult<Connection> bindUserTestResult = bindUserToLdapConnection(ldapConfiguration);

        switch (bindUserTestResult.type()) {
        case SUCCESSFUL_BIND:
            return new TestReport(buildMessage(SUCCESS,
                    "Successfully binded user to the LDAP connection"));

        case CANNOT_BIND:
            return new TestReport(buildMessage(FAILURE,
                    "Unable to bind the user to the LDAP connection. Try a different username or password. Make sure the username is in the format of a distinguished name."));

        case CANNOT_CONNECT:
            return new TestReport(buildMessage(FAILURE,
                    "Unable to bind the user because there was a failure to reach the LDAP. Make sure your LDAP is running."));

        case CANNOT_CONFIGURE:
            return new TestReport(buildMessage(FAILURE,
                    "Unable to create configuration used for the LDAP connection. The system may not be properly installed to setup LDAP."));

        // TODO: tbatie - 11/4/16 - This should never happen, clean this part up
        default:
            return new TestReport(buildMessage(FAILURE, "Something went wrong"));
        }

    }

    public TestReport testLdapDirectoryStructure(LdapConfiguration config) {
        List<ConfigurationMessage> directoryStructureResults = new ArrayList<>();
        LdapTestResult<Connection> ldapConnectionResult = bindUserToLdapConnection(config);

        switch (ldapConnectionResult.type()) {
        case CANNOT_CONNECT:
            return new TestReport(buildMessage(FAILURE,
                    "Unable to connect to LDAP anymore, make sure the LDAP is running."));

        case CANNOT_BIND:
            return new TestReport(buildMessage(FAILURE,
                    "Unable to bind user to LDAP connection. Make sure the user still has the correct credentials to connect"));

        case CANNOT_CONFIGURE:
            return new TestReport(buildMessage(FAILURE,
                    "Unable to create configuration used for the LDAP connection. The system may not be properly installed to setup LDAP."));
        }

        Connection ldapConnection = ldapConnectionResult.value();
        List<SearchResultEntry> baseUsersResults = getLdapQueryResults(ldapConnection,
                "objectClass=*",
                config.baseUserDn());

        if (baseUsersResults.isEmpty()) {
            directoryStructureResults.add(buildMessage(FAILURE,
                    "The specified base user DN does not appear to exist"));
        } else if (baseUsersResults.size() <= 1) {
            directoryStructureResults.add(buildMessage(FAILURE,
                    "No users found in the base user DN").configId("baseUserDn"));
        } else {
            directoryStructureResults.add(buildMessage(SUCCESS,
                    "Found users in base user dn").configId("baseUserDn"));
            List<SearchResultEntry> userNameAttribute = getLdapQueryResults(ldapConnection,
                    config.userNameAttribute() + "=*",
                    config.baseUserDn());
            if (userNameAttribute.isEmpty()) {
                directoryStructureResults.add(buildMessage(FAILURE,
                        "No users found with the described attribute in the base user DN").configId(
                        "userNameAttribute"));
            } else {
                directoryStructureResults.add(buildMessage(SUCCESS,
                        "Users with given user attribute found in base user dn").configId(
                        "userNameAttribute"));
            }
        }

        List<SearchResultEntry> baseGroupResults = getLdapQueryResults(ldapConnection,
                "objectClass=*",
                config.baseUserDn());

        if (baseGroupResults.isEmpty()) {
            directoryStructureResults.add(buildMessage(FAILURE,
                    "The specified base group DN does not appear to exist").configId("baseGroupDn"));
        } else if (baseGroupResults.size() <= 1) {
            directoryStructureResults.add(buildMessage(FAILURE,
                    "No groups found in the base group DN").configId("baseGroupDn"));
        } else {
            directoryStructureResults.add(buildMessage(SUCCESS,
                    "Found groups in base group DN").configId("baseGroupDn"));
        }
        ldapConnection.close();
        return new TestReport(directoryStructureResults);
    }

    public LdapTestResult<Connection> getLdapConnection(LdapConfiguration ldapConfiguration) {

        LDAPOptions ldapOptions = new LDAPOptions();

        try {
            if (ldapConfiguration.encryptionMethod()
                    .equals(LDAPS)) {
                ldapOptions.setSSLContext(SSLContext.getDefault());
            } else if (ldapConfiguration.encryptionMethod()
                    .equals(TLS)) {
                ldapOptions.setUseStartTLS(true);
            }

            ldapOptions.addEnabledCipherSuite(System.getProperty("https.cipherSuites")
                    .split(","));
            ldapOptions.addEnabledProtocol(System.getProperty("https.protocols")
                    .split(","));

            //sets the classloader so it can find the grizzly protocol handler class
            ldapOptions.setProviderClassLoader(LdapConfigurationHandler.class.getClassLoader());

        } catch (Exception e) {
            return new LdapTestResult<>(CANNOT_CONFIGURE);
        }

        Connection ldapConnection;

        try {
            ldapConnection = new LDAPConnectionFactory(ldapConfiguration.hostName(),
                    ldapConfiguration.port(),
                    ldapOptions).getConnection();
        } catch (Exception e) {
            return new LdapTestResult<>(CANNOT_CONNECT);
        }

        return new LdapTestResult<>(SUCCESSFUL_CONNECTION, ldapConnection);
    }

    public LdapTestResult<Connection> bindUserToLdapConnection(
            LdapConfiguration ldapConfiguration) {

        LdapTestResult<Connection> ldapConnectionResult = getLdapConnection(ldapConfiguration);
        if (ldapConnectionResult.type() != SUCCESSFUL_CONNECTION) {
            return ldapConnectionResult;
        }

        Connection connection = ldapConnectionResult.value();

        try {
            connection.bind(ldapConfiguration.bindUserDn(),
                    ldapConfiguration.bindUserPassword()
                            .toCharArray());
        } catch (Exception e) {
            return new LdapTestResult<>(CANNOT_BIND);
        }

        return new LdapTestResult<>(SUCCESSFUL_BIND, connection);
    }

    public List<SearchResultEntry> getLdapQueryResults(Connection ldapConnection, String ldapQuery,
            String ldapSearchBaseDN) {

        final ConnectionEntryReader reader = ldapConnection.search(ldapSearchBaseDN,
                SearchScope.WHOLE_SUBTREE,
                ldapQuery);

        List<SearchResultEntry> entries = new ArrayList<>();

        try {
            while (reader.hasNext()) {
                if (!reader.isReference()) {
                    SearchResultEntry resultEntry = reader.readEntry();
                    entries.add(resultEntry);
                } else {
                    reader.readReference();
                }
            }
        } catch (IOException e) {
            reader.close();
        }

        reader.close();
        return entries;
    }

    public TestReport cannotBeNullFields(Map<String, Object> fieldsToCheck) {
        List<ConfigurationMessage> missingFields = new ArrayList<>();

        fieldsToCheck.entrySet()
                .stream()
                .filter(field -> field.getValue() == null && (field.getValue() instanceof String
                        && StringUtils.isEmpty((String) field.getValue())))
                .forEach(field -> missingFields.add(buildMessage(REQUIRED_FIELDS,
                        "Field cannot be empty").configId(field.getKey())));

        return new TestReport(missingFields);
    }

    public enum LdapTestResultType {
        SUCCESSFUL_CONNECTION, CANNOT_CONNECT, CANNOT_CONFIGURE, CANNOT_BIND, SUCCESSFUL_BIND
    }

    public static class LdapTestResult<T> {

        private LdapTestResultType type;

        private T value;

        public LdapTestResult(LdapTestResultType ldapTestResultType) {
            this.type = ldapTestResultType;
        }

        public LdapTestResult(LdapTestResultType ldapTestResultType, T value) {
            this.type = ldapTestResultType;
            this.value = value;
        }

        public T value() {
            return value;
        }

        public LdapTestResultType type() {
            return type;
        }
    }
}
