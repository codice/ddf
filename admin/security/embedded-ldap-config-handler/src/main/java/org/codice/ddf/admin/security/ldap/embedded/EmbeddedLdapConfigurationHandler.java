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

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.NO_METHOD_FOUND;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.codice.ddf.admin.api.config.ConfigurationType;
import org.codice.ddf.admin.api.config.security.ldap.EmbeddedLdapConfiguration;
import org.codice.ddf.admin.api.handler.ConfigurationHandler;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.method.PersistMethod;
import org.codice.ddf.admin.api.handler.report.CapabilitiesReport;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.api.persist.Configurator;
import org.codice.ddf.admin.api.persist.ConfiguratorException;
import org.codice.ddf.admin.security.ldap.embedded.persist.DefaultEmbeddedLdapPersistMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public class EmbeddedLdapConfigurationHandler
        implements ConfigurationHandler<EmbeddedLdapConfiguration> {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(EmbeddedLdapConfigurationHandler.class);

    private static final String EMBEDDED_LDAP_CONFIGURATION_HANDLER_ID = EmbeddedLdapConfiguration.CONFIGURATION_TYPE;

    public static final ImmutableList<PersistMethod> PERSIST_METHODS =
            ImmutableList.of(new DefaultEmbeddedLdapPersistMethod());

    @Override
    public ProbeReport probe(String probeId, EmbeddedLdapConfiguration configuration) {
        return new ProbeReport(new ConfigurationMessage(FAILURE, NO_METHOD_FOUND, null));
    }

    @Override
    public TestReport test(String testId, EmbeddedLdapConfiguration configuration) {
        return new TestReport(new ConfigurationMessage(FAILURE, NO_METHOD_FOUND, null));
    }

    @Override
    public TestReport persist(String persistId, EmbeddedLdapConfiguration configuration) {
        Optional<PersistMethod> persistMethod = PERSIST_METHODS.stream()
                .filter(method -> method.id()
                        .equals(persistId))
                .findFirst();

        return persistMethod.isPresent() ?
                persistMethod.get()
                        .persist(configuration) :
                new TestReport(new ConfigurationMessage(FAILURE,
                        ConfigurationMessage.NO_METHOD_FOUND, null));
    }

    @Override
    public List<EmbeddedLdapConfiguration> getConfigurations() {
        Configurator configurator = new Configurator();
        try {
            if (configurator.isFeatureStarted("opendj-embedded")) {
                Map<String, Object> props = configurator.getConfig(
                        "org.codice.opendj.embedded.server.LDAPManager");
                return Collections.singletonList(EmbeddedLdapConfiguration.fromProperties(props));
            } else {
                return Collections.emptyList();
            }
        } catch (ConfiguratorException e) {
            LOGGER.info("Error retrieving configuration", e);
            return Collections.emptyList();
        }
    }

    @Override
    public CapabilitiesReport getCapabilities() {
        return new CapabilitiesReport(EMBEDDED_LDAP_CONFIGURATION_HANDLER_ID,
                EMBEDDED_LDAP_CONFIGURATION_HANDLER_ID,
                null,
                null,
                PERSIST_METHODS);
    }

    @Override
    public String getConfigurationHandlerId() {
        return EMBEDDED_LDAP_CONFIGURATION_HANDLER_ID;
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new EmbeddedLdapConfiguration().getConfigurationType();
    }
}
