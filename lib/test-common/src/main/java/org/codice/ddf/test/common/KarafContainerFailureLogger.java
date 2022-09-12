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
package org.codice.ddf.test.common;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.Optional;
import java.util.stream.Stream;
import org.apache.commons.io.IOUtils;
import org.junit.runner.Result;
import org.junit.runner.notification.RunListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JUnit {@link RunListener} class that logs extra information (e.g., content of the karaf.log file)
 * to the console when a Karaf test container fails to start.
 */
class KarafContainerFailureLogger extends RunListener {
  private static final Logger LOGGER = LoggerFactory.getLogger(KarafContainerFailureLogger.class);

  private static final PrintStream OUT = System.out;

  private static final Path EXAM_DIR = Paths.get("target", "exam");
  private static final Path KARAF_LOG_FILE = Paths.get("data", "log", "karaf.log");
  private static final Path TEST_DEPENDENCIES_FILE = Paths.get("test-dependencies.xml");

  @Override
  public void testRunFinished(Result result) {
    if (result.getFailureCount() > 0) {
      LOGGER.error("Tests failed!");
      getLatestExamFolder()
          .ifPresent(
              p -> {
                printFileContent(p, KARAF_LOG_FILE);
                printFileContent(p, TEST_DEPENDENCIES_FILE);
              });
    }
  }

  private Optional<Path> getLatestExamFolder() {
    try (Stream<Path> files = Files.list(EXAM_DIR)) {
      return files
          .filter(Files::isDirectory)
          .max(Comparator.comparingLong(p -> p.toFile().lastModified()));
    } catch (IOException e) {
      LOGGER.error("No exam directory under {}", EXAM_DIR.toAbsolutePath());
      return Optional.empty();
    }
  }

  private void printFileContent(Path rootPath, Path fileRelativePath) {
    File file = rootPath.resolve(fileRelativePath).toFile();

    try (FileInputStream fileInputStream = new FileInputStream(file)) {
      LOGGER.error("===== Printing {} content =====", fileRelativePath.toString());
      IOUtils.lineIterator(fileInputStream, "UTF-8").forEachRemaining(OUT::println);
    } catch (IOException e) {
      LOGGER.error("Couldn't find {}", file.getAbsolutePath());
    }
  }
}
