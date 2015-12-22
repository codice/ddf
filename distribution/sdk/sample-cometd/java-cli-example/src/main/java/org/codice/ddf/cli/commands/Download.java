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
package org.codice.ddf.cli.commands;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.codice.ddf.cli.RunnableCommand;
import org.codice.ddf.cli.modules.GlobalOptions;
import org.codice.ddf.cli.ui.Notify;
import org.codice.ddf.sdk.cometd.AsyncClient;

import com.github.rvesse.airline.annotations.Arguments;
import com.github.rvesse.airline.annotations.Command;
import com.github.rvesse.airline.annotations.Option;
import com.github.rvesse.airline.annotations.restrictions.Required;

/**
 * DDF Download Cli Implementation
 */
@Command(name = "download", description = "Downloads resources from the ddf")
public class Download implements RunnableCommand {

    @Inject
    private GlobalOptions globtions = new GlobalOptions();

    @Option(name = {"-s", "--source"}, title = "DDF Source", description = "Source name of the DDF instance to download from")
    private String sourceName = "ddf.distribution";

    @Arguments(description = "Catalog-Id(s) to download", usage = "foo bar baz")
    @Required
    private List<String> cids = new ArrayList<>();

    private AsyncClient asyncClient;

    @Override
    public int run() {
        String url = globtions.getUrl();
        try {
            asyncClient = new AsyncClient(url, globtions.getValidation());

        } catch (Exception e) {
            Notify.error("Client Error", "Problem creating client for: " + url, e.getMessage());
            return 1;
        }
        for (String cid:cids) {
            Notify.normal("Queuing Download", cid);
            try {
                asyncClient.downloadById(cid, sourceName);
            } catch (MalformedURLException e) {
                Notify.error("Download Error", "Problem downloading file for: " + cid, e.toString());
                return 1;
            }
        }
        return 0;
    }
}
