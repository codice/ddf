/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.admin.insecure.defaults.service;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.codice.ddf.admin.insecure.defaults.service.Alert.Level;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class PropertiesFileValidator implements Validator {

    private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesFileValidator.class);

    protected Path path;

    protected List<Alert> alerts;

    public PropertiesFileValidator() {
        alerts = new ArrayList<>();
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public abstract List<Alert> validate();

    protected Properties readFile() {
        File file = new File(path.toString());
        Properties properties = new Properties();

        try (FileInputStream fis = new FileInputStream(file)) {
            properties.load(fis);
        } catch (IOException e) {
            String msg = "Unable to determine if [" + path.toString()
                    + "] is using insecure defaults. ";
            LOGGER.warn(msg, e);
            alerts.add(new Alert(Level.WARN, msg + e.getMessage()));
        }

        return properties;
    }
    
    protected void resetAlerts() {
        alerts = new ArrayList<>();
    }

}
