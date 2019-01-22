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

import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.YELLOW;
import static org.fusesource.jansi.Ansi.ansi;

import com.google.common.annotations.VisibleForTesting;
import ddf.catalog.CatalogFramework;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.Metacard;
import java.util.List;
import javax.xml.bind.JAXBException;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.catalog.ui.forms.SearchFormsLoader;
import org.codice.ddf.catalog.ui.forms.TemplateTransformer;
import org.codice.ddf.catalog.ui.forms.filter.FilterWriter;
import org.codice.ddf.catalog.ui.util.EndpointUtil;
import org.codice.ddf.commands.catalog.SubjectCommands;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;

@Service
@Command(
  scope = "forms",
  name = "load",
  description = "Handles the ingestion of search forms and result set forms into the system"
)
public class SearchFormsLoaderCommand extends SubjectCommands implements Action {

  private final CatalogFramework catalogFramework;

  private final AttributeRegistry registry;

  private final EndpointUtil endpointUtil;

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

  public SearchFormsLoaderCommand() {
    this(get(CatalogFramework.class), get(AttributeRegistry.class), get(EndpointUtil.class));
  }

  public SearchFormsLoaderCommand(
      CatalogFramework catalogFramework, AttributeRegistry registry, EndpointUtil endpointUtil) {
    this.catalogFramework = catalogFramework;
    this.registry = registry;
    this.endpointUtil = endpointUtil;
  }

  @Override
  protected Object executeWithSubject() throws Exception {

    console.println(ansi().fgBrightCyan().a("Initializing Search Form Template Loader").reset());
    final SearchFormsLoader loader = generateLoader();

    List<Metacard> parsedMetacards = loader.retrieveSystemTemplateMetacards();

    if (!parsedMetacards.isEmpty()) {
      printColor(GREEN, "Loader initialized, beginning ingestion of system templates.");
      loader.bootstrap(parsedMetacards);
      printColor(GREEN, "System templates successfully ingested.");
    } else {
      printColor(YELLOW, "No system forms to load, halting ingest.");
    }
    return null;
  }

  @VisibleForTesting
  protected SearchFormsLoader generateLoader() throws JAXBException {
    FilterWriter writer = new FilterWriter(true);
    TemplateTransformer transformer = new TemplateTransformer(writer, registry);
    return new SearchFormsLoader(
        catalogFramework, transformer, endpointUtil, formsDir, formsFile, resultsFile);
  }

  private static <T> T get(Class<T> type) {
    BundleContext context =
        FrameworkUtil.getBundle(SearchFormsLoaderCommand.class).getBundleContext();
    return context.getService(context.getServiceReference(type));
  }
}
