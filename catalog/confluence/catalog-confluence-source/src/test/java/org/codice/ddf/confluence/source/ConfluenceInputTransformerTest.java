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
package org.codice.ddf.confluence.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.data.AttributeInjector;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.types.Associations;
import ddf.catalog.data.types.Contact;
import ddf.catalog.data.types.Security;
import ddf.catalog.transform.CatalogTransformerException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

public class ConfluenceInputTransformerTest {

  private ConfluenceInputTransformer transformer;

  @Before
  public void setup() {
    AttributeInjector injector = mock(AttributeInjector.class);
    when(injector.injectAttributes(any(MetacardType.class)))
        .thenReturn(
            new MetacardTypeImpl(
                "confluence",
                MetacardImpl.BASIC_METACARD,
                Collections.singleton(
                    new AttributeDescriptorImpl(
                        "injected.attribute", true, true, true, false, BasicTypes.STRING_TYPE))));
    transformer =
        new ConfluenceInputTransformer(
            new MetacardTypeImpl(
                "confluence", MetacardImpl.BASIC_METACARD.getAttributeDescriptors()),
            Collections.singletonList(injector));
  }

  @Test
  public void testPageTransform() throws Exception {
    String fileContent = getFileContent("confluence_page.json");
    InputStream stream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
    validatePageMetacard(transformer.transform(stream), null, null, true);
  }

  @Test
  public void testTransformWithId() throws Exception {
    String fileContent = getFileContent("confluence_page.json");
    InputStream stream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
    validatePageMetacard(transformer.transform(stream, "myId"), "myId", null, true);
  }

  @Test
  public void testPageTransformWithoutRestrictionInfo() throws Exception {
    String fileContent = getFileContent("confluence_page_no_restrictions.json");
    InputStream stream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
    validatePageMetacard(
        transformer.transform(stream), null, "https://codice.atlassian.net/wiki", false);
  }

  @Test
  public void testFullResponseTransform() throws Exception {
    String fileContent = getFileContent("full_response.json");
    InputStream stream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
    validatePageMetacard(
        transformer.transformConfluenceResponse(stream).get(0),
        null,
        "https://codice.atlassian.net/wiki",
        true);
  }

  @Test
  public void testFullResponseTransformWithAlternateBodyPath() throws Exception {
    String fileContent = getFileContent("confluence_page_storage_body.json");
    InputStream stream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
    validatePageMetacard(
        transformer.transformConfluenceResponse(stream, "body.storage.value").get(0),
        null,
        "https://codice.atlassian.net/wiki",
        false);
  }

  @Test
  public void testAttachmentTransform() throws Exception {
    String fileContent = getFileContent("confluence_attachment.json");
    InputStream stream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
    Metacard mcard = transformer.transform(stream);
    assertThat(mcard.getId(), equalTo("att1182017"));
    assertThat(mcard.getTitle(), equalTo("ddf-eclipse-code-formatter.xml"));
    assertThat(
        mcard.getAttribute(Associations.EXTERNAL).getValues(),
        contains(
            "https://codice.atlassian.net/wiki/display/DDF/Formatting+Source+Code?preview=%2F1179681%2F1182017%2Fddf-eclipse-code-formatter.xml"));
    assertThat(mcard.getCreatedDate(), is(getDate("2013-09-18T14:44:04.892-07:00")));
    assertThat(mcard.getModifiedDate(), is(getDate("2013-09-18T14:50:42.655-07:00")));
    assertThat(mcard.getAttribute(Contact.CREATOR_NAME).getValue().toString(), equalTo("user"));
    assertThat(mcard.getAttribute(Contact.CONTRIBUTOR_NAME).getValues().contains("user"), is(true));
    assertThat(
        mcard.getAttribute(Metacard.RESOURCE_URI).getValue().toString(),
        equalTo(
            "https://codice.atlassian.net/wiki/download/attachments/1179681/ddf-eclipse-code-formatter.xml?version=1&modificationDate=1379541042655&api=v2"));
    assertThat(mcard.getResourceSize(), equalTo("30895"));
  }

  @Test(expected = CatalogTransformerException.class)
  public void testBadResponseData() throws Exception {
    InputStream stream =
        new ByteArrayInputStream("<some><xml></xml></some>".getBytes(StandardCharsets.UTF_8));
    transformer.transform(stream);
  }

  @Test(expected = CatalogTransformerException.class)
  public void testMissingRequiredData() throws Exception {
    InputStream stream = new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8));
    transformer.transform(stream);
  }

  @Test
  public void testProcessResponseMissingRequiredData() throws Exception {
    String jsonResponse =
        "{ \"results\": [ {} ], \"_links\": {"
            + "    \"base\": \"https://codice.atlassian.net/wiki\"}}";
    InputStream stream = new ByteArrayInputStream(jsonResponse.getBytes(StandardCharsets.UTF_8));
    List<Metacard> metacards = transformer.transformConfluenceResponse(stream);
    assertThat(metacards.size(), is(0));
  }

  @Test
  public void testTransformingAnonymousContent() throws Exception {
    String fileContent = getFileContent("anonymous_user_response.json");
    InputStream stream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
    Metacard mcard = transformer.transformConfluenceResponse(stream).get(0);
    assertThat(
        "Creator should be Unknown",
        mcard.getAttribute(Contact.CREATOR_NAME).getValue(),
        equalTo("Unknown"));
  }

  private String getFileContent(String filePath) {
    try {
      return IOUtils.toString(
          this.getClass().getClassLoader().getResourceAsStream(filePath), "UTF-8");
    } catch (IOException e) {
      throw new RuntimeException("Failed to read filepath: " + filePath);
    }
  }

  private void validatePageMetacard(
      Metacard mcard, String expectedId, String baseUrl, boolean checkRestrictions) {
    String id = "1179681";
    if (expectedId != null) {
      id = expectedId;
    }

    assertThat(mcard.getId(), equalTo(id));
    assertThat(mcard.getTitle(), equalTo("Formatting Source Code"));
    assertThat(
        mcard.getAttribute(Associations.EXTERNAL).getValues(),
        contains("https://codice.atlassian.net/wiki/display/DDF/Formatting+Source+Code"));
    assertThat(mcard.getCreatedDate(), is(getDate("2013-09-18T14:50:42.616-07:00")));
    assertThat(mcard.getModifiedDate(), is(getDate("2015-06-16T19:21:39.141-07:00")));
    assertThat(mcard.getAttribute(Contact.CREATOR_NAME).getValue().toString(), equalTo("another"));
    assertThat(
        mcard.getAttribute(Contact.CONTRIBUTOR_NAME).getValues().contains("first.last"), is(true));

    if (checkRestrictions) {
      assertThat(
          mcard.getAttribute(Security.ACCESS_INDIVIDUALS).getValues().contains("first.last"),
          is(true));
      assertThat(
          mcard.getAttribute(Security.ACCESS_GROUPS).getValues().contains("ddf-developers"),
          is(true));
    }
    assertThat(
        mcard.getMetacardType().getAttributeDescriptor("injected.attribute"), notNullValue());
  }

  private Date getDate(String dateTime) {
    return Date.from(Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(dateTime)));
  }
}
