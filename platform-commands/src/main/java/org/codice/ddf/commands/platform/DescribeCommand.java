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
package org.codice.ddf.commands.platform;

import java.util.Map;
import java.util.TreeSet;

import org.apache.felix.gogo.commands.Command;

import org.codice.ddf.configuration.ConfigurationWatcher;

@Command(scope = PlatformCommands.NAMESPACE, name = "describe", description = "Provides a description of the platform")
public class DescribeCommand extends PlatformCommands implements ConfigurationWatcher {

    private static Map<String, String> configurationMap;

    @Override
    protected Object doExecute() throws Exception {
        TreeSet<String> keys = new TreeSet<String>(configurationMap.keySet());
        for (String key : keys) {
            System.out.printf("%s=%s\n", key, configurationMap.get(key));
        }
        return null;
    }

    @Override
    public void configurationUpdateCallback(Map<String, String> configuration) {
        configurationMap = configuration;
    }
}
