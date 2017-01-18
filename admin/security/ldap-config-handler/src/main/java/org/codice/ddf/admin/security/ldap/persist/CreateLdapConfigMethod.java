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

import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.ATTRIBUTE_MAPPINGS;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.BASE_GROUP_DN;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.BASE_USER_DN;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.BIND_KDC;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.BIND_METHOD;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.BIND_REALM;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.BIND_USER_DN;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.BIND_USER_PASSWORD;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.ENCRYPTION_METHOD;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.GROUP_OBJECT_CLASS;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.HOST_NAME;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.LDAP_USE_CASE;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.MEMBERSHIP_ATTRIBUTE;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.PORT;
import static org.codice.ddf.admin.api.config.ldap.LdapConfiguration.USER_NAME_ATTRIBUTE;
import static org.codice.ddf.admin.api.config.services.LdapClaimsHandlerServiceProperties.LDAP_CLAIMS_HANDLER_FEATURE;
import static org.codice.ddf.admin.api.config.services.LdapClaimsHandlerServiceProperties.LDAP_CLAIMS_HANDLER_MANAGED_SERVICE_FACTORY_PID;
import static org.codice.ddf.admin.api.config.services.LdapClaimsHandlerServiceProperties.ldapConfigToLdapClaimsHandlerService;
import static org.codice.ddf.admin.api.config.services.LdapLoginServiceProperties.LDAP_LOGIN_FEATURE;
import static org.codice.ddf.admin.api.config.services.LdapLoginServiceProperties.LDAP_LOGIN_MANAGED_SERVICE_FACTORY_PID;
import static org.codice.ddf.admin.api.config.services.LdapLoginServiceProperties.ldapConfigurationToLdapLoginService;
import static org.codice.ddf.admin.api.config.validation.LdapValidationUtils.CREDENTIAL_STORE;
import static org.codice.ddf.admin.api.config.validation.LdapValidationUtils.LOGIN;
import static org.codice.ddf.admin.api.config.validation.LdapValidationUtils.LOGIN_AND_CREDENTIAL_STORE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.FAILED_PERSIST;
import static org.codice.ddf.admin.api.handler.commons.HandlerCommons.CREATE;
import static org.codice.ddf.admin.api.handler.commons.HandlerCommons.SUCCESSFUL_PERSIST;
import static org.codice.ddf.admin.api.handler.report.Report.createReport;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.codice.ddf.admin.api.config.ldap.LdapConfiguration;
import org.codice.ddf.admin.api.configurator.Configurator;
import org.codice.ddf.admin.api.configurator.OperationReport;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.report.Report;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;

public class CreateLdapConfigMethod extends PersistMethod<LdapConfiguration>{

    public static final String LDAP_CREATE_ID = CREATE;

    public static final String DESCRIPTION = "Persists the ldap configuration depending on the ldap use case.";
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

    public static final Map<String, String> SUCCESS_TYPES = ImmutableMap.of(SUCCESSFUL_PERSIST, "Successfully saved LDAP settings.");
    public static final Map<String, String> FAILURE_TYPES = ImmutableMap.of(FAILED_PERSIST, "Unable to persist changes.");

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

            // TODO: tbatie - 1/15/17 - Validate optional fields
            Report validationReport = new Report(config.validate(LOGIN_REQUIRED_FIELDS));
            if(validationReport.containsFailureMessages()) {
                return validationReport;
            }

            Map<String, Object> ldapLoginServiceProps = ldapConfigurationToLdapLoginService(config);
            configurator.startFeature(LDAP_LOGIN_FEATURE);
            configurator.createManagedService(LDAP_LOGIN_MANAGED_SERVICE_FACTORY_PID, ldapLoginServiceProps);
        }

        if (config.ldapUseCase().equals(CREDENTIAL_STORE) || config.ldapUseCase().equals(LOGIN_AND_CREDENTIAL_STORE)) {
            Report validationReport = new Report(config.validate(
                    ALL_REQUIRED_FIELDS));
            if(validationReport.containsFailureMessages()) {
                return validationReport;
            }

            Path newAttributeMappingPath = Paths.get(System.getProperty("ddf.home"),
                    "etc",
                    "ws-security",
                    "ldapAttributeMap-" + UUID.randomUUID()
                            .toString() + ".props");
            config.attributeMappingsPath(newAttributeMappingPath.toString());

            Map<String, Object> ldapClaimsServiceProps = ldapConfigToLdapClaimsHandlerService(config);
            configurator.createPropertyFile(newAttributeMappingPath, config.attributeMappings());
            configurator.startFeature(LDAP_CLAIMS_HANDLER_FEATURE);
            configurator.createManagedService(LDAP_CLAIMS_HANDLER_MANAGED_SERVICE_FACTORY_PID, ldapClaimsServiceProps);
        }

        report = configurator.commit();
        return createReport(SUCCESS_TYPES,
                FAILURE_TYPES,
                null,
                report.containsFailedResults() ? FAILED_PERSIST : SUCCESSFUL_PERSIST);
    }
}
