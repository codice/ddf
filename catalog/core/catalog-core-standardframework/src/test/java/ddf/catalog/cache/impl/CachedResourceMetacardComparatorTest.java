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
import static ddf.catalog.cache.impl.CachedResourceMetacardComparator.isSame;

import java.net.URI;
import java.net.URL;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.EmptyMetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;

public class CachedResourceMetacardComparatorTest {

    private MetacardImpl cachedMetacard;

    private MetacardImpl updatedMetacard;

    @Before
    public void setup() throws Exception {
        Instant createdDate = Instant.now();
        Instant effectiveDate = createdDate.plus(1, ChronoUnit.HOURS);
        Instant modificationDate = createdDate.plus(2, ChronoUnit.DAYS);
        Instant expirationDate = createdDate.plus(60, ChronoUnit.DAYS);

        String metacardId = UUID.randomUUID()
                .toString();

        cachedMetacard = createMetacard(metacardId,
                createdDate,
                effectiveDate,
                expirationDate,
                modificationDate);
        updatedMetacard = createMetacard(metacardId,
                createdDate,
                effectiveDate,
                expirationDate,
                modificationDate);
    }

    @Test
    public void isSameMetacard() throws Exception {
        assertThat(isSame(cachedMetacard, updatedMetacard), is(true));
    }

    @Test
    public void isSameWhenBothMetacardTypesNull() {
        cachedMetacard.setType(null);
        updatedMetacard.setType(null);

        assertThat(isSame(cachedMetacard, updatedMetacard), is(true));
    }

    /**
     * See the {@link CachedResourceMetacardComparator} class for more information on why
     * {@link CachedResourceMetacardComparator#isSame(Metacard, Metacard)} will return {@code true}
     * even when the resource size attributes are different.
     */
    @Test
    public void isSameEvenWhenResourceSizeAreDifferent() {
        updatedMetacard.setResourceSize("2");

        assertThat(isSame(cachedMetacard, updatedMetacard), is(true));
    }

    @Test
    public void isNotSameWhenMetacardIdsDifferent() {
        cachedMetacard.setId("bad");

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
    }

    @Test
    public void isNotSameWhenCachedMetacardIsNull() {
        assertThat(isSame(null, updatedMetacard), is(false));
    }

    @Test
    public void isNotSameWhenUpdatedMetacardIsNull() {
        assertThat(isSame(cachedMetacard, null), is(false));
    }

    @Test
    public void isSameWhenBothMetacardsAreNull() {
        assertThat(isSame(null, null), is(true));
    }

    @Test
    public void isNotSameWhenACachedMetacardMethodReturnsNull() {
        cachedMetacard.setModifiedDate(null);

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
    }

    @Test
    public void isNotSameWhenAnUpdatedMetacardMethodReturnsNull() {
        updatedMetacard.setModifiedDate(null);

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
    }

    @Test
    public void isNotSameWhenCachedMetacardHasNullAttribute() {
        cachedMetacard.setDescription(null);

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
    }

    @Test
    public void isNotSameWhenUpdatedMetacardHasNullAttribute() {
        updatedMetacard.setDescription(null);

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
    }

    @Test
    public void isNotSameWhenMetacardsHaveDifferentTypes() {
        cachedMetacard.setType(new EmptyMetacardType());

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
    }

    @Test
    public void isSameWhenMetacardsHaveNullAttributeDescriptorLists() {
        cachedMetacard.setType(new EmptyMetacardType());
        updatedMetacard.setType(new EmptyMetacardType());

        assertThat(isSame(cachedMetacard, updatedMetacard), is(true));
    }

    @Test
    public void isNotSameWhenMetacardsHaveAttributeDescriptorListsOfDifferentSizes() {
        cachedMetacard.setType(new MetacardTypeImpl(MetacardType.DEFAULT_METACARD_TYPE_NAME,
                new HashSet<>()));

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
    }

    @Test
    public void isNotSameEffectiveDate() throws Exception {
        updatedMetacard.setExpirationDate(Date.from(Instant.now()
                .plusSeconds(2)));

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
    }

