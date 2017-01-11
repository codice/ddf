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
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.NO_TEST_FOUND;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.buildMessage;
import static org.codice.ddf.admin.security.ldap.LdapConfigurationHandler.LdapTestResultType.CANNOT_BIND;
import static org.codice.ddf.admin.security.ldap.LdapConfigurationHandler.LdapTestResultType.CANNOT_CONFIGURE;
import static org.codice.ddf.admin.security.ldap.LdapConfigurationHandler.LdapTestResultType.CANNOT_CONNECT;
import static org.codice.ddf.admin.security.ldap.LdapConfigurationHandler.LdapTestResultType.SUCCESSFUL_BIND;
import static org.codice.ddf.admin.security.ldap.LdapConfigurationHandler.LdapTestResultType.SUCCESSFUL_CONNECTION;
import static org.codice.ddf.admin.security.ldap.test.LdapTestingCommons.selectBindMethod;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import javax.net.ssl.SSLContext;

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
import org.codice.ddf.admin.security.ldap.probe.BindUserExampleProbe;
import org.codice.ddf.admin.security.ldap.probe.DefaultDirectoryStructureProbe;
import org.codice.ddf.admin.security.ldap.probe.LdapQueryProbe;
import org.codice.ddf.admin.security.ldap.probe.SubjectAttributeProbe;
import org.codice.ddf.admin.security.ldap.test.AttributeMappingTestMethod;
import org.codice.ddf.admin.security.ldap.test.BindUserTestMethod;
import org.codice.ddf.admin.security.ldap.test.ConnectTestMethod;
import org.codice.ddf.admin.security.ldap.test.DirectoryStructTestMethod;
import org.forgerock.opendj.ldap.Connection;
import org.forgerock.opendj.ldap.LDAPConnectionFactory;
import org.forgerock.opendj.ldap.LDAPOptions;
import org.forgerock.opendj.ldap.requests.BindRequest;
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
        return new Configurator().getManagedServiceConfigs("Ldap_Login_Config")
                .values()
                .stream()
                .map(LdapConfiguration::new)
                .collect(Collectors.toList());
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
                new ProbeReport(Collections.singletonList(buildMessage(FAILURE,
                        "UNKNOWN PROBE ID")));
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
                new TestReport(new ConfigurationMessage(NO_TEST_FOUND));
    }

    @Override
    public TestReport persist(LdapConfiguration config, String persistId) {
        Configurator configurator = new Configurator();
        ConfigReport report;

        switch (persistId) {
        case "create":
            if (!LDAP_USE_CASES.contains(config.ldapUseCase())) {
                return new TestReport(buildMessage(FAILURE, "No ldap use case specified"));
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
                return new TestReport(buildMessage(FAILURE, "Unable to persist changes"));
            } else {
                return new TestReport(buildMessage(ConfigurationMessage.MessageType.SUCCESS,
                        "Successfully saved LDAP settings"));
            }
        case "delete":
            configurator.deleteManagedService(config.servicePid());
            report = configurator.commit();
            if (!report.getFailedResults()
                    .isEmpty()) {
                return new TestReport(buildMessage(FAILURE, "Unable to delete LDAP Configuration"));
            } else {
                return new TestReport(buildMessage(ConfigurationMessage.MessageType.SUCCESS,
                        "Successfully deleted LDAP Configuration"));
            }
        default:
            return new TestReport(buildMessage(FAILURE, "Uknown persist id: " + persistId));
        }
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

    private boolean isStartTls(LdapConfiguration config) {
        return config.encryptionMethod()
                .equalsIgnoreCase(TLS);
    }

    private String getLdapUrl(LdapConfiguration config) {
        return config.encryptionMethod()
                .equalsIgnoreCase(LDAPS) ? "ldaps://" : "ldap://";
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
