/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.spatial.ogc.catalog.common;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.message.Message;
import org.junit.Before;
import org.junit.Test;

public class EndpointOperationInfoResourceComparatorTest {

  private static final String SECOND_OPERATION = "secondOperation";

  private static final String FIRST_OPERATION = "firstOperation";

  private static final String UNKNOWN_SERVICE = "unknownService";

  private static final String UNKNOWN_OPERATION = "unknownOperation";

  private OperationResourceInfo firstOperation;

  private OperationResourceInfo secondOperation;

  private OperationResourceInfo unknownService;

  private OperationResourceInfo unknownOperation;

  private Message mockMessage = mock(Message.class);

  private static final String FIRST_OPERATION_BODY =
      "<csw:firstOperation resultType=\"results\" outputFormat=\"application/xml\" outputSchema=\"http://www.opengis.net/cat/csw/2.0.2\" startPosition=\"1\" maxRecords=\"10\" service=\"CSW\" version=\"2.0.2\" xmlns:ns2=\"http://www.opengis.net/ogc\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.w3.org/1999/xlink\" xmlns:ns3=\"http://www.opengis.net/gml\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.opengis.net/ows\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">\n"
          + "    <ns10:Query typeNames=\"csw:Record\" xmlns=\"\" xmlns:ns10=\"http://www.opengis.net/cat/csw/2.0.2\">\n"
          + "        <ns10:ElementSetName>full</ns10:ElementSetName>\n"
          + "        <ns10:Constraint version=\"1.1.0\">\n"
          + "            <ns2:Filter>\n"
          + "                <ns2:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">\n"
          + "                  <ns2:PropertyName>title</ns2:PropertyName>\n"
          + "                    <ns2:Literal>Aliquam</ns2:Literal>\n"
          + "                </ns2:PropertyIsLike>\n"
          + "            </ns2:Filter>\n"
          + "        </ns10:Constraint>\n"
          + "    </ns10:Query>\n"
          + "</csw:firstOperation>";

  private static final String SECOND_OPERATION_BODY =
      "<csw:secondOperation resultType=\"results\" outputFormat=\"application/xml\" outputSchema=\"http://www.opengis.net/cat/csw/2.0.2\" startPosition=\"1\" maxRecords=\"10\" service=\"CSW\" version=\"2.0.2\" xmlns:ns2=\"http://www.opengis.net/ogc\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\" xmlns:ns4=\"http://www.w3.org/1999/xlink\" xmlns:ns3=\"http://www.opengis.net/gml\" xmlns:ns9=\"http://www.w3.org/2001/SMIL20/Language\" xmlns:ns5=\"http://www.opengis.net/ows\" xmlns:ns6=\"http://purl.org/dc/elements/1.1/\" xmlns:ns7=\"http://purl.org/dc/terms/\" xmlns:ns8=\"http://www.w3.org/2001/SMIL20/\">\n"
          + "    <ns10:Query typeNames=\"csw:Record\" xmlns=\"\" xmlns:ns10=\"http://www.opengis.net/cat/csw/2.0.2\">\n"
          + "        <ns10:ElementSetName>full</ns10:ElementSetName>\n"
          + "        <ns10:Constraint version=\"1.1.0\">\n"
          + "            <ns2:Filter>\n"
          + "                <ns2:PropertyIsLike wildCard=\"*\" singleChar=\"#\" escapeChar=\"!\">\n"
          + "                  <ns2:PropertyName>title</ns2:PropertyName>\n"
          + "                    <ns2:Literal>Aliquam</ns2:Literal>\n"
          + "                </ns2:PropertyIsLike>\n"
          + "            </ns2:Filter>\n"
          + "        </ns10:Constraint>\n"
          + "    </ns10:Query>\n"
          + "</csw:secondOperation>";

  @Before
  public void setUp() throws NoSuchMethodException {
    firstOperation = new OperationResourceInfo(getClass().getMethod(FIRST_OPERATION), null);
    secondOperation = new OperationResourceInfo(getClass().getMethod(SECOND_OPERATION), null);
    unknownService = new OperationResourceInfo(getClass().getMethod(UNKNOWN_SERVICE), null);
    unknownOperation = new OperationResourceInfo(getClass().getMethod(UNKNOWN_OPERATION), null);
  }

