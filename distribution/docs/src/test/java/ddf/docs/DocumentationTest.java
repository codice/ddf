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
package ddf.docs;

import static org.hamcrest.MatcherAssert.assertThat;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

public class DocumentationTest {

  private static final String UNRESOLVED_DIRECTORY_MSG = "Unresolved directive";

  private static final String FREEMARKER_MSG = "freemarker";

  private static final String DOCS_DIRECTORY = "docs";

  private static final String HTML_DIRECTORY = "html";

  private static final String BASE64_MISSING = "base64,\"";

  private static final String HREF_ANCHOR = "href=\"#";

  private static final String CLOSE = "\"";

  private static final String ID = " id=\"";

  private static final String EMPTY_STRING = "";

  @Test
  public void testDocumentationIncludes() throws IOException, URISyntaxException {
    Stream<Path> docs = Files.list(getPath()).filter(f -> f.toString().endsWith(HTML_DIRECTORY));

    assertThat(
        "Unresolved directive, FreeMarker reference or broken image found.",
        docs.noneMatch(
            f -> {
              try (Stream<String> lines = Files.lines(f)) {
                return lines.anyMatch(
                    s ->
                        s.contains(UNRESOLVED_DIRECTORY_MSG)
                            || s.toLowerCase().contains(FREEMARKER_MSG)
                            || s.contains(BASE64_MISSING));
              } catch (IOException e) {
                throw new RuntimeException(e);
              }
            }));
  }

  @Test
  public void testBrokenAnchorsPresent() throws IOException, URISyntaxException {
    List<Path> docs =
        Files.list(getPath())
            .filter(f -> f.toString().endsWith(HTML_DIRECTORY))
            .collect(Collectors.toList());

    Set<String> links = new HashSet<>();
    Set<String> anchors = new HashSet<>();

    for (Path path : docs) {
      Document doc = Jsoup.parse(path.toFile(), "UTF-8", EMPTY_STRING);

      String thisDoc = StringUtils.substringAfterLast(path.toString(), File.separator);
      Elements elements = doc.body().getAllElements();
      for (Element element : elements) {
        if (!element.toString().contains(":")
            && StringUtils.substringBetween(element.toString(), HREF_ANCHOR, CLOSE) != null) {
          links.add(
              thisDoc + "#" + StringUtils.substringBetween(element.toString(), HREF_ANCHOR, CLOSE));
        }

        anchors.add(thisDoc + "#" + StringUtils.substringBetween(element.toString(), ID, CLOSE));
      }
    }
    links.removeAll(anchors);
    assertThat("Anchors missing section reference: " + links.toString(), links.isEmpty());
  }

  private Path getPath() throws URISyntaxException {
    Path testPath = Paths.get(this.getClass().getResource(EMPTY_STRING).toURI());
    Path targetDirectory = testPath.getParent().getParent().getParent();
    return Paths.get(targetDirectory.toString()).resolve(DOCS_DIRECTORY).resolve(HTML_DIRECTORY);
  }
}
