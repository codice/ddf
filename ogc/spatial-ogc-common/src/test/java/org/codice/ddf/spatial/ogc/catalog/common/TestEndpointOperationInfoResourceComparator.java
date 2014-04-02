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
package org.codice.ddf.spatial.ogc.catalog.common;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;
import org.junit.Before;
import org.junit.Test;

public class TestEndpointOperationInfoResourceComparator {

    private OperationResourceInfo getCapabilities;

    private OperationResourceInfo describeFeatureType;

    private OperationResourceInfo unknownService;

    private OperationResourceInfo unknownOperation;

    private Message mockMessage = mock(Message.class);

    private static final String DESCRIBE_FEATURES = "describeFeatureType";
    private static final String GET_CAPABILITIES = "getCapabilities";
    
    @Before
    public void setUp() throws NoSuchMethodException {
        getCapabilities = new OperationResourceInfo(getClass().getMethod(GET_CAPABILITIES), null);
        describeFeatureType = new OperationResourceInfo(getClass().getMethod(DESCRIBE_FEATURES), null);
        unknownService = new OperationResourceInfo(getClass().getMethod("unknownService"), null);
        unknownOperation = new OperationResourceInfo(getClass().getMethod("unknownOperation"), null);
    }

    @Test
    public void testCompareRequestMatchesFirst() {
        when(mockMessage.get(Message.HTTP_REQUEST_METHOD)).thenReturn(
                EndpointOperationInfoResourceComparator.HTTP_GET);
        when(mockMessage.get(Message.QUERY_STRING)).thenReturn(
                EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + GET_CAPABILITIES);
        EndpointOperationInfoResourceComparator comparator = new EndpointOperationInfoResourceComparator();
        assertEquals(-1, comparator.compare(getCapabilities, describeFeatureType, mockMessage));
    }

    @Test
    public void testCompareRequestMatchesSecond() {
        when(mockMessage.get(Message.HTTP_REQUEST_METHOD)).thenReturn(
                EndpointOperationInfoResourceComparator.HTTP_GET);
        when(mockMessage.get(Message.QUERY_STRING)).thenReturn(
                EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + DESCRIBE_FEATURES);
        EndpointOperationInfoResourceComparator comparator = new EndpointOperationInfoResourceComparator();
        assertEquals(1, comparator.compare(getCapabilities, describeFeatureType, mockMessage));
    }

