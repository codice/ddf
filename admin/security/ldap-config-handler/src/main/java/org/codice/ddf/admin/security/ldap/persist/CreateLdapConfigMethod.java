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

package org.codice.ddf.admin.security.ldap.persist;

import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.ATTRIBUTE_MAPPINGS;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.BASE_GROUP_DN;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.BASE_USER_DN;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.BIND_KDC;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.BIND_METHOD;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.BIND_REALM;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.BIND_USER_DN;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.BIND_USER_PASSWORD;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.CREDENTIAL_STORE;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.ENCRYPTION_METHOD;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.GROUP_OBJECT_CLASS;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.HOST_NAME;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.LDAPS;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.LDAP_USE_CASE;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.LOGIN;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.LOGIN_AND_CREDENTIAL_STORE;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.MEMBERSHIP_ATTRIBUTE;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.PORT;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.TLS;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.USER_NAME_ATTRIBUTE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.FAILED_PERSIST;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.SUCCESSFUL_PERSIST;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.report.Report;
import org.codice.ddf.admin.api.configurator.OperationReport;
import org.codice.ddf.admin.api.configurator.Configurator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class CreateLdapConfigMethod extends PersistMethod<LdapConfiguration>{

    public static final String LDAP_CREATE_ID = "create";
    public static final String DESCRIPTION = "Persists the ldap configuration depending on the ldap use case.";

    public static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(SUCCESSFUL_PERSIST, "Successfully saved LDAP settings.");
    public static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(FAILED_PERSIST, "Unable to persist changes.");
    public static final List<String> LOGIN_REQUIRED_FIELDS = ImmutableList.of(LDAP_USE_CASE,
            HOST_NAME,
            PORT,
            ENCRYPTION_METHOD,
            BIND_USER_DN,
            BIND_USER_PASSWORD,
            BIND_METHOD,
            USER_NAME_ATTRIBUTE,
            BASE_USER_DN,
            BASE_GROUP_DN);

    public static final List<String> LOGIN_OPTIONAL_FIELDS = ImmutableList.of(BIND_KDC, BIND_REALM);

    public static final List<String> ALL_REQUIRED_FIELDS = ImmutableList.<String>builder()
            .addAll(LOGIN_REQUIRED_FIELDS)
            .add(GROUP_OBJECT_CLASS)
            .add(MEMBERSHIP_ATTRIBUTE)
            .add(ATTRIBUTE_MAPPINGS).build();

    public CreateLdapConfigMethod() {
        super(LDAP_CREATE_ID,
                DESCRIPTION,
                ALL_REQUIRED_FIELDS,
                LOGIN_OPTIONAL_FIELDS,
                SUCCESS_TYPES,
                FAILURE_TYPES,
                null);
    }

    @Override
    public Report persist(LdapConfiguration config) {
        OperationReport report;
        Configurator configurator = new Configurator();
        if (config.ldapUseCase()
                .equals(LOGIN) || config.ldapUseCase()
                .equals(LOGIN_AND_CREDENTIAL_STORE)) {
            // TODO adimka Move validation to use the validate method instead of this stuff
            Report validationReport = new Report(config.checkRequiredFields(LOGIN_REQUIRED_FIELDS));
            if(validationReport.containsFailureMessages()) {
                return validationReport;
            }
            // TODO: tbatie - 1/15/17 - Validate optional fields

            Map<String, Object> ldapStsConfig = new HashMap<>();

            String ldapUrl = getLdapUrl(config);
            boolean startTls = isStartTls(config);

            ldapStsConfig.put("ldapUrl", ldapUrl + config.hostName() + ":" + config.port());
            ldapStsConfig.put("startTls", Boolean.toString(startTls));
            ldapStsConfig.put("ldapBindUserDn", config.bindUserDn());
            ldapStsConfig.put("ldapBindUserPass", config.bindUserPassword());
            ldapStsConfig.put("bindMethod", config.bindUserMethod());
            ldapStsConfig.put("kdcAddress", config.bindKdcAddress());
            ldapStsConfig.put("realm", config.bindRealm());

            ldapStsConfig.put("userNameAttribute", config.userNameAttribute());
            ldapStsConfig.put("userBaseDn", config.baseUserDn());
            ldapStsConfig.put("groupBaseDn", config.baseGroupDn());

            configurator.startFeature("security-sts-ldaplogin");
            configurator.createManagedService("Ldap_Login_Config", ldapStsConfig);
        }

        if (config.ldapUseCase()
                .equals(CREDENTIAL_STORE) || config.ldapUseCase()
                .equals(LOGIN_AND_CREDENTIAL_STORE)) {
            Report validationReport = new Report(config.checkRequiredFields(
                    ALL_REQUIRED_FIELDS));
            if(validationReport.containsFailureMessages()) {
                return validationReport;
            }

            Map<String, Object> ldapClaimsHandlerConfig = new HashMap<>();
            String ldapUrl = getLdapUrl(config);
            boolean startTls = isStartTls(config);

            ldapClaimsHandlerConfig.put("url",
                    ldapUrl + config.hostName() + ":" + config.port());
            ldapClaimsHandlerConfig.put("startTls", startTls);
            ldapClaimsHandlerConfig.put("ldapBindUserDn", config.bindUserDn());
            ldapClaimsHandlerConfig.put("password", config.bindUserPassword());
            ldapClaimsHandlerConfig.put("bindMethod", config.bindUserMethod());
            ldapClaimsHandlerConfig.put("loginUserAttribute", config.userNameAttribute());
            ldapClaimsHandlerConfig.put("userBaseDn", config.baseUserDn());
            ldapClaimsHandlerConfig.put("groupBaseDn", config.baseGroupDn());
            ldapClaimsHandlerConfig.put("objectClass", config.groupObjectClass());
            ldapClaimsHandlerConfig.put("membershipUserAttribute", config.membershipAttribute());

            // TODO: tbatie - 1/15/17 - memberNameAttribute is not implemented in UI
            ldapClaimsHandlerConfig.put("memberNameAttribute", config.membershipAttribute());
            Path newAttributeMappingPath = Paths.get(System.getProperty("ddf.home"),
                    "etc",
                    "ws-security",
                    "ldapAttributeMap-" + UUID.randomUUID()
                            .toString() + ".props");
            configurator.createPropertyFile(newAttributeMappingPath, config.attributeMappings());
            ldapClaimsHandlerConfig.put("propertyFileLocation", newAttributeMappingPath.toString());
            configurator.startFeature("security-sts-ldapclaimshandler");
            configurator.createManagedService("Claims_Handler_Manager",
                    ldapClaimsHandlerConfig);
        }

        report = configurator.commit();
        if (!report.getFailedResults()
                .isEmpty()) {
            return new Report(buildMessage(FAILURE, FAILED_PERSIST, FAILURE_TYPES.get(FAILED_PERSIST)));
        } else {
            return new Report(buildMessage(SUCCESS, SUCCESSFUL_PERSIST,
                SUCCESS_TYPES.get(SUCCESSFUL_PERSIST)));
        }
    }

    private boolean isStartTls(LdapConfiguration config) {
        return config.encryptionMethod()
                .equalsIgnoreCase(TLS);
    }

    private String getLdapUrl(LdapConfiguration config) {
        return config.encryptionMethod()
                .equalsIgnoreCase(LDAPS) ? "ldaps://" : "ldap://";
    }
}
