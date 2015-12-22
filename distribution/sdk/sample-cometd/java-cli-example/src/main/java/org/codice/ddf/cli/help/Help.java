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
package org.codice.ddf.cli.help;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.codice.ddf.cli.RunnableCommand;
import org.codice.ddf.cli.ui.Notify;

import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.model.GlobalMetadata;

@Command(name = "help", description = "A command that provides help on other commands")
public class Help implements RunnableCommand {

    @Inject
    private GlobalMetadata<RunnableCommand> globalMetadata;

    @Arguments(description = "Provides the name of the command you want to provide help for")

    private List<String> commandNames = new ArrayList<>();

    @Option(name = "--include-hidden", description = "When set, hidden commands and options are shown in help", hidden = true)
    private boolean includeHidden = false;

    @Override
    public int run() {
        try {
            com.github.rvesse.airline.help.Help.help(globalMetadata, commandNames, this.includeHidden);
        } catch (IOException e) {
            Notify.error("Error", "Failed to display help", e.getMessage());
            return 1;
        }
        return 0;
    }
}
