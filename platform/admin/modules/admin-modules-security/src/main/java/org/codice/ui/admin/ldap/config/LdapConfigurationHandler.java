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

import static org.codice.ui.admin.ldap.config.LdapConfiguration.CREDENTIAL_STORE;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAPS;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_USE_CASES;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LOGIN;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LOGIN_AND_CREDENTIAL_STORE;
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
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.SSLContext;

import org.apache.cxf.common.util.StringUtils;
import org.codice.ddf.configuration.PropertyResolver;
import org.codice.ui.admin.wizard.api.CapabilitiesReport;
import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.api.ConfigurationMessage;
import org.codice.ui.admin.wizard.api.ProbeReport;
import org.codice.ui.admin.wizard.api.TestReport;
import org.codice.ui.admin.wizard.config.ConfigReport;
import org.codice.ui.admin.wizard.config.Configurator;
import org.codice.ui.admin.wizard.config.ConfiguratorException;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LDAPOptions;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.requests.BindRequest;
import org.forgerock.opendj.ldap.requests.DigestMD5SASLBindRequest;
import org.forgerock.opendj.ldap.requests.GSSAPISASLBindRequest;
import org.forgerock.opendj.ldap.requests.Requests;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

public class LdapConfigurationHandler implements ConfigurationHandler<LdapConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigurationHandler.class);

    public static final String LDAP_CONFIGURATION_HANDLER_ID = "ldap";

    // Test Ids
    public static final String LDAP_CONNECTION_TEST_ID = "testLdapConnection";

    public static final String LDAP_BIND_TEST_ID = "testLdapBind";

    public static final String LDAP_DIRECTORY_STRUCT_TEST_ID = "testLdapDirStruct";

    public static final String LDAP_ATTRIBUTE_MAPPING_TEST_ID = "testAttributeMapping";
    // Probe Ids
    public static final String LDAP_QUERY_PROBE_ID = "ldapQuery";

    public static final String LDAP_QUERY_RESULTS_ID = "ldapQueryResults";

    public static final String DISCOVER_LDAP_DIR_STRUCT_ID = "directoryStructure";

    public static final String BIND_USER_EXAMPLE = "bindUserExample";

    public static final String ATTRIBUTE_MAP_ID = "subjectAttributeMap";

    public static final String SUBJECT_CLAIMS_ID = "subjectClaims";

    public static final String LDAP_USER_ATTRIBUTES = "ldapUserAttributes";

    @Override
    public String getConfigurationHandlerId() {
        return LDAP_CONFIGURATION_HANDLER_ID;
    }

    @Override
    public Class<LdapConfiguration> getConfigClass() {
        return LdapConfiguration.class;
    }

    @Override
    public List<LdapConfiguration> getConfigurations() {
        Configurator configurator = new Configurator();
        if (configurator.isFeatureStarted("security-sts-ldaplogin")) {
            try {
                Map<String, Map<String, Object>> configs = configurator.getManagedServiceConfigs(
                        "Ldap_Login_Config");
                if (!configs.isEmpty()) {
                    ArrayList<LdapConfiguration> configurations = new ArrayList<>(configs.size());
                    for (Map.Entry<String, Map<String, Object>> row : configs.entrySet()) {
                        LdapConfiguration config = new LdapConfiguration();
                        Map<String, Object> props = row.getValue();

                        config.pid(row.getKey())
                                .bindUserDn((String) props.get("ldapBindUserDn"))
                                .bindUserPassword((String) props.get("ldapBindUserPass"))
                                .bindUserMethod((String) props.get("bindMethod"))
                                .bindKdcAddress((String) props.get("kdcAddress"))
                                .bindRealm((String) props.get("realm"))
                                .userNameAttribute((String) props.get("userNameAttribute"))
                                .baseUserDn((String) props.get("userBaseDn"))
                                .baseGroupDn((String) props.get("groupBaseDn"));
                        URI ldapUri = getUriFromProperty((String) props.get("ldapUrl"));
                        config.encryptionMethod(ldapUri.getScheme());
                        config.hostName(ldapUri.getHost());
                        config.port(ldapUri.getPort());
                        if ((Boolean) props.get("startTls")) {
                            config.encryptionMethod(TLS);
                        }

                        configurations.add(config);
                    }
                    return configurations;
                } else {
                    return getDefaultConfiguration();
                }
            } catch (ConfiguratorException | MalformedURLException e) {
                LOGGER.info("Error retrieving factory configurations", e);
                return getDefaultConfiguration();
            }
        } else {
            return getDefaultConfiguration();
        }
    }

    private URI getUriFromProperty(String ldapUrl) throws MalformedURLException {
        ldapUrl = PropertyResolver.resolveProperties(ldapUrl);
        if (!ldapUrl.matches("\\w*://.*")) {
            ldapUrl = "ldap://" + ldapUrl;
        }

        return URI.create(ldapUrl);
    }

    private List<LdapConfiguration> getDefaultConfiguration() {
        LdapConfiguration config = new LdapConfiguration();
        config.hostName("localhost")
                .port(1389)
                .encryptionMethod(LdapConfiguration.NONE)
                .bindUserDn("Example,Bind,User,DN")
                .bindUserPassword("*******")
                .userNameAttribute("User name attribute")
                .baseGroupDn("Example,Group,DN");

        return Collections.singletonList(config);
    }

    @Override
    public CapabilitiesReport getCapabilities() {
        return new CapabilitiesReport(LdapConfiguration.class.getSimpleName(),
                LdapConfiguration.class);
    }

    @Override
    public ProbeReport probe(String probeId, LdapConfiguration configuration) {

        switch (probeId) {
        case DISCOVER_LDAP_DIR_STRUCT_ID:
            return getDefaultDirectoryStructure(configuration);
        case BIND_USER_EXAMPLE:
            switch (configuration.ldapType()) {
            case "activeDirectory":
                return new ProbeReport(new ArrayList<>()).addProbeResult("bindUserDn",
                        "user@domain");
            default:
                return new ProbeReport(new ArrayList<>()).addProbeResult("bindUserDn", "cn=admin");
            }
            // TODO RAP 07 Dec 16:

        case LDAP_QUERY_PROBE_ID:
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

        case ATTRIBUTE_MAP_ID:
            // TODO: tbatie - 12/7/16 - Need to also return a default map is embedded ldap and set
            Object subjectClaims = new Configurator().getConfig("ddf.security.sts.client.configuration").get("claims");
            // TODO: tbatie - 12/6/16 - Clean up this naming conventions
            LdapTestResult<Connection> connection = bindUserToLdapConnection(configuration);
            List<SearchResultEntry> ldapSearchResults = getLdapQueryResults(connection.value(),
                    "objectClass=*",
                    configuration.queryBase());

            Set<String> ldapEntryAttributes = new HashSet<>();
            for (SearchResultEntry entry : ldapSearchResults) {
                for (Attribute attri : entry.getAllAttributes()) {
                    ldapEntryAttributes.add(attri.getAttributeDescriptionAsString());
                }
            }
            // TODO: tbatie - 12/6/16 - Probably need to do some filtering at this part on values like objectClass
            return new ProbeReport(new ArrayList<>()).addProbeResult(SUBJECT_CLAIMS_ID,
                    subjectClaims).addProbeResult(LDAP_USER_ATTRIBUTES, ldapEntryAttributes);
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
        bindRequiredFields.put("bindMethod", ldapConfiguration.bindUserMethod());

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
            // TODO: tbatie - 12/7/16 - Add the bind user method types here
            TestReport bindFieldsResults = cannotBeNullFields(bindRequiredFields);
            if (bindFieldsResults.containsUnsuccessfulMessages()) {
                return bindFieldsResults;
            }
            bindFieldsResults = testConditionalBindFields(ldapConfiguration);
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

        case LDAP_ATTRIBUTE_MAPPING_TEST_ID:
            // TODO: tbatie - 12/7/16 - Test the attribute mappings
            return new TestReport(buildMessage(SUCCESS, "TODO: Validate me"));
        default:
            return new TestReport(new ConfigurationMessage(NO_TEST_FOUND));
        }
    }

    @Override
    public TestReport persist(LdapConfiguration config) {

        // TODO: tbatie - 12/8/16 - Perform validation
        Map<String, Object> ldapStsConfig = new HashMap<>();

        String ldapUrl = "";
        boolean startTls = false;
        ldapStsConfig.put("ldapBindUserDn", config.bindUserDn());
        ldapStsConfig.put("ldapBindUserPass", config.bindUserPassword());
        ldapStsConfig.put("bindMethod", config.bindUserMethod());
        ldapStsConfig.put("kdcAddress", config.bindKdcAddress());
        ldapStsConfig.put("realm", config.bindRealm());

        ldapStsConfig.put("userNameAttribute", config.userNameAttribute());
        ldapStsConfig.put("userBaseDn", config.baseUserDn());
        ldapStsConfig.put("groupBaseDn", config.baseGroupDn());

        // TODO RAP 08 Dec 16: This is brittle
        switch (config.encryptionMethod()
                .toLowerCase()) {
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

        if(!Arrays.asList(LDAP_USE_CASES).contains(config.ldapUseCase())) {
            return new TestReport(buildMessage(ConfigurationMessage.MessageType.FAILURE,
                    "No ldap use case specified"));
        }

        if(config.ldapUseCase().equals(LOGIN) || config.ldapUseCase().equals(LOGIN_AND_CREDENTIAL_STORE)) {
            configurator.startFeature("security-sts-ldaplogin");
            configurator.createManagedService("Ldap_Login_Config", ldapStsConfig);
        }

        if(config.ldapUseCase().equals(CREDENTIAL_STORE) || config.ldapUseCase().equals(LOGIN_AND_CREDENTIAL_STORE)) {
            // TODO: tbatie - 12/8/16 - Fields do not map directly with the ldap login fields, might want to wait to persist until they are identical
//            ldapStsConfig.put("groupObjectClass", config.groupObjectClass());
//            ldapStsConfig.put("membershipAttribute", config.membershipAttribute());
//            configurator.startFeature("security-sts-ldapclaimshandler");
//            configurator.createManagedService("Claims_Handler_Manager", ldapStsConfig);
        }


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
        Collections.copy(Arrays.asList(LdapConfiguration.LDAP_ENCRYPTION_METHODS),
                encryptionMethodsToTry);
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
                    .equalsIgnoreCase(LDAPS)) {
                ldapOptions.setSSLContext(SSLContext.getDefault());
            } else if (ldapConfiguration.encryptionMethod()
                    .equalsIgnoreCase(TLS)) {
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
            BindRequest bindRequest = selectBindMethod(ldapConfiguration.bindUserMethod(),
                    ldapConfiguration.bindUserDn(),
                    ldapConfiguration.bindUserPassword(),
                    ldapConfiguration.bindRealm(),
                    ldapConfiguration.bindKdcAddress());
            connection.bind(bindRequest);
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

    private TestReport testConditionalBindFields(LdapConfiguration ldapConfiguration) {
        List<ConfigurationMessage> missingFields = new ArrayList<>();

        // TODO RAP 08 Dec 16: So many magic strings
        // TODO RAP 08 Dec 16: StringUtils
        String bindMethod = ldapConfiguration.bindUserMethod();
        if (bindMethod.equals("GSSAPI SASL")) {
            if (ldapConfiguration.bindKdcAddress() == null || ldapConfiguration.bindKdcAddress()
                    .equals("")) {
                missingFields.add(buildMessage(REQUIRED_FIELDS,
                        "Field cannot be empty for GSSAPI SASL bind type").configId("bindKdcAddress"));
            }
            if (ldapConfiguration.bindRealm() == null || ldapConfiguration.bindRealm()
                    .equals("")) {
                missingFields.add(buildMessage(REQUIRED_FIELDS,
                        "Field cannot be empty for GSSAPI SASL bind type").configId("bindRealm"));
            }
        }

        return new TestReport(missingFields);
    }

    ProbeReport getDefaultDirectoryStructure(LdapConfiguration configuration) {
        ProbeReport probeReport = new ProbeReport(new ArrayList<>());

        String ldapType = configuration.ldapType();
        ServerGuesser guesser = ServerGuesser.buildGuesser(ldapType,
                bindUserToLdapConnection(configuration).value);

        if (guesser != null) {
            probeReport.addProbeResult("baseUserDn", guesser.getUserBaseChoices());
            probeReport.addProbeResult("baseGroupDn", guesser.getGroupBaseChoices());
            probeReport.addProbeResult("userNameAttribute", guesser.getUserNameAttribute());
        }

        return probeReport;
    }

    // TODO RAP 08 Dec 16: Refactor to common location...this functionality is in BindMethodChooser
    // and SslLdapLoginModule as well
    private static BindRequest selectBindMethod(String bindMethod, String bindUserDN,
            String bindUserCredentials, String realm, String kdcAddress) {
        BindRequest request;
        switch (bindMethod) {
        case "Simple":
            request = Requests.newSimpleBindRequest(bindUserDN, bindUserCredentials.toCharArray());
            break;
        case "SASL":
            request = Requests.newPlainSASLBindRequest(bindUserDN,
                    bindUserCredentials.toCharArray());
            break;
        case "GSSAPI SASL":
            request = Requests.newGSSAPISASLBindRequest(bindUserDN,
                    bindUserCredentials.toCharArray());
            ((GSSAPISASLBindRequest) request).setRealm(realm);
            ((GSSAPISASLBindRequest) request).setKDCAddress(kdcAddress);
            break;
        case "Digest MD5 SASL":
            request = Requests.newDigestMD5SASLBindRequest(bindUserDN,
                    bindUserCredentials.toCharArray());
            ((DigestMD5SASLBindRequest) request).setCipher(DigestMD5SASLBindRequest.CIPHER_HIGH);
            ((DigestMD5SASLBindRequest) request).getQOPs()
                    .clear();
            ((DigestMD5SASLBindRequest) request).getQOPs()
                    .add(DigestMD5SASLBindRequest.QOP_AUTH_CONF);
            ((DigestMD5SASLBindRequest) request).getQOPs()
                    .add(DigestMD5SASLBindRequest.QOP_AUTH_INT);
            ((DigestMD5SASLBindRequest) request).getQOPs()
                    .add(DigestMD5SASLBindRequest.QOP_AUTH);
            if (realm != null && !realm.equals("")) {
                //            if (StringUtils.isNotEmpty(realm)) {
                ((DigestMD5SASLBindRequest) request).setRealm(realm);
            }
            break;
        default:
            request = Requests.newSimpleBindRequest(bindUserDN, bindUserCredentials.toCharArray());
            break;
        }

        return request;
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
