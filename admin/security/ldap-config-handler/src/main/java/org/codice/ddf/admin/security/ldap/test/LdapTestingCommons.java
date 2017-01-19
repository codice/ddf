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

import static org.codice.ddf.admin.api.validation.LdapValidationUtils.DIGEST_MD5_SASL;
import static org.codice.ddf.admin.api.validation.LdapValidationUtils.GSSAPI_SASL;
import static org.codice.ddf.admin.api.validation.LdapValidationUtils.LDAPS;
import static org.codice.ddf.admin.api.validation.LdapValidationUtils.SASL;
import static org.codice.ddf.admin.api.validation.LdapValidationUtils.SIMPLE;
import static org.codice.ddf.admin.api.validation.LdapValidationUtils.TLS;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.CANNOT_BIND;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.CANNOT_CONFIGURE;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.CANNOT_CONNECT;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.SUCCESSFUL_BIND;
import static org.codice.ddf.admin.security.ldap.LdapConnectionResult.SUCCESSFUL_CONNECTION;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLContext;

import org.codice.ddf.admin.api.config.ldap.LdapConfiguration;
import org.codice.ddf.admin.security.ldap.LdapConnectionResult;
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

    public static BindRequest selectBindMethod(String bindMethod, String bindUserDN,
            String bindUserCredentials, String realm, String kdcAddress) {
        BindRequest request;
        switch (bindMethod) {
        case SIMPLE:
            request = Requests.newSimpleBindRequest(bindUserDN, bindUserCredentials.toCharArray());
            break;
        case SASL:
            request = Requests.newPlainSASLBindRequest(bindUserDN,
                    bindUserCredentials.toCharArray());
            break;
        case GSSAPI_SASL:
            request = Requests.newGSSAPISASLBindRequest(bindUserDN,
                    bindUserCredentials.toCharArray());
            ((GSSAPISASLBindRequest) request).setRealm(realm);
            ((GSSAPISASLBindRequest) request).setKDCAddress(kdcAddress);
            break;
        case DIGEST_MD5_SASL:
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

    public static List<SearchResultEntry> getLdapQueryResults(Connection ldapConnection,
            String ldapQuery, String ldapSearchBaseDN) {

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

}
