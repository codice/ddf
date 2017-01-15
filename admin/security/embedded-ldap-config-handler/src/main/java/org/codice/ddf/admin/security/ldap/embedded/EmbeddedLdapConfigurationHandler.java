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

package org.codice.ddf.admin.security.ldap.embedded;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.config.ConfigurationType;
import org.codice.ddf.admin.api.config.security.ldap.EmbeddedLdapConfiguration;
import org.codice.ddf.admin.api.handler.DefaultConfigurationHandler;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.method.ProbeMethod;
import org.codice.ddf.admin.api.handler.method.TestMethod;
import org.codice.ddf.admin.api.persist.Configurator;
import org.codice.ddf.admin.api.persist.ConfiguratorException;
import org.codice.ddf.admin.security.ldap.embedded.persist.DefaultEmbeddedLdapPersistMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class EmbeddedLdapConfigurationHandler
        extends DefaultConfigurationHandler<EmbeddedLdapConfiguration> {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(EmbeddedLdapConfigurationHandler.class);

    private static final String EMBEDDED_LDAP_CONFIGURATION_HANDLER_ID = EmbeddedLdapConfiguration.CONFIGURATION_TYPE;

    @Override
    public String getConfigurationHandlerId() {
        return EMBEDDED_LDAP_CONFIGURATION_HANDLER_ID;
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new EmbeddedLdapConfiguration().getConfigurationType();
    }

    @Override
    public List<ProbeMethod> getProbeMethods() {
        return null;
    }

    @Override
    public List<TestMethod> getTestMethods() {
        return null;
    }

    public List<PersistMethod> getPersistMethods(){
        return  ImmutableList.of(new DefaultEmbeddedLdapPersistMethod());
    }

    @Override
    public List<EmbeddedLdapConfiguration> getConfigurations() {
        Configurator configurator = new Configurator();
        try {
            if (configurator.isFeatureStarted("opendj-embedded")) {
                Map<String, Object> props = configurator.getConfig(
                        "org.codice.opendj.embedded.server.LDAPManager");
                EmbeddedLdapConfiguration config = new EmbeddedLdapConfiguration();
                config.embeddedLdapPort((int) props.get("embeddedLdapPort"));
                config.embeddedLdapsPort((int) props.get("embeddedLdapsPort"));
                config.embeddedLdapAdminPort((int) props.get("embeddedLdapAdminPort"));
                config.ldifPath((String) props.get("ldifPath"));
                config.embeddedLdapStorageLocation((String) props.get("embeddedLdapStorageLocation"));

                return Collections.singletonList(config);
            } else {
                return Collections.emptyList();
            }
        } catch (ConfiguratorException e) {
            LOGGER.info("Error retrieving configuration", e);
            return Collections.emptyList();
        }
    }
}
