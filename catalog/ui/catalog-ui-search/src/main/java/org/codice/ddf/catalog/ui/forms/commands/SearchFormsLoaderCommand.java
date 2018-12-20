/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.ui.forms.commands;

import static org.fusesource.jansi.Ansi.ansi;

import com.google.common.annotations.VisibleForTesting;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.Metacard;
import java.util.List;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.catalog.ui.forms.SearchFormsLoader;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.ddf.commands.catalog.SubjectCommands;
import org.fusesource.jansi.Ansi;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Command(
  scope = "forms",
  name = "load",
  description = "Handles the ingestion of search forms and result set forms into the system"
)
public class SearchFormsLoaderCommand extends SubjectCommands {
  private static final Logger LOGGER = LoggerFactory.getLogger(SearchFormsLoaderCommand.class);

  private static final Ansi.Color WARNING_COLOR = Ansi.Color.YELLOW;

  private @Reference CatalogFramework catalogFramework;

  private @Reference EndpointUtil endpointUtil;

  @Option(
    name = "--formsDirectory",
    aliases = {"-d"},
    description = "Overrides the default forms directory path. Default: /etc/forms"
  )
  protected String formsDir = null;

  @Option(
    name = "--forms",
    aliases = {"-f"},
    description = "Override the default forms.json file name. Default: forms.json"
  )
  protected String formsFile = null;

  @Option(
    name = "--results",
    aliases = {"-r"},
    description = "Override the default results.json file name. Default: results.json"
  )
  protected String resultsFile = null;

  @Override
  protected Object executeWithSubject() {

    console.println(ansi().fgBrightCyan().a("Initializing Search Form Template Loader").reset());
    final SearchFormsLoader loader =
        generateLoader(catalogFramework, endpointUtil, formsDir, formsFile, resultsFile);

    List<Metacard> parsedMetacards = loader.retrieveSystemTemplateMetacards();

    if (!parsedMetacards.isEmpty()) {
      printColor(Ansi.Color.GREEN, "Loader initialized, beginning ingestion of system templates.");
      loader.bootstrap(parsedMetacards);
      printColor(Ansi.Color.GREEN, "System templates successfully ingested.");
    } else {
      printColor(WARNING_COLOR, "No system forms to load, halting ingest.");
    }
    return null;
  }

  @VisibleForTesting
  protected SearchFormsLoader generateLoader(
      CatalogFramework catalogFramework,
      EndpointUtil endpointUtil,
      String formsDir,
      String formsFile,
      String resultsFile) {
    return new SearchFormsLoader(catalogFramework, endpointUtil, formsDir, formsFile, resultsFile);
  }
}
