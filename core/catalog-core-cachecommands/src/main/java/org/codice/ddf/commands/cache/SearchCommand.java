/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.commands.cache;

import java.util.List;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import ddf.catalog.data.Metacard;

@Command(scope = CacheCommands.NAMESPACE, name = "search", description = "Searches metacards in the cache.")
public class SearchCommand extends CacheCommands {

    private static final String ID = "ID ";

    private static final String TITLE = "Title ";

    private static final String DATE = "Modified ";

    private static final int TITLE_MAX_LENGTH = 50;

    @Argument(name = "SEARCH_PHRASE", index = 0, multiValued = false, required = false,
            description = "Phrase to query the cache. \nExample:\n \"*:*\" will return all records in the cache. \n \"title:abc\" will return records with title = abc")

    String searchPhrase = null;

    @Override
    protected Object doExecute() throws Exception {

        String formatString = "%1$-33s %2$-26s %3$-" + TITLE_MAX_LENGTH + "s %n";

        if (searchPhrase == null) {
            searchPhrase = "*:*";
        }

        long start = System.currentTimeMillis();

        List<Metacard> results = getCacheProxy().query(searchPhrase);

        long end = System.currentTimeMillis();

        console.printf(formatString, "", "", "");
        printHeaderMessage(String.format(formatString, ID, DATE, TITLE));

        for (Metacard metacard : results) {
            String title = (metacard.getTitle() != null ? metacard.getTitle() : "N/A");
            String excerpt = "N/A";
            String modifiedDate = "";

            if (metacard.getModifiedDate() != null) {
                DateTime dt = new DateTime(DateTimeZone.UTC);
                modifiedDate = dt.toString(DATETIME_FORMATTER);
            }

            console.printf(formatString, metacard.getId(), modifiedDate,
                    title.substring(0, Math.min(title.length(), TITLE_MAX_LENGTH)));

        }

        return null;
    }
}
