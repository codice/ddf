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
package org.codice.ddf.commands.catalog;

import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.commands.catalog.facade.Framework;
import org.junit.Test;

import ddf.catalog.CatalogFramework;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;

public class TestLatestCommand extends AbstractCommandTest {

    /**
     * When no title is provided, output should still be displayed.
     * 
     * @throws Exception
     */
    @Test
    public void testNoTitle() throws Exception {

        ConsoleOutput consoleOutput = new ConsoleOutput();

        consoleOutput.interceptSystemOut();

        try {
            // given
            final CatalogFramework catalogFramework = givenCatalogFramework(getResultList("id1",
                    "id2"));

            LatestCommand latestCommand = new LatestCommand() {
                @Override
                protected CatalogFacade getCatalog() throws InterruptedException {
                    return new Framework(catalogFramework);
                }

                @Override
                protected FilterBuilder getFilterBuilder() throws InterruptedException {
                    return new GeotoolsFilterBuilder();
                }
            };

            // when
            latestCommand.doExecute();

            // then

            assertThat(consoleOutput.getOutput(), containsString("id1"));
            assertThat(consoleOutput.getOutput(), containsString("id2"));

        } finally {
            /* cleanup */
            consoleOutput.resetSystemOut();

            consoleOutput.closeBuffer();
        }

    }



}