    @Test
    public void isNotSameExpirationDate() throws Exception {
        updatedMetacard.setExpirationDate(Date.from(Instant.now()
                .plusSeconds(2)));

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
    }

    @Test
    public void isNotSameCreatedDate() throws Exception {
        updatedMetacard.setCreatedDate(Date.from(Instant.now()
                .plusSeconds(2)));

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
        URI nsUri = new URI("http://" + CachedResourceMetacardComparatorTest.class.getName());
        URI resourceUri = new URI(nsUri.toString() + "/resource2.html");
        updatedMetacard.setResourceURI(resourceUri);

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
    }

    @Test
    public void isNotSameModifiedDate() throws Exception {
        updatedMetacard.setModifiedDate(Date.from(Instant.now()
                .plusSeconds(2)));

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
    }

    @Test
    public void isNotSameChecksum() throws Exception {
        updatedMetacard.setAttribute(Metacard.CHECKSUM, "2");

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
    }

    @Test
    public void isNotSameTags() throws Exception {
        updatedMetacard.setAttribute(Metacard.TAGS, ImmutableSet.of("tag99"));

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
    }

    @Test
    public void isNotSameLocation() throws Exception {
        String locWkt = "POLYGON ((29 10, 10 20, 20 40, 40 40, 30 10))";
        updatedMetacard.setLocation(locWkt);

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
    }

    @Test
    public void isNotSameThumbnail() throws Exception {
        updatedMetacard.setThumbnail(new byte[] {5, 4, 3, 2, 1});

        assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
    }

    private MetacardImpl createMetacard(String metacardId, Instant createdDate,
            Instant effectiveDate, Instant expireDate, Instant modDate) throws Exception {
        String locWkt = "POLYGON ((30 10, 10 20, 20 40, 40 40, 30 10))";
        URI nsUri = new URI("http://" + CachedResourceMetacardComparatorTest.class.getName());
        URI resourceUri = new URI(nsUri.toString() + "/resource1.png");
        URI derivedResourceUri = new URI(nsUri.toString() + "/derived.png");
        URL deriverResourceUrl = derivedResourceUri.toURL();

        HashMap<String, List<String>> securityMap = new HashMap<>();
        securityMap.put("key1", ImmutableList.of("value1"));
        securityMap.put("key2", ImmutableList.of("value1", "value2"));

        MetacardImpl metacard = new MetacardImpl(BasicTypes.BASIC_METACARD);

        metacard.setContentTypeName("testContentType");
        metacard.setContentTypeVersion("testContentTypeVersion");
        metacard.setCreatedDate(Date.from(createdDate));
        metacard.setDescription("testDescription");
        metacard.setEffectiveDate(Date.from(effectiveDate));
        metacard.setExpirationDate(Date.from(expireDate));
        metacard.setId(metacardId);
        metacard.setLocation(locWkt);
        metacard.setMetadata("testMetadata");
        metacard.setModifiedDate(Date.from(modDate));
        metacard.setPointOfContact("pointOfContact");
        metacard.setResourceURI(resourceUri);
        metacard.setSourceId("testSourceId");
        metacard.setTargetNamespace(nsUri);
        metacard.setThumbnail(new byte[] {1, 2, 3, 4, 5});
        metacard.setTitle("testTitle");
        metacard.setResourceSize("1");
        metacard.setSecurity(securityMap);
        metacard.setTags(ImmutableSet.of("tag1", "tag2"));
        metacard.setAttribute(Metacard.CHECKSUM, "1");
        metacard.setAttribute(new AttributeImpl(Metacard.CHECKSUM_ALGORITHM, "sha1"));
        metacard.setAttribute(new AttributeImpl(Metacard.DEFAULT_TAG, "tag1"));
        metacard.setAttribute(new AttributeImpl(Metacard.DERIVED, "derivedMetacard"));
        metacard.setAttribute(new AttributeImpl(Metacard.DERIVED_RESOURCE_DOWNLOAD_URL,
                deriverResourceUrl));
        metacard.setAttribute(new AttributeImpl(Metacard.DERIVED_RESOURCE_URI, derivedResourceUri));
        metacard.setAttribute(new AttributeImpl(Metacard.RELATED, "otherMetacardId"));
        return metacard;
    }
}
