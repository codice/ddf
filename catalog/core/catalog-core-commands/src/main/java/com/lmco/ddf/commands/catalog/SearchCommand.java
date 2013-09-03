/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package com.lmco.ddf.commands.catalog;

import java.io.PrintStream;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;
import org.fusesource.jansi.Ansi;
import org.joda.time.DateTime;
import org.opengis.filter.Filter;

import com.lmco.ddf.commands.catalog.facade.CatalogFacade;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.operation.QueryImpl;
import ddf.catalog.operation.QueryRequestImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.util.XPathHelper;

@Command(scope = CatalogCommands.NAMESPACE, name = "search", description = "Searches records in the catalog provider.")
public class SearchCommand extends CatalogCommands {

	private static final String ID = "ID ";

	private static final String TITLE = "Title ";

	private static final String DATE = "Modified ";

	private static final String EXCERPT = "Excerpt ";

	private static final int TITLE_MAX_LENGTH = 30;

	private static final int EXCERPT_MAX_LENGTH = 50;

	@Argument(name = "NUMBER_OF_ITEMS", description = "Number of maximum records to display.", index = 1, multiValued = false, required = false)
	int numberOfItems = -1;

	@Argument(name = "SEARCH_PHRASE", description = "Phrase to query the catalog provider.", index = 0, multiValued = false, required = true)
	String searchPhrase = null;

	@Option(name = "case-sensitive", required = false, aliases = { "-c" }, multiValued = false, description = "Makes the search case sensitive")
	boolean caseSensitive = false;

	@Override
	protected Object doExecute() throws Exception {

		PrintStream console = System.out;

		String formatString = "%1$-33s %2$-26s %3$-" + TITLE_MAX_LENGTH + "s %4$-" + EXCERPT_MAX_LENGTH + "s%n";

		CatalogFacade catalogProvider = getCatalog();

        Filter filter = null;
        if (caseSensitive) {
            filter = getFilterBuilder().attribute(Metacard.ANY_TEXT).is().like().caseSensitiveText(searchPhrase);
        } else {
            filter = getFilterBuilder().attribute(Metacard.ANY_TEXT).is().like().text(searchPhrase);
        }
        
		QueryImpl query = new QueryImpl(filter);

		query.setRequestsTotalResultsCount(true);

		if (numberOfItems > -1) {
			query.setPageSize(numberOfItems);
		}

		long start = System.currentTimeMillis();

		SourceResponse response = catalogProvider.query(new QueryRequestImpl(query));

		long end = System.currentTimeMillis();

		int size = 0;
		if (response.getResults() != null) {
			size = response.getResults().size();
		}

		console.println();
		console.printf(" %d result(s) out of %s%d%s in %3.3f seconds", (size), Ansi.ansi().fg(Ansi.Color.CYAN)
				.toString(), response.getHits(), Ansi.ansi().reset().toString(), (end - start) / MILLISECONDS_PER_SECOND);
		console.printf(formatString, "", "", "", "");
		console.print(Ansi.ansi().fg(Ansi.Color.CYAN).toString());
		console.printf(formatString, ID, DATE, TITLE, EXCERPT);
		console.print(Ansi.ansi().reset().toString());

		for (Result result : response.getResults()) {
			Metacard metacard = result.getMetacard();
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

			String title = (metacard.getTitle() != null ? metacard.getTitle() : "N/A");
			String excerpt = "N/A";
			String modifiedDate = "";
			if (index != -1) {
				int contextLength = (EXCERPT_MAX_LENGTH - normalizedSearchPhrase.length() - 8) / 2;
				excerpt = "..." + indexedText.substring(Math.max(index - contextLength, 0), index);
				excerpt = excerpt + Ansi.ansi().fg(Ansi.Color.GREEN).toString();
				excerpt = excerpt + indexedText.substring(index, index + normalizedSearchPhrase.length());
				excerpt = excerpt + Ansi.ansi().reset().toString();
				excerpt = excerpt
						+ indexedText
								.substring(
										index + normalizedSearchPhrase.length(),
										Math.min(indexedText.length(), index + normalizedSearchPhrase.length()
												+ contextLength)) + "...";

			}

			if (metacard.getModifiedDate() != null) {
				modifiedDate = new DateTime(metacard.getModifiedDate().getTime()).toString(DATETIME_FORMATTER);
			}

			console.printf(formatString, metacard.getId(), modifiedDate,
					title.substring(0, Math.min(title.length(), TITLE_MAX_LENGTH)), excerpt);
		}

		return null;
	}

}
