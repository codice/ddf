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
package com.lmco.ddf.commands.catalog;

import java.util.Map;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

@Command(scope = CatalogCommands.NAMESPACE, name = "envlist", description = "Provides a list of environment variables")
public class EnvListCommand extends OsgiCommandSupport {

    protected Object doExecute() throws Exception {

        Map<String, String> env = System.getenv();

        for (String envName : env.keySet()) {
            System.out.printf("%s=%s%n", envName, env.get(envName));
        }

        return null;

    }
}
