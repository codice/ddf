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
package org.codice.ddf.security.idp.client;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

import ddf.security.samlp.impl.SamlProtocol;
import java.io.IOException;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.opensaml.saml.saml2.metadata.IDPSSODescriptor;

public class IdpMetadataTest {
  public static final String CACHE_DURATION_REGEX = "cacheDuration=\"\\w*\"";
  public static final String ISO_ZERO_SECONDS = "PT0S";
  public static final Instant THE_FUTURE = Instant.now().plusSeconds((long) 1e8);
  public static final String ISO_ONE_YEAR = "P1Y";
  public static final Instant THE_PAST = Instant.now().minusSeconds(1);

  String entityXml;
  IdpMetadata metadata;

  @Before
  public void setup() throws IOException {
    metadata = new IdpMetadata();
    entityXml = IOUtils.toString(getClass().getResourceAsStream("/entityDescriptor.xml"), "UTF-8");
  }

  @Test
  public void beforeEntityIsCreated() {
    assertThat("Test uninitialized state", metadata.isMetadataExpired(), is(false));
    assertThat("Test uninitialized state", metadata.isMetadataValid(), is(true));
  }

  @Test
  public void dateIsGood() {
    metadata.setMetadata(setValidUntil(THE_FUTURE, ISO_ZERO_SECONDS, entityXml));
    initializeMetadata();
    assertThat("Expected metadata to be valid", not(metadata.isMetadataValid()));
  }

  @Test
  public void dateIsExpired() {
    metadata.setMetadata(setValidUntil(THE_PAST, ISO_ZERO_SECONDS, entityXml));
    initializeMetadata();
    assertThat("Expected metadata to be invalid", metadata.isMetadataValid(), is(false));
  }

  @Test
  public void dateIsAbsent() {
    metadata.setMetadata(entityXml);
    initializeMetadata();
    assertThat("Precondition not met", metadata.getEntityDescriptor().getValidUntil(), nullValue());
    assertThat(
        "Absent validUntil date mean indefinite validity", metadata.isMetadataValid(), is(true));
  }

  @Test
  public void defaultCacheDurationFromFile() throws Exception {
    metadata.setMetadata(entityXml);
    initializeMetadata();
    assertThat("Expected metadata to be unexpired", not(metadata.isMetadataExpired()));
  }

  @Test
  public void cacheDurationAbsent() {
    metadata.setMetadata(deleteCacheDuration(entityXml));
    assertThat("Expected metadata to be expired", metadata.isMetadataExpired(), is(false));
  }

  @Test
  public void cacheDurationGood() {
    metadata.setMetadata(setCacheDuration(ISO_ONE_YEAR, entityXml));
    initializeMetadata();
    assertThat("Expected metadata to be unexpired", not(metadata.isMetadataExpired()));
  }

  @Test
  public void cacheDurationIsZero() {
    metadata.setMetadata(setCacheDuration(ISO_ZERO_SECONDS, entityXml));
    initializeMetadata();
    assertThat("Expected metadata to be expired", metadata.isMetadataExpired());
  }

  @Test
  public void intializeObject() {
    metadata.setMetadata(entityXml);
    initializeMetadata();
    assertThat(metadata.entityData.get(), notNullValue());
  }

  @Test
  public void reuseValidButExpiredData() {

    metadata.setMetadata(setCacheDuration(ISO_ZERO_SECONDS, entityXml));

    IDPSSODescriptor originalDescriptor = metadata.getDescriptor();

    // Assert preconditions
    assertThat(originalDescriptor, notNullValue());
    assertThat(metadata.isMetadataValid(), is(true));
    assertThat(metadata.isMetadataExpired(), is(true));

    IdpMetadata metadataSpy = spy(metadata);

    // Do not allow the class under test to use the XML document to create a new SAML entity
    // descriptor. For the class to use the existing SAML entity data.
    doReturn(new HashMap()).when(metadataSpy).parseMetadata();

    IDPSSODescriptor nextDescriptor = metadataSpy.getDescriptor();

    assertThat(metadata.isMetadataExpired(), is(true));
    assertThat(
        "Expected cached metadata to be used again",
        nextDescriptor,
        sameInstance(originalDescriptor));
  }

  @Test
  public void bothAreAbsent() {
    metadata.setMetadata(deleteCacheDuration(entityXml));
    initializeMetadata();
    assertThat(
        "Either cache duration or valid-until must exist",
        metadata.entityData.get().getCacheDuration(),
        is(SamlProtocol.getCacheDuration()));
  }

  /**
   * Return a modified version of the (XML) input. The cache duration is changed to matched the
   * value of the first parameter
   */
  private String setCacheDuration(String iso8601Duration, String xml) {
    return xml.replaceFirst(
        CACHE_DURATION_REGEX, String.format("cacheDuration=\"%s\"", iso8601Duration));
  }

  private String deleteCacheDuration(String xml) {
    return xml.replaceFirst(CACHE_DURATION_REGEX, "");
  }

  /**
   * Return a modified version of the (XML) input. The cache duration and valid-until time are
   * modified to match the respective input parameters. If null is passed for the cache duration,
   * the value of the cache duration already in the XML is used. Because of how the substitution
   * works, this method can only be called only once per test. Otherwise, it will create multiple
   * "validUntil" XML attributes.
   *
   * @param validUntil the validUntil instant
   * @param xml the SAML entity description document
   * @return SAML entity description document with a validUntil date
   */
  private String setValidUntil(Instant validUntil, String iso8601Duration, String xml) {
    Pattern pattern = Pattern.compile("cacheDuration=\"(\\w*)\"");
    Matcher matcher = pattern.matcher(xml);
    assertThat("Cannot setup test data - precondition not met", matcher.find(), is(true));
    assertThat("Cannot setup test data - precondition not met", matcher.groupCount(), is(1));
    String duration = iso8601Duration == null ? matcher.group(1) : iso8601Duration;

    DateTimeFormatter formatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME;
    ZonedDateTime temporalAccessor = ZonedDateTime.ofInstant(validUntil, ZoneId.systemDefault());
    String isoTimestamp = formatter.format(temporalAccessor);
    return xml.replaceFirst(
        CACHE_DURATION_REGEX,
        String.format("cacheDuration=\"%s\" validUntil=\"%s\"", duration, isoTimestamp));
  }

  private void initializeMetadata() {
    metadata.getDescriptor();
  }
}
