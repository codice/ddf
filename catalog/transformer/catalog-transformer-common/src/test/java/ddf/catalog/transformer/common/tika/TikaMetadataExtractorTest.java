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
package ddf.catalog.transformer.common.tika;

import static junit.framework.Assert.assertNotNull;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.InputStream;
import org.apache.tika.exception.TikaException;
import org.junit.Before;
import org.junit.Test;

public class TikaMetadataExtractorTest {
  private TikaMetadataExtractor tikaMetadataExtractor;

  private static final String BODY = "this is a test\n";

  private InputStream stream = null;

  @Before
  public void setup() {
    stream = Thread.currentThread().getContextClassLoader().getResourceAsStream("test.txt");
  }

  @Test(expected = IllegalArgumentException.class)
  public void testNullInputStream() throws Exception {
    InputStream stream = null;
    tikaMetadataExtractor = new TikaMetadataExtractor(stream, 1000, 1000);
  }

  @Test
  public void testNormalParse() throws Exception {
    tikaMetadataExtractor = new TikaMetadataExtractor(stream);

    assertThat(tikaMetadataExtractor.getBodyText(), equalTo(BODY));
    assertNotNull(tikaMetadataExtractor.getMetadata());
    assertNotNull(tikaMetadataExtractor.getMetadataXml());
  }

  @Test
  public void testBodyParseLimitExceeded() throws Exception {
    tikaMetadataExtractor = new TikaMetadataExtractor(stream, 1, 1000);

    assertThat(tikaMetadataExtractor.getBodyText(), equalTo("t"));
    assertNotNull(tikaMetadataExtractor.getMetadata());
    assertNotNull(tikaMetadataExtractor.getMetadataXml());
  }

  @Test(expected = TikaException.class)
  public void testClosedStream() throws Exception {
    stream.close();
    tikaMetadataExtractor = new TikaMetadataExtractor(stream, 1, 1000);
  }

  @Test
  public void testMetadataParseLimitExceeded() throws Exception {
    tikaMetadataExtractor = new TikaMetadataExtractor(stream, 1000, 1);

    assertThat(tikaMetadataExtractor.getBodyText(), equalTo(BODY));
    assertNotNull(tikaMetadataExtractor.getMetadata());
    assertNotNull(tikaMetadataExtractor.getMetadataXml());
    assertThat(
        tikaMetadataExtractor.getMetadataXml(),
        equalTo(TikaMetadataExtractor.METADATA_LIMIT_REACHED_MSG));
  }

  @Test
  public void testBothBodyAndMetadataParseLimitExceeded() throws Exception {
    tikaMetadataExtractor = new TikaMetadataExtractor(stream, 1, 1);

    assertThat(tikaMetadataExtractor.getBodyText(), equalTo("t"));
    assertNotNull(tikaMetadataExtractor.getMetadata());
    assertNotNull(tikaMetadataExtractor.getMetadataXml());
    assertThat(
        tikaMetadataExtractor.getMetadataXml(),
        equalTo(TikaMetadataExtractor.METADATA_LIMIT_REACHED_MSG));
  }
}