    @Test
    public void testCompareRequestMatchesNeither() {
        when(mockMessage.get(Message.HTTP_REQUEST_METHOD)).thenReturn(
                EndpointOperationInfoResourceComparator.HTTP_GET);
        when(mockMessage.get(Message.QUERY_STRING)).thenReturn(
                EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=getFeature");
        EndpointOperationInfoResourceComparator comparator = new EndpointOperationInfoResourceComparator();
        assertEquals(0, comparator.compare(getCapabilities, describeFeatureType, mockMessage));
    }

    @Test
    public void testCompareUnknownHttpMethod() {
        when(mockMessage.get(Message.HTTP_REQUEST_METHOD)).thenReturn("WFS");
        when(mockMessage.get(Message.QUERY_STRING)).thenReturn(
                EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + GET_CAPABILITIES);
        EndpointOperationInfoResourceComparator comparator = new EndpointOperationInfoResourceComparator();
        assertEquals(0, comparator.compare(getCapabilities, describeFeatureType, mockMessage));
    }

    @Test
    public void testCompareUnknownRequestType() {
        when(mockMessage.get(Message.HTTP_REQUEST_METHOD)).thenReturn(
                EndpointOperationInfoResourceComparator.HTTP_GET);
        when(mockMessage.get(Message.QUERY_STRING)).thenReturn(
                EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=badFunction");
        EndpointOperationInfoResourceComparator comparator = new EndpointOperationInfoResourceComparator();
        assertEquals(0, comparator.compare(getCapabilities, describeFeatureType, mockMessage));
    }

    @Test
    public void testCompareNullOper1() {
        EndpointOperationInfoResourceComparator comparator = new EndpointOperationInfoResourceComparator();
        assertEquals(0, comparator.compare(null, describeFeatureType, mockMessage));
    }

    @Test
    public void testCompareNullOper2() {
        EndpointOperationInfoResourceComparator comparator = new EndpointOperationInfoResourceComparator();
        assertEquals(0, comparator.compare(getCapabilities, null, mockMessage));
    }

    @Test
    public void testCompareNullMessage() {
        EndpointOperationInfoResourceComparator comparator = new EndpointOperationInfoResourceComparator();
        assertEquals(0, comparator.compare(getCapabilities, describeFeatureType, null));
    }

    @Test
    public void testCompareUnknownServiceToMatchingOperationWhenNoServiceSet() {
        when(mockMessage.get(Message.HTTP_REQUEST_METHOD)).thenReturn(
                EndpointOperationInfoResourceComparator.HTTP_GET);
        when(mockMessage.get(Message.QUERY_STRING)).thenReturn(
                EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + DESCRIBE_FEATURES);
        when(mockMessage.get(Message.QUERY_STRING)).thenReturn(
                EndpointOperationInfoResourceComparator.SERVICE_PARAM + "=noGood&" + 
                EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + DESCRIBE_FEATURES);
        EndpointOperationInfoResourceComparator comparator = new EndpointOperationInfoResourceComparator();
        assertEquals(1, comparator.compare(unknownService, describeFeatureType, mockMessage));
    }

    @Test
    public void testCompareUnknownServiceToMatchingOperationWhenServiceMatches() {
        when(mockMessage.get(Message.HTTP_REQUEST_METHOD)).thenReturn(
                EndpointOperationInfoResourceComparator.HTTP_GET);
        when(mockMessage.get(Message.QUERY_STRING)).thenReturn(
                EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + DESCRIBE_FEATURES);
        when(mockMessage.get(Message.QUERY_STRING)).thenReturn(
                EndpointOperationInfoResourceComparator.SERVICE_PARAM + "=CSW&" + 
                EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + DESCRIBE_FEATURES);
        EndpointOperationInfoResourceComparator comparator = new EndpointOperationInfoResourceComparator("CSW");
        assertEquals(1, comparator.compare(unknownService, describeFeatureType, mockMessage));
    }

    @Test
    public void testCompareUnknownServiceToMatchingOperationWhenServiceDoesNotMatch() {
        when(mockMessage.get(Message.HTTP_REQUEST_METHOD)).thenReturn(
                EndpointOperationInfoResourceComparator.HTTP_GET);
        when(mockMessage.get(Message.QUERY_STRING)).thenReturn(
                EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + DESCRIBE_FEATURES);
        when(mockMessage.get(Message.QUERY_STRING)).thenReturn(
                EndpointOperationInfoResourceComparator.SERVICE_PARAM + "=noGood&" + 
                EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + DESCRIBE_FEATURES);
        EndpointOperationInfoResourceComparator comparator = new EndpointOperationInfoResourceComparator("CSW");
        assertEquals(-1, comparator.compare(unknownService, describeFeatureType, mockMessage));
    }

    @Test
    public void testCompareUnknownOperationToMatchingOperation() {
        when(mockMessage.get(Message.HTTP_REQUEST_METHOD)).thenReturn(
                EndpointOperationInfoResourceComparator.HTTP_GET);
        when(mockMessage.get(Message.QUERY_STRING)).thenReturn(
                EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + DESCRIBE_FEATURES);
        when(mockMessage.get(Message.QUERY_STRING)).thenReturn(
                EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + DESCRIBE_FEATURES);
        EndpointOperationInfoResourceComparator comparator = new EndpointOperationInfoResourceComparator();
        assertEquals(1, comparator.compare(unknownOperation, describeFeatureType, mockMessage));
    }

    @Test
    public void testCompareUnknownOperationToUnMatchedOperation() {
        when(mockMessage.get(Message.HTTP_REQUEST_METHOD)).thenReturn(
                EndpointOperationInfoResourceComparator.HTTP_GET);
        when(mockMessage.get(Message.QUERY_STRING)).thenReturn(
                EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + DESCRIBE_FEATURES);
        when(mockMessage.get(Message.QUERY_STRING)).thenReturn(
                EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + DESCRIBE_FEATURES);
        EndpointOperationInfoResourceComparator comparator = new EndpointOperationInfoResourceComparator();
        assertEquals(-1, comparator.compare(unknownOperation, getCapabilities, mockMessage));
    }

    // Allows us to create a "mockMethod" with the name of the method
    public void getCapabilities() {
    }

    // Allows us to create a "mockMethod" with the name of the method
    public void describeFeatureType() {
    }

    // Allows us to create a "mockMethod" with the name of the method
    public void unknownOperation() {
    }

    // Allows us to create a "mockMethod" with the name of the method
    public void unknownService() {
    }

}
