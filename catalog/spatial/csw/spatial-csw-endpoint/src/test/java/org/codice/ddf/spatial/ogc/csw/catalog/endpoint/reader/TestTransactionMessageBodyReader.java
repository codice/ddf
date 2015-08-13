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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.reader;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.DeleteTransaction;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.InsertTransaction;
import org.junit.Test;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;

import ddf.catalog.data.Metacard;

public class TestTransactionMessageBodyReader {

    private static final int COUNT = 100;

    private static final String INSERT_REQUEST_START =
            "<csw:Transaction service=\"CSW\" version=\"2.0.2\" verboseResponse=\"true\"\n"
                    + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\">\n"
                    + "    <csw:Insert typeName=\"csw:Record\">\n";

    private static final String INSERT_REQUEST_END = "</csw:Insert>\n" + "</csw:Transaction>";

    private static final String RECORD_XML =
            "<csw:Record\n" + "            xmlns:ows=\"http://www.opengis.net/ows\"\n"
                    + "            xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                    + "            xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
                    + "            xmlns:dct=\"http://purl.org/dc/terms/\"\n"
                    + "            xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n"
                    + "            <dc:identifier>123</dc:identifier>\n"
                    + "            <dc:title>Aliquam fermentum purus quis arcu</dc:title>\n"
                    + "            <dc:type>http://purl.org/dc/dcmitype/Text</dc:type>\n"
                    + "            <dc:subject>Hydrography--Dictionaries</dc:subject>\n"
                    + "            <dc:format>application/pdf</dc:format>\n"
                    + "            <dc:date>2006-05-12</dc:date>\n"
                    + "            <dct:abstract>Vestibulum quis ipsum sit amet metus imperdiet vehicula. Nulla scelerisque cursus mi.</dct:abstract>\n"
                    + "            <ows:BoundingBox crs=\"urn:x-ogc:def:crs:EPSG:6.11:4326\">\n"
                    + "                <ows:LowerCorner>44.792 -6.171</ows:LowerCorner>\n"
                    + "                <ows:UpperCorner>51.126 -2.228</ows:UpperCorner>\n"
                    + "            </ows:BoundingBox>\n" + "        </csw:Record>\n";

    private static final String DELETE_REQUEST_FILTER_XML =
            "<csw:Transaction service=\"CSW\"\n"
                    + "   version=\"2.0.2\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                    + "   xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
                    + "  <csw:Delete typeName=\"csw:Record\" handle=\"something\">\n"
                    + "    <csw:Constraint version=\"2.0.0\">\n"
                    + "      <ogc:Filter>\n"
                    + "        <ogc:PropertyIsEqualTo>\n"
                    + "            <ogc:PropertyName>title</ogc:PropertyName>\n"
                    + "            <ogc:Literal>Aliquam fermentum purus quis arcu</ogc:Literal>\n"
                    + "        </ogc:PropertyIsEqualTo>\n"
                    + "      </ogc:Filter>\n"
                    + "    </csw:Constraint>\n"
                    + "  </csw:Delete>\n"
                    + "</csw:Transaction>";

    private static final String DELETE_REQUEST_CQL_XML =
            "<csw:Transaction service=\"CSW\"\n"
                    + "   version=\"2.0.2\" xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
                    + "   xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
                    + "  <csw:Delete typeName=\"csw:Record\" handle=\"something\">\n"
                    + "    <csw:Constraint version=\"2.0.0\">\n"
                    + "      <ogc:CqlText>\n"
                    + "        title = 'foo'\n"
                    + "      </ogc:CqlText>\n"
                    + "    </csw:Constraint>\n"
                    + "  </csw:Delete>\n"
                    + "</csw:Transaction>";

