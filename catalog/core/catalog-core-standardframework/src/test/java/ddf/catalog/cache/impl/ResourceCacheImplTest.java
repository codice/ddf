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

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Associations;
import ddf.catalog.data.types.Core;
import ddf.catalog.operation.impl.ResourceRequestById;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.data.ReliableResource;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Calendar;
import java.util.Optional;
import javax.activation.MimeType;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ResourceCacheImplTest {

  private static final String SOURCE_ID = "ddf-1";

  private static final String METACARD_ID = "abc123";

  private static final String NOT_CACHED_METACARD_ID = "cde456";

  private static final String CACHED_RESOURCE_KEY = String.format("%s-%s", SOURCE_ID, METACARD_ID);

  private ResourceCacheImpl resourceCache;

  // Currently testing the new ResourceCacheImpl using this test class. This will be moved out
  // when the code gets moved form the old to the new ResourceCacheImpl.
  private org.codice.ddf.catalog.resource.cache.impl.ResourceCacheImpl newResourceCache;

  private Path defaultProductCacheDirectory;

  private Metacard cachedMetacard;

  private Metacard notCachedMetacard;

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  @Before
  public void setUp() throws IOException, URISyntaxException {
    cachedMetacard = createMetacard(SOURCE_ID, METACARD_ID);
    notCachedMetacard = createMetacard(SOURCE_ID, NOT_CACHED_METACARD_ID);

    Path ddfData = testFolder.newFolder("data").toPath();

    defaultProductCacheDirectory = Paths.get(ddfData.toString(), "Product_Cache");
    defaultProductCacheDirectory.toFile().mkdirs();

    resourceCache = new ResourceCacheImpl(defaultProductCacheDirectory.toString());

    newResourceCache =
        new org.codice.ddf.catalog.resource.cache.impl.ResourceCacheImpl(resourceCache);
  }

  @After
  public void teardownTest() throws IOException {
    resourceCache.teardownCache();
  }

  @Test
  public void testContainsWithNullKey() {
    assertFalse(resourceCache.containsValid(null, new MetacardImpl()));
  }

  /** Verifies that put() method works. */
  @Test
  public void testPutThenGet() throws URISyntaxException {
    Metacard metacard = generateMetacard();
    ReliableResource reliableResource = createCachedResource(metacard);

    resourceCache.addPendingCacheEntry(reliableResource);
    assertTrue(resourceCache.isPending(CACHED_RESOURCE_KEY));
    resourceCache.put(reliableResource);
    assertTrue(
        assertReliableResourceEquals(
            reliableResource, resourceCache.getValid(CACHED_RESOURCE_KEY, metacard)));
    assertFalse(resourceCache.isPending(CACHED_RESOURCE_KEY));
  }

  /**
   * Verifies that put() method works even if entry being added was never in the pending cache list.
   */
  @Test
  public void testPutThenGetNotPending() throws URISyntaxException {
    MetacardImpl metacard = generateMetacard();
    ReliableResource reliableResource = createCachedResource(metacard);

    resourceCache.put(reliableResource);
    assertFalse(resourceCache.isPending(CACHED_RESOURCE_KEY));
    assertTrue(
        assertReliableResourceEquals(
            reliableResource, resourceCache.getValid(CACHED_RESOURCE_KEY, metacard)));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testGetValidWhenNullKey() {
    resourceCache.getValid(null, new MetacardImpl());
  }

  @Test
  public void testGetValidWhenNoProductInCache() {
    String key = CACHED_RESOURCE_KEY;
    assertNull(resourceCache.getValid(key, new MetacardImpl()));
  }

  /**
   * Verifies that if there is a entry in the cache but no associated product file in the cache
   * directory, when a get() is done on that cached entry that the missing product is detected, the
   * cache entry is removed.
   */
  @Test
  public void testGetValidWhenNoProductInCacheDirectory() {
    String key = CACHED_RESOURCE_KEY;
    MetacardImpl metacard = new MetacardImpl();

    assertNull(resourceCache.getValid(key, metacard));
  }

  @Test
  public void testValidationEqualMetacards() throws URISyntaxException {
    MetacardImpl metacard = generateMetacard();
    MetacardImpl metacard1 = generateMetacard();

    ReliableResource cachedResource = new ReliableResource("key", "", null, null, metacard);
    assertTrue(resourceCache.validateCacheEntry(cachedResource, metacard1));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidationNullResource() throws URISyntaxException {
    resourceCache.validateCacheEntry(null, generateMetacard());
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidationNullMetacard() throws URISyntaxException {
    MetacardImpl metacard = generateMetacard();
    ReliableResource cachedResource = new ReliableResource("key", "", null, null, metacard);
    resourceCache.validateCacheEntry(cachedResource, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testValidationNullParams() throws URISyntaxException {
    resourceCache.validateCacheEntry(null, null);
  }

  @Test
  public void testValidationNotEqual() throws URISyntaxException, IOException {
    MetacardImpl metacard = generateMetacard();
    MetacardImpl metacard1 = generateMetacard();
    metacard1.setId("differentId");

    String fileName = "10bytes.txt";
    simulateAddFileToCacheDir(fileName);
    String cachedResourceMetacardKey = "keyA1";
    Path cachedResourceFilePath = Paths.get(defaultProductCacheDirectory.toString(), fileName);
    File cachedResourceFile = cachedResourceFilePath.toFile();
    assertTrue(cachedResourceFile.exists());

    ReliableResource cachedResource =
        new ReliableResource(
            cachedResourceMetacardKey, cachedResourceFilePath.toString(), null, null, metacard);
    resourceCache.validateCacheEntry(cachedResource, metacard1);
    assertFalse(cachedResourceFile.exists());
  }

  @Test
  public void testContainsTrueValid() throws URISyntaxException {
    MetacardImpl cachedMetacard = generateMetacard();
    MetacardImpl latestMetacard = generateMetacard();

    String cacheKey = "cacheKey1";
    resourceCache.put(new ReliableResource(cacheKey, "", null, "name", cachedMetacard));
    assertTrue(resourceCache.containsValid(cacheKey, latestMetacard));
  }

  @Test
  public void testContainsFalseValid() throws URISyntaxException {
    MetacardImpl latestMetacard = generateMetacard();

    String key = "cacheKey1";
    assertFalse(resourceCache.containsValid(key, latestMetacard));
  }

  @Test(expected = IllegalArgumentException.class)
  public void testContainsNullLatestMetacard() throws URISyntaxException {
    MetacardImpl cachedMetacard = generateMetacard();

    String cacheKey = "cacheKey1";
    resourceCache.put(new ReliableResource(cacheKey, "", null, "name", cachedMetacard));
    assertFalse(resourceCache.containsValid("cacheKey1", null));
  }

  @Test
  public void testContainsTrueInvalid() throws URISyntaxException, IOException {
    MetacardImpl cachedMetacard = generateMetacard();
    MetacardImpl latestMetacard = generateMetacard();
    latestMetacard.setId("different-id");

    String fileName = "10bytes.txt";
    simulateAddFileToCacheDir(fileName);
    String cachedResourceMetacardKey = "keyA1";
    Path cachedResourceFilePath = Paths.get(defaultProductCacheDirectory.toString(), fileName);
    File cachedResourceFile = cachedResourceFilePath.toFile();
    assertTrue(cachedResourceFile.exists());

    resourceCache.put(
        new ReliableResource(
            cachedResourceMetacardKey,
            cachedResourceFilePath.toString(),
            null,
            "name",
            cachedMetacard));
    assertFalse(resourceCache.containsValid(cachedResourceMetacardKey, latestMetacard));
    assertFalse(cachedResourceFile.exists());
  }

  @Test
  public void testContainsTrueInvalid2CantFindFile() throws URISyntaxException {
    MetacardImpl cachedMetacard = generateMetacard();
    cachedMetacard.setId("different-id");
    MetacardImpl latestMetacard = generateMetacard();

    String cacheKey = "cacheKey1";
    resourceCache.put(new ReliableResource(cacheKey, "", null, "name", cachedMetacard));
    assertFalse(resourceCache.containsValid(cacheKey, latestMetacard));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getDefaultResourceWithNullMetacard() {
    newResourceCache.get(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void getSpecificResourceWithNullMetacard() {
    newResourceCache.get(null, new ResourceRequestById(METACARD_ID));
  }

  @Test(expected = IllegalArgumentException.class)
  public void getSpecificResourceWithNullResourceRequest() {
    newResourceCache.get(cachedMetacard, null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void containsDefaultResourceWithNullMetacard() {
    newResourceCache.contains(null);
  }

  @Test(expected = IllegalArgumentException.class)
  public void containsSpecificResourceWithNullMetacard() {
    newResourceCache.contains(null, new ResourceRequestById(METACARD_ID));
  }

  @Test(expected = IllegalArgumentException.class)
  public void containsSpecificResourceWithNullResourceRequest() {
    newResourceCache.contains(cachedMetacard, null);
  }

  @Test
  public void getDefaultResourceInCache() {
    ReliableResource cachedResource = createCachedResource(cachedMetacard);
    resourceCache.put(cachedResource);

    Optional<Resource> optionalResource = newResourceCache.get(cachedMetacard);

    assertTrue(optionalResource.isPresent());
    assertTrue(assertReliableResourceEquals(cachedResource, optionalResource.get()));
  }

  @Test
  public void getSpecificResourceInCache() {
    ReliableResource cachedResource = createCachedResource(cachedMetacard);
    resourceCache.put(cachedResource);

    Optional<Resource> optionalResource =
        newResourceCache.get(cachedMetacard, new ResourceRequestById(METACARD_ID));

    assertTrue(optionalResource.isPresent());
    assertTrue(assertReliableResourceEquals(cachedResource, optionalResource.get()));
  }

  @Test
  public void getDefaultResourceNotInCache() {
    Optional<Resource> optionalResource = newResourceCache.get(notCachedMetacard);

    assertFalse(optionalResource.isPresent());
  }

  @Test
  public void getSpecificResourceNotInCache() {
    Optional<Resource> optionalResource =
        newResourceCache.get(notCachedMetacard, new ResourceRequestById(NOT_CACHED_METACARD_ID));

    assertFalse(optionalResource.isPresent());
  }

  @Test
  public void containsDefaultResourceInCache() {
    ReliableResource cachedResource = createCachedResource(cachedMetacard);
    resourceCache.put(cachedResource);

    assertThat(newResourceCache.contains(cachedMetacard), is(true));
  }

  @Test
  public void containsSpecificResourceInCache() {
    ReliableResource cachedResource = createCachedResource(cachedMetacard);
    resourceCache.put(cachedResource);

    assertThat(
        newResourceCache.contains(cachedMetacard, new ResourceRequestById(METACARD_ID)), is(true));
  }

  @Test
  public void containsDefaultResourceNotInCache() {
    assertThat(newResourceCache.contains(notCachedMetacard), is(false));
  }

  @Test
  public void containsSpecificResourceNotInCache() {
    assertThat(
        newResourceCache.contains(
            notCachedMetacard, new ResourceRequestById(NOT_CACHED_METACARD_ID)),
        is(false));
  }

  private Metacard createMetacard(String sourceId, String metacardId) throws URISyntaxException {
    Metacard metacard = generateMetacard();
    metacard.setSourceId(sourceId);
    metacard.setAttribute(new AttributeImpl(Core.ID, metacardId));
    return metacard;
  }

  private ReliableResource createCachedResource(Metacard metacard) {
    String fileName = "15bytes.txt";
    String productLocation = this.getClass().getResource("/" + fileName).getFile();
    File rrCachedFile = new File(productLocation);
    return new ReliableResource(
        CACHED_RESOURCE_KEY, rrCachedFile.getAbsolutePath(), new MimeType(), fileName, metacard);
  }

  private void simulateAddFileToCacheDir(String fileName) throws IOException {
    String originalFilePath = this.getClass().getResource("/" + fileName).getFile();
    String destinationFilePath =
        Paths.get(defaultProductCacheDirectory.toString(), fileName).toString();
    FileUtils.copyFile(new File(originalFilePath), new File(destinationFilePath));
  }

  private MetacardImpl generateMetacard() throws URISyntaxException {
    MetacardImpl metacard = new MetacardImpl();
    metacard.setAttribute(Associations.DERIVED, "1");
    metacard.setSourceId("source1");
    metacard.setId("id123");
    metacard.setContentTypeName("content-type-name");
    metacard.setContentTypeVersion("content-type-version");
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis(1400673600); // hardcode date to ensure it is the same time.
    metacard.setCreatedDate(c.getTime());
    metacard.setEffectiveDate(c.getTime());
    metacard.setExpirationDate(c.getTime());
    metacard.setModifiedDate(c.getTime());
    metacard.setLocation("POINT(0 10)");
    metacard.setMetadata("<metadata>abc</metadata>");
    metacard.setResourceSize("100");
    metacard.setResourceURI(new URI("https://github.com/codice"));
    metacard.setTitle("title");
    return metacard;
  }

  /**
   * Check the values of the ReliableResource objects since equals fails once they've been
   * serialized and reconstituted.
   */
  private boolean assertReliableResourceEquals(ReliableResource expected, Resource actual) {
    ReliableResource rrActual = null;
    boolean result = false;

    if (actual instanceof ReliableResource) {
      rrActual = (ReliableResource) actual;
      result = true;
    }

    if (result) {
      result = rrActual.getFilePath().equals(expected.getFilePath());

      if (result) {
        result = rrActual.getMimeTypeValue().equals((expected.getMimeTypeValue()));

        if (result) {
          result = rrActual.getName().equals(expected.getName());

          if (result) {
            result = rrActual.getSize() == expected.getSize();

            if (result) {
              result = rrActual.getLastTouchedMillis() == expected.getLastTouchedMillis();
            }
          }
        }
      }
    }

    return result;
  }
}
