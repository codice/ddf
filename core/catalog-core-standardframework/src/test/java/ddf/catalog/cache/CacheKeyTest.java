package ddf.catalog.cache;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import org.junit.Test;

import ddf.cache.CacheException;
import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;

/**
 * Tests that keys are unique and proper for use with a Cache implementation
 */
public class CacheKeyTest {

    @Test(expected = CacheException.class)
    public void testNullMetacard() throws CacheException {

        // given
        CacheKey cacheKey = new CacheKey(null, mock(ResourceRequest.class));

        // when
        cacheKey.generateKey();

        // then
        // throws an exception
    }

    @Test(expected = CacheException.class)
    public void testNullResourceRequest() throws CacheException {

        // given
        CacheKey cacheKey = new CacheKey(mock(Metacard.class), null);

        // when
        cacheKey.generateKey();

        // then
        // throws an exception
    }

    @Test()
    public void testKeyGeneration() throws CacheException {

        // given
        CacheKey cacheKey = new CacheKey(getMetacardStub("sampleId"), getResourceRequestStub());

        // when
        String key = cacheKey.generateKey();

        // then
        assertNotNull("Key must not be null.", key);
        assertThat("Key must not be empty.", key, not(equalToIgnoringWhiteSpace("")));

    }

    @Test()
    public void testKeyUniquenessMetacardId() throws CacheException {

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
    public void testKeyUniquenessFromSources() throws CacheException {

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
    public void testKeyUniquenessFromSourcesAndIds() throws CacheException {

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
     * 
     * @throws CacheException
     */
    @Test()
    public void testKeyUniquenessProperty() throws CacheException {

        // given
        Map<String, Serializable> propertyMap = new HashMap<String, Serializable>();
        propertyMap.put("pdf", "sample.pdf");
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
     * 
     * @throws CacheException
     */
    @Test()
    public void testKeyUniquenessProperties() throws CacheException {

        // given
        Map<String, Serializable> propertyMap1 = new HashMap<String, Serializable>();
        propertyMap1.put("pdf", "sample1.pdf");
        propertyMap1.put("html", "sample1.html");
        Map<String, Serializable> propertyMap2 = new HashMap<String, Serializable>();
        propertyMap2.put("pdf", "sample2.pdf");
        propertyMap2.put("html", "sample2.html");
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
    public void testKeyConsistency() throws CacheException {

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
