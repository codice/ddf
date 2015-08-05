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
package org.codice.ddf.commands.catalog;

import java.util.Map;

import org.apache.felix.gogo.commands.Command;
import org.apache.karaf.shell.console.OsgiCommandSupport;

@Command(scope = CatalogCommands.NAMESPACE, name = "envlist", description = "(Deprecated) Provides a list of environment variables")
public class EnvListCommand extends OsgiCommandSupport {

    @Override
    protected Object doExecute() throws Exception {

        Map<String, String> env = System.getenv();

        for (Map.Entry<String, String> entry : env.entrySet()) {
            System.out.printf("%s=%s%n", entry.getKey(), entry.getValue());
        }

        System.out.println("This command is deprecated and will be removed in DDF 3.0.");
        System.out.println("Please use platform:envlist instead");

        return null;
    }
}
