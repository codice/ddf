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
package ddf.mime.tika;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.Test;

import ddf.mime.tika.TikaMimeTypeResolver;

public class TikaMimeTypeResolverTest {
    @Test
    public void testGetFileExtensionForMimeType() throws Exception {
        TikaMimeTypeResolver resolver = new TikaMimeTypeResolver();
        String fileExtension = resolver.getFileExtensionForMimeType("application/pdf");
        assertEquals(".pdf", fileExtension);
    }

    @Test
    public void testGetMimeTypeForPdfFileExtension() throws Exception {
        TikaMimeTypeResolver resolver = new TikaMimeTypeResolver();
        String mimeType = resolver.getMimeTypeForFileExtension("pdf");
        assertEquals("application/pdf", mimeType);
    }

    @Test
    public void testGetMimeTypeForXmlFileExtension() throws Exception {
        TikaMimeTypeResolver resolver = new TikaMimeTypeResolver();
        String mimeType = resolver.getMimeTypeForFileExtension("xml");
        assertEquals("application/xml", mimeType);
    }

    @Test
    public void testGetFileExtensionForNullMimeType() throws Exception {
        TikaMimeTypeResolver resolver = new TikaMimeTypeResolver();
        String fileExtension = resolver.getFileExtensionForMimeType(null);
        assertEquals(null, fileExtension);
    }

    @Test
    public void testGetMimeTypeForNullFileExtension() throws Exception {
        TikaMimeTypeResolver resolver = new TikaMimeTypeResolver();
        String mimeType = resolver.getMimeTypeForFileExtension(null);
        assertEquals(null, mimeType);
    }

    @Test
    public void testGetFileExtensionForEmptyMimeType() throws Exception {
        TikaMimeTypeResolver resolver = new TikaMimeTypeResolver();
        String fileExtension = resolver.getFileExtensionForMimeType("");
        assertEquals(null, fileExtension);
    }

    @Test
    public void testGetMimeTypeForEmptyFileExtension() throws Exception {
        TikaMimeTypeResolver resolver = new TikaMimeTypeResolver();
        String mimeType = resolver.getMimeTypeForFileExtension("");
        assertEquals(null, mimeType);
    }
}
