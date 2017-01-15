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

import static org.codice.ddf.admin.api.config.Configuration.SERVICE_PID_KEY;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.LOGIN;
import static org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration.TLS;

import java.net.URI;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codice.ddf.admin.api.config.ConfigurationType;
import org.codice.ddf.admin.api.config.security.ldap.LdapConfiguration;
import org.codice.ddf.admin.api.handler.DefaultConfigurationHandler;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.persist.Configurator;
import org.codice.ddf.admin.api.persist.ConfiguratorException;
import org.codice.ddf.admin.security.ldap.persist.CreateLdapConfigMethod;
import org.codice.ddf.admin.security.ldap.persist.DeleteLdapConfigMethod;
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

public class LdapConfigurationHandler extends DefaultConfigurationHandler<LdapConfiguration> {

    private static final String LDAP_CONFIGURATION_HANDLER_ID = LdapConfiguration.CONFIGURATION_TYPE;

    private static final Logger LOGGER = LoggerFactory.getLogger(LdapConfigurationHandler.class);

    @Override
    public String getConfigurationHandlerId() {
        return LDAP_CONFIGURATION_HANDLER_ID;
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new LdapConfiguration().getConfigurationType();
    }

    @Override
    public List<ProbeMethod> getProbeMethods() {
        return ImmutableList.of(new DefaultDirectoryStructureProbe(),
                new BindUserExampleProbe(),
                new LdapQueryProbe(),
                new SubjectAttributeProbe());
    }

    @Override
    public List<TestMethod> getTestMethods() {
        return ImmutableList.of(new ConnectTestMethod(),
                new BindUserTestMethod(),
                new DirectoryStructTestMethod(),
                new AttributeMappingTestMethod());
    }

    @Override
    public List<PersistMethod> getPersistMethods() {
        return ImmutableList.of(new CreateLdapConfigMethod(),
                new DeleteLdapConfigMethod());
    }

    @Override
    public List<LdapConfiguration> getConfigurations() {
        // TODO: tbatie - 1/12/17 - Also need to show configs when only configured as a claims handler. fpid = Claims_Handler_Manager. will need provide mulitple service pids
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
}
