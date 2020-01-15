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
package org.codice.ddf.commands.catalog;

import ddf.catalog.source.CatalogProvider;
import ddf.catalog.util.impl.ServiceComparator;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.apache.commons.lang.StringUtils;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.commands.catalog.facade.Provider;
import org.osgi.framework.ServiceReference;
import org.osgi.util.tracker.ServiceTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Command(
  scope = CatalogCommands.NAMESPACE,
  name = "migrate",
  description = "Migrates Metacards from one Provider to another Provider."
)
public class MigrateCommand extends DuplicateCommands {

  private static final Logger LOGGER = LoggerFactory.getLogger(MigrateCommand.class);

  @Option(
    name = "--list",
    required = false,
    aliases = {"-list"},
    multiValued = false,
    description = "Print a list of Providers."
  )
  boolean listProviders = false;

  @Option(
    name = "--from",
    required = false,
    aliases = {"-from"},
    multiValued = false,
    description = "The Source ID of the Provider to migrate from."
  )
  String fromProviderId;

  @Option(
    name = "--to",
    required = false,
    aliases = {"-to"},
    multiValued = false,
    description = "The Source ID of the Provider to migrate to."
  )
  String toProviderId;

  @Override
  protected Object executeWithSubject() throws Exception {
    final List<CatalogProvider> providers = getCatalogProviders();

    if (listProviders) {
      if (providers.isEmpty()) {
        console.println("There are no available Providers.");
        return null;
      }
      console.println("Available Providers:");
      providers
          .stream()
          .map(p -> p.getClass().getSimpleName())
          .forEach(id -> console.println("\t" + id));

      return null;
    }

    if (batchSize > MAX_BATCH_SIZE || batchSize < 1) {
      console.println("Batch Size must be between 1 and " + MAX_BATCH_SIZE + ".");
      return null;
    }

    if (providers.isEmpty() || providers.size() < 2) {
      console.println("Not enough CatalogProviders installed to migrate.");
      return null;
    }

    final CatalogProvider fromProvider = promptForProvider("FROM", fromProviderId, providers);
    if (fromProvider == null) {
      console.println("Invalid \"FROM\" Provider id.");
      return null;
    }
    console.println("FROM Provider ID: " + fromProvider.getClass().getSimpleName());

    final CatalogProvider toProvider = promptForProvider("TO", toProviderId, providers);
    if (toProvider == null) {
      console.println("Invalid \"TO\" Provider id.");
      return null;
    }
    console.println("TO Provider ID: " + toProvider.getClass().getSimpleName());

    CatalogFacade queryProvider = new Provider(fromProvider);
    CatalogFacade ingestProvider = new Provider(toProvider);

    start = System.currentTimeMillis();

    console.println("Starting migration.");

    duplicateInBatches(queryProvider, ingestProvider, getFilter(), fromProviderId);

    console.println();
    long end = System.currentTimeMillis();
    String completed =
        String.format(
            " %d record(s) migrated; %d record(s) failed; completed in %3.3f seconds.",
            ingestedCount.get(), failedCount.get(), (end - start) / MS_PER_SECOND);
    LOGGER.debug("Migration Complete: {}", completed);
    console.println(completed);

    return null;
  }

  @SuppressWarnings(
      "squid:S1872" /*Checking class by a name that is passed as an argument, can't use instanceOf*/)
  private CatalogProvider promptForProvider(
      String whichProvider, String id, List<CatalogProvider> providers) throws IOException {
    List<String> providersIdList =
        providers.stream().map(p -> p.getClass().getSimpleName()).collect(Collectors.toList());
    while (true) {
      if (StringUtils.isBlank(id) || !providersIdList.contains(id)) {
        console.println("Please enter the Source ID of the \"" + whichProvider + "\" Provider:");
      } else {
        break;
      }
      id = session.readLine(whichProvider + " Provider ID: ", null);
    }

    final String providerId = id;
    final CatalogProvider provider =
        providers
            .stream()
            .filter(p -> p.getClass().getSimpleName().equals(providerId))
            .findFirst()
            .orElse(null);

    return provider;
  }

  private List<CatalogProvider> getCatalogProviders() {
    ServiceTracker st = new ServiceTracker(bundleContext, CatalogProvider.class.getName(), null);
    st.open();
    ServiceReference<CatalogProvider>[] serviceRefs = st.getServiceReferences();

    Map<ServiceReference<CatalogProvider>, CatalogProvider> map =
        new TreeMap<>(new ServiceComparator());

    if (null != serviceRefs) {
      for (ServiceReference<CatalogProvider> serviceReference : serviceRefs) {
        map.put(serviceReference, (CatalogProvider) st.getService(serviceReference));
      }
    }

    return new ArrayList<>(map.values());
  }
}
