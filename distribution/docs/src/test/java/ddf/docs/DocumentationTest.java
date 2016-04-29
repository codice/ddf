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
 */
package ddf.docs;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

public class DocumentationTest {

    private static final String UNRESOLVED_DIRECTORY_MSG = "Unresolved directive";

    private static final String DOCS_DIRECTORY = "docs";

    private static final String HTML_DIRECTORY = "html";

    @Test
    public void testDocumentationIncludes() throws IOException, URISyntaxException {

        Path testPath = Paths.get(this.getClass().getResource("").toURI());
        Path targetDirectory = testPath.getParent().getParent().getParent();
        Path docPath = Paths.get(targetDirectory.toString()).resolve(DOCS_DIRECTORY).resolve(
                HTML_DIRECTORY);


        assertThat("Unresolved directive found.",
                Files.list(docPath).filter(f -> f.toString().endsWith(".html")).noneMatch(f -> {
                    try {
                        return Files.lines(f).anyMatch(s -> s.toString().contains(UNRESOLVED_DIRECTORY_MSG));
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }));
    }
}

