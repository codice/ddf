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
package ddf.catalog.cache;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.equalToIgnoringWhiteSpace;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import ddf.catalog.cache.impl.CacheKey;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;

/**
 * Tests that keys are unique and proper for use with a Cache implementation
 */
public class CacheKeyTest {
    //dummy comment

    @Test(expected = Exception.class)
    public void testNullMetacard() throws Exception {

        // given
        CacheKey cacheKey = new CacheKey(null, mock(ResourceRequest.class));

        // when
        cacheKey.generateKey();

        // then
        // throws an exception
    }

    @Test(expected = Exception.class)
    public void testNullResourceRequest() throws Exception {

        // given
        CacheKey cacheKey = new CacheKey(mock(Metacard.class), null);

        // when
        cacheKey.generateKey();

        // then
        // throws an exception
    }

    @Test()
    public void testKeyGeneration() throws Exception {

        // given
        CacheKey cacheKey = new CacheKey(getMetacardStub("sampleId"), getResourceRequestStub());

        // when
        String key = cacheKey.generateKey();

        // then
        assertNotNull("Key must not be null.", key);
        assertThat("Key must not be empty.", key, not(equalToIgnoringWhiteSpace("")));

    }

    @Test()
    public void testKeyUniquenessMetacardId() throws Exception {

        // given
        CacheKey cacheKey1 = new CacheKey(getMetacardStub("sampleId"), getResourceRequestStub());
        CacheKey cacheKey2 = new CacheKey(getMetacardStub("sampledI"), getResourceRequestStub());

        // when
        String key1 = cacheKey1.generateKey();
        String key2 = cacheKey2.generateKey();

        // then
        assertThat("Keys must be different.", key1, not(equalTo(key2)));

    }

    @Test()
    public void testKeyUniquenessFromSources() throws Exception {

        // given
        CacheKey cacheKey1 = new CacheKey(getMetacardStub("sampleId", "source1"),
                getResourceRequestStub());
        CacheKey cacheKey2 = new CacheKey(getMetacardStub("sampleId", "source2"),
                getResourceRequestStub());

        // when
        String key1 = cacheKey1.generateKey();
        String key2 = cacheKey2.generateKey();

        // then
        assertThat("Keys must be different.", key1, not(equalTo(key2)));

    }

    @Test()
    public void testKeyUniquenessFromSourcesAndIds() throws Exception {

        // given
        CacheKey cacheKey1 = new CacheKey(getMetacardStub("sampleId1", "source1"),
                getResourceRequestStub());
        CacheKey cacheKey2 = new CacheKey(getMetacardStub("sampleId2", "source2"),
                getResourceRequestStub());

        // when
        String key1 = cacheKey1.generateKey();
        String key2 = cacheKey2.generateKey();

        // then
        assertThat("Keys must be different.", key1, not(equalTo(key2)));

    }

    /**
     * Tests the key will be unique if given a different property in the ResourceRequest.
     */
    @Test()
    public void testKeyUniquenessProperty() throws Exception {

        // given
        Map<String, Serializable> propertyMap = new HashMap<String, Serializable>();
        propertyMap.put(ResourceRequest.OPTION_ARGUMENT, "pdf");
        CacheKey cacheKey1 = new CacheKey(getMetacardStub("sampleId1", "source1"),
                getResourceRequestStub(propertyMap));
        CacheKey cacheKey2 = new CacheKey(getMetacardStub("sampleId1", "source1"),
                getResourceRequestStub());

        // when
        String key1 = cacheKey1.generateKey();
        String key2 = cacheKey2.generateKey();

        // then
        assertThat("Keys must be different.", key1, not(equalTo(key2)));

    }

    /**
     * Tests keys will be unique if given different properties in the ResourceRequest.
     */
    @Test()
    public void testKeyUniquenessProperties() throws Exception {

        // given
        Map<String, Serializable> propertyMap1 = new HashMap<String, Serializable>();
        propertyMap1.put(ResourceRequest.OPTION_ARGUMENT, "pdf");
        Map<String, Serializable> propertyMap2 = new HashMap<String, Serializable>();
        propertyMap2.put(ResourceRequest.OPTION_ARGUMENT, "html");
        CacheKey cacheKey1 = new CacheKey(getMetacardStub("sampleId1", "source1"),
                getResourceRequestStub(propertyMap1));
        CacheKey cacheKey2 = new CacheKey(getMetacardStub("sampleId1", "source1"),
                getResourceRequestStub(propertyMap2));

        // when
        String key1 = cacheKey1.generateKey();
        String key2 = cacheKey2.generateKey();

        // then
        assertThat("Keys must be different.", key1, not(equalTo(key2)));

    }

    @Test()
    public void testKeyConsistency() throws Exception {

        // given
        Map<String, Serializable> propertyMap = new HashMap<String, Serializable>();
        propertyMap.put("pdf", "sample.pdf");
        CacheKey cacheKey1 = new CacheKey(getMetacardStub("sampleId1", "source1"),
                getResourceRequestStub(propertyMap));
        CacheKey cacheKey2 = new CacheKey(getMetacardStub("sampleId1", "source1"),
                getResourceRequestStub(propertyMap));

        // when
        String key1 = cacheKey1.generateKey();
        String key2 = cacheKey2.generateKey();

        // then
        assertThat("The same input to cache key should generate the same output.", key1,
                equalTo(key2));

    }

    private ResourceRequest getResourceRequestStub(final Map<String, Serializable> properties) {

        return new ResourceRequest() {

            @Override
            public boolean containsPropertyName(String arg0) {

                return properties.containsKey(arg0);
            }

            @Override
            public Map<String, Serializable> getProperties() {
                return properties;
            }

            @Override
            public Set<String> getPropertyNames() {
                return properties.keySet();
            }

            @Override
            public Serializable getPropertyValue(String arg0) {
                return properties.get(arg0);
            }

            @Override
            public boolean hasProperties() {
                return true;
            }

            // unimplemented
            @Override
            public String getAttributeName() {
                return null;
            }

            // unimplemented
            @Override
            public Serializable getAttributeValue() {
                return null;
            }
        };

    }

    private ResourceRequest getResourceRequestStub() {

        ResourceRequest request = mock(ResourceRequest.class);

        return request;
    }

    private Metacard getMetacardStub(String id) {

        return getMetacardStub(id, null);
    }

    private Metacard getMetacardStub(String id, String source) {

        Metacard metacard = mock(Metacard.class);

        when(metacard.getId()).thenReturn(id);

        when(metacard.getSourceId()).thenReturn(source);

        return metacard;
    }
}
