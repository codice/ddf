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
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.NO_TEST_FOUND;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.handler.ConfigurationHandler;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.codice.ddf.admin.api.handler.report.CapabilitiesReport;
import org.codice.ddf.admin.api.handler.report.ProbeReport;
import org.codice.ddf.admin.api.handler.report.TestReport;
import org.codice.ddf.admin.api.persist.ConfigReport;
import org.codice.ddf.admin.api.persist.Configurator;
import org.codice.ddf.admin.api.persist.ConfiguratorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EmbeddedLdapConfigurationHandler
        implements ConfigurationHandler<EmbeddedLdapConfiguration> {
    private static final Logger LOGGER =
            LoggerFactory.getLogger(EmbeddedLdapConfigurationHandler.class);

    private static final String EMBEDDED_LDAP_CONFIGURATION_HANDLER_ID =
            "embeddedLdap";

    @Override
    public ProbeReport probe(String probeId, EmbeddedLdapConfiguration configuration) {
        return null;
    }

    @Override
    public TestReport test(String testId, EmbeddedLdapConfiguration configuration) {
        return new TestReport(new ConfigurationMessage(NO_TEST_FOUND));
    }

    @Override
    public TestReport persist(EmbeddedLdapConfiguration configuration, String persistId) {
        // TODO: tbatie - 12/1/16 - VALIDATE REQUIRED FIELDS

        Configurator configurator = new Configurator();
        configurator.startFeature("opendj-embedded");
        configurator.updateConfigFile("org.codice.opendj.embedded.server.LDAPManager",
                configuration.toPropertiesMap(),
                true);
        ConfigReport report = configurator.commit();

        if(report.containsFailedResults()) {
            return new TestReport(new ConfigurationMessage("Unable to install DDF Embedded LDAP",
                    FAILURE));
        }
        // TODO: tbatie - 12/2/16 - do something with this key
        return new TestReport(new ConfigurationMessage("DDF Embedded Has Successfully Been Started",
                SUCCESS));
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
        return new CapabilitiesReport(EmbeddedLdapConfiguration.class.getSimpleName(),
                EmbeddedLdapConfiguration.class);
    }

    @Override
    public String getConfigurationHandlerId() {
        return EMBEDDED_LDAP_CONFIGURATION_HANDLER_ID;
    }

    @Override
    public Class getConfigClass() {
        return EmbeddedLdapConfiguration.class;
    }
}
