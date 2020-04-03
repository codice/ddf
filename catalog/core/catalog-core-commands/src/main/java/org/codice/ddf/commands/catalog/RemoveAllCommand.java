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

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.data.types.Validation;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.impl.DeleteRequestImpl;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.IngestException;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import java.io.IOException;
import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.joda.time.DateTime;
import org.opengis.filter.Filter;
import org.slf4j.LoggerFactory;

/** Command used to remove all or a subset of records (in bulk) from the Catalog. */
@Service
@Command(
    scope = CatalogCommands.NAMESPACE,
    name = "removeall",
    description = "Attempts to delete all records from the Catalog.")
public class RemoveAllCommand extends CatalogCommands {

  private static final String COMMAND_SCOPE = CatalogCommands.NAMESPACE;

  private static final String COMMAND_NAME = "removeall";

  private static final String COMMAND = String.format("%s:%s", COMMAND_SCOPE, COMMAND_NAME);

  private static final int PAGE_SIZE_LOWER_LIMIT = 1;

  private static final long UNKNOWN_AMOUNT = -1;

  private static final String PROGRESS_FORMAT = " Currently %1$s record(s) removed out of %2$s \r";

  static final String BATCH_SIZE_ERROR_MESSAGE_FORMAT =
      "Improper batch size [%1$s]. For help with usage: removeall --help";

  private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(RemoveAllCommand.class);

  private static final int DEFAULT_BATCH_SIZE = 100;

  @Argument(
      name = "Batch size",
      description =
          "Number of Metacards to delete at a time until completion. Change this argument based on system memory and Catalog limits. "
              + "Must be a positive integer.\nNOTE: Batch size may not be honored given system constraints.")
  int batchSize = DEFAULT_BATCH_SIZE;

  @Option(
      name = "--expired",
      aliases = {"-e"},
      description =
          "Remove only expired records from the Catalog. "
              + "Expired records are based on the Metacard EXPIRATION field.")
  private boolean expired = false;

  @Option(
      name = "--force",
      aliases = {"-f"},
      description = "Force the removal without a confirmation message.")
  boolean force = false;

  @Option(name = "--cache", description = "Only remove cached entries.")
  boolean cache = false;

  @Override
  protected Object executeWithSubject() throws Exception {
    if (batchSize < PAGE_SIZE_LOWER_LIMIT) {
      printErrorMessage(String.format(BATCH_SIZE_ERROR_MESSAGE_FORMAT, batchSize));
      return null;
    }

    if (isAccidentalRemoval()) {
      return null;
    }

    if (this.cache) {
      executeRemoveAllFromCache();

    } else {
      executeRemoveAllFromStore();
    }

    return null;
  }

  private void executeRemoveAllFromCache() throws Exception {
    long start = System.currentTimeMillis();

    getCacheProxy().removeAll();

    long end = System.currentTimeMillis();

    String info = String.format("Cache cleared in %3.3f seconds", (end - start) / MS_PER_SECOND);

    LOGGER.info(info);
    LOGGER.info("Cache cleared using the \"{} --cache\" command", COMMAND);

    console.println();
    console.println(info);
  }

  private void executeRemoveAllFromStore()
      throws InterruptedException, SourceUnavailableException, FederationException,
          UnsupportedQueryException, IngestException {
    CatalogFacade catalog = getCatalog();

    QueryRequest firstQuery = getIntendedQuery(filterBuilder, true);
    QueryRequest subsequentQuery = getIntendedQuery(filterBuilder, false);

    long totalAmountDeleted = 0;
    long start = System.currentTimeMillis();

    SourceResponse response;
    try {
      response = catalog.query(firstQuery);
    } catch (UnsupportedQueryException e) {
      firstQuery = getAlternateQuery(filterBuilder, true);
      subsequentQuery = getAlternateQuery(filterBuilder, false);

      response = catalog.query(firstQuery);
    }

    if (response == null) {
      printErrorMessage("No response from Catalog.");
      return;
    }

    if (needsAlternateQueryAndResponse(response)) {
      firstQuery = getAlternateQuery(filterBuilder, true);
      subsequentQuery = getAlternateQuery(filterBuilder, false);

      response = catalog.query(firstQuery);
    }

    String totalAmount = getTotalAmount(response.getHits());

    while (response.getResults().size() > 0) {

      // Add metacard ids to string array
      List<String> ids =
          response.getResults().stream()
              .filter(Objects::nonNull)
              .map(Result::getMetacard)
              .filter(Objects::nonNull)
              .map(Metacard::getId)
              .collect(Collectors.toList());

      // Delete the records
      DeleteRequestImpl request = new DeleteRequestImpl(ids.toArray(new String[ids.size()]));

      DeleteResponse deleteResponse = catalog.delete(request);

      int amountDeleted = deleteResponse.getDeletedMetacards().size();

      totalAmountDeleted += amountDeleted;
      console.print(String.format(PROGRESS_FORMAT, totalAmountDeleted, totalAmount));
      console.flush();

      // Break out if there are no more records to delete
      // Prevents additional query to catalog framework
      if (response.getResults().isEmpty()) {
        break;
      }

      // Re-query when necessary
      response = catalog.query(subsequentQuery);
    }

    long end = System.currentTimeMillis();

    String info =
        String.format(
            "%d file(s) removed in %3.3f seconds",
            totalAmountDeleted, (end - start) / MS_PER_SECOND);

    LOGGER.info(info);
    LOGGER.info("{} file(s) removed using the \"{}\" command", totalAmountDeleted, COMMAND);

    console.println();
    console.println(info);
  }

