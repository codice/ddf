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
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import ddf.action.Action;
import ddf.action.ActionProvider;
import org.codice.ddf.configuration.ConfigurationManager;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.github.jknack.handlebars.Options;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;

public class TestDescriptionTemplateHelper {

    private static final String ID = "id";
    private static final String SOURCE = "sourceId";

    private static ActionProvider mockActionProvider;
    private static Action mockAction;

    private static final String ACTION_URL = "http://example.com/source/id?transform=resource";

    private static final String DEFAULT_URI = "http://example.com/something/different";

    private static DescriptionTemplateHelper helper;

    @BeforeClass
    public static void setUp() throws MalformedURLException {
        mockActionProvider = mock(ActionProvider.class);
        mockAction = mock(Action.class);
        when(mockActionProvider.getAction(any(Metacard.class))).thenReturn(mockAction);
        when(mockAction.getUrl()).thenReturn(new URL(ACTION_URL));
        helper = new DescriptionTemplateHelper(mockActionProvider);
    }
    
    @Test
    public void testUnsetEffectiveTime() throws ParseException {
        MetacardImpl metacard = new MetacardImpl();

        String effectiveTime = helper.prettyPrint(metacard.getAttribute(Metacard.EFFECTIVE),
                metacard.getMetacardType().getAttributeDescriptor(Metacard.EFFECTIVE).getType()
                        .getAttributeFormat());
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        assertNotNull(effectiveTime);
    }
    
    @Test
    public void testSetEffectiveTime() throws ParseException {
        MetacardImpl metacard = new MetacardImpl();
        Calendar calendar = new GregorianCalendar();
        calendar.set(Calendar.YEAR, 1988);
        calendar.set(Calendar.MILLISECOND, 0);
        Date date = calendar.getTime();
        metacard.setEffectiveDate(date);
        
        String effectiveTime = helper.prettyPrint(metacard.getAttribute(Metacard.EFFECTIVE), metacard.getMetacardType().getAttributeDescriptor(Metacard.EFFECTIVE).getType().getAttributeFormat());
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ");
        Date retrievedDate = dateFormat.parse(effectiveTime);
        assertNotNull(effectiveTime);
        assertThat(effectiveTime, is(dateFormat.format(date)));
        assertThat(date, is(dateFormat.parse(effectiveTime)));
        assertEquals(date, retrievedDate);        
    }

    @Test
    public void testNoActionProviderResourceUrl() throws URISyntaxException {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setSourceId(SOURCE);
        metacard.setId(ID);
        metacard.setResourceURI(new URI(DEFAULT_URI));
        DescriptionTemplateHelper noActionHelper = new DescriptionTemplateHelper(null);
        String url = noActionHelper.resourceUrl(metacard);
        assertThat(url, is(DEFAULT_URI));
    }

    @Test
    public void testActionProviderResourceUrl() {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setSourceId(SOURCE);
        metacard.setId(ID);
        
        String url = helper.resourceUrl(metacard);
        assertThat(url, is(ACTION_URL));
    }

    @Test
    public void testNoThumbnailHasThumbnail() throws IOException {
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
        byte[] expected = new byte[] {1, 2, 3};
        MetacardImpl metacard = new MetacardImpl();
        metacard.setThumbnail(expected);
        
        String result = helper.base64Thumbnail(metacard);
        
        byte[] actual = DatatypeConverter.parseBase64Binary(result);
        
        assertArrayEquals(expected, actual);        
    }

    @Test
    public void testResourceSizeStringNoneSet() {
        MetacardImpl metacard = new MetacardImpl();
        
        String result = helper.resourceSizeString(metacard);
        
        assertNull(result);        
    }

    @Test
    public void testResourceSizeStringNASet() {
        MetacardImpl metacard = new MetacardImpl();
        metacard.setResourceSize("N/A");
        
        String result = helper.resourceSizeString(metacard);
        
        assertNull(result);        
    }

    @Test
    public void testResourceSizeStringNonNumericSet() {
        String size = "foo";

        MetacardImpl metacard = new MetacardImpl();
        metacard.setResourceSize(size);
        
        String result = helper.resourceSizeString(metacard);
        
        assertEquals(size, result);        
    }


    @Test
    public void testResourceSizeStringNumericSet() {
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
