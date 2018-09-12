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
package ddf.catalog.cache.impl;

import static ddf.catalog.cache.impl.CachedResourceMetacardComparator.isSame;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.EmptyMetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;
import org.junit.Before;
import org.junit.Test;

public class CachedResourceMetacardComparatorTest {

  private MetacardImpl cachedMetacard;

  private MetacardImpl updatedMetacard;

  @Before
  public void setup() throws Exception {
    Instant productCreatedDate = Instant.now();
    Instant productModificationDate = productCreatedDate.plus(2, ChronoUnit.DAYS);
    Instant metacardCreateDate = Instant.now();
    Instant metacardModificationDate = metacardCreateDate.plus(2, ChronoUnit.DAYS);

    String metacardId = UUID.randomUUID().toString();

    cachedMetacard =
        createMetacard(
            metacardId,
            productCreatedDate,
            productModificationDate,
            metacardCreateDate,
            metacardModificationDate);
    updatedMetacard =
        createMetacard(
            metacardId,
            productCreatedDate,
            productModificationDate,
            metacardCreateDate,
            metacardModificationDate);
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
   * See the {@link CachedResourceMetacardComparator} class for more information on why {@link
   * CachedResourceMetacardComparator#isSame(Metacard, Metacard)} will return {@code true} even when
   * the resource size attributes are different.
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
  public void isSameWhenMetacardsHaveNullAttributeDescriptorLists() {
    cachedMetacard.setType(new EmptyMetacardType());
    updatedMetacard.setType(new EmptyMetacardType());

    assertThat(isSame(cachedMetacard, updatedMetacard), is(true));
  }

  @Test
  public void isNotSameProductCreatedDate() {
    updatedMetacard.setCreatedDate(Date.from(Instant.now().plusSeconds(2)));

    assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
  }

  @Test
  public void isNotSameProductModifiedDate() {
    updatedMetacard.setModifiedDate(Date.from(Instant.now().plusSeconds(2)));

    assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
  }

  @Test
  public void isNotSameMetacardCreatedDate() {
    updatedMetacard.setAttribute(Core.METACARD_CREATED, Date.from(Instant.now().plusSeconds(2)));

    assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
  }

  @Test
  public void isNotSameMetacardModifiedDate() {
    updatedMetacard.setAttribute(Core.METACARD_MODIFIED, Date.from(Instant.now().plusSeconds(2)));

    assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
  }

  @Test
  public void isNotSameChecksum() {
    updatedMetacard.setAttribute(Metacard.CHECKSUM, "2");

    assertThat(isSame(cachedMetacard, updatedMetacard), is(false));
  }

  private MetacardImpl createMetacard(
      String metacardId,
      Instant productCreatedDate,
      Instant productModDate,
      Instant metaCreatedDate,
      Instant metaModDate) {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setCreatedDate(Date.from(productCreatedDate));
    metacard.setAttribute(Core.METACARD_CREATED, metaCreatedDate);
    metacard.setId(metacardId);
    metacard.setModifiedDate(Date.from(productModDate));
    metacard.setAttribute(Core.METACARD_MODIFIED, metaModDate);
    metacard.setSourceId("testSourceId");
    metacard.setAttribute(Metacard.CHECKSUM, "1");
    metacard.setAttribute(new AttributeImpl(Metacard.CHECKSUM_ALGORITHM, "sha1"));
    metacard.setAttribute(new AttributeImpl(Metacard.DERIVED, "derivedMetacard"));
    return metacard;
  }
}
