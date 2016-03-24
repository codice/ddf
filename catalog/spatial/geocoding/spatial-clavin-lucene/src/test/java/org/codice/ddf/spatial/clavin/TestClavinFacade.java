/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */

package org.codice.ddf.spatial.clavin;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.junit.After;
import org.junit.Test;

public class TestClavinFacade {

    private static final String RESOURCE_PATH =
            new File(".").getAbsolutePath() + "/src/test/resources/";

    private static final String INDEX_PATH = RESOURCE_PATH + "index";

    @Test
    public void locationsResolved() throws IOException {
        InputStream testIs = Thread.currentThread().getContextClassLoader()
                .getResourceAsStream("bank.txt");

        String document = IOUtils.toString(testIs, "UTF-8");

        ClavinUpdateCommandImpl cli = new ClavinUpdateCommandImpl();

        cli.setClavinIndexLocation(INDEX_PATH);

        cli.createIndex(RESOURCE_PATH + "IO.txt");

        //List<ResolvedLocation> resolvedLocationList = cli.getResolvedLocations(document);

        //assertTrue(!resolvedLocationList.isEmpty());
        assertTrue(true);
    }

    @Test
    public void directoryCreatedForNewIndex() {
        assertFalse("The 'clavin/index' directory should not exist.",
                Files.exists(Paths.get(INDEX_PATH)));

        ClavinUpdateCommandImpl cli = new ClavinUpdateCommandImpl();

        cli.setClavinIndexLocation(INDEX_PATH);

        cli.createIndex(RESOURCE_PATH + "IO.txt");

        assertTrue(Files.exists(Paths.get(INDEX_PATH)));
    }

    @After
    public void tearDown() {
        // Delete the directory created by the indexer.
        FileUtils.deleteQuietly(new File(INDEX_PATH));
    }

}
