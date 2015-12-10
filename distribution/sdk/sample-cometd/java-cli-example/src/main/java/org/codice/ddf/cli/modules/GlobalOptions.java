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
package org.codice.ddf.cli.modules;

import com.github.rvesse.airline.annotations.Option;

/**
 * Defines global options that affect all commands
 */
public class GlobalOptions {

    @Option(name = {"-h", "--host"}, title = "Host", description = "DDF Host")
    private String host = "localhost";

    @Option(name = {"-p", "--port"}, title = "Port", description = "DDF Port")
    private String port = "8993";

    @Option(name =  {"-P", "--protocol"}, title = "Protocol", description = "DDF Protocol")
    private String protocol = "https";

    @Option(name = {"-n", "--no-validation"}, title = "Disable Validation", description = "Disables validation of SSL certificates")
    private boolean disableValidation = true;

    public String getUrl() {
        return protocol + "://" + host + ":" + port;
    }

    public boolean getValidation() {
        return disableValidation;
    }
}
