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

package org.codice.ddf.admin.api.config.security.ldap;

import static org.codice.ddf.admin.api.commons.ValidationUtils.validateFactoryPid;
import static org.codice.ddf.admin.api.commons.ValidationUtils.validateHostName;
import static org.codice.ddf.admin.api.commons.ValidationUtils.validateMapping;
import static org.codice.ddf.admin.api.commons.ValidationUtils.validateNonEmptyString;
import static org.codice.ddf.admin.api.commons.ValidationUtils.validatePort;
import static org.codice.ddf.admin.api.commons.ValidationUtils.validateServicePid;
import static org.codice.ddf.admin.api.commons.ldap.LdapValidationUtils.validateBindKdcAddress;
import static org.codice.ddf.admin.api.commons.ldap.LdapValidationUtils.validateBindRealm;
import static org.codice.ddf.admin.api.commons.ldap.LdapValidationUtils.validateBindUserMethod;
import static org.codice.ddf.admin.api.commons.ldap.LdapValidationUtils.validateDn;
import static org.codice.ddf.admin.api.commons.ldap.LdapValidationUtils.validateEncryptionMethod;
import static org.codice.ddf.admin.api.commons.ldap.LdapValidationUtils.validateGroupObjectClass;
import static org.codice.ddf.admin.api.commons.ldap.LdapValidationUtils.validateLdapQuery;
import static org.codice.ddf.admin.api.commons.ldap.LdapValidationUtils.validateLdapType;
import static org.codice.ddf.admin.api.commons.ldap.LdapValidationUtils.validateLdapUseCase;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.codice.ddf.admin.api.commons.ValidationUtils;
import org.codice.ddf.admin.api.config.Configuration;
import org.codice.ddf.admin.api.config.ConfigurationType;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class LdapConfiguration extends Configuration {

    public static final String CONFIGURATION_TYPE = "ldap";

    public static final String LDAPS = "ldaps";
    public static final String TLS = "tls";
    public static final String NONE = "none";
    public static final String ENCRYPTION_METHOD = "encryptionMethod";
    public static final ImmutableList<String> LDAP_ENCRYPTION_METHODS = ImmutableList.of(LDAPS, TLS, NONE);
    public static final String LOGIN = "login";
    public static final String CREDENTIAL_STORE = "credentialStore";
    public static final String LOGIN_AND_CREDENTIAL_STORE = "loginAndCredentialStore";
    public static final ImmutableList LDAP_USE_CASES = ImmutableList.of(LOGIN, CREDENTIAL_STORE, LOGIN_AND_CREDENTIAL_STORE);
    public static final String LDAP_TYPE = "ldapType";
    public static final String BIND_USER_DN = "bindUserDn";
    public static final String BIND_USER_PASSWORD = "bindUserPassword";
    public static final String BIND_METHOD = "bindMethod";
    public static final String SIMPLE = "Simple";
    public static final String SASL = "SASL";
    public static final String GSSAPI_SASL = "GSSAPI SASL";
    public static final String DIGEST_MD5_SASL = "Digest MD5 SASL";
    public static final List<String> BIND_METHODS = ImmutableList.of(SIMPLE, SASL, GSSAPI_SASL, DIGEST_MD5_SASL);
    public static final String BIND_REALM = "realm";
    public static final String BIND_KDC = "kdcAddress";
    public static final String HOST_NAME = "hostName";
    public static final String PORT = "port";
    public static final String BASE_USER_DN = "baseUserDn";
    public static final String BASE_GROUP_DN = "baseGroupDn";
    public static final String USER_NAME_ATTRIBUTE = "userNameAttribute";
    public static final String QUERY = "query";
    public static final String QUERY_BASE = "queryBase";
    public static final String MEMBERSHIP_ATTRIBUTE = "membershipAttribute";
    public static final String LDAP_USE_CASE = "ldapUseCase";
    public static final String GROUP_OBJECT_CLASS = "groupObjectClass";
    public static final String ATTRIBUTE_MAPPINGS = "attributeMappings";
    public static final String QUERY_RESULTS = "queryResults";
    public static final String SERVICE_PID = "servicePid";
    public static final String FACTORY_PID = "factoryPid";


    private static final Map<String, Function<LdapConfiguration, List<ConfigurationMessage>>> FIELD_TO_VALIDATION_FUNC = new ImmutableMap.Builder<String, Function<LdapConfiguration, List<ConfigurationMessage>>>()
                    .put(SERVICE_PID, config -> validateServicePid(config.servicePid(), SERVICE_PID))
                    .put(FACTORY_PID, config -> validateFactoryPid(config.factoryPid(), FACTORY_PID))
                    .put(HOST_NAME, config -> validateHostName(config.hostName(), HOST_NAME))
                    .put(PORT, config -> validatePort(config.port(), PORT))
                    .put(ENCRYPTION_METHOD, config -> validateEncryptionMethod(config.encryptionMethod(), ENCRYPTION_METHOD))
                    .put(BIND_USER_DN, config -> validateDn(config.bindUserDn(), BIND_USER_DN))
                    .put(BIND_USER_PASSWORD, config -> validateNonEmptyString(config.bindUserPassword(), BIND_USER_PASSWORD))
                    .put(BIND_METHOD, config -> validateBindUserMethod(config.bindUserMethod(), BIND_METHOD))
                    .put(BIND_KDC, config -> validateBindKdcAddress(config.bindKdcAddress(), BIND_KDC))
                    .put(BIND_REALM, config -> validateBindRealm(config.bindRealm(), BIND_REALM))
                    .put(USER_NAME_ATTRIBUTE, config -> validateNonEmptyString(config.userNameAttribute(), USER_NAME_ATTRIBUTE))
                    .put(BASE_GROUP_DN, config -> validateDn(config.baseGroupDn(), BASE_GROUP_DN))
                    .put(BASE_USER_DN, config -> validateDn(config.baseUserDn(), BASE_USER_DN))
                    .put(QUERY, config -> validateLdapQuery(config.query(), QUERY))
                    .put(QUERY_BASE, config -> validateDn(config.queryBase(), QUERY_BASE))
                    .put(LDAP_TYPE, config -> validateLdapType(config.ldapType(), LDAP_TYPE))
                    .put(LDAP_USE_CASE, config -> validateLdapUseCase(config.ldapUseCase(), LDAP_USE_CASE))
                    .put(GROUP_OBJECT_CLASS, config -> validateGroupObjectClass(config.groupObjectClass(), GROUP_OBJECT_CLASS))
                    .put(MEMBERSHIP_ATTRIBUTE, config -> validateNonEmptyString(config.membershipAttribute(), MEMBERSHIP_ATTRIBUTE))
                    .put(ATTRIBUTE_MAPPINGS, config -> validateMapping(config.attributeMappings(), ATTRIBUTE_MAPPINGS))
                    .build();

    private String servicePid;
    private String factoryPid;
    private String hostName;
    private int port;
    private String encryptionMethod;
    private String bindUserDn;
    private String bindUserPassword;
    private String bindUserMethod;
    private String bindKdcAddress;
    private String bindRealm;
    private String userNameAttribute;
    private String baseGroupDn;
    private String baseUserDn;
    private String query;
    private String queryBase;
    private String ldapType;
    private String ldapUseCase;
    private String groupObjectClass;
    private String membershipAttribute;
    public Map<String, String> attributeMappings;
    private List<Map<String, String>> queryResults;

    //Getters
    public String factoryPid() {
        return factoryPid;
    }
    public String servicePid() {
        return servicePid;
    }
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
    public String bindUserMethod() {
        return bindUserMethod;
    }
    public String bindKdcAddress() {
        return bindKdcAddress;
    }
    public String bindRealm() {
        return bindRealm;
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
    public String ldapType() {
        return ldapType;
    }
    public String ldapUseCase() {
        return ldapUseCase;
    }
    public List<Map<String, String>> queryResults() {
        return queryResults;
    }
    public LdapConfiguration servicePid(String servicePid) {
        this.servicePid = servicePid;
        return this;
    }
    public LdapConfiguration factoryPid(String factoryPid) {
        this.factoryPid = factoryPid;
        return this;
    }
    public String groupObjectClass() {
        return groupObjectClass;
    }
    public String membershipAttribute() {
        return membershipAttribute;
    }
    public Map<String, String> attributeMappings() {
        return attributeMappings;
    }

    //Setters
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
    public LdapConfiguration bindUserMethod(String bindUserMethod) {
        this.bindUserMethod = bindUserMethod;
        return this;
    }
    public LdapConfiguration bindKdcAddress(String bindKdcAddress) {
        this.bindKdcAddress = bindKdcAddress;
        return this;
    }
    public LdapConfiguration bindRealm(String bindRealm) {
        this.bindRealm = bindRealm;
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
    public LdapConfiguration ldapType(String ldapType) {
        this.ldapType = ldapType;
        return this;
    }
    public LdapConfiguration ldapUseCase(String ldapUseCase) {
        this.ldapUseCase = ldapUseCase;
        return this;
    }
    public LdapConfiguration groupObjectClass(String groupObjectClass) {
        this.groupObjectClass = groupObjectClass;
        return this;
    }
    public LdapConfiguration membershipAttribute(String membershipAttribute) {
        this.membershipAttribute = membershipAttribute;
        return this;
    }
    public LdapConfiguration attributeMappings(Map<String, String> attributeMapping) {
        this.attributeMappings = attributeMapping;
        return this;
    }

    public List<ConfigurationMessage> validate(List<String> fields) {
        return ValidationUtils.validate(fields, this, FIELD_TO_VALIDATION_FUNC);
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new ConfigurationType(CONFIGURATION_TYPE, LdapConfiguration.class);
    }
}
