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

import static ddf.catalog.util.impl.ResultIterable.resultIterable;

import com.google.common.collect.Lists;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.util.XPathHelper;
import java.io.IOException;
import java.util.List;
import javax.management.InstanceNotFoundException;
import javax.management.MalformedObjectNameException;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.commands.util.CatalogCommandException;
import org.fusesource.jansi.Ansi;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
@Command(
    scope = CatalogCommands.NAMESPACE,
    name = "search",
    description = "Searches records in the Catalog Provider.")
public class SearchCommand extends CqlCommands {

  private static final Logger LOGGER = LoggerFactory.getLogger(SearchCommand.class);

  private static final String ID = "ID ";

  private static final String TITLE = "Title ";

  private static final String DATE = "Modified ";

  private static final String EXCERPT = "Excerpt ";

  private static final int TITLE_MAX_LENGTH = 30;

  private static final int EXCERPT_MAX_LENGTH = 50;

  @Argument(
      name = "NUMBER_OF_ITEMS",
      description = "Number of maximum records to display.",
      index = 1,
      multiValued = false,
      required = false)
  int numberOfItems = -1;

  @Argument(
      name = "SEARCH_PHRASE",
      index = 0,
      multiValued = false,
      required = false,
      description =
          "Phrase to query the Catalog Provider. Will take precedence over --searchPhrase option.")
  String searchPhraseArgument = WILDCARD;

  @Option(
      name = "--cache",
      required = false,
      multiValued = false,
      aliases = {},
      description = "Only search cached entries.")
  boolean cache = false;

  @Override
  protected Object executeWithSubject() throws Exception {
    searchPhrase = searchPhraseArgument;

    final Filter filter = getFilter();

    if (this.cache) {
      return executeSearchCache(filter);
    } else {
      return executeSearchStore(filter);
    }
  }

  private Object executeSearchStore(Filter filter) {
    String formatString =
        "%1$-33s %2$-26s %3$-" + TITLE_MAX_LENGTH + "s %4$-" + EXCERPT_MAX_LENGTH + "s%n";

    CatalogFacade catalogProvider = getCatalog();

    QueryImpl query = new QueryImpl(filter);
    if (numberOfItems > -1) {
      query.setPageSize(numberOfItems);
    }

    long start = System.currentTimeMillis();

    List<Result> results =
        Lists.newArrayList(
            resultIterable(
                catalogProvider::query,
                new QueryRequestImpl(query),
                numberOfItems > 0 ? numberOfItems : 1000));

    long end = System.currentTimeMillis();

    final long hits = getHits(filter, catalogProvider);

    console.println();
    console.printf(
        " %d result(s) out of %s%s%s in %3.3f seconds",
        results.size(),
        Ansi.ansi().fg(Ansi.Color.CYAN).toString(),
        hits == -1 ? "?" : hits,
        Ansi.ansi().reset().toString(),
        (end - start) / MS_PER_SECOND);
    console.printf(formatString, "", "", "", "");
    printHeaderMessage(String.format(formatString, ID, DATE, TITLE, EXCERPT));

    for (Result result : results) {
      Metacard metacard = result.getMetacard();

      String title = (metacard.getTitle() != null ? metacard.getTitle() : "N/A");
      String excerpt = "N/A";
      String modifiedDate = "";

      if (searchPhrase != null && metacard.getMetadata() != null) {
        XPathHelper helper = new XPathHelper(metacard.getMetadata());
        String indexedText = helper.getDocument().getDocumentElement().getTextContent();
        indexedText = indexedText.replaceAll("\\r\\n|\\r|\\n", " ");

        String normalizedSearchPhrase = searchPhrase.replaceAll("\\*", "");

        int index = -1;

        if (caseSensitive) {
          index = indexedText.indexOf(normalizedSearchPhrase);
        } else {
          index = indexedText.toLowerCase().indexOf(normalizedSearchPhrase.toLowerCase());
        }

        if (index != -1) {
          int contextLength = (EXCERPT_MAX_LENGTH - normalizedSearchPhrase.length() - 8) / 2;
          excerpt = "..." + indexedText.substring(Math.max(index - contextLength, 0), index);
          excerpt = excerpt + Ansi.ansi().fg(Ansi.Color.GREEN).toString();
          excerpt = excerpt + indexedText.substring(index, index + normalizedSearchPhrase.length());
          excerpt = excerpt + Ansi.ansi().reset().toString();
          excerpt =
              excerpt
                  + indexedText.substring(
                      index + normalizedSearchPhrase.length(),
                      Math.min(
                          indexedText.length(),
                          index + normalizedSearchPhrase.length() + contextLength))
                  + "...";
        }
      }

      if (metacard.getModifiedDate() != null) {
        modifiedDate =
            new DateTime(metacard.getModifiedDate().getTime()).toString(DATETIME_FORMATTER);
      }

      console.printf(
          formatString,
          metacard.getId(),
          modifiedDate,
          title.substring(0, Math.min(title.length(), TITLE_MAX_LENGTH)),
          excerpt);
    }

    return null;
  }

  /** Returns the total hits matching {@param filter} or -1 if unknown */
  private static long getHits(final Filter filter, final CatalogFacade catalogProvider) {
    try {
      final QueryImpl hitsQuery = new QueryImpl(filter);
      hitsQuery.setRequestsTotalResultsCount(true);
      hitsQuery.setPageSize(1);
      return catalogProvider.query(new QueryRequestImpl(hitsQuery)).getHits();
    } catch (UnsupportedQueryException | SourceUnavailableException | FederationException e) {
      LOGGER.debug("Unable to get hits for catalog:search command", e);
      return -1;
    }
  }

  private Object executeSearchCache(Filter filter) throws CatalogCommandException {
    String formatString = "%1$-33s %2$-26s %3$-" + TITLE_MAX_LENGTH + "s %n";

    long start = System.currentTimeMillis();

    try {
      List<Metacard> results = getCacheProxy().query(filter);

      long end = System.currentTimeMillis();

      console.println();
      console.printf(
          " %d result(s) in %3.3f seconds", (results.size()), (end - start) / MS_PER_SECOND);
      console.printf(formatString, "", "", "");
      printHeaderMessage(String.format(formatString, ID, DATE, TITLE));

      for (Metacard metacard : results) {
        String title = (metacard.getTitle() != null ? metacard.getTitle() : "N/A");
        String modifiedDate = "";

        if (metacard.getModifiedDate() != null) {
          DateTime dt = new DateTime(DateTimeZone.UTC);
          modifiedDate = dt.toString(DATETIME_FORMATTER);
        }

        console.printf(
            formatString,
            metacard.getId(),
            modifiedDate,
            title.substring(0, Math.min(title.length(), TITLE_MAX_LENGTH)));
      }
    } catch (UnsupportedQueryException
        | IOException
        | MalformedObjectNameException
        | InstanceNotFoundException e) {
      throw new CatalogCommandException("Error executing catalog:search", e);
    }
    return null;
  }
}
