/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.catalog.content.plugin.clavin;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNot.not;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.activation.MimeType;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;

import com.bericotech.clavin.gazetteer.CountryCode;
import com.bericotech.clavin.gazetteer.GeoName;
import com.bericotech.clavin.resolver.ResolvedLocation;

import ddf.catalog.content.data.ContentItem;
import ddf.catalog.content.operation.CreateStorageResponse;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.plugin.PluginExecutionException;

public class TestClavinPlugin {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private ClavinPlugin clavinPlugin;

    private CreateStorageResponse mockStorageResponse;

    private ContentItem mockContentItem;

    private MimeType mockMimeType;

    private ClavinWrapper mockClavinWrapper;

    private Metacard mockMetacard;

    private GeoName mockGeoname;

    private ResolvedLocation mockLocation;

    private ArgumentCaptor<Attribute> attributeArgumentCaptor;

    private List<ResolvedLocation> locationList;

    private File emptyFile;

    @Before
    public void setUp() throws Exception {
        clavinPlugin = new ClavinPlugin();
        mockStorageResponse = mock(CreateStorageResponse.class);
        mockContentItem = mock(ContentItem.class);
        mockMimeType = mock(MimeType.class);
        mockClavinWrapper = mock(ClavinWrapper.class);
        mockMetacard = mock(Metacard.class);
        mockLocation = mock(ResolvedLocation.class);
        mockGeoname = mock(GeoName.class);
        emptyFile = temporaryFolder.newFile("empty");

        List<ContentItem> itemList = new ArrayList<>();
        itemList.add(mockContentItem);
        when(mockStorageResponse.getCreatedContentItems()).thenReturn(itemList);

        when(mockContentItem.getMimeType()).thenReturn(mockMimeType);
        when(mockContentItem.getFile()).thenReturn(emptyFile);
        when(mockContentItem.getMetacard()).thenReturn(mockMetacard);

        when(mockMimeType.getSubType()).thenReturn("msword");
        when(mockMimeType.getPrimaryType()).thenReturn("application");

        locationList = new ArrayList<>();
        locationList.add(mockLocation);
        when(mockLocation.getGeoname()).thenReturn(mockGeoname);
        when(mockLocation.getMatchedName()).thenReturn("xx");

        when(mockClavinWrapper.parse(any(String.class))).thenReturn(locationList);
        clavinPlugin.setClavinWrapper(mockClavinWrapper);

        List<CountryCode> countryCodes = new ArrayList<>();
        countryCodes.add(CountryCode.ZA);
        when(mockGeoname.getAlternateCountryCodes()).thenReturn(countryCodes);
        when(mockGeoname.getPrimaryCountryCode()).thenReturn(CountryCode.AD);
        when(mockGeoname.getLongitude()).thenReturn(1.61803);
        when(mockGeoname.getLatitude()).thenReturn(3.14159);

        attributeArgumentCaptor = ArgumentCaptor.forClass(Attribute.class);
    }

    @After
    public void afterClassTearDown() {
        FileUtils.deleteQuietly(emptyFile);
    }

    @Test
    public void processThrowsPluginExecutionExceptionWhenMimeTypeIsNull()
            throws IOException, PluginExecutionException {
        expectedEx.expect(PluginExecutionException.class);
        expectedEx.expectMessage("Failed to get mimetype from content item.");

        when(mockContentItem.getMimeType()).thenReturn(null);

        clavinPlugin.process(mockStorageResponse);
    }

    @Test
    public void processThrowsPluginExecutionExceptionWhenContentItemGetFileFails()
            throws IOException, PluginExecutionException {
        expectedEx.expect(PluginExecutionException.class);
        expectedEx.expectMessage("Failed to parse file with clavin.");

        when(mockContentItem.getFile()).thenThrow(IOException.class);

        clavinPlugin.process(mockStorageResponse);
    }

    @Test
    public void processAddsNothingToMetacardWhenNoLocationsResolved() throws Exception {
        when(mockClavinWrapper.parse(any(String.class))).thenReturn(Collections.EMPTY_LIST);

        clavinPlugin.process(mockStorageResponse);

        verify(mockLocation, never()).getGeoname();
        verify(mockMetacard, never()).setAttribute(any(AttributeImpl.class));
    }

    @Test
    public void processAddsNothingToMetacardWhenLocationIsBasedOnOneCharacter()
            throws PluginExecutionException {
        when(mockLocation.getMatchedName()).thenReturn("U");

        clavinPlugin.process(mockStorageResponse);

        verify(mockLocation, never()).getGeoname();
        verify(mockMetacard, never()).setAttribute(any(AttributeImpl.class));
    }

    @Test
    public void processThrowsPluginExecutionExeptionWhenClavinParseFails() throws Exception {
        expectedEx.expect(PluginExecutionException.class);
        expectedEx.expectMessage("Failed to parse file with clavin.");

        when(mockClavinWrapper.parse(any(String.class))).thenThrow(new Exception());
        clavinPlugin.process(mockStorageResponse);
    }

    @Test
    public void processAddsNoLocationWhenLatitudeIsNaN() throws PluginExecutionException {
        when(mockGeoname.getLatitude()).thenReturn(Double.NaN);
        clavinPlugin.process(mockStorageResponse);

        verify(mockMetacard, atMost(1)).setAttribute(attributeArgumentCaptor.capture());

        List<Attribute> attributeList = attributeArgumentCaptor.getAllValues();
        assertThat(attributeList.get(0)
                .getName(), is(not(Metacard.GEOGRAPHY)));
    }

