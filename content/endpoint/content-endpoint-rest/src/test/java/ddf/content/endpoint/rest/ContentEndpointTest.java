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
package ddf.content.endpoint.rest;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ContentEndpointTest {
    // used to print logging while running as a JUnit test
    static {
        org.apache.log4j.BasicConfigurator.configure();
    }

    private final String NITF_CONTENT_TYPE = "application/octet-stream";

    // @Test
    // public void testCreate() throws Exception
    // {
    // InputStream stream = IOUtils.toInputStream( "Hello World" );
    // MockBundleContext bundleContext = new MockBundleContext();
    // StorageProvider provider = new MockStorageProvider();
    // ContentFramework framework = new ContentFrameworkImpl( bundleContext, provider );
    // ContentEndpoint contentEndpoint = new ContentEndpoint( framework );
    // Response response = contentEndpoint.doCreate( stream, NITF_CONTENT_TYPE, false, false );
    //
    // }

    public InputStream getInputStream(String filename) throws IOException {
        final InputStream in = new BufferedInputStream(new FileInputStream(filename));
        final long start = System.currentTimeMillis();
        int cnt = 0;
        final byte[] buf = new byte[1000];
        while (in.read(buf) != -1)
            cnt++;
        in.close();
        System.out.println("Elapsed " + (System.currentTimeMillis() - start) + " ms");

        return in;
    }

}
