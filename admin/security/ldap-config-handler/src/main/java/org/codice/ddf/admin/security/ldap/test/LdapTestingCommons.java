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

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.REQUIRED_FIELDS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.LDAPS;
import static org.codice.ddf.admin.security.ldap.LdapConfiguration.TLS;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.CANNOT_BIND;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.CANNOT_CONFIGURE;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.CANNOT_CONNECT;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.SUCCESSFUL_BIND;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.LdapConnectionResult.SUCCESSFUL_CONNECTION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

import org.apache.cxf.common.util.StringUtils;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.security.ldap.LdapConfiguration;
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

public class LdapTestingCommons {

    public static LdapConnectionAttempt getLdapConnection(LdapConfiguration ldapConfiguration) {
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
            ldapOptions.setProviderClassLoader(LdapTestingCommons.class.getClassLoader());

        } catch (Exception e) {
            return new LdapConnectionAttempt(CANNOT_CONFIGURE);
        }

        Connection ldapConnection;

        try {
            ldapConnection = new LDAPConnectionFactory(ldapConfiguration.hostName(),
                    ldapConfiguration.port(),
                    ldapOptions).getConnection();
        } catch (Exception e) {
            return new LdapConnectionAttempt(CANNOT_CONNECT);
        }

        return new LdapConnectionAttempt(SUCCESSFUL_CONNECTION, ldapConnection);
    }

    public static LdapConnectionAttempt bindUserToLdapConnection(
            LdapConfiguration ldapConfiguration) {

        LdapConnectionAttempt ldapConnectionResult = getLdapConnection(ldapConfiguration);
        if (ldapConnectionResult.result() != SUCCESSFUL_CONNECTION) {
            return ldapConnectionResult;
        }

        Connection connection = ldapConnectionResult.connection();

        try {
            BindRequest bindRequest = selectBindMethod(ldapConfiguration.bindUserMethod(),
                    ldapConfiguration.bindUserDn(),
                    ldapConfiguration.bindUserPassword(),
                    ldapConfiguration.bindRealm(),
                    ldapConfiguration.bindKdcAddress());
            connection.bind(bindRequest);
        } catch (Exception e) {
            return new LdapConnectionAttempt(CANNOT_BIND);
        }

        return new LdapConnectionAttempt(SUCCESSFUL_BIND, connection);
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

    public static List<SearchResultEntry> getLdapQueryResults(Connection ldapConnection, String ldapQuery,
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

    // TODO: tbatie - 1/3/17 - I'd like this to eventually go away and move all validation of fields to the ldap configuration class
    public static TestReport cannotBeNullFields(Map<String, Object> fieldsToCheck) {
        List<ConfigurationMessage> missingFields = new ArrayList<>();

        fieldsToCheck.entrySet()
                .stream()
                .filter(field -> field.getValue() == null && (field.getValue() instanceof String
                        && StringUtils.isEmpty((String) field.getValue())))
                .forEach(field -> missingFields.add(buildMessage(REQUIRED_FIELDS,
                        "Field cannot be empty").configId(field.getKey())));

        return new TestReport(missingFields);
    }

    // TODO: tbatie - 1/3/17 - This validation should be done in the ldap configuration
    public static TestReport testConditionalBindFields(LdapConfiguration ldapConfiguration) {
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

    public static class LdapConnectionAttempt {

        private LdapConnectionResult result;
        private Connection connection;

        public LdapConnectionAttempt(LdapConnectionResult result) {
            this.result = result;
        }

        public LdapConnectionAttempt(LdapConnectionResult result, Connection value) {
            this.result = result;
            this.connection = value;
        }

        public Connection connection() {
            return connection;
        }

        public LdapConnectionResult result() {
            return result;
        }
    }

    public enum LdapConnectionResult {
        CANNOT_CONNECT("Unable to reach the specified host."),
        CANNOT_CONFIGURE("Unable to setup test environment."),
        CANNOT_BIND("Unable to bind the user to the LDAP connection. Try a different username or password. Make sure the username is in the format of a distinguished name."),
        BASE_USER_DN_NOT_FOUND("The specified base user DN does not appear to exist."),
        BASE_GROUP_DN_NOT_FOUND("The specified base group DN does not appear to exist."),
        USER_NAME_ATTRIBUTE_NOT_FOUND("No users found with the described attribute in the base user DN"),
        NO_USERS_IN_BASE_USER_DN("The base user DN was found, but there are no users in it."),
        NO_GROUPS_IN_BASE_GROUP_DN("The base group DN was found, but there are no groups in it."),

        SUCCESSFUL_CONNECTION("A connection with the LDAP was successfully established."),
        SUCCESSFUL_BIND("Successfully binded the user to the LDAP connection."),
        FOUND_BASE_USER_DN("Found users in base user dn"),
        FOUND_BASE_GROUP_DN("Found groups in base group DN"),
        FOUND_USER_NAME_ATTRIBUTE("Users with given user attribute found in base user dn");

        private String description;

        LdapConnectionResult(String description) {
            this.description = description;
        }

        public String description() {
            return description;
        }

        public static Map<String, String> toDescriptionMap(List<LdapConnectionResult> resultTypes) {
            return resultTypes.stream()
                    .collect(Collectors.toMap(resultType -> resultType.name(),
                            resultType -> resultType.description()));
        }
    }
}
