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
package ddf.catalog.cache.impl;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static ddf.catalog.cache.impl.MetacardComparator.isSame;

import java.net.URI;
import java.time.Instant;
import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;

public class MetacardComparatorTest {

    MetacardImpl cachedMetacard;

    MetacardImpl updatedMetacard;

    @Before
    public void setup() throws Exception {

        Calendar calendar = Calendar.getInstance();
        Date createdDate = calendar.getTime();
        calendar.add(Calendar.HOUR, 1);
        Date effectiveDate = calendar.getTime();
        calendar.add(Calendar.DAY_OF_YEAR, 1);
        Date modDate = calendar.getTime();
        calendar.add(Calendar.YEAR, 1);
        Date expireDate = calendar.getTime();
        String metacardId = UUID.randomUUID()
                .toString();

        setupCachedMetacard(createdDate, effectiveDate, expireDate, modDate, metacardId);
        setupLatestMetacard(createdDate, effectiveDate, expireDate, modDate, metacardId);

    }

    public void setupCachedMetacard(Date createdDate, Date effectiveDate, Date expireDate,
            Date modDate, String metacardId) throws Exception {

        String locWkt = "POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))";
        URI nsUri = new URI("http://" + MetacardComparatorTest.class.getName());
        URI resourceUri = new URI(nsUri.toString() + "/resource1.html");
        cachedMetacard = new MetacardImpl(BasicTypes.BASIC_METACARD);
        cachedMetacard.setContentTypeName("testContentType");
        cachedMetacard.setContentTypeVersion("testContentTypeVersion");
        cachedMetacard.setAttribute(Metacard.CHECKSUM, "1");
        cachedMetacard.setCreatedDate(createdDate);
        cachedMetacard.setEffectiveDate(effectiveDate);
        cachedMetacard.setExpirationDate(expireDate);
        cachedMetacard.setModifiedDate(modDate);
        cachedMetacard.setId(metacardId);
        cachedMetacard.setLocation(locWkt);
        cachedMetacard.setMetadata("testMetadata");
        cachedMetacard.setResourceURI(resourceUri);
        cachedMetacard.setSourceId("testSourceId");
        cachedMetacard.setTargetNamespace(nsUri);
        cachedMetacard.setTitle("testTitle");
        cachedMetacard.setThumbnail(cachedMetacard.getId()
                .getBytes());
        cachedMetacard.setDescription("testDescription");
        cachedMetacard.setPointOfContact("pointOfContact");
        cachedMetacard.setResourceSize("1");
        cachedMetacard.setAttribute(Metacard.CHECKSUM, "1");

    }

    public void setupLatestMetacard(Date createdDate, Date effectiveDate, Date expireDate,
            Date modDate, String metacardId) throws Exception {

        String locWkt = "POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))";
        URI nsUri = new URI("http://" + MetacardComparatorTest.class.getName());
        URI resourceUri = new URI(nsUri.toString() + "/resource1.html");
        updatedMetacard = new MetacardImpl(BasicTypes.BASIC_METACARD);
        updatedMetacard.setContentTypeName("testContentType");
        updatedMetacard.setContentTypeVersion("testContentTypeVersion");
        updatedMetacard.setAttribute(Metacard.CHECKSUM, "1");
        updatedMetacard.setCreatedDate(createdDate);
        updatedMetacard.setEffectiveDate(effectiveDate);
        updatedMetacard.setExpirationDate(expireDate);
        updatedMetacard.setModifiedDate(modDate);
        updatedMetacard.setId(metacardId);
        updatedMetacard.setLocation(locWkt);
        updatedMetacard.setMetadata("testMetadata");
        updatedMetacard.setResourceURI(resourceUri);
        updatedMetacard.setSourceId("testSourceId");
        updatedMetacard.setTargetNamespace(nsUri);
        updatedMetacard.setTitle("testTitle");
        updatedMetacard.setThumbnail(cachedMetacard.getThumbnail());
        updatedMetacard.setDescription("testDescription");
        updatedMetacard.setPointOfContact("pointOfContact");
        updatedMetacard.setResourceSize("1");
        updatedMetacard.setAttribute(Metacard.CHECKSUM, "1");

    }

    @Test
    public void isSameMetacard() throws Exception {

        assertThat(isSame(cachedMetacard, updatedMetacard), is(true));
    }

    @Test
    public void isNotSameEffectiveDate() throws Exception {

        updatedMetacard.setExpirationDate(new Date().from(Instant.now()
                .plusSeconds(2)));
        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));

    }

    @Test
    public void isNotSameExpireDate() throws Exception {

        updatedMetacard.setExpirationDate(new Date().from(Instant.now()
                .plusSeconds(2)));

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));

    }

    @Test
    public void isNotSameCreatedDate() throws Exception {

        updatedMetacard.setCreatedDate(new Date().from(Instant.now()
                .plusSeconds(2)));

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));

    }

    @Test
    public void isNotSameResourceSize() {
        updatedMetacard.setResourceSize("2");

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));

    }

    @Test
    public void isNotSameContentType() throws Exception {
        updatedMetacard.setContentTypeName("testContentType2");
        cachedMetacard.setContentTypeName("phil");

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));

    }

    @Test
    public void isNotSameResourceURI() throws Exception {

        URI nsUri = new URI("http://" + MetacardComparatorTest.class.getName());
        URI resourceUri = new URI(nsUri.toString() + "/resource2.html");
        updatedMetacard.setResourceURI(resourceUri);

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));

    }

    @Test
    public void isNotSameModDate() throws Exception {

        updatedMetacard.setModifiedDate(new Date().from(Instant.now()
                .plusSeconds(2)));

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
    }

    @Test
    public void isNotSameChecksum() throws Exception {

        updatedMetacard.setAttribute(Metacard.CHECKSUM, "2");
        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
    }

    @Test
    public void isNotSameLocation() throws Exception {

        String locWkt = "POLYGON ((29 10, 10 20, 20 40, 40 40, 30 10))";
        updatedMetacard.setLocation(locWkt);

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));

    }

    @Test
    public void isNotSame() throws Exception {

        String locWkt = "POLYGON ((29 10, 10 20, 20 40, 40 40, 30 10))";
        updatedMetacard.setLocation(locWkt);
        updatedMetacard.setAttribute(Metacard.CHECKSUM, "2");

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));

    }

    @Test
    public void isNotSameAttribute() throws Exception {
        updatedMetacard.setAttribute(Metacard.CONTENT_TYPE_VERSION, "3");
        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));

    }

}
