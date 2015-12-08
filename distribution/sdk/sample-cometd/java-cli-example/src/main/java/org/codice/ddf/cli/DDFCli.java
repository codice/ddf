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
package org.codice.ddf.cli;

import org.codice.ddf.cli.commands.Download;
import org.codice.ddf.cli.help.Help;

import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.builder.CliBuilder;

public class DDFCli {

    public static void main(String[] args) {
        CliBuilder builder = Cli.builder("ddf")
                .withDescription("DDF Command Line Client");

        builder.withParser()
                .withCommandAbbreviation()
                .withOptionAbbreviation();

        builder.withCommand(Download.class)
                .withCommand(Help.class)
                .withDefaultCommand(Help.class);

        CommandExecutor.executeCli(builder.build(), args);
    }
}
