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

package org.codice.ddf.admin.security.ldap;

import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.CREDENTIAL_STORE;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.LDAPS;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.LDAP_USE_CASES;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.LOGIN;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.LOGIN_AND_CREDENTIAL_STORE;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.TLS;
import static org.codice.ddf.admin.api.handler.Configuration.SERVICE_PID_KEY;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.FAILED_PERSIST;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.INVALID_FIELD;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.NO_METHOD_FOUND;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.SUCCESSFUL_PERSIST;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;

import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationHandler;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.handler.report.CapabilitiesReport;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.api.persist.ConfigReport;
import org.codice.ddf.admin.api.persist.Configurator;
import org.codice.ddf.admin.api.persist.ConfiguratorException;
import org.codice.ddf.admin.security.ldap.probe.BindUserExampleProbe;
import org.codice.ddf.admin.security.ldap.probe.DefaultDirectoryStructureProbe;
import org.codice.ddf.admin.security.ldap.probe.LdapQueryProbe;
import org.codice.ddf.admin.security.ldap.probe.SubjectAttributeProbe;
import org.codice.ddf.admin.security.ldap.test.AttributeMappingTestMethod;
import org.codice.ddf.admin.security.ldap.test.BindUserTestMethod;
import org.codice.ddf.admin.security.ldap.test.ConnectTestMethod;
import org.codice.ddf.admin.security.ldap.test.DirectoryStructTestMethod;
import org.codice.ddf.configuration.PropertyResolver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class LdapConfigurationHandler implements ConfigurationHandler<LdapConfiguration> {
    private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigurationHandler.class);

    private List<TestMethod> testMethods = ImmutableList.of(new ConnectTestMethod(),
            new BindUserTestMethod(),
            new DirectoryStructTestMethod(),
            new AttributeMappingTestMethod());

    private List<ProbeMethod> probeMethods = ImmutableList.of(new DefaultDirectoryStructureProbe(),
            new BindUserExampleProbe(),
            new LdapQueryProbe(),
            new SubjectAttributeProbe());

    private static final String LDAP_CONFIGURATION_HANDLER_ID = "ldap";

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
        // TODO: tbatie - 1/12/17 - Also need to show configs when only configured as a claims handler. fpid = Claims_Handler_Manager
        // TODO: tbatie - 1/12/17 - Also need to cross reference this list with the ldap claim handler configs to only show one ldap config. LdapUseCase should be LoginAndCredientalSotre
        return new Configurator().getManagedServiceConfigs("Ldap_Login_Config")
                .values()
                .stream()
                .map(LdapConfigurationHandler::ldapLoginServiceToLdapLoginConfiguration)
                .collect(Collectors.toList());
    }

    private static final LdapConfiguration ldapLoginServiceToLdapLoginConfiguration(Map<String, Object> props) {
        //The keys below are specific to the Ldap_Login_Config service and mapped to the general LDAP configuration class fields
        //This should eventually be cleaned up and structured data should be sent between the ldap login and claims services rather than map
        // TODO: tbatie - 1/11/17 - Make sure to use the same constants as the persist method uses
        LdapConfiguration ldapConfiguration = new LdapConfiguration();
        ldapConfiguration.servicePid(
                props.get(SERVICE_PID_KEY) == null ? null : (String) props.get(SERVICE_PID_KEY));
        ldapConfiguration.bindUserDn((String) props.get("ldapBindUserDn"));
        ldapConfiguration.bindUserPassword((String) props.get("ldapBindUserPass"));
        ldapConfiguration.bindUserMethod((String) props.get("bindMethod"));
        ldapConfiguration.bindKdcAddress((String) props.get("kdcAddress"));
        ldapConfiguration.bindRealm((String) props.get("realm"));
        ldapConfiguration.userNameAttribute((String) props.get("userNameAttribute"));
        ldapConfiguration.baseUserDn((String) props.get("userBaseDn"));
        ldapConfiguration.baseGroupDn((String) props.get("groupBaseDn"));
        URI ldapUri = getUriFromProperty((String) props.get("ldapUrl"));
        ldapConfiguration.encryptionMethod(ldapUri.getScheme());
        ldapConfiguration.hostName(ldapUri.getHost());
        ldapConfiguration.port(ldapUri.getPort());
        if ((Boolean) props.get("startTls")) {
            ldapConfiguration.encryptionMethod(TLS);
        }
        ldapConfiguration.ldapUseCase(LOGIN);
        return ldapConfiguration;
    }

    private static final URI getUriFromProperty(String ldapUrl) {
        try {
            ldapUrl = PropertyResolver.resolveProperties(ldapUrl);
            if (!ldapUrl.matches("\\w*://.*")) {
                ldapUrl = "ldap://" + ldapUrl;
            }
        } catch (ConfiguratorException e) {
            return null;
        }
        return URI.create(ldapUrl);
    }

    @Override
    public CapabilitiesReport getCapabilities() {
        return new CapabilitiesReport(getConfigurationHandlerId(),
                getConfigurationHandlerId(),
                testMethods,
                probeMethods,
                null);
    }

    @Override
    public ProbeReport probe(String probeId, LdapConfiguration ldapConfiguration) {
        Optional<ProbeMethod> probeMethod = probeMethods.stream()
                .filter(method -> method.id()
                        .equals(probeId))
                .findFirst();

        // TODO RAP 10 Jan 17: Clean up default path
        return probeMethod.isPresent() ?
                probeMethod.get()
                        .probe(ldapConfiguration) :
                new ProbeReport(Collections.singletonList(buildMessage(FAILURE, NO_METHOD_FOUND, null)));
    }

    @Override
    public TestReport test(String testId, LdapConfiguration ldapConfiguration) {
        Optional<TestMethod> testMethod = testMethods.stream()
                .filter(method -> method.id()
                        .equals(testId))
                .findFirst();

        return testMethod.isPresent() ?
                testMethod.get()
                        .test(ldapConfiguration) :
                new TestReport(new ConfigurationMessage(FAILURE, NO_METHOD_FOUND, null));
    }

    @Override
    public TestReport persist(LdapConfiguration config, String persistId) {
        Configurator configurator = new Configurator();
        ConfigReport report;

        switch (persistId) {
        case "create":
            if (!LDAP_USE_CASES.contains(config.ldapUseCase())) {
                return new TestReport(buildMessage(FAILURE, INVALID_FIELD, "No ldap use case specified or not supported."));
            }

            if (config.ldapUseCase()
                    .equals(LOGIN) || config.ldapUseCase()
                    .equals(LOGIN_AND_CREDENTIAL_STORE)) {
                // TODO: tbatie - 12/8/16 - Perform validation and add a config to map option like the sources config
                Map<String, Object> ldapStsConfig = new HashMap<>();

                String ldapUrl = getLdapUrl(config);
                boolean startTls = isStartTls(config);

                ldapStsConfig.put("ldapBindUserDn", config.bindUserDn());
                ldapStsConfig.put("ldapBindUserPass", config.bindUserPassword());
                ldapStsConfig.put("bindMethod", config.bindUserMethod());
                ldapStsConfig.put("kdcAddress", config.bindKdcAddress());
                ldapStsConfig.put("realm", config.bindRealm());

                ldapStsConfig.put("userNameAttribute", config.userNameAttribute());
                ldapStsConfig.put("userBaseDn", config.baseUserDn());
                ldapStsConfig.put("groupBaseDn", config.baseGroupDn());

                ldapStsConfig.put("ldapUrl", ldapUrl + config.hostName() + ":" + config.port());
                ldapStsConfig.put("startTls", Boolean.toString(startTls));
                configurator.startFeature("security-sts-ldaplogin");
                configurator.createManagedService("Ldap_Login_Config", ldapStsConfig);
            }

            if (config.ldapUseCase()
                    .equals(CREDENTIAL_STORE) || config.ldapUseCase()
                    .equals(LOGIN_AND_CREDENTIAL_STORE)) {
                Path newAttributeMappingPath = Paths.get(System.getProperty("ddf.home"),
                        "etc",
                        "ws-security",
                        "ldapAttributeMap-" + UUID.randomUUID()
                                .toString() + ".props");
                configurator.createPropertyFile(newAttributeMappingPath,
                        config.attributeMappings());
                String ldapUrl = getLdapUrl(config);
                boolean startTls = isStartTls(config);

                Map<String, Object> ldapClaimsHandlerConfig = new HashMap<>();
                ldapClaimsHandlerConfig.put("url",
                        ldapUrl + config.hostName() + ":" + config.port());
                ldapClaimsHandlerConfig.put("startTls", startTls);
                ldapClaimsHandlerConfig.put("ldapBindUserDn", config.bindUserDn());
                ldapClaimsHandlerConfig.put("password", config.bindUserPassword());
                ldapClaimsHandlerConfig.put("membershipUserAttribute", config.userNameAttribute());
                ldapClaimsHandlerConfig.put("loginUserAttribute", config.userNameAttribute());
                ldapClaimsHandlerConfig.put("userBaseDn", config.baseUserDn());
                ldapClaimsHandlerConfig.put("objectClass", config.groupObjectClass());
                ldapClaimsHandlerConfig.put("memberNameAttribute", config.membershipAttribute());
                ldapClaimsHandlerConfig.put("groupBaseDn", config.baseGroupDn());
                ldapClaimsHandlerConfig.put("bindMethod", config.bindUserMethod());
                ldapClaimsHandlerConfig.put("propertyFileLocation",
                        newAttributeMappingPath.toString());

                configurator.startFeature("security-sts-ldapclaimshandler");
                configurator.createManagedService("Claims_Handler_Manager",
                        ldapClaimsHandlerConfig);
            }

            report = configurator.commit();
            if (!report.getFailedResults()
                    .isEmpty()) {
                return new TestReport(buildMessage(FAILURE, FAILED_PERSIST, "Unable to persist changes"));
            } else {
                return new TestReport(buildMessage(SUCCESS, SUCCESSFUL_PERSIST,
                        "Successfully saved LDAP settings"));
            }
        case "delete":
            configurator.deleteManagedService(config.servicePid());
            report = configurator.commit();
            if (!report.getFailedResults()
                    .isEmpty()) {
                return new TestReport(buildMessage(FAILURE, FAILED_PERSIST, "Unable to delete LDAP Configuration"));
            } else {
                return new TestReport(buildMessage(SUCCESS, SUCCESSFUL_PERSIST,
                        "Successfully deleted LDAP Configuration"));
            }
        default:
            return new TestReport(buildMessage(FAILURE, ConfigurationMessage.NO_METHOD_FOUND, null));
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
