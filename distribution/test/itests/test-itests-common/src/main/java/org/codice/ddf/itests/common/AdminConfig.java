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
 **/
package org.codice.ddf.itests.common;

import java.io.IOException;
import java.util.ArrayList;

import javax.management.NotCompliantMBeanException;

import org.apache.shiro.subject.Subject;
import org.codice.ddf.admin.core.impl.AdminConsoleService;
import org.codice.ddf.admin.core.impl.ConfigurationAdminImpl;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;

public class AdminConfig {

    private final ConfigurationAdmin configAdmin;

    public AdminConfig(ConfigurationAdmin configAdmin) {
        this.configAdmin = configAdmin;
    }

    public AdminConsoleService getAdminConsoleService() throws NotCompliantMBeanException {
        return new AdminConsoleService(configAdmin,
                new ConfigurationAdminImpl(configAdmin, new ArrayList<>()) {
                    @Override
                    public boolean isPermittedToViewService(String servicePid) {
                        return true;
                    }

                    @Override
                    public boolean isPermittedToViewService(String servicePid, Subject subject) {
                        return true;
                    }
                }) {
            @Override
            public boolean isPermittedToViewService(String servicePid) {
                return true;
            }
        };
    }

    public Configuration createFactoryConfiguration(String s) throws IOException {
        return configAdmin.createFactoryConfiguration(s);
    }

    public Configuration createFactoryConfiguration(String s, String s1) throws IOException {
        return configAdmin.createFactoryConfiguration(s, s1);
    }

    public Configuration getConfiguration(String s, String s1) throws IOException {
        return configAdmin.getConfiguration(s, s1);
    }

    public Configuration getConfiguration(String s) throws IOException {
        return configAdmin.getConfiguration(s);
    }

    public Configuration[] listConfigurations(String s) throws IOException, InvalidSyntaxException {
        return configAdmin.listConfigurations(s);
    }
}
