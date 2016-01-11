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
package ddf.catalog.transformer.common.tika;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.junit.Test;

import ddf.catalog.data.Metacard;

public class MetacardCreatorTest {
    @Test
    public void testNoMetadata() {
        final Metadata metadata = new Metadata();

        final Metacard metacard = MetacardCreator.createBasicMetacard(metadata, null, null);

        assertThat(metacard, notNullValue());
        assertThat(metacard.getTitle(), nullValue());
        assertThat(metacard.getContentTypeName(), nullValue());
        assertThat(metacard.getCreatedDate(), nullValue());
        assertThat(metacard.getModifiedDate(), nullValue());
        assertThat(metacard.getLocation(), nullValue());
        assertThat(metacard.getId(), nullValue());
        assertThat(metacard.getMetadata(), nullValue());
    }

    @Test
    public void testBasicMetacard() {
        final Metadata metadata = new Metadata();

        final String title = "title";
        final String contentType = "content type";
        final String created = "2015-12-31T01:23:45Z";
        final String modified = "2016-01-01T02:34:56Z";
        final String latitude = "12.345";
        final String longitude = "67.891";

        metadata.add(TikaCoreProperties.TITLE, title);
        metadata.add(Metadata.CONTENT_TYPE, contentType);
        metadata.add(TikaCoreProperties.CREATED, created);
        metadata.add(TikaCoreProperties.MODIFIED, modified);
        metadata.add(Metadata.LATITUDE, latitude);
        metadata.add(Metadata.LONGITUDE, longitude);

        final String id = "id";
        final String metadataXml = "<xml>test</xml>";

        final Metacard metacard = MetacardCreator.createBasicMetacard(metadata, id, metadataXml);

        assertThat(metacard, notNullValue());
        assertThat(metacard.getTitle(), is(title));
        assertThat(metacard.getContentTypeName(), is(contentType));
        assertThat(convertDate(metacard.getCreatedDate()), is(created));
        assertThat(convertDate(metacard.getModifiedDate()), is(modified));
        assertThat(metacard.getLocation(), is(String.format("POINT(%s %s)", longitude, latitude)));
        assertThat(metacard.getId(), is(id));
        assertThat(metacard.getMetadata(), is(metadataXml));
    }

    private String convertDate(final Date date) {
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(date);
    }
}
