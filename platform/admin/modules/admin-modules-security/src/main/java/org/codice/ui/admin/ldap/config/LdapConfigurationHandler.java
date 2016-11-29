package org.codice.ui.admin.ldap.config;

import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAPS;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.ENCRYPTION_METHOD;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.HOST_NAME;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.LDAP_CONFIGURATION_KEYS.PORT;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.TLS;
import static org.codice.ui.admin.ldap.config.LdapConfiguration.NONE;
import static org.codice.ui.admin.ldap.config.LdapConfigurationHandler.LdapTestResultType.CANNOT_BIND;
import static org.codice.ui.admin.ldap.config.LdapConfigurationHandler.LdapTestResultType.CANNOT_CONFIGURE;
import static org.codice.ui.admin.ldap.config.LdapConfigurationHandler.LdapTestResultType.CANNOT_CONNECT;
import static org.codice.ui.admin.ldap.config.LdapConfigurationHandler.LdapTestResultType.SUCCESSFUL_BIND;
import static org.codice.ui.admin.ldap.config.LdapConfigurationHandler.LdapTestResultType.SUCCESSFUL_CONNECTION;
import static org.codice.ui.admin.wizard.api.ConfigurationTestMessage.MessageType.FAILURE;
import static org.codice.ui.admin.wizard.api.ConfigurationTestMessage.MessageType.INFO;
import static org.codice.ui.admin.wizard.api.ConfigurationTestMessage.MessageType.NO_TEST_FOUND;
import static org.codice.ui.admin.wizard.api.ConfigurationTestMessage.MessageType.SUCCESS;
import static org.codice.ui.admin.wizard.api.ConfigurationTestMessage.buildMessage;

import org.apache.cxf.common.util.StringUtils;
import org.codice.ui.admin.wizard.api.ConfigurationTestMessage;
import org.codice.ui.admin.wizard.config.ConfigReport;
import org.codice.ui.admin.wizard.config.Configuration;
import org.codice.ui.admin.wizard.config.Configurator;
import org.forgerock.opendj.ldap.Attribute;
import org.forgerock.opendj.ldap.ByteString;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LDAPOptions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.net.ssl.SSLContext;

import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.forgerock.opendj.ldap.SearchScope;
import org.forgerock.opendj.ldap.responses.SearchResultEntry;
import org.forgerock.opendj.ldif.ConnectionEntryReader;

public class LdapConfigurationHandler implements ConfigurationHandler<LdapConfiguration> {

    public static final String LDAP_CONFIGURATION_HANDLER_ID = "ldapConfigurationHandler";

    // Test Ids
    public static final String LDAP_CONNECTION_TEST_ID = "testLdapConnection";

    public static final String LDAP_BIND_TEST_ID = "testLdapBind";

    public static final String LDAP_DIRECTORY_STRUCT_TEST_ID = "testLdapDirStruct";

    // Probe Ids
    public static final String LDAP_QUERY_PROBE_ID = "ldapQuery";

    @Override
    public String getConfigurationHandlerId() {
        return LDAP_CONFIGURATION_HANDLER_ID;
    }

    @Override
    public LdapConfiguration probe(String probeId, LdapConfiguration configuration) {
        // TODO: tbatie - 11/7/16 - Consider returning not only the config but any errors that occurred
        // TODO: tbatie - 11/4/16 - Maybe we should take in a list of probeId's so we can search for different information at once
        switch (probeId) {
        case LDAP_QUERY_PROBE_ID:
            // TODO: tbatie - 11/10/16 - Error reporting when connection doesn't work
            LdapTestResult<Connection> connectionResult =  bindUserToLdapConnection(configuration);
            Connection ldapConnection = connectionResult.value();
            List<SearchResultEntry> searchResults = getLdapQueryResults(ldapConnection, configuration.query(), configuration.queryBase());
            ldapConnection.close();

            List<Map<String, String>> convertedSearchResults = new ArrayList<>();

            for (SearchResultEntry entry : searchResults) {
                Map<String, String> entryMap = new HashMap<>();
                entryMap.put("name", entry.getName().toString());

                for(Attribute attri : entry.getAllAttributes()) {
                    entryMap.put(attri.getAttributeDescriptionAsString(), Arrays.toString(attri.toArray()));
                }

                convertedSearchResults.add(entryMap);
            }

            configuration.queryResult(convertedSearchResults);
            return configuration;
        }
        return configuration;
    }

