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

import java.util.List;
import java.util.Map;

import org.codice.ui.admin.wizard.config.Configuration;

public class LdapConfiguration extends Configuration {

    public static final String LDAPS = "ldaps";

    public static final String TLS = "tls";

    public static final String NONE = "none";

    static final String[] LDAP_ENCRYPTION_METHODS = new String[] {LDAPS, TLS, NONE};

    private String hostName;

    private int port;

    private String encryptionMethod;

    private String bindUserDn;

    private String bindUserPassword;

    private String userNameAttribute;

    private String baseGroupDn;

    private String baseUserDn;

    private String query;

    private String queryBase;

    private List<Map<String, String>> queryResults;

    public String hostName() {
        return hostName;
    }

    public int port() {
        return port;
    }

    public String encryptionMethod() {
        return encryptionMethod;
    }

    public String bindUserDn() {
        return bindUserDn;
    }

    public String bindUserPassword() {
        return bindUserPassword;
    }

    public String userNameAttribute() {
        return userNameAttribute;
    }

    public String baseUserDn() {
        return baseUserDn;
    }

    public String baseGroupDn() {
        return baseGroupDn;
    }

    public String query() {
        return query;
    }

    public String queryBase() {
        return queryBase;
    }

    public List<Map<String, String>> queryResults() {
        return queryResults;
    }

    public LdapConfiguration hostName(String hostName) {
        this.hostName = hostName;
        return this;
    }

    public LdapConfiguration port(int port) {
        this.port = port;
        return this;
    }

    public LdapConfiguration encryptionMethod(String encryptionMethod) {
        this.encryptionMethod = encryptionMethod;
        return this;
    }

    public LdapConfiguration bindUserDn(String bindUserDn) {
        this.bindUserDn = bindUserDn;
        return this;
    }

    public LdapConfiguration bindUserPassword(String bindUserPassword) {
        this.bindUserPassword = bindUserPassword;
        return this;
    }

    public LdapConfiguration userNameAttribute(String userNameAttribute) {
        this.userNameAttribute = userNameAttribute;
        return this;
    }

    public LdapConfiguration baseGroupDn(String baseGroupDn) {
        this.baseGroupDn = baseGroupDn;
        return this;
    }

    public LdapConfiguration baseUserDn(String baseUserDn) {
        this.baseUserDn = baseUserDn;
        return this;
    }

    public LdapConfiguration query(String query) {
        this.query = query;
        return this;
    }

    public LdapConfiguration queryBase(String queryBase) {
        this.queryBase = queryBase;
        return this;
    }

    public LdapConfiguration queryResults(List<Map<String, String>> queryResults) {
        this.queryResults = queryResults;
        return this;
    }

    public LdapConfiguration copy() {
        return new LdapConfiguration().hostName(hostName)
                .port(port)
                .baseUserDn(baseUserDn)
                .encryptionMethod(encryptionMethod)
                .bindUserDn(bindUserDn)
                .bindUserPassword(bindUserPassword)
                .query(query)
                .queryBase(queryBase)
                .baseUserDn(baseUserDn)
                .baseGroupDn(baseGroupDn)
                .userNameAttribute(userNameAttribute);
    }
}