    @Test
    public void processAddsNoLocationWhenLongitudeIsNaN() throws PluginExecutionException {
        when(mockGeoname.getLongitude()).thenReturn(Double.NaN);
        clavinPlugin.process(mockStorageResponse);

        verify(mockMetacard, atMost(1)).setAttribute(attributeArgumentCaptor.capture());

        List<Attribute> attributeList = attributeArgumentCaptor.getAllValues();
        assertThat(attributeList.get(0)
                .getName(), is(not(Metacard.GEOGRAPHY)));
    }

    @Test
    public void processAddsNoLocationWhenLatitudeIsInfinite() throws PluginExecutionException {
        when(mockGeoname.getLatitude()).thenReturn(Double.NEGATIVE_INFINITY);

        clavinPlugin.process(mockStorageResponse);

        verify(mockMetacard, atMost(1)).setAttribute(attributeArgumentCaptor.capture());

        List<Attribute> attributeList = attributeArgumentCaptor.getAllValues();
        assertThat(attributeList.get(0)
                .getName(), is(not(Metacard.GEOGRAPHY)));
    }

    @Test
    public void processAddsNoLocationWhenLongitudeIsInfinite() throws PluginExecutionException {
        when(mockGeoname.getLongitude()).thenReturn(Double.POSITIVE_INFINITY);

        clavinPlugin.process(mockStorageResponse);

        verify(mockMetacard, atMost(1)).setAttribute(attributeArgumentCaptor.capture());

        List<Attribute> attributeList = attributeArgumentCaptor.getAllValues();
        assertThat(attributeList.get(0)
                .getName(), is(not(Metacard.GEOGRAPHY)));
    }

    @Test
    public void processAddsNothing() throws PluginExecutionException {
        when(mockGeoname.getLongitude()).thenReturn(Double.POSITIVE_INFINITY); // skip setting location

        // return no country codes.
        when(mockGeoname.getAlternateCountryCodes()).thenReturn(Collections.EMPTY_LIST);
        when(mockGeoname.getPrimaryCountryCode()).thenReturn(null);

        clavinPlugin.process(mockStorageResponse);

        verify(mockMetacard, never()).setAttribute(any(Attribute.class));
    }

    @Test
    public void processAddsOnlySingleAlternateCountryCode() throws PluginExecutionException {
        when(mockGeoname.getLongitude()).thenReturn(Double.NaN); // skip setting location

        when(mockGeoname.getPrimaryCountryCode()).thenReturn(null);

        clavinPlugin.process(mockStorageResponse);

        verify(mockMetacard, atMost(1)).setAttribute(attributeArgumentCaptor.capture());

        List<Attribute> attributeList = attributeArgumentCaptor.getAllValues();
        assertThat(attributeList.get(0)
                .getName(), is("country-codes"));
        assertThat(attributeList.get(0)
                .getValue(), is(CountryCode.ZA.toString()));
    }

    @Test
    public void processAddsOnlyPrimaryCountryCode() throws PluginExecutionException {
        when(mockGeoname.getLongitude()).thenReturn(Double.NaN); // skip setting location

        when(mockGeoname.getAlternateCountryCodes()).thenReturn(Collections.EMPTY_LIST);
        clavinPlugin.process(mockStorageResponse);

        verify(mockMetacard, atMost(1)).setAttribute(attributeArgumentCaptor.capture());

        List<Attribute> attributeList = attributeArgumentCaptor.getAllValues();
        assertThat(attributeList.get(0)
                .getName(), is("country-codes"));
        assertThat(attributeList.get(0)
                .getValue(), is(CountryCode.AD.toString()));
    }

    @Test
    public void processAddsSinglePointLocation() throws PluginExecutionException {
        clavinPlugin.process(mockStorageResponse);

        verify(mockMetacard, atMost(2)).setAttribute(attributeArgumentCaptor.capture());

        List<Attribute> attributeList = attributeArgumentCaptor.getAllValues();
        assertThat(attributeList.get(0)
                .getName(), is(Metacard.GEOGRAPHY));
        assertThat(attributeList.get(0)
                .getValue(), is("POINT (1.61803 3.14159)"));
    }

    @Test
    public void processAddsMultiPointLocation() throws PluginExecutionException {
        ResolvedLocation mockLocation2 = mock(ResolvedLocation.class);
        locationList.add(mockLocation2);

        GeoName mockGeoname2 = mock(GeoName.class);
        when(mockLocation2.getGeoname()).thenReturn(mockGeoname2);
        when(mockLocation2.getMatchedName()).thenReturn("zz");

        when(mockGeoname2.getLongitude()).thenReturn(9.11);
        when(mockGeoname2.getLatitude()).thenReturn(911.1991);

        clavinPlugin.process(mockStorageResponse);

        verify(mockMetacard, atMost(2)).setAttribute(attributeArgumentCaptor.capture());

        List<Attribute> attributeList = attributeArgumentCaptor.getAllValues();
        assertThat(attributeList.get(0)
                .getName(), is(Metacard.GEOGRAPHY));
        assertThat(attributeList.get(0)
                .getValue(), is("MULTIPOINT ((1.61803 3.14159), (9.11 911.1991))"));
    }

}