    @Override
    public List<ConfigurationTestMessage> test(String testId, LdapConfiguration ldapConfiguration) {
        // TODO: tbatie - 11/4/16 - Maybe we should take in a list of test id's so we can test different things at once
        switch (testId) {
        case LDAP_CONNECTION_TEST_ID:
            return testLdapConnection(ldapConfiguration);
        case LDAP_BIND_TEST_ID:
            return testLdapBind(ldapConfiguration);
        case LDAP_DIRECTORY_STRUCT_TEST_ID:
            return testLdapDirectoryStructure(ldapConfiguration);
        }

        return Arrays.asList(new ConfigurationTestMessage(NO_TEST_FOUND));
    }

    @Override
    public List<ConfigurationTestMessage> persist(LdapConfiguration config) {

        Map<String, String> ldapStsConfig = new HashMap<>();

        String ldapUrl = "";
        boolean startTls = false;
        ldapStsConfig.put("ldapBindUserDn", config.bindUserDN());
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
        if(!configReport.getFailedResults().isEmpty()) {
            return Arrays.asList(buildMessage(ConfigurationTestMessage.MessageType.FAILURE, "Unable to persist changes"));
        }

        return new ArrayList<>();
    }

    public List<ConfigurationTestMessage> testLdapConnection(LdapConfiguration ldapConfiguration) {

        List<LdapConfiguration.LDAP_CONFIGURATION_KEYS> requiredFields = Arrays.asList(
                HOST_NAME, PORT, ENCRYPTION_METHOD);

        List<ConfigurationTestMessage> testResults = cannotBeNullFields(requiredFields, ldapConfiguration, new ArrayList<>());
        if(!testResults.isEmpty()) {
            return testResults;
        }

        LdapTestResult<Connection> connectionTestResult = getLdapConnection(ldapConfiguration);

        if(connectionTestResult.type() == SUCCESSFUL_CONNECTION) {
            connectionTestResult.value().close();
            testResults.add(buildMessage(SUCCESS, "Successfully connected to LDAP"));
            return testResults;
        }


        if(connectionTestResult.type() == CANNOT_CONFIGURE) {
            testResults.add(buildMessage(FAILURE, "Unable to create configuration used for the LDAP connection. The system may not be properly installed to setup LDAP."));
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

        for(LdapConfiguration testConfig : configsToTest) {
            LdapTestResult<Connection> connectionTestRetryResult = getLdapConnection(testConfig);

            if(connectionTestRetryResult.type() == SUCCESSFUL_CONNECTION) {
                connectionTestRetryResult.value().close();
                testResults.add(buildMessage(INFO,
                        "We were unable to connect to the host with the given encryption method but we were able successfully connect using the encryption method " + testConfig.encryptionMethod() + ". If this is acceptable, please change the encryption method field and resubmit."));
                return testResults;
            }
        }

        testResults.add(buildMessage(FAILURE,
                "Unable to reach the specified host. We tried the other available encryption methods without success. Make sure your host and port are correct, your LDAP is running and that your network is not restricting access."));

        return testResults;
    }

    public List<ConfigurationTestMessage> testLdapBind(LdapConfiguration ldapConfiguration) {
        LdapTestResult<Connection> bindUserTestResult = bindUserToLdapConnection(ldapConfiguration);

        switch (bindUserTestResult.type()) {
            case SUCCESSFUL_BIND:
                return Arrays.asList(buildMessage(SUCCESS,
                        "Successfully binded user to the LDAP connection"));

            case CANNOT_BIND:
                return Arrays.asList(buildMessage(FAILURE,
                        "Unable to bind the user to the LDAP connection. Try a different username or password. Make sure the username is in the format of a distinguished name."));

            case CANNOT_CONNECT:
                return Arrays.asList(buildMessage(FAILURE,
                        "Unable to bind the user because there was a failure to reach the LDAP. Make sure your LDAP is running."));

            case CANNOT_CONFIGURE:
                return Arrays.asList(buildMessage(FAILURE, "Unable to create configuration used for the LDAP connection. The system may not be properly installed to setup LDAP."));

        // TODO: tbatie - 11/4/16 - This should never happen, clean this part up
             default:
                return Arrays.asList(buildMessage(FAILURE, "Something went wrong"));
        }

    }

    public List<ConfigurationTestMessage> testLdapDirectoryStructure(LdapConfiguration config) {
        List<ConfigurationTestMessage> directoryStructureResults = new ArrayList<>();
        LdapTestResult<Connection> ldapConnectionResult = bindUserToLdapConnection(config);

        switch (ldapConnectionResult.type()) {
            case CANNOT_CONNECT:
                return Arrays.asList(buildMessage(FAILURE,
                        "Unable to connect to LDAP anymore, make sure the LDAP is running."));

            case CANNOT_BIND:
                return Arrays.asList(buildMessage(FAILURE,
                        "Unable to bind user to LDAP connection. Make sure the user still has the correct credentials to connect"));

            case CANNOT_CONFIGURE:
                return Arrays.asList(buildMessage(FAILURE,
                        "Unable to create configuration used for the LDAP connection. The system may not be properly installed to setup LDAP."));
        }

        Connection ldapConnection = ldapConnectionResult.value();
        List<SearchResultEntry> baseUsersResults = getLdapQueryResults(ldapConnection, "objectClass=*", config.baseUserDn());

        if(baseUsersResults.isEmpty()) {
            directoryStructureResults.add(buildMessage(FAILURE,
                    "The specified base user DN does not appear to exist"));
        }

        else if(baseUsersResults.size() <= 1) {
            directoryStructureResults.add(buildMessage(FAILURE, "No users found in the base user DN"));
        } else {
            directoryStructureResults.add(buildMessage(SUCCESS, "Found users in base user dn"));
            List<SearchResultEntry> userAttribute = getLdapQueryResults(ldapConnection, config.userNameAttribute() + "=*", config.baseUserDn());
            if(userAttribute.isEmpty()) {
                directoryStructureResults.add(buildMessage(FAILURE, "No users found with the described attribute in the base user DN"));
            } else {
                directoryStructureResults.add(buildMessage(SUCCESS, "Users with given user attribute found in base user dn"));
            }
        }

        List<SearchResultEntry> baseGroupResults = getLdapQueryResults(ldapConnection, "objectClass=*", config.baseUserDn());

        if(baseGroupResults.isEmpty()) {
            directoryStructureResults.add(buildMessage(FAILURE, "The specified base group DN does not appear to exist"));
        } else if(baseGroupResults.size() <= 1) {
            directoryStructureResults.add(buildMessage(FAILURE, "No groups found in the base group DN"));
        }
        else {
            directoryStructureResults.add(buildMessage(SUCCESS, "Found groups in base group DN"));
        }
        ldapConnection.close();
        return directoryStructureResults;
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

    public LdapTestResult<Connection> bindUserToLdapConnection(LdapConfiguration ldapConfiguration) {

        LdapTestResult<Connection> ldapConnectionResult = getLdapConnection(ldapConfiguration);
        if(ldapConnectionResult.type() != SUCCESSFUL_CONNECTION) {
            return ldapConnectionResult;
        }

        Connection connection = ldapConnectionResult.value();

        try {
            connection.bind(ldapConfiguration.bindUserDN(), ldapConfiguration.bindUserPassword().toCharArray());
        } catch (Exception e) {
            return new LdapTestResult<>(CANNOT_BIND);
        }

        return new LdapTestResult<>(SUCCESSFUL_BIND, connection);
    }


    public List<SearchResultEntry> getLdapQueryResults(Connection ldapConnection, String ldapQuery, String ldapSearchBaseDN) {

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

    public <T extends Enum> List<ConfigurationTestMessage> cannotBeNullFields(List<T> requiredFields, Configuration config,
            List<ConfigurationTestMessage> errors) {
        for (T field : requiredFields) {
            if (config.getValue(field) == null) {
                String errorMsg = "Entry " + field + " cannot be empty";
                errors.add(buildMessage(FAILURE, errorMsg));
            }
        }

        return errors;
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

        public T value(){
            return value;
        }

        public LdapTestResultType type(){
            return type;
        }
    }

    public enum LdapTestResultType {
        SUCCESSFUL_CONNECTION, CANNOT_CONNECT, CANNOT_CONFIGURE, CANNOT_BIND, SUCCESSFUL_BIND
    }
}
