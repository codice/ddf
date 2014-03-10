/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.catalog.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import javax.activation.MimeType;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import ddf.catalog.data.Metacard;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.resource.Resource;

public class CachedResourceTest {
    
    private static final transient Logger LOGGER = Logger.getLogger(CachedResourceTest.class);
    
    public String workingDir;

    public InputStream is;

    public MockInputStream ris;
    

    @Test
    public void testHasProductWithNullFilepath() {
        assertFalse(new CachedResource("").hasProduct());
    }

    @Test
    public void testGetProductWithNullFilepath() throws IOException {
        assertNull(new CachedResource("").getProduct());
    }

    @Test
    public void testStore() throws Exception {
        workingDir = System.getProperty("user.dir");
        String inputFilename = workingDir + "/src/test/resources/foo_10_lines.txt";
        ris = new MockInputStream(inputFilename);
        
        Metacard metacard = getMetacardStub("abc123", "ddf-1");       

        ResourceRequest resourceRequest = mock(ResourceRequest.class);
        Map<String, Serializable> requestProperties = new HashMap<String, Serializable>();
        when(resourceRequest.getPropertyNames()).thenReturn(requestProperties.keySet());

        Resource resource = mock(Resource.class);
        when(resource.getInputStream()).thenReturn(ris);
        when(resource.getName()).thenReturn("test-resource");
        when(resource.getMimeType()).thenReturn(new MimeType("text/plain"));

        ResourceResponse resourceResponse = mock(ResourceResponse.class);
        when(resourceResponse.getRequest()).thenReturn(resourceRequest);
        when(resourceResponse.getResource()).thenReturn(resource);
        when(resourceResponse.getProperties()).thenReturn(null);

        ArgumentCaptor<CachedResource> argument = ArgumentCaptor.forClass(CachedResource.class);
        ResourceCache resourceCache = mock(ResourceCache.class);
        
        CachedResource cachedResource = new CachedResource(workingDir + "/product-cache");
        int chunkSize = 50;
        cachedResource.setChunkSize(chunkSize);
        
        Resource newResource = cachedResource.store(metacard, resourceResponse, resourceCache);
        assertNotNull(newResource);
        clientRead(chunkSize, newResource.getInputStream());
        
        // Verifies that the CachedResource that is cached was assigned the correct key
        // and that the file it points to has the expected content
        verify(resourceCache).put(argument.capture());
        CachedResource cr = argument.getValue();
        assertEquals("ddf-1-abc123", cr.getKey());
        byte[] cachedData = cr.getByteArray();
        assertNotNull(cachedData);
        assertTrue(new String(cachedData).contains("10. Help I am trapped in a fortune cookie factory"));
    }

    @Test
    //@Ignore
    public void testStoreWithInputStreamRecoverableError() throws Exception {
        workingDir = System.getProperty("user.dir");
        String inputFilename = workingDir + "/src/test/resources/foo_10_lines.txt";
        ris = new MockInputStream(inputFilename);
        ris.setInvocationCountToThrowIOException(53);
        
        Metacard metacard = getMetacardStub("abc123", "ddf-1");       

        ResourceRequest resourceRequest = mock(ResourceRequest.class);
        Map<String, Serializable> requestProperties = new HashMap<String, Serializable>();
        when(resourceRequest.getPropertyNames()).thenReturn(requestProperties.keySet());

        Resource resource = mock(Resource.class);
        when(resource.getInputStream()).thenReturn(ris);
        when(resource.getName()).thenReturn("test-resource");
        when(resource.getMimeType()).thenReturn(new MimeType("text/plain"));

        ResourceResponse resourceResponse = mock(ResourceResponse.class);
        when(resourceResponse.getRequest()).thenReturn(resourceRequest);
        when(resourceResponse.getResource()).thenReturn(resource);
        when(resourceResponse.getProperties()).thenReturn(null);

        ArgumentCaptor<CachedResource> argument = ArgumentCaptor.forClass(CachedResource.class);
        ResourceCache resourceCache = mock(ResourceCache.class);
        
        CachedResource cachedResource = new CachedResource(workingDir + "/product-cache");
        int chunkSize = 50;
        cachedResource.setChunkSize(chunkSize);
        
        Resource newResource = cachedResource.store(metacard, resourceResponse, resourceCache);
        assertNotNull(newResource);
        clientRead(chunkSize, newResource.getInputStream());
        
        // Verifies that the CachedResource that is cached was assigned the correct key
        // and that the file it points to has the expected content
        verify(resourceCache).put(argument.capture());
        CachedResource cr = argument.getValue();
        assertEquals("ddf-1-abc123", cr.getKey());
        byte[] cachedData = cr.getByteArray();
        assertNotNull(cachedData);
        assertTrue(new String(cachedData).contains("10. Help I am trapped in a fortune cookie factory"));
    }
    
//////////////////////////////////////////////////////////////////////////////////////////////////
    
    private Metacard getMetacardStub(String id, String source) {

        Metacard metacard = mock(Metacard.class);

        when(metacard.getId()).thenReturn(id);

        when(metacard.getSourceId()).thenReturn(source);

        return metacard;
    }

    // Simulates client/endpoint reading product from piped input stream
    public void clientRead(int chunkSize, InputStream pis) {
        long size = 0;
        byte[] buffer = new byte[chunkSize];
        try {
            // LOGGER.info("Client read:\n");
            FileOutputStream fos = FileUtils.openOutputStream(new File("temp.out"));
            int n = 0;
            while (true) {
                // LOGGER.info("Reading from pipe input stream");
                n = pis.read(buffer);
                if (n == -1) {
                    // LOGGER.info("detected -1");
                    break;
                }
                // LOGGER.info(new String(buffer, 0, n));
                fos.write(buffer, 0, n);
                size += n;
            }
            IOUtils.closeQuietly(pis);
            IOUtils.closeQuietly(fos);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            LOGGER.error("Exception", e);
        }
        LOGGER.info("Client DONE - size = " + size);
    }
}