  private boolean needsAlternateQueryAndResponse(SourceResponse response) {
    Set<ProcessingDetails> processingDetails =
        (Set<ProcessingDetails>) response.getProcessingDetails();

    if (processingDetails == null) {
      return false;
    }

    for (ProcessingDetails next : processingDetails) {
      if (next != null
          && next.getException() != null
          && next.getException().getMessage() != null
          && next.getException()
              .getMessage()
              .contains(UnsupportedQueryException.class.getSimpleName())) {
        return true;
      }
    }
    return false;
  }

  private boolean isAccidentalRemoval() {
    if (!force) {
      // use a message specific to cache and expired options
      final String warning =
          String.format(
              "WARNING: This will permanently remove all %srecords from the %s. Do you want to proceed? (yes/no): ",
              (expired ? "expired " : ""), (cache ? "cache" : "Catalog"));

      final String response;
      try {
        response = session.readLine(warning, null);
      } catch (IOException e) {
        final String useForceOptionMessage =
            String.format("Please use the \"%s --force\" command instead", COMMAND);
        console.println(useForceOptionMessage);
        return true;
      }
      final String noActionTakenMessage = "No action taken.";
      if (response.equalsIgnoreCase("yes")) {
        return false;
      } else if (response.equalsIgnoreCase("no")) {
        console.println(noActionTakenMessage);
        return true;
      } else {
        console.println("\"" + response + "\" is invalid. " + noActionTakenMessage);
        return true;
      }
    }

    return false;
  }

  private String getTotalAmount(long hits) {
    if (hits <= UNKNOWN_AMOUNT) {
      return "UNKNOWN";
    }

    return Long.toString(hits);
  }

  private QueryRequest getIntendedQuery(FilterBuilder filterBuilder, boolean isRequestForTotal) {
    Filter filter =
        addValidationAttributeToQuery(
            filterBuilder.attribute(Metacard.ID).is().like().text(WILDCARD), filterBuilder);

    if (expired) {
      filter =
          addValidationAttributeToQuery(
              filterBuilder.attribute(Metacard.EXPIRATION).before().date(new Date()),
              filterBuilder);
    }

    QueryImpl query = new QueryImpl(filter);

    query.setRequestsTotalResultsCount(isRequestForTotal);

    query.setPageSize(batchSize);

    Map<String, Serializable> properties = new HashMap<>();
    properties.put("mode", "native");

    return new QueryRequestImpl(query, properties);
  }

  private QueryRequest getAlternateQuery(FilterBuilder filterBuilder, boolean isRequestForTotal) {
    Filter filter =
        addValidationAttributeToQuery(
            filterBuilder.attribute(Metacard.ANY_TEXT).is().like().text(WILDCARD), filterBuilder);

    if (expired) {
      DateTime twoThousandYearsAgo = new DateTime().minusYears(2000);

      // less accurate than a Before filter, this is only used for those
      // Sources who cannot understand the Before filter.
      filter =
          addValidationAttributeToQuery(
              filterBuilder
                  .attribute(Metacard.EXPIRATION)
                  .during()
                  .dates(twoThousandYearsAgo.toDate(), new Date()),
              filterBuilder);
    }

    QueryImpl query = new QueryImpl(filter);

    query.setRequestsTotalResultsCount(isRequestForTotal);

    query.setPageSize(batchSize);

    Map<String, Serializable> properties = new HashMap<>();
    properties.put("mode", "native");

    return new QueryRequestImpl(query, properties);
  }

  private Filter addValidationAttributeToQuery(Filter filter, FilterBuilder filterBuilder) {
    return filterBuilder.allOf(
        filter,
        filterBuilder.anyOf(
            filterBuilder.attribute(Validation.VALIDATION_ERRORS).is().empty(),
            filterBuilder.attribute(Validation.VALIDATION_ERRORS).is().like().text(WILDCARD)),
        filterBuilder.anyOf(
            filterBuilder.attribute(Validation.VALIDATION_WARNINGS).is().empty(),
            filterBuilder.attribute(Validation.VALIDATION_WARNINGS).is().like().text(WILDCARD)));
  }
}
