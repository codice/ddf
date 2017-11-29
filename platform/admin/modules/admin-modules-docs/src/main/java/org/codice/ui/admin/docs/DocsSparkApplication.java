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
package org.codice.ui.admin.docs;

import static spark.Spark.get;
import static spark.Spark.staticFiles;

import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.NameFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import spark.servlet.SparkApplication;

public class DocsSparkApplication implements SparkApplication {

  public static final Path DOCS_ROOT_DIR =
      Paths.get(System.getProperty("karaf.home"), "documentation");

  @Override
  public void init() {

    File docHtml = getDocumentationHtml();

    if (docHtml != null) {
      staticFiles.externalLocation(docHtml.getParent());
    }

    get("/*", (req, res) -> getClass().getClassLoader().getResourceAsStream("/404.html"));
  }

  public File getDocumentationHtml() {

    if (!DOCS_ROOT_DIR.toFile().exists()) {
      return null;
    }

    Collection<File> files =
        FileUtils.listFiles(
            DOCS_ROOT_DIR.toFile(),
            new NameFileFilter("documentation.html"),
            TrueFileFilter.INSTANCE);

    if (files.isEmpty()) {
      return null;
    }

    return files.iterator().next().getAbsoluteFile();
  }
}
