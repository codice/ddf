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

import org.codice.ddf.cli.ui.Notify;

import com.github.rvesse.airline.Cli;
import com.github.rvesse.airline.SingleCommand;
import com.github.rvesse.airline.parser.errors.ParseException;


public class CommandExecutor {

    private static <T extends RunnableCommand> void execute(T cmd) {
        try {
            int exitCode = cmd.run();
            System.exit(exitCode);
        } catch (Throwable e) {
            Notify.error("Command threw error: ", e.getMessage());
        }
    }

    public static <T extends RunnableCommand> void executeSingleCommand(Class<T> cls, String[] args) {
        SingleCommand<T> parser = SingleCommand.singleCommand(cls);
        try {
            T cmd = parser.parse(args);
            execute(cmd);
        } catch (ParseException e) {
            Notify.error("Parser error: ", e.getMessage());
        } catch (Throwable e) {
            Notify.error("Unexpected error: ", e.getMessage());
        }
    }

    public static <T extends RunnableCommand> void executeCli(Cli<T> cli, String[] args) {
        try {
            T cmd = cli.parse(args);
            execute(cmd);
        } catch (ParseException e) {
            Notify.error("Parser error: ", e.getMessage());
        } catch (Throwable e) {
            Notify.error("Unexpected error: ", e.getMessage());
        }
    }
}
