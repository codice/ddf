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
package org.codice.ddf.confluence.source;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.codice.ddf.confluence.common.Confluence;
import org.codice.ddf.confluence.common.ConfluenceMetacardType;
import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Contact;
import ddf.catalog.transform.CatalogTransformerException;

public class ConfluenceInputTransformerTest {

    private static final ObjectMapper MAPPER = JsonFactory.create();

    private ConfluenceInputTransformer transformer;

    @Before
    public void setup() {
        transformer = new ConfluenceInputTransformer(new ConfluenceMetacardType());
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
        validatePageMetacard(transformer.transform(stream),
                null,
                "https://myconfluence/wiki",
                false);
    }

    @Test
    public void testFullResponseTransform() throws Exception {
        String fileContent = getFileContent("full_response.json");
        InputStream stream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
        validatePageMetacard(transformer.transformConfluenceResponse(stream)
                .get(0), null, "https://codice.atlassian.net/wiki", true);
    }

    @Test
    public void testAttachmentTransform() throws Exception {
        String fileContent = getFileContent("confluence_attachment.json");
        InputStream stream = new ByteArrayInputStream(fileContent.getBytes(StandardCharsets.UTF_8));
        Metacard mcard = transformer.transform(stream);
        assertThat(mcard.getId(), equalTo("att1182017"));
        assertThat(mcard.getTitle(), equalTo("ddf-eclipse-code-formatter.xml"));
        assertThat(mcard.getAttribute(Confluence.SPACE)
                .getValue()
                .toString(), equalTo("DDF"));
        assertThat(mcard.getCreatedDate(), is(getDate("2013-09-18T14:44:04.892-07:00")));
        assertThat(mcard.getModifiedDate(), is(getDate("2013-09-18T14:50:42.655-07:00")));
        assertThat(mcard.getAttribute(Contact.CREATOR_NAME)
                .getValue()
                .toString(), equalTo("user"));
        assertThat(mcard.getAttribute(Contact.CONTRIBUTOR_NAME)
                .getValues()
                .contains("user"), is(true));
        assertThat(mcard.getAttribute(Confluence.LINK)
                        .getValue()
                        .toString(),
                equalTo("/display/DDF/Formatting+Source+Code?preview=%2F1179681%2F1182017%2Fddf-eclipse-code-formatter.xml"));
        assertThat(mcard.getResourceURI()
                        .toString(),
                equalTo("/download/attachments/1179681/ddf-eclipse-code-formatter.xml?version=1&modificationDate=1379541042655&api=v2"));
        assertThat(mcard.getResourceSize(), equalTo("30895"));

    }

    @Test(expected = CatalogTransformerException.class)
    public void testBadResponseData() throws Exception {
        InputStream stream = new ByteArrayInputStream("<some><xml></xml></some>".getBytes(
                StandardCharsets.UTF_8));
        transformer.transform(stream);
    }

    @Test(expected = CatalogTransformerException.class)
    public void testMissingRequiredData() throws Exception {
        InputStream stream = new ByteArrayInputStream("{}".getBytes(StandardCharsets.UTF_8));
        transformer.transform(stream);
    }

    @Test
    public void testProcessResponseMissingRequiredData() throws Exception {
        String jsonResponse = "{ \"results\": [ {} ], \"_links\": {"
                + "    \"base\": \"https://codice.atlassian.net/wiki\"}}";
        InputStream stream =
                new ByteArrayInputStream(jsonResponse.getBytes(StandardCharsets.UTF_8));
        List<Metacard> metacards = transformer.transformConfluenceResponse(stream);
        assertThat(metacards.size(), is(0));
    }

    private String getFileContent(String filePath) {
        try {
            return IOUtils.toString(this.getClass()
                    .getClassLoader()
                    .getResourceAsStream(filePath), "UTF-8");
        } catch (IOException e) {
            throw new RuntimeException("Failed to read filepath: " + filePath);
        }
    }

    private void validatePageMetacard(Metacard mcard, String expectedId, String baseUrl,
            boolean checkRestrictions) {
        String id = "1179681";
        if (expectedId != null) {
            id = expectedId;
        }
        String url = "";
        if (baseUrl != null) {
            url = baseUrl;
        }
        assertThat(mcard.getId(), equalTo(id));
        assertThat(mcard.getTitle(), equalTo("Formatting Source Code"));
        assertThat(mcard.getAttribute(Confluence.SPACE)
                .getValue()
                .toString(), equalTo("DDF"));
        assertThat(mcard.getCreatedDate(), is(getDate("2013-09-18T14:50:42.616-07:00")));
        assertThat(mcard.getModifiedDate(), is(getDate("2015-06-16T19:21:39.141-07:00")));
        assertThat(mcard.getAttribute(Contact.CREATOR_NAME)
                .getValue()
                .toString(), equalTo("another"));
        assertThat(mcard.getAttribute(Contact.CONTRIBUTOR_NAME)
                .getValues()
                .contains("first.last"), is(true));
        assertThat(mcard.getAttribute(Confluence.LINK)
                .getValue()
                .toString(), equalTo(url + "/display/DDF/Formatting+Source+Code"));

        if (checkRestrictions) {
            assertThat(mcard.getAttribute(Confluence.RESTRICTION_USERS)
                    .getValues()
                    .contains("first.last"), is(true));
            assertThat(mcard.getAttribute(Confluence.RESTRICTION_GROUPS)
                    .getValues()
                    .contains("ddf-developers"), is(true));
        }
    }

    private Date getDate(String dateTime) {
        return Date.from(Instant.from(DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(dateTime)));
    }
}
