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

import org.apache.felix.gogo.commands.Command;

import java.util.Map;


@Command(scope = PlatformCommands.NAMESPACE, name = "envlist", description = "Provides a list of environment variables")
public class EnvListCommand extends PlatformCommands {

    @Override
    protected Object doExecute() throws Exception {

        Map<String, String> env = System.getenv();

        for (Map.Entry<String, String> entry : env.entrySet()) {
            System.out.printf("%s=%s%n", entry.getKey(), entry.getValue());
        }

        return null;
    }
}
