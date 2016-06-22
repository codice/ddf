/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.security.config.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.AccessController;
import java.util.Dictionary;

import javax.security.auth.Subject;

import org.apache.felix.cm.file.ConfigurationHandler;
import org.apache.shiro.util.ThreadContext;
import org.osgi.service.cm.Configuration;
import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.cm.ConfigurationEvent;
import org.osgi.service.cm.SynchronousConfigurationListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.common.audit.SecurityLogger;

public class ConfigurationSecurityLogger implements SynchronousConfigurationListener {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConfigurationSecurityLogger.class);

    private ConfigurationAdmin configurationAdmin;

    @Override
    public void configurationEvent(ConfigurationEvent event) {
        try {
            String type = getType(event);
            Configuration[] configuration = configurationAdmin.listConfigurations(
                    "(service.pid=" + event.getPid() + ')');

            String updatedConfiguration = "{}";
            //the service.pid is unique so we just need to check if we got anything.
            if (configuration != null && configuration.length > 0) {
                updatedConfiguration = dictionaryToString(configuration[0].getProperties());
            }

            //check if there is a subject associated with the configuration change
            if (ThreadContext.getSubject() != null
                    || Subject.getSubject(AccessController.getContext()) != null) {
                SecurityLogger.audit("Configuration {}: {}", type, updatedConfiguration);
            } else {
                //there was no subject change was caused by an update to the config file on the filesystem
                SecurityLogger.auditWarn("Configuration {} via filesystem: {}",
                        type,
                        updatedConfiguration);
            }
        } catch (Throwable e) {
            LOGGER.error("Error auditing config update for " + event.getPid(), e);
            SecurityLogger.audit("Error auditing config update for {}", event.getPid());
        }

    }

    private String dictionaryToString(Dictionary<String, Object> properties) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {

            ConfigurationHandler.write(baos, properties);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return '{' + new String(baos.toByteArray(), StandardCharsets.UTF_8).replace("\r\n", " ")
                + '}';
    }

    public void setConfigurationAdmin(ConfigurationAdmin configurationAdmin) {
        this.configurationAdmin = configurationAdmin;
    }

    private String getType(ConfigurationEvent event) {
        switch (event.getType()) {
        case 1:
            return "updated";
        case 2:
            return "deleted";
        case 3:
            return "location changed";
        default:
            return "unknown";
        }
    }

}