  @Test
  public void testCompareRequestMatchesFirst() {
    when(mockMessage.get(Message.HTTP_REQUEST_METHOD))
        .thenReturn(EndpointOperationInfoResourceComparator.HTTP_GET);
    when(mockMessage.get(Message.QUERY_STRING))
        .thenReturn(EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + FIRST_OPERATION);
    EndpointOperationInfoResourceComparator comparator =
        new EndpointOperationInfoResourceComparator();
    assertEquals(-1, comparator.compare(firstOperation, secondOperation, mockMessage));
  }

  @Test
  public void testCompareRequestMatchesSecond() {
    when(mockMessage.get(Message.HTTP_REQUEST_METHOD))
        .thenReturn(EndpointOperationInfoResourceComparator.HTTP_GET);
    when(mockMessage.get(Message.QUERY_STRING))
        .thenReturn(EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + SECOND_OPERATION);
    EndpointOperationInfoResourceComparator comparator =
        new EndpointOperationInfoResourceComparator();
    assertEquals(1, comparator.compare(firstOperation, secondOperation, mockMessage));
  }

  @Test
  public void testCompareRequestMatchesNeither() {
    when(mockMessage.get(Message.HTTP_REQUEST_METHOD))
        .thenReturn(EndpointOperationInfoResourceComparator.HTTP_GET);
    when(mockMessage.get(Message.QUERY_STRING))
        .thenReturn(EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=getFeature");
    EndpointOperationInfoResourceComparator comparator =
        new EndpointOperationInfoResourceComparator();
    assertEquals(-1, comparator.compare(firstOperation, secondOperation, mockMessage));
  }

  @Test
  public void testCompareUnknownHttpMethod() {
    when(mockMessage.get(Message.HTTP_REQUEST_METHOD)).thenReturn("WFS");
    when(mockMessage.get(Message.QUERY_STRING))
        .thenReturn(EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + FIRST_OPERATION);
    EndpointOperationInfoResourceComparator comparator =
        new EndpointOperationInfoResourceComparator();
    assertEquals(-1, comparator.compare(firstOperation, secondOperation, mockMessage));
  }

  @Test
  public void testCompareUnknownRequestType() {
    when(mockMessage.get(Message.HTTP_REQUEST_METHOD))
        .thenReturn(EndpointOperationInfoResourceComparator.HTTP_GET);
    when(mockMessage.get(Message.QUERY_STRING))
        .thenReturn(EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=badFunction");
    EndpointOperationInfoResourceComparator comparator =
        new EndpointOperationInfoResourceComparator();
    assertEquals(-1, comparator.compare(firstOperation, secondOperation, mockMessage));
  }

  @Test
  public void testCompareNullOper1() {
    EndpointOperationInfoResourceComparator comparator =
        new EndpointOperationInfoResourceComparator();
    assertEquals(-1, comparator.compare(null, secondOperation, mockMessage));
  }

  @Test
  public void testCompareNullOper2() {
    EndpointOperationInfoResourceComparator comparator =
        new EndpointOperationInfoResourceComparator();
    assertEquals(-1, comparator.compare(firstOperation, null, mockMessage));
  }

  @Test
  public void testCompareNullMessage() {
    EndpointOperationInfoResourceComparator comparator =
        new EndpointOperationInfoResourceComparator();
    assertEquals(-1, comparator.compare(firstOperation, secondOperation, null));
  }

  @Test
  public void testCompareUnknownServiceToMatchingOperationWhenNoServiceSet() {
    when(mockMessage.get(Message.HTTP_REQUEST_METHOD))
        .thenReturn(EndpointOperationInfoResourceComparator.HTTP_GET);
    when(mockMessage.get(Message.QUERY_STRING))
        .thenReturn(EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + SECOND_OPERATION);
    when(mockMessage.get(Message.QUERY_STRING))
        .thenReturn(
            EndpointOperationInfoResourceComparator.SERVICE_PARAM
                + "=noGood&"
                + EndpointOperationInfoResourceComparator.REQUEST_PARAM
                + "="
                + SECOND_OPERATION);
    EndpointOperationInfoResourceComparator comparator =
        new EndpointOperationInfoResourceComparator();
    assertEquals(1, comparator.compare(unknownService, secondOperation, mockMessage));
  }

  @Test
  public void testCompareUnknownServiceToMatchingOperationWhenServiceMatches() {
    when(mockMessage.get(Message.HTTP_REQUEST_METHOD))
        .thenReturn(EndpointOperationInfoResourceComparator.HTTP_GET);
    when(mockMessage.get(Message.QUERY_STRING))
        .thenReturn(EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + SECOND_OPERATION);
    when(mockMessage.get(Message.QUERY_STRING))
        .thenReturn(
            EndpointOperationInfoResourceComparator.SERVICE_PARAM
                + "=CSW&"
                + EndpointOperationInfoResourceComparator.REQUEST_PARAM
                + "="
                + SECOND_OPERATION);
    EndpointOperationInfoResourceComparator comparator =
        new EndpointOperationInfoResourceComparator("CSW");
    assertEquals(1, comparator.compare(unknownService, secondOperation, mockMessage));
  }

  @Test
  public void testCompareUnknownServiceToMatchingOperationWhenServiceDoesNotMatch() {
    when(mockMessage.get(Message.HTTP_REQUEST_METHOD))
        .thenReturn(EndpointOperationInfoResourceComparator.HTTP_GET);
    when(mockMessage.get(Message.QUERY_STRING))
        .thenReturn(EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + SECOND_OPERATION);
    when(mockMessage.get(Message.QUERY_STRING))
        .thenReturn(
            EndpointOperationInfoResourceComparator.SERVICE_PARAM
                + "=noGood&"
                + EndpointOperationInfoResourceComparator.REQUEST_PARAM
                + "="
                + SECOND_OPERATION);
    EndpointOperationInfoResourceComparator comparator =
        new EndpointOperationInfoResourceComparator("CSW");
    assertEquals(-1, comparator.compare(unknownService, secondOperation, mockMessage));
  }

  @Test
  public void testCompareUnknownOperationToMatchingOperation() {
    when(mockMessage.get(Message.HTTP_REQUEST_METHOD))
        .thenReturn(EndpointOperationInfoResourceComparator.HTTP_GET);
    when(mockMessage.get(Message.QUERY_STRING))
        .thenReturn(EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + SECOND_OPERATION);
    when(mockMessage.get(Message.QUERY_STRING))
        .thenReturn(EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + SECOND_OPERATION);
    EndpointOperationInfoResourceComparator comparator =
        new EndpointOperationInfoResourceComparator();
    assertEquals(1, comparator.compare(unknownOperation, secondOperation, mockMessage));
  }

  @Test
  public void testCompareUnknownOperationToUnMatchedOperation() {
    when(mockMessage.get(Message.HTTP_REQUEST_METHOD))
        .thenReturn(EndpointOperationInfoResourceComparator.HTTP_GET);
    when(mockMessage.get(Message.QUERY_STRING))
        .thenReturn(EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + SECOND_OPERATION);
    when(mockMessage.get(Message.QUERY_STRING))
        .thenReturn(EndpointOperationInfoResourceComparator.REQUEST_PARAM + "=" + SECOND_OPERATION);
    EndpointOperationInfoResourceComparator comparator =
        new EndpointOperationInfoResourceComparator();
    assertEquals(-1, comparator.compare(unknownOperation, firstOperation, mockMessage));
  }

  @Test
  public void testHttpPostMatchesFirst() {
    when(mockMessage.get(Message.HTTP_REQUEST_METHOD))
        .thenReturn(EndpointOperationInfoResourceComparator.HTTP_POST);
    when(mockMessage.getContent(InputStream.class))
        .thenReturn(IOUtils.toInputStream(FIRST_OPERATION_BODY));
    EndpointOperationInfoResourceComparator comparator =
        new EndpointOperationInfoResourceComparator("CSW");
    assertEquals(-1, comparator.compare(firstOperation, secondOperation, mockMessage));
  }

  @Test
  public void testHttpPostMatchesSecond() {
    when(mockMessage.get(Message.HTTP_REQUEST_METHOD))
        .thenReturn(EndpointOperationInfoResourceComparator.HTTP_POST);
    when(mockMessage.getContent(InputStream.class))
        .thenReturn(IOUtils.toInputStream(SECOND_OPERATION_BODY));
    EndpointOperationInfoResourceComparator comparator =
        new EndpointOperationInfoResourceComparator("CSW");
    assertEquals(1, comparator.compare(firstOperation, secondOperation, mockMessage));
  }

  // Allows us to create a "mockMethod" with the name of the method
  public void firstOperation() {}

  // Allows us to create a "mockMethod" with the name of the method
  public void secondOperation() {}

  // Allows us to create a "mockMethod" with the name of the method
  public void unknownOperation() {}

  // Allows us to create a "mockMethod" with the name of the method
  public void unknownService() {}
}
