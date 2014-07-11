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
package ddf.mime.custom;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.Test;

public class CustomMimeTypeResolverTest {
    @Test
    public void testGetCustomFileExtensionsToMimeTypesMap() {
        CustomMimeTypeResolver resolver = new CustomMimeTypeResolver();

        Map<String, String> fileExtensionsToMimeTypes = resolver
                .getCustomFileExtensionsToMimeTypesMap();
        assertTrue(fileExtensionsToMimeTypes.size() == 0);
    }

    @Test
    public void testGetCustomMimeTypesToFileExtensionsMap() {
        CustomMimeTypeResolver resolver = new CustomMimeTypeResolver();

        Map<String, List<String>> mimeTypesToFileExtensions = resolver
                .getCustomMimeTypesToFileExtensionsMap();
        assertTrue(mimeTypesToFileExtensions.size() == 0);
    }

    @Test
    public void testSetSingleCustomMimeTypes() {
        CustomMimeTypeResolver resolver = new CustomMimeTypeResolver();
        resolver.setCustomMimeTypes(new String[] {"nitf=image/nitf"});

        Map<String, List<String>> mimeTypesToFileExtensions = resolver
                .getCustomMimeTypesToFileExtensionsMap();
        assertTrue(mimeTypesToFileExtensions.containsKey("image/nitf"));
        List<String> fileExtensions = mimeTypesToFileExtensions.get("image/nitf");
        assertTrue(fileExtensions.contains("nitf"));

        Map<String, String> fileExtensionsMimeTypes = resolver
                .getCustomFileExtensionsToMimeTypesMap();
        assertTrue(fileExtensionsMimeTypes.containsKey("nitf"));
        assertEquals("image/nitf", fileExtensionsMimeTypes.get("nitf"));
    }

    @Test
    public void testSetMultipleFileExtensionsToSameCustomMimeType() {
        CustomMimeTypeResolver resolver = new CustomMimeTypeResolver();

        resolver.setCustomMimeTypes(new String[] {"nitf=image/nitf", "ntf=image/nitf"});

        Map<String, List<String>> mimeTypesToFileExtensions = resolver
                .getCustomMimeTypesToFileExtensionsMap();
        assertTrue(mimeTypesToFileExtensions.containsKey("image/nitf"));
        List<String> fileExtensions = mimeTypesToFileExtensions.get("image/nitf");
        assertTrue(fileExtensions.contains("nitf"));
        assertTrue(fileExtensions.contains("ntf"));

        Map<String, String> fileExtensionsMimeTypes = resolver
                .getCustomFileExtensionsToMimeTypesMap();
        assertTrue(fileExtensionsMimeTypes.containsKey("nitf"));
        assertEquals("image/nitf", fileExtensionsMimeTypes.get("nitf"));
        assertTrue(fileExtensionsMimeTypes.containsKey("ntf"));
        assertEquals("image/nitf", fileExtensionsMimeTypes.get("ntf"));
    }

    @Test
    public void testSetMultipleCustomMimeTypesToSameFileExtension() {
        CustomMimeTypeResolver resolver = new CustomMimeTypeResolver();

        resolver.setCustomMimeTypes(new String[] {"xml=text/xml", "xml=application/xml"});

        Map<String, List<String>> mimeTypesToFileExtensions = resolver
                .getCustomMimeTypesToFileExtensionsMap();
        assertTrue(mimeTypesToFileExtensions.containsKey("text/xml"));
        assertTrue(mimeTypesToFileExtensions.containsKey("application/xml"));
        List<String> fileExtensions = mimeTypesToFileExtensions.get("text/xml");
        assertTrue(fileExtensions.contains("xml"));
        fileExtensions = mimeTypesToFileExtensions.get("application/xml");
        assertTrue(fileExtensions.contains("xml"));

        Map<String, String> fileExtensionsMimeTypes = resolver
                .getCustomFileExtensionsToMimeTypesMap();
        assertTrue(fileExtensionsMimeTypes.containsKey("xml"));
        // assertEquals( "text/xml", fileExtensionsMimeTypes.get( "xml" ) );
        assertEquals("application/xml", fileExtensionsMimeTypes.get("xml"));
    }

