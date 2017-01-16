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

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MISSING_REQUIRED_FIELD;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.createMissingRequiredFieldMsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
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
    public static final String LOGIN = "login";
    public static final String CREDENTIAL_STORE = "credentialStore";
    public static final String LOGIN_AND_CREDENTIAL_STORE = "loginAndCredentialStore";
    public static final String LDAP_TYPE = "ldapType";
    public static final String BIND_USER_DN = "bindUserDn";
    public static final String BIND_USER_PASSWORD = "bindUserPassword";
    public static final String BIND_METHOD = "bindMethod";
    public static final String BIND_REALM = "realm";
    public static final String BIND_KDC = "kdcAddress";
    public static final String HOST_NAME = "hostName";
    public static final String PORT = "port";
    public static final String ENCRYPTION_METHOD = "encryptionMethod";
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
    public static final String SERVICE_PID ="servicePid";

    public static final ImmutableList LDAP_ENCRYPTION_METHODS = ImmutableList.of(LDAPS, TLS, NONE);

    public static final ImmutableList LDAP_USE_CASES = ImmutableList.of(LOGIN,
            CREDENTIAL_STORE,
            LOGIN_AND_CREDENTIAL_STORE);

    private static final ImmutableMap<String, String> FIELD_DESCS =
            new ImmutableMap.Builder<String, String>()
                    .put(SERVICE_PID, "Service pid of the LDAP service related to this LDAP configuration.")
                    .put(HOST_NAME, "Host name of the LDAP server.")
                    .put(PORT, "Port on which the LDAP server listens.")
                    .put(LDAP_TYPE, "The LDAP server type.")
                    .put(BIND_USER_DN, "User to bind to the LDAP connection.")
                    .put(BIND_USER_PASSWORD, "Password of the user binding to the LDAP connection.")
                    // TODO RAP 11 Jan 17: Fix the bind methods
                    .put(BIND_METHOD,
                            "Bind method to use for the connection. Must be one of: " + "TODO")
                    .put(BIND_REALM, "Realm for MD5/Kerberos bind types.")
                    .put(BIND_KDC, "KDC Address for Kerberos binding.")
                    .put(ENCRYPTION_METHOD,
                            "Encryption method for connection. Must be one of: " + Arrays.toString(
                                    LDAP_ENCRYPTION_METHODS.toArray()))
                    .put(BASE_USER_DN,
                            "The DN containing users that will be used for LDAP authentication")
                    .put(BASE_GROUP_DN,
                            "The DN containing groups associated to the users for LDAP authentication")
                    .put(USER_NAME_ATTRIBUTE,
                            "The attribute of the users that should be as a display name.")
                    .put(QUERY, "The LDAP query to execute in order to search for users.")
                    .put(QUERY_BASE,
                            "The base DN against which to execute the LDAP search for users.")
                    .put(MEMBERSHIP_ATTRIBUTE,
                            "The attribute in the group object containing member references.")
                    .put(LDAP_USE_CASE, "How the LDAP configuration is intended to be used. Must be either: " + Arrays.toString(LDAP_USE_CASES.toArray()))
                    .put(GROUP_OBJECT_CLASS, "ObjectClass that defines structure for group membership in LDAP. Usually this is groupOfNames or groupOfUniqueNames.")
                    .put(ATTRIBUTE_MAPPINGS, "The mapping of STS claims to LDAP user attributes.")
                    .put(QUERY_RESULTS, "Results returned from an LDAP query.")
                    .build();

    private static final Map<String, Function<LdapConfiguration, Object>> FIELD_FUNC_MAP =
            new ImmutableMap.Builder<String, Function<LdapConfiguration, Object>>()
                    .put(SERVICE_PID, LdapConfiguration::servicePid)
                    .put(HOST_NAME, LdapConfiguration::hostName)
                    .put(PORT, LdapConfiguration::port)
                    .put(ENCRYPTION_METHOD, LdapConfiguration::encryptionMethod)
                    .put(BIND_USER_DN, LdapConfiguration::bindUserDn)
                    .put(BIND_USER_PASSWORD, LdapConfiguration::bindUserPassword)
                    .put(BIND_METHOD, LdapConfiguration::bindUserMethod)
                    .put(BIND_KDC, LdapConfiguration::bindKdcAddress)
                    .put(BIND_REALM, LdapConfiguration::bindRealm)
                    .put(USER_NAME_ATTRIBUTE, LdapConfiguration::userNameAttribute)
                    .put(BASE_GROUP_DN, LdapConfiguration::baseGroupDn)
                    .put(BASE_USER_DN, LdapConfiguration::baseUserDn)
                    .put(QUERY, LdapConfiguration::query)
                    .put(QUERY_BASE, LdapConfiguration::queryBase)
                    .put(LDAP_TYPE, LdapConfiguration::ldapType)
                    .put(LDAP_USE_CASE, LdapConfiguration::ldapUseCase)
                    .put(GROUP_OBJECT_CLASS, LdapConfiguration::groupObjectClass)
                    .put(MEMBERSHIP_ATTRIBUTE, LdapConfiguration::membershipAttribute)
                    .put(ATTRIBUTE_MAPPINGS, LdapConfiguration::attributeMappings)
                    .put(QUERY_RESULTS, LdapConfiguration::queryResults)
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

    public static Map<String, String> buildFieldMap(String... keys) {
        ImmutableMap.Builder<String, String> map = new ImmutableMap.Builder<>();
        Arrays.stream(keys)
                .forEach(s -> map.put(s, FIELD_DESCS.get(s)));
        return map.build();
    }

    public List<ConfigurationMessage> checkRequiredFields(Set<String> fields) {
        final Function<Object, Boolean> findEmpties =
                o -> o == null || (o instanceof String && StringUtils.isEmpty((String) o));
        // TODO: tbatie - 1/14/17 - Need further validation of each field
        return fields.stream()
                .filter(s -> findEmpties.apply(FIELD_FUNC_MAP.get(s)
                        .apply(this)))
                .map(s -> createMissingRequiredFieldMsg(s))
                .collect(Collectors.toList());
    }

    public List<ConfigurationMessage> testConditionalBindFields() {
        List<ConfigurationMessage> missingFields = new ArrayList<>();

        // TODO RAP 08 Dec 16: So many magic strings
        String bindMethod = bindUserMethod();
        if (bindMethod.equals("GSSAPI SASL")) {
            if (StringUtils.isEmpty(bindKdcAddress())) {
                missingFields.add(new ConfigurationMessage(FAILURE, MISSING_REQUIRED_FIELD,
                        "Field cannot be empty for GSSAPI SASL bind type").configId(bindKdcAddress));
            }
            if (StringUtils.isEmpty(bindRealm())) {
                missingFields.add(new ConfigurationMessage(FAILURE, MISSING_REQUIRED_FIELD,
                        "Field cannot be empty for GSSAPI SASL bind type").configId(BIND_REALM));
            }
        }

        return missingFields;
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new ConfigurationType(CONFIGURATION_TYPE, LdapConfiguration.class);
    }
}
