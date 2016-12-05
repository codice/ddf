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

package org.codice.ui.admin.ldap.config;

import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.NO_TEST_FOUND;
import static org.codice.ui.admin.wizard.api.ConfigurationMessage.MessageType.SUCCESS;

import java.util.List;

import org.codice.ui.admin.wizard.api.CapabilitiesReport;
import org.codice.ui.admin.wizard.api.ConfigurationHandler;
import org.codice.ui.admin.wizard.api.ConfigurationMessage;
import org.codice.ui.admin.wizard.api.ProbeReport;
import org.codice.ui.admin.wizard.api.TestReport;
import org.codice.ui.admin.wizard.config.ConfigReport;
import org.codice.ui.admin.wizard.config.Configurator;

public class EmbeddedLdapConfigurationHandler
        implements ConfigurationHandler<EmbeddedLdapConfiguration> {
    @Override
    public ProbeReport probe(String probeId, EmbeddedLdapConfiguration configuration) {
        // TODO: tbatie - 12/1/16 - Implement embedded LDAP probe
        return null;
    }

    @Override
    public TestReport test(String testId, EmbeddedLdapConfiguration configuration) {
        return new TestReport(new ConfigurationMessage(NO_TEST_FOUND));
    }

    @Override
    public TestReport persist(EmbeddedLdapConfiguration configuration) {
        // TODO: tbatie - 12/1/16 - VALIDATE REQUIRED FIELDS

        Configurator configurator = new Configurator();
        configurator.startFeature("opendj-embedded");
        configurator.updateConfigFile("org.codice.opendj.embedded.server.LDAPManager",
                configuration.toPropertiesMap(),
                true);
        ConfigReport report = configurator.commit();
        // TODO: tbatie - 12/2/16 - do something with this key
        return new TestReport(new ConfigurationMessage("DDF Embedded Has Successfully Been Started", SUCCESS));
    }

    @Override
    public List<EmbeddedLdapConfiguration> getConfigurations() {
        // TODO: tbatie - 12/1/16 - Implement embedded LDAP configuring
        return null;
    }

    @Override
    public CapabilitiesReport getCapabilities() {
        return new CapabilitiesReport(EmbeddedLdapConfiguration.class.getSimpleName(), EmbeddedLdapConfiguration.class);
    }

    @Override
    public String getConfigurationHandlerId() {
        return "embeddedLdap";
    }
}
