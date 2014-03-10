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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.io.InputStream;

import org.apache.log4j.Logger;
import org.junit.Test;

public class CachedResourceTest {
    
    private static final transient Logger LOGGER = Logger.getLogger(CachedResourceTest.class);
    
    public String workingDir;

    public InputStream is;

    public RestartableResourceInputStream ris;
    

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
        String inputFilename = workingDir + "/src/test/resources/foo.txt";
        ris = new RestartableResourceInputStream(inputFilename);
        
        CachedResource cachedResource = new CachedResource(workingDir + "/product-cache");
        
        //cachedResource.store(metacard, resourceResponse, resourceCache);
    }
}