    private static final String INSERT_AND_DELETE_REQUEST_XML =
            "<csw:Transaction\n" + "    service=\"CSW\"\n" + "    version=\"2.0.2\"\n"
            + "    verboseResponse=\"true\"\n"
            + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
            + "    xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
            + "    <csw:Insert typeName=\"csw:Record\">\n" + "        <csw:Record\n"
            + "            xmlns:ows=\"http://www.opengis.net/ows\"\n"
            + "            xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
            + "            xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
            + "            xmlns:dct=\"http://purl.org/dc/terms/\"\n"
            + "            xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n"
            + "            <dc:identifier>123</dc:identifier>\n"
            + "            <dc:title>A Different Title</dc:title>\n"
            + "            <dc:type>http://purl.org/dc/dcmitype/Text</dc:type>\n"
            + "            <dc:subject>Hydrography--Dictionaries</dc:subject>\n"
            + "            <dc:format>application/pdf</dc:format>\n"
            + "            <dc:date>2006-05-12</dc:date>\n"
            + "            <dct:abstract>Vestibulum quis ipsum sit amet metus imperdiet vehicula. Nulla scelerisque cursus mi.</dct:abstract>\n"
            + "            <ows:BoundingBox crs=\"urn:x-ogc:def:crs:EPSG:6.11:4326\">\n"
            + "                <ows:LowerCorner>44.792 -6.171</ows:LowerCorner>\n"
            + "                <ows:UpperCorner>51.126 -2.228</ows:UpperCorner>\n"
            + "            </ows:BoundingBox>\n" + "        </csw:Record>\n"
            + "    </csw:Insert>\n"
            + "    <csw:Delete typeName=\"csw:Record\" handle=\"something\">\n"
            + "      <csw:Constraint version=\"2.0.0\">\n"
            + "        <ogc:CqlText>\n"
            + "          title = 'foo'\n"
            + "        </ogc:CqlText>\n"
            + "      </csw:Constraint>\n"
            + "    </csw:Delete>\n"
            + "  </csw:Transaction>";

    @Test
    public void testIsReadable() throws Exception {
        TransactionMessageBodyReader reader = new TransactionMessageBodyReader(
                mock(Converter.class));
        assertThat(reader.isReadable(CswTransactionRequest.class, null, null, null), is(true));
        assertThat(reader.isReadable(Object.class, null, null, null), is(false));
    }

    @Test
    public void testReadInsertFrom() throws Exception {
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.canConvert(any(Metacard.class.getClass()))).thenReturn(true);
        when(mockConverter
                .unmarshal(any(HierarchicalStreamReader.class), any(UnmarshallingContext.class)))
                .thenReturn(mock(Metacard.class));
        TransactionMessageBodyReader reader = new TransactionMessageBodyReader(mockConverter);
        CswTransactionRequest request = reader
                .readFrom(CswTransactionRequest.class, null, null, null, null,
                        IOUtils.toInputStream(getInsertRequest(COUNT)));
        assertThat(request, notNullValue());
        assertThat(request.getInsertTransactions().size(), is(1));
        assertThat(request.getDeleteTransactions().size(), is(0));