    @Test
    public void testSetCustomMimeTypeWithMimeParameter() {
        CustomMimeTypeResolver resolver = new CustomMimeTypeResolver();

        resolver.setCustomMimeTypes(new String[] {"xml=text/xml;id=xml"});

        Map<String, List<String>> mimeTypesToFileExtensions = resolver
                .getCustomMimeTypesToFileExtensionsMap();
        assertTrue(mimeTypesToFileExtensions.containsKey("text/xml;id=xml"));
        assertFalse(mimeTypesToFileExtensions.containsKey("text/xml"));
        List<String> fileExtensions = mimeTypesToFileExtensions.get("text/xml;id=xml");
        assertTrue(fileExtensions.contains("xml"));

        Map<String, String> fileExtensionsMimeTypes = resolver
                .getCustomFileExtensionsToMimeTypesMap();
        assertTrue(fileExtensionsMimeTypes.containsKey("xml"));
        assertEquals("text/xml;id=xml", fileExtensionsMimeTypes.get("xml"));
    }

    @Test
    public void testGetFileExtensionForMimeType() throws Exception {
        CustomMimeTypeResolver resolver = new CustomMimeTypeResolver();
        resolver.setCustomMimeTypes(new String[] {"nitf=image/nitf"});

        String fileExtension = resolver.getFileExtensionForMimeType("image/nitf");
        assertEquals(".nitf", fileExtension);
    }

    @Test
    public void testGetFileExtensionForMimeType_MultipleMappings() throws Exception {
        CustomMimeTypeResolver resolver = new CustomMimeTypeResolver();
        resolver.setCustomMimeTypes(new String[] {"nitf=image/nitf", "ntf=image/nitf"});

        String fileExtension = resolver.getFileExtensionForMimeType("image/nitf");
        assertEquals(".nitf", fileExtension);
    }

    @Test
    public void testGetMimeTypeForFileExtension() throws Exception {
        CustomMimeTypeResolver resolver = new CustomMimeTypeResolver();
        resolver.setCustomMimeTypes(new String[] {"nitf=image/nitf", "ntf=image/nitf"});

        String mimeType = resolver.getMimeTypeForFileExtension("ntf");
        assertEquals("image/nitf", mimeType);
    }

    @Test
    public void testGetFileExtensionForNullMimeType() throws Exception {
        CustomMimeTypeResolver resolver = new CustomMimeTypeResolver();
        resolver.setCustomMimeTypes(new String[] {"nitf=image/nitf", "ntf=image/nitf"});

        String fileExtension = resolver.getFileExtensionForMimeType(null);
        assertEquals(null, fileExtension);
    }

    @Test
    public void testGetMimeTypeForNullFileExtension() throws Exception {
        CustomMimeTypeResolver resolver = new CustomMimeTypeResolver();
        resolver.setCustomMimeTypes(new String[] {"nitf=image/nitf", "ntf=image/nitf"});

        String mimeType = resolver.getMimeTypeForFileExtension(null);
        assertEquals(null, mimeType);
    }

    @Test
    public void testGetFileExtensionForEmptyMimeType() throws Exception {
        CustomMimeTypeResolver resolver = new CustomMimeTypeResolver();
        resolver.setCustomMimeTypes(new String[] {"nitf=image/nitf", "ntf=image/nitf"});

        String fileExtension = resolver.getFileExtensionForMimeType("");
        assertEquals(null, fileExtension);
    }

    @Test
    public void testGetMimeTypeForEmptyFileExtension() throws Exception {
        CustomMimeTypeResolver resolver = new CustomMimeTypeResolver();
        resolver.setCustomMimeTypes(new String[] {"nitf=image/nitf", "ntf=image/nitf"});

        String mimeType = resolver.getMimeTypeForFileExtension("");
        assertEquals(null, mimeType);
    }
    
    @Test
    public void testGetMimeTypes() throws Exception {
        String[] mimeTypes = new String[] {"abc=123/456"};
        CustomMimeTypeResolver resolver = new CustomMimeTypeResolver();
        resolver.setCustomMimeTypes(mimeTypes);
        mimeTypes[0] = "1234";
        
        String[] mimeTypeTest = resolver.getCustomMimeTypes();
        assertEquals(mimeTypeTest[0], "abc=123/456");
        mimeTypeTest[0] = "1234";
        assertEquals(resolver.getCustomMimeTypes()[0], "abc=123/456");
    }

}
