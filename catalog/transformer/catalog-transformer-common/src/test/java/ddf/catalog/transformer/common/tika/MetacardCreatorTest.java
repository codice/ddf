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
import java.util.Set;
import java.util.TimeZone;

import org.apache.commons.collections.CollectionUtils;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.metadata.TikaCoreProperties;
import org.junit.Test;

import com.google.common.collect.ImmutableSet;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;

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
        Metacard metacard = testMetacard(null);
        assertThat(metacard.getMetacardType(), is(BasicTypes.BASIC_METACARD));
    }

    @Test
    public void testMetacardExtended() {
        Metacard metacard = testMetacard(ImmutableSet.of(createObjectAttr("attr1"),
                createObjectAttr("attr2")));
        assertThat(metacard.getMetacardType()
                .getName(), is(BasicTypes.BASIC_METACARD.getName()));

        ImmutableSet<String> attrNames = ImmutableSet.of("attr1", "attr2");
        int count = (int) metacard.getMetacardType()
                .getAttributeDescriptors()
                .stream()
                .map(AttributeDescriptor::getName)
                .filter(attrNames::contains)
                .count();
        assertThat(count, is(attrNames.size()));
    }

    private AttributeDescriptorImpl createObjectAttr(String name) {
        return new AttributeDescriptorImpl(name,
                false,
                false,
                false,
                false,
                BasicTypes.OBJECT_TYPE);
    }

    private Metacard testMetacard(Set<AttributeDescriptor> extraAttributes) {
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

        final Metacard metacard;
        if (CollectionUtils.isEmpty(extraAttributes)) {
            metacard = MetacardCreator.createBasicMetacard(metadata, id, metadataXml);
        } else {
            metacard = MetacardCreator.createEnhancedMetacard(metadata,
                    id,
                    metadataXml,
                    extraAttributes);
        }

        assertThat(metacard, notNullValue());
        assertThat(metacard.getTitle(), is(title));
        assertThat(metacard.getContentTypeName(), is(contentType));
        assertThat(convertDate(metacard.getCreatedDate()), is(created));
        assertThat(convertDate(metacard.getModifiedDate()), is(modified));
        assertThat(metacard.getLocation(), is(String.format("POINT(%s %s)", longitude, latitude)));
        assertThat(metacard.getId(), is(id));
        assertThat(metacard.getMetadata(), is(metadataXml));

        return metacard;
    }

    private String convertDate(final Date date) {
        final DateFormat df = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        return df.format(date);
    }
}
