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
package org.codice.ddf.spatial.kml.transformer;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import org.codice.ddf.configuration.ConfigurationManager;
import org.junit.Test;

import com.github.jknack.handlebars.Options;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;

public class TestDescriptionTemplateHelper {

    private static final String ID = "id";
    private static final String SOURCE = "sourceId";
    
    @Test
    public void testUnsetEffectiveTime() throws ParseException {
        DescriptionTemplateHelper helper = new DescriptionTemplateHelper(null, null);
        
        Metacard metacard = new MetacardImpl();
        
        String effectiveTime = helper.effectiveTime(metacard);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        assertNotNull(dateFormat.parse(effectiveTime));        
    }
    
    @Test
    public void testSetEffectiveTime() throws ParseException {
        DescriptionTemplateHelper helper = new DescriptionTemplateHelper(null, null);
        
        MetacardImpl metacard = new MetacardImpl();
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, 1988);
        calendar.set(Calendar.MILLISECOND, 0);
        Date date = calendar.getTime();
        metacard.setEffectiveDate(date);
        
        String effectiveTime = helper.effectiveTime(metacard);
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        Date retrievedDate = dateFormat.parse(effectiveTime); 
        assertEquals(date, retrievedDate);        
    }

    @Test
    public void testNoContextMetacardUrl() {
        DescriptionTemplateHelper helper = new DescriptionTemplateHelper(null, null);
        
        MetacardImpl metacard = new MetacardImpl();
        metacard.setSourceId(SOURCE);
        metacard.setId(ID);
        
        String url = helper.metacardUrl(metacard);
        String expected = "/catalog/sources/" + SOURCE + "/" + ID + "?transform=html";
        assertEquals(expected, url);        
    }

    @Test
    public void testUrlContextMetacardUrl() {
        
        String callingUrl = "http://someHost:9898/anyContext";
        DescriptionTemplateHelper helper = new DescriptionTemplateHelper(callingUrl, null);
        
        MetacardImpl metacard = new MetacardImpl();
        metacard.setSourceId(SOURCE);
        metacard.setId(ID);
        
        String url = helper.metacardUrl(metacard);
        String expected = "http://someHost:9898/catalog/sources/" + SOURCE + "/" + ID + "?transform=html";
        assertEquals(expected, url);        
    }

    @Test
    public void testPlatformContextMetacardUrl() {
        Map<String, String> platformContext = new HashMap<String, String>();
        platformContext.put(ConfigurationManager.HOST, "someHost");
        platformContext.put(ConfigurationManager.PORT, "9898");
        platformContext.put(ConfigurationManager.PROTOCOL, "http://");
        platformContext.put(ConfigurationManager.SERVICES_CONTEXT_ROOT, "/services");
        
        DescriptionTemplateHelper helper = new DescriptionTemplateHelper(null, platformContext);
        
        MetacardImpl metacard = new MetacardImpl();
        metacard.setSourceId(SOURCE);
        metacard.setId(ID);
        
        String url = helper.metacardUrl(metacard);
        String expected = "http://someHost:9898/services/catalog/sources/" + SOURCE + "/" + ID + "?transform=html";
        assertEquals(expected, url);
    }

    @Test
    public void testPlatformandUrlContextMetacardUrl() {
        String callingUrl = "http://someHost:9898/anyContext";
        Map<String, String> platformContext = new HashMap<String, String>();
        
        platformContext.put(ConfigurationManager.HOST, "someOtherHost");
        platformContext.put(ConfigurationManager.PORT, "1111");
        platformContext.put(ConfigurationManager.PROTOCOL, "http://");
        platformContext.put(ConfigurationManager.SERVICES_CONTEXT_ROOT, "/services");
        
        DescriptionTemplateHelper helper = new DescriptionTemplateHelper(callingUrl, platformContext);
        
        MetacardImpl metacard = new MetacardImpl();
        metacard.setSourceId(SOURCE);
        metacard.setId(ID);
        
        String url = helper.metacardUrl(metacard);
        String expected = "http://someHost:9898/services/catalog/sources/" + SOURCE + "/" + ID + "?transform=html";
        assertEquals(expected, url);
    }

    @Test
    public void testNoContextResourceUrl() {
        DescriptionTemplateHelper helper = new DescriptionTemplateHelper(null, null);
        
        MetacardImpl metacard = new MetacardImpl();
        metacard.setSourceId(SOURCE);
        metacard.setId(ID);
        
        String url = helper.resourceUrl(metacard);
        String expected = "/catalog/sources/" + SOURCE + "/" + ID + "?transform=resource";
        assertEquals(expected, url);        
    }

    @Test
    public void testUrlContextResourceUrl() {
        
        String callingUrl = "http://someHost:9898/anyContext";
        DescriptionTemplateHelper helper = new DescriptionTemplateHelper(callingUrl, null);
        
        MetacardImpl metacard = new MetacardImpl();
        metacard.setSourceId(SOURCE);
        metacard.setId(ID);
        
        String url = helper.resourceUrl(metacard);
        String expected = "http://someHost:9898/catalog/sources/" + SOURCE + "/" + ID + "?transform=resource";
        assertEquals(expected, url);        
    }

    @Test
    public void testPlatformContextResourceUrl() {
        Map<String, String> platformContext = new HashMap<String, String>();
        platformContext.put(ConfigurationManager.HOST, "someHost");
        platformContext.put(ConfigurationManager.PORT, "9898");
        platformContext.put(ConfigurationManager.PROTOCOL, "http://");
        platformContext.put(ConfigurationManager.SERVICES_CONTEXT_ROOT, "/services");
        
        DescriptionTemplateHelper helper = new DescriptionTemplateHelper(null, platformContext);
        
        MetacardImpl metacard = new MetacardImpl();
        metacard.setSourceId(SOURCE);
        metacard.setId(ID);
        
        String url = helper.resourceUrl(metacard);
        String expected = "http://someHost:9898/services/catalog/sources/" + SOURCE + "/" + ID + "?transform=resource";
        assertEquals(expected, url);
    }

    @Test
    public void testPlatformandUrlContextResourceUrl() {
        String callingUrl = "http://someHost:9898/anyContext";
        Map<String, String> platformContext = new HashMap<String, String>();
       
        platformContext.put(ConfigurationManager.HOST, "someOtherHost");
        platformContext.put(ConfigurationManager.PORT, "1111");
        platformContext.put(ConfigurationManager.PROTOCOL, "http://");
        platformContext.put(ConfigurationManager.SERVICES_CONTEXT_ROOT, "/services");
        
        DescriptionTemplateHelper helper = new DescriptionTemplateHelper(callingUrl, platformContext);
        
        MetacardImpl metacard = new MetacardImpl();
        metacard.setSourceId(SOURCE);
        metacard.setId(ID);
        
        String url = helper.resourceUrl(metacard);
        String expected = "http://someHost:9898/services/catalog/sources/" + SOURCE + "/" + ID + "?transform=resource";
        assertEquals(expected, url);
    }

    @Test
    public void testNoThumbnailHasThumbnail() throws IOException {
        DescriptionTemplateHelper helper = new DescriptionTemplateHelper(null, null);

        String ifOption = "if";
        String elseOption = "else";
        Metacard metacard = new MetacardImpl();

        Options mockOptions = mock(Options.class);
        when(mockOptions.fn()).thenReturn(ifOption);
        when(mockOptions.inverse()).thenReturn(elseOption);
        
        String result = helper.hasThumbnail(metacard, mockOptions).toString();
        assertEquals(elseOption, result);
    }

    @Test
    public void testThumbnailHasThumbnail() throws IOException {
        DescriptionTemplateHelper helper = new DescriptionTemplateHelper(null, null);

        String ifOption = "if";
        String elseOption = "else";
        MetacardImpl metacard = new MetacardImpl();
        metacard.setThumbnail(new byte[] {1, 2, 3});

        Options mockOptions = mock(Options.class);
        when(mockOptions.fn()).thenReturn(ifOption);
        when(mockOptions.inverse()).thenReturn(elseOption);
        
        String result = helper.hasThumbnail(metacard, mockOptions).toString();
        assertEquals(ifOption, result);
        
    }

    @Test
    public void testBase64Thumbnail() {
        DescriptionTemplateHelper helper = new DescriptionTemplateHelper(null, null);

        byte[] expected = new byte[] {1, 2, 3};
        MetacardImpl metacard = new MetacardImpl();
        metacard.setThumbnail(expected);
        
        String result = helper.base64Thumbnail(metacard);
        
        byte[] actual = DatatypeConverter.parseBase64Binary(result);
        
        assertArrayEquals(expected, actual);        
    }

    @Test
    public void testResourceSizeStringNoneSet() {
        DescriptionTemplateHelper helper = new DescriptionTemplateHelper(null, null);

        MetacardImpl metacard = new MetacardImpl();
        
        String result = helper.resourceSizeString(metacard);
        
        assertNull(result);        
    }

    @Test
    public void testResourceSizeStringNASet() {
        DescriptionTemplateHelper helper = new DescriptionTemplateHelper(null, null);

        MetacardImpl metacard = new MetacardImpl();
        metacard.setResourceSize("N/A");
        
        String result = helper.resourceSizeString(metacard);
        
        assertNull(result);        
    }

    @Test
    public void testResourceSizeStringNonNumericSet() {
        DescriptionTemplateHelper helper = new DescriptionTemplateHelper(null, null);
        String size = "foo";

        MetacardImpl metacard = new MetacardImpl();
        metacard.setResourceSize(size);
        
        String result = helper.resourceSizeString(metacard);
        
        assertEquals(size, result);        
    }


    @Test
    public void testResourceSizeStringNumericSet() {
        DescriptionTemplateHelper helper = new DescriptionTemplateHelper(null, null);

        Map<String, String> sizes = new HashMap<String, String>();
        sizes.put("1", "1 B");
        sizes.put("76", "76 B");
        sizes.put("1024", "1 KB");
        sizes.put("4096", "4 KB");
        sizes.put("1100000", "1 MB");
        sizes.put("2200000000", "2 GB");
        sizes.put("3300000000000", "3 TB");

        MetacardImpl metacard = new MetacardImpl();
        String result;
        for (String val : sizes.keySet()) {
            metacard.setResourceSize(val);
            result = helper.resourceSizeString(metacard);        
            assertEquals(sizes.get(val), result);        
        }
    }
}