        InsertTransaction insertTransaction = request.getInsertTransactions().get(0);
        assertThat(insertTransaction, notNullValue());
        assertThat(insertTransaction.getRecords().size(), is(COUNT));
        assertThat(request.getService(), is(CswConstants.CSW));
        assertThat(request.getVersion(), is(CswConstants.VERSION_2_0_2));
        assertThat(request.isVerbose(), is(true));
    }

    @Test
    public void testReadDeleteWithFilterFrom() throws IOException {
        TransactionMessageBodyReader reader = new TransactionMessageBodyReader(
                mock(Converter.class));
        CswTransactionRequest request = reader
                .readFrom(CswTransactionRequest.class, null, null, null, null,
                        IOUtils.toInputStream(DELETE_REQUEST_FILTER_XML));
        assertThat(request, notNullValue());
        assertThat(request.getDeleteTransactions().size(), is(1));
        assertThat(request.getInsertTransactions().size(), is(0));

        DeleteTransaction deleteTransaction = request.getDeleteTransactions().get(0);
        assertThat(deleteTransaction, notNullValue());
        assertThat(deleteTransaction.getTypeName(), is(CswConstants.CSW_RECORD));
        assertThat(deleteTransaction.getHandle(), is("something"));
        assertThat(deleteTransaction.getConstraint(), notNullValue());
        assertThat(deleteTransaction.getConstraint().getFilter(), notNullValue());
        assertThat(request.getService(), is(CswConstants.CSW));
        assertThat(request.getVersion(), is(CswConstants.VERSION_2_0_2));
        assertThat(request.isVerbose(), is(false));
    }

    @Test
    public void testReadDeleteWithCqlFrom() throws IOException {
        TransactionMessageBodyReader reader = new TransactionMessageBodyReader(
                mock(Converter.class));
        CswTransactionRequest request = reader
                .readFrom(CswTransactionRequest.class, null, null, null, null,
                        IOUtils.toInputStream(DELETE_REQUEST_CQL_XML));
        assertThat(request, notNullValue());
        assertThat(request.getDeleteTransactions().size(), is(1));
        assertThat(request.getInsertTransactions().size(), is(0));

        DeleteTransaction deleteTransaction = request.getDeleteTransactions().get(0);
        assertThat(deleteTransaction, notNullValue());
        assertThat(deleteTransaction.getTypeName(), is(CswConstants.CSW_RECORD));
        assertThat(deleteTransaction.getHandle(), is("something"));
        assertThat(deleteTransaction.getConstraint(), notNullValue());
        assertThat(deleteTransaction.getConstraint().getCqlText().trim(), is("title = 'foo'"));
        assertThat(request.getService(), is(CswConstants.CSW));
        assertThat(request.getVersion(), is(CswConstants.VERSION_2_0_2));
        assertThat(request.isVerbose(), is(false));
    }

    @Test
    public void testReadInsertAndDeleteFrom() throws IOException {
        Converter mockConverter = mock(Converter.class);
        when(mockConverter.canConvert(any(Metacard.class.getClass()))).thenReturn(true);
        when(mockConverter
                .unmarshal(any(HierarchicalStreamReader.class), any(UnmarshallingContext.class)))
                .thenReturn(mock(Metacard.class));

        TransactionMessageBodyReader reader = new TransactionMessageBodyReader(mockConverter);

        CswTransactionRequest request = reader
                .readFrom(CswTransactionRequest.class, null, null, null, null,
                        IOUtils.toInputStream(INSERT_AND_DELETE_REQUEST_XML));
        assertThat(request, notNullValue());
        assertThat(request.getDeleteTransactions().size(), is(1));
        assertThat(request.getInsertTransactions().size(), is(1));

        DeleteTransaction deleteTransaction = request.getDeleteTransactions().get(0);
        assertThat(deleteTransaction, notNullValue());
        assertThat(deleteTransaction.getTypeName(), is(CswConstants.CSW_RECORD));
        assertThat(deleteTransaction.getHandle(), is("something"));
        assertThat(deleteTransaction.getConstraint(), notNullValue());
        assertThat(deleteTransaction.getConstraint().getCqlText().trim(), is("title = 'foo'"));

        InsertTransaction insertTransaction = request.getInsertTransactions().get(0);
        assertThat(insertTransaction, notNullValue());
        assertThat(insertTransaction.getRecords().size(), is(1));

        assertThat(request.getService(), is(CswConstants.CSW));
        assertThat(request.getVersion(), is(CswConstants.VERSION_2_0_2));
        assertThat(request.isVerbose(), is(true));
    }

    private String getInsertRequest(int count) {
        StringBuilder builder = new StringBuilder();
        builder.append(INSERT_REQUEST_START);
        for (int i = 0; i < count; i++) {
            builder.append(RECORD_XML);
        }
        builder.append(INSERT_REQUEST_END);
        return builder.toString();
    }
}