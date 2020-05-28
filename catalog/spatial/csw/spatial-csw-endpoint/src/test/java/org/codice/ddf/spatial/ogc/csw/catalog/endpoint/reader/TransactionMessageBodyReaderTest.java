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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.reader;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.thoughtworks.xstream.converters.ConversionException;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.security.ForbiddenClassException;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.AttributeRegistryImpl;
import ddf.catalog.data.impl.types.AssociationsAttributes;
import ddf.catalog.data.impl.types.ContactAttributes;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.data.impl.types.LocationAttributes;
import ddf.catalog.data.impl.types.MediaAttributes;
import ddf.catalog.data.impl.types.TopicAttributes;
import ddf.catalog.data.types.Topic;
import java.io.IOException;
import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import net.opengis.cat.csw.v_2_0_2.QueryConstraintType;
import net.opengis.filter.v_1_1_0.FilterType;
import org.apache.commons.io.IOUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.DeleteAction;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.InsertAction;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.UpdateAction;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.CswRecordConverter;
import org.codice.ddf.spatial.ogc.csw.catalog.endpoint.CswQueryFactoryTest;
import org.junit.Before;
import org.junit.Test;

public class TransactionMessageBodyReaderTest {
  private static final int COUNT = 100;

  private static final String INSERT_REQUEST_START =
      "<csw:Transaction service=\"CSW\" version=\"2.0.2\" verboseResponse=\"true\"\n"
          + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\">\n"
          + "    <csw:Insert typeName=\"csw:Record\">\n";

  private static final String INSERT_REQUEST_END = "</csw:Insert>\n" + "</csw:Transaction>";

  private static final String RECORD_XML =
      "<csw:Record\n"
          + "            xmlns:ows=\"http://www.opengis.net/ows\"\n"
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
          + "            </ows:BoundingBox>\n"
          + "        </csw:Record>\n";

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
      "<csw:Transaction\n"
          + "    service=\"CSW\"\n"
          + "    version=\"2.0.2\"\n"
          + "    verboseResponse=\"true\"\n"
          + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
          + "    xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
          + "    <csw:Insert typeName=\"csw:Record\">\n"
          + "        <csw:Record\n"
          + "            xmlns:ows=\"http://www.opengis.net/ows\"\n"
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
          + "            </ows:BoundingBox>\n"
          + "        </csw:Record>\n"
          + "    </csw:Insert>\n"
          + "    <csw:Delete typeName=\"csw:Record\" handle=\"something\">\n"
          + "      <csw:Constraint version=\"2.0.0\">\n"
          + "        <ogc:CqlText>\n"
          + "          title = 'foo'\n"
          + "        </ogc:CqlText>\n"
          + "      </csw:Constraint>\n"
          + "    </csw:Delete>\n"
          + "  </csw:Transaction>";

  private static final String UPDATE_REQUEST_BY_RECORD_XML =
      "<csw:Transaction\n"
          + "    service=\"CSW\"\n"
          + "    version=\"2.0.2\"\n"
          + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\">\n"
          + "    <csw:Update>\n"
          + "        <csw:Record\n"
          + "            xmlns:ows=\"http://www.opengis.net/ows\"\n"
          + "            xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
          + "            xmlns:dct=\"http://purl.org/dc/terms/\"\n"
          + "            xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n"
          + "            <dc:identifier>123</dc:identifier>\n"
          + "            <dc:title>Aliquam fermentum purus quis arcu</dc:title>\n"
          + "            <dc:type>http://purl.org/dc/dcmitype/Text</dc:type>\n"
          + "            <dc:subject>Hydrography--Dictionaries</dc:subject>\n"
          + "            <dc:format>application/pdf</dc:format>\n"
          + "            <dc:date>2008-08-10</dc:date>\n"
          + "            <dct:abstract>Vestibulum quis ipsum sit amet metus imperdiet vehicula. Nulla scelerisque cursus mi.</dct:abstract>\n"
          + "            <ows:BoundingBox crs=\"urn:x-ogc:def:crs:EPSG:6.11:4326\">\n"
          + "                <ows:LowerCorner>1.0 2.0</ows:LowerCorner>\n"
          + "                <ows:UpperCorner>3.0 4.0</ows:UpperCorner>\n"
          + "            </ows:BoundingBox>\n"
          + "        </csw:Record>\n"
          + "    </csw:Update>\n"
          + "</csw:Transaction>";

  private static final String UPDATE_REQUEST_BY_CONSTRAINT_XML =
      "<csw:Transaction\n"
          + "    service=\"CSW\"\n"
          + "    version=\"2.0.2\"\n"
          + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\">\n"
          + "    <csw:Update xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
          + "      <csw:RecordProperty>\n"
          + "        <csw:Name>subject</csw:Name>\n"
          + "        <csw:Value>Foo</csw:Value>\n"
          + "      </csw:RecordProperty>\n"
          + "      <csw:RecordProperty>\n"
          + "        <csw:Name>date</csw:Name>\n"
          + "        <csw:Value>2015-07-21</csw:Value>\n"
          + "      </csw:RecordProperty>\n"
          + "      <csw:RecordProperty>\n"
          + "        <csw:Name>location</csw:Name>\n"
          + "        <csw:Value>\n"
          + "            <ows:BoundingBox>\n"
          + "                <ows:LowerCorner>1.0 2.0</ows:LowerCorner>\n"
          + "                <ows:UpperCorner>3.0 4.0</ows:UpperCorner>\n"
          + "            </ows:BoundingBox>\n"
          + "        </csw:Value>\n"
          + "      </csw:RecordProperty>\n"
          + "      <csw:RecordProperty>\n"
          + "        <csw:Name>format</csw:Name>\n"
          + "      </csw:RecordProperty>\n"
          + "      <csw:Constraint version=\"2.0.0\">\n"
          + "        <ogc:Filter>\n"
          + "          <ogc:PropertyIsEqualTo>\n"
          + "            <ogc:PropertyName>format</ogc:PropertyName>\n"
          + "            <ogc:Literal>application/pdf</ogc:Literal>\n"
          + "          </ogc:PropertyIsEqualTo>\n"
          + "        </ogc:Filter>\n"
          + "      </csw:Constraint>\n"
          + "    </csw:Update>\n"
          + "</csw:Transaction>";

  private static final String MULTIPLE_UPDATES_REQUEST_XML =
      "<csw:Transaction\n"
          + "    service=\"CSW\"\n"
          + "    version=\"2.0.2\"\n"
          + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\"\n"
          + "    xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
          + "    <csw:Update handle=\"handle1\">\n"
          + "        <csw:Record\n"
          + "            xmlns:ows=\"http://www.opengis.net/ows\"\n"
          + "            xmlns:dc=\"http://purl.org/dc/elements/1.1/\"\n"
          + "            xmlns:dct=\"http://purl.org/dc/terms/\"\n"
          + "            xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\">\n"
          + "            <dc:identifier>123</dc:identifier>\n"
          + "            <dc:title>Aliquam fermentum purus quis arcu</dc:title>\n"
          + "            <dc:type>http://purl.org/dc/dcmitype/Text</dc:type>\n"
          + "            <dc:subject>Hydrography--Dictionaries</dc:subject>\n"
          + "            <dc:format>application/pdf</dc:format>\n"
          + "            <dc:date>2008-08-10</dc:date>\n"
          + "            <dct:abstract>Vestibulum quis ipsum sit amet metus imperdiet vehicula. Nulla scelerisque cursus mi.</dct:abstract>\n"
          + "            <ows:BoundingBox crs=\"urn:x-ogc:def:crs:EPSG:6.11:4326\">\n"
          + "                <ows:LowerCorner>1.0 2.0</ows:LowerCorner>\n"
          + "                <ows:UpperCorner>3.0 4.0</ows:UpperCorner>\n"
          + "            </ows:BoundingBox>\n"
          + "        </csw:Record>\n"
          + "    </csw:Update>\n"
          + "    <csw:Update handle=\"handle2\" typeName=\"csw:Record\">\n"
          + "      <csw:RecordProperty>\n"
          + "        <csw:Name>subject</csw:Name>\n"
          + "        <csw:Value>foo</csw:Value>\n"
          + "      </csw:RecordProperty>\n"
          + "      <csw:Constraint version=\"2.0.0\">\n"
          + "        <ogc:CqlText>\n"
          + "          title = 'bar'\n"
          + "        </ogc:CqlText>\n"
          + "      </csw:Constraint>\n"
          + "    </csw:Update>\n"
          + "</csw:Transaction>";

  private static final String UPDATE_REQUEST_NO_RECORDPROPERTY_NAME_XML =
      "<csw:Transaction\n"
          + "    service=\"CSW\"\n"
          + "    version=\"2.0.2\"\n"
          + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\">\n"
          + "    <csw:Update xmlns:ogc=\"http://www.opengis.net/ogc\">\n"
          + "      <csw:RecordProperty>\n"
          + "        <csw:Value>Foo</csw:Value>\n"
          + "      </csw:RecordProperty>\n"
          + "      <csw:Constraint version=\"2.0.0\">\n"
          + "        <ogc:Filter>\n"
          + "          <ogc:PropertyIsEqualTo>\n"
          + "            <ogc:PropertyName>format</ogc:PropertyName>\n"
          + "            <ogc:Literal>application/pdf</ogc:Literal>\n"
          + "          </ogc:PropertyIsEqualTo>\n"
          + "        </ogc:Filter>\n"
          + "      </csw:Constraint>\n"
          + "    </csw:Update>\n"
          + "</csw:Transaction>";

  private static final String UPDATE_REQUEST_NO_CONSTRAINT_XML =
      "<csw:Transaction\n"
          + "    service=\"CSW\"\n"
          + "    version=\"2.0.2\"\n"
          + "    xmlns:csw=\"http://www.opengis.net/cat/csw/2.0.2\">\n"
          + "    <csw:Update>\n"
          + "      <csw:RecordProperty>\n"
          + "        <csw:Name>subject</csw:Name>\n"
          + "        <csw:Value>Foo</csw:Value>\n"
          + "      </csw:RecordProperty>\n"
          + "    </csw:Update>\n"
          + "</csw:Transaction>";

  private static final String DYNAMIC_PROXY_SERIALIZED_XML =
      "<dynamic-proxy>  \n"
          + "            <interface>org.codice.ddf.spatial.ogc.csw.catalog.common.transaction.CswTransactionRequest</interface>  \n"
          + "            <handler class=\"java.beans.EventHandler\">  \n"
          + "                <target class=\"java.lang.ProcessBuilder\">\n"
          + "                    <command><string>/bin/sh</string><string>-c</string><string>'''+ echo ThisShouldntWork +'''</string></command>\n"
          + "                </target>\n"
          + "                <action>start</action>\n"
          + "            </handler>  \n"
          + "        </dynamic-proxy>";

  private CswRecordConverter cswRecordConverter;

  private AttributeRegistry registry = new AttributeRegistryImpl();

  @Before
  public void setup() {
    cswRecordConverter = new CswRecordConverter(CswQueryFactoryTest.getCswMetacardType());
    new CoreAttributes().getAttributeDescriptors().stream().forEach(d -> registry.register(d));
    new ContactAttributes().getAttributeDescriptors().stream().forEach(d -> registry.register(d));
    new LocationAttributes().getAttributeDescriptors().stream().forEach(d -> registry.register(d));
    new MediaAttributes().getAttributeDescriptors().stream().forEach(d -> registry.register(d));
    new TopicAttributes().getAttributeDescriptors().stream().forEach(d -> registry.register(d));
    new AssociationsAttributes()
        .getAttributeDescriptors()
        .stream()
        .forEach(d -> registry.register(d));
  }

  @Test
  public void testIsReadable() throws Exception {
    TransactionMessageBodyReader reader =
        new TransactionMessageBodyReader(
            cswRecordConverter, CswQueryFactoryTest.getCswMetacardType(), registry);
    assertThat(reader.isReadable(CswTransactionRequest.class, null, null, null), is(true));
    assertThat(reader.isReadable(Object.class, null, null, null), is(false));
  }

  @Test
  public void testReadInsertFrom() throws Exception {
    Converter mockConverter = mock(Converter.class);
    when(mockConverter.canConvert(any(Class.class))).thenReturn(true);
    when(mockConverter.unmarshal(
            any(HierarchicalStreamReader.class), any(UnmarshallingContext.class)))
        .thenReturn(mock(Metacard.class));
    TransactionMessageBodyReader reader =
        new TransactionMessageBodyReader(
            mockConverter, CswQueryFactoryTest.getCswMetacardType(), registry);
    CswTransactionRequest request =
        reader.readFrom(
            CswTransactionRequest.class,
            null,
            null,
            null,
            null,
            IOUtils.toInputStream(getInsertRequest(COUNT)));
    assertThat(request, notNullValue());
    assertThat(request.getInsertActions().size(), is(1));
    assertThat(request.getDeleteActions().size(), is(0));
    assertThat(request.getUpdateActions().size(), is(0));

    InsertAction insertAction = request.getInsertActions().get(0);
    assertThat(insertAction, notNullValue());
    assertThat(insertAction.getRecords().size(), is(COUNT));
    assertThat(request.getService(), is(CswConstants.CSW));
    assertThat(request.getVersion(), is(CswConstants.VERSION_2_0_2));
    assertThat(request.isVerbose(), is(true));
  }

  @Test
  public void testReadDeleteWithFilterFrom() throws IOException {
    TransactionMessageBodyReader reader =
        new TransactionMessageBodyReader(
            mock(Converter.class), CswQueryFactoryTest.getCswMetacardType(), registry);
    CswTransactionRequest request =
        reader.readFrom(
            CswTransactionRequest.class,
            null,
            null,
            null,
            null,
            IOUtils.toInputStream(DELETE_REQUEST_FILTER_XML));
    assertThat(request, notNullValue());
    assertThat(request.getDeleteActions().size(), is(1));
    assertThat(request.getInsertActions().size(), is(0));
    assertThat(request.getUpdateActions().size(), is(0));

    DeleteAction deleteAction = request.getDeleteActions().get(0);
    assertThat(deleteAction, notNullValue());
    assertThat(deleteAction.getTypeName(), is(CswConstants.CSW_RECORD));
    assertThat(deleteAction.getHandle(), is("something"));
    assertThat(deleteAction.getConstraint(), notNullValue());
    assertThat(deleteAction.getConstraint().getFilter(), notNullValue());
    assertThat(request.getService(), is(CswConstants.CSW));
    assertThat(request.getVersion(), is(CswConstants.VERSION_2_0_2));
    assertThat(request.isVerbose(), is(false));
  }

  @Test
  public void testReadDeleteWithCqlFrom() throws IOException {
    TransactionMessageBodyReader reader =
        new TransactionMessageBodyReader(
            mock(Converter.class), CswQueryFactoryTest.getCswMetacardType(), registry);
    CswTransactionRequest request =
        reader.readFrom(
            CswTransactionRequest.class,
            null,
            null,
            null,
            null,
            IOUtils.toInputStream(DELETE_REQUEST_CQL_XML));
    assertThat(request, notNullValue());
    assertThat(request.getDeleteActions().size(), is(1));
    assertThat(request.getInsertActions().size(), is(0));
    assertThat(request.getUpdateActions().size(), is(0));

    DeleteAction deleteAction = request.getDeleteActions().get(0);
    assertThat(deleteAction, notNullValue());
    assertThat(deleteAction.getTypeName(), is(CswConstants.CSW_RECORD));
    assertThat(deleteAction.getHandle(), is("something"));
    assertThat(deleteAction.getConstraint(), notNullValue());
    assertThat(deleteAction.getConstraint().getCqlText().trim(), is("title = 'foo'"));
    assertThat(request.getService(), is(CswConstants.CSW));
    assertThat(request.getVersion(), is(CswConstants.VERSION_2_0_2));
    assertThat(request.isVerbose(), is(false));
  }

  @Test
  public void testReadInsertAndDeleteFrom() throws IOException {
    Converter mockConverter = mock(Converter.class);
    when(mockConverter.canConvert(any(Class.class))).thenReturn(true);
    when(mockConverter.unmarshal(
            any(HierarchicalStreamReader.class), any(UnmarshallingContext.class)))
        .thenReturn(mock(Metacard.class));

    TransactionMessageBodyReader reader =
        new TransactionMessageBodyReader(
            mockConverter, CswQueryFactoryTest.getCswMetacardType(), registry);

    CswTransactionRequest request =
        reader.readFrom(
            CswTransactionRequest.class,
            null,
            null,
            null,
            null,
            IOUtils.toInputStream(INSERT_AND_DELETE_REQUEST_XML));
    assertThat(request, notNullValue());
    assertThat(request.getDeleteActions().size(), is(1));
    assertThat(request.getInsertActions().size(), is(1));
    assertThat(request.getUpdateActions().size(), is(0));

    DeleteAction deleteAction = request.getDeleteActions().get(0);
    assertThat(deleteAction, notNullValue());
    assertThat(deleteAction.getTypeName(), is(CswConstants.CSW_RECORD));
    assertThat(deleteAction.getHandle(), is("something"));
    assertThat(deleteAction.getConstraint(), notNullValue());
    assertThat(deleteAction.getConstraint().getCqlText().trim(), is("title = 'foo'"));

    InsertAction insertAction = request.getInsertActions().get(0);
    assertThat(insertAction, notNullValue());
    assertThat(insertAction.getRecords().size(), is(1));

    assertThat(request.getService(), is(CswConstants.CSW));
    assertThat(request.getVersion(), is(CswConstants.VERSION_2_0_2));
    assertThat(request.isVerbose(), is(true));
  }

  @Test
  public void testReadUpdateByNewRecordFrom() throws IOException, ParseException {
    TransactionMessageBodyReader reader =
        new TransactionMessageBodyReader(
            cswRecordConverter, CswQueryFactoryTest.getCswMetacardType(), registry);
    CswTransactionRequest request =
        reader.readFrom(
            CswTransactionRequest.class,
            null,
            null,
            null,
            null,
            IOUtils.toInputStream(UPDATE_REQUEST_BY_RECORD_XML));

    assertThat(request, notNullValue());
    assertThat(request.getInsertActions().size(), is(0));
    assertThat(request.getDeleteActions().size(), is(0));
    assertThat(request.getUpdateActions().size(), is(1));

    UpdateAction updateAction = request.getUpdateActions().get(0);
    assertThat(updateAction, notNullValue());
    assertThat(updateAction.getMetacard(), notNullValue());

    Metacard metacard = updateAction.getMetacard();
    assertThat(metacard.getId(), is("123"));
    assertThat(metacard.getTitle(), is("Aliquam fermentum purus quis arcu"));

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    Date date = simpleDateFormat.parse("2008-08-10");
    assertThat(metacard.getModifiedDate(), is(date));

    assertThat(
        metacard.getLocation(), is("POLYGON ((1.0 2.0, 3.0 2.0, 3.0 4.0, 1.0 4.0, 1.0 2.0))"));

    assertThat(request.getService(), is(CswConstants.CSW));
    assertThat(request.getVersion(), is(CswConstants.VERSION_2_0_2));
    assertThat(request.isVerbose(), is(false));
  }

  @Test
  public void testReadUpdateByConstraintFrom() throws IOException, ParseException {
    TransactionMessageBodyReader reader =
        new TransactionMessageBodyReader(
            mock(Converter.class), CswQueryFactoryTest.getCswMetacardType(), registry);
    CswTransactionRequest request =
        reader.readFrom(
            CswTransactionRequest.class,
            null,
            null,
            null,
            null,
            IOUtils.toInputStream(UPDATE_REQUEST_BY_CONSTRAINT_XML));

    assertThat(request, notNullValue());
    assertThat(request.getInsertActions().size(), is(0));
    assertThat(request.getDeleteActions().size(), is(0));
    assertThat(request.getUpdateActions().size(), is(1));

    UpdateAction updateAction = request.getUpdateActions().get(0);
    assertThat(updateAction, notNullValue());
    assertThat(updateAction.getMetacard(), nullValue());

    Map<String, Serializable> recordProperties = updateAction.getRecordProperties();

    assertThat(recordProperties, notNullValue());
    assertThat(recordProperties.size(), is(4));

    Serializable newSubjectValue = recordProperties.get(Topic.CATEGORY);
    assertThat(newSubjectValue, notNullValue());
    assertThat(newSubjectValue, is("Foo"));

    Serializable newDateValue = recordProperties.get("modified");
    assertThat(newDateValue, notNullValue());

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    Date date = simpleDateFormat.parse("2015-07-21");
    assertThat(newDateValue, is(date));

    Serializable newLocationValue = recordProperties.get("location");
    assertThat(newLocationValue, notNullValue());
    assertThat(newLocationValue, is("POLYGON ((1.0 2.0, 3.0 2.0, 3.0 4.0, 1.0 4.0, 1.0 2.0))"));

    Serializable newFormatValue = recordProperties.get("media.format");
    // No <Value> was specified in the request.
    assertThat(newFormatValue, nullValue());

    QueryConstraintType constraint = updateAction.getConstraint();
    assertThat(constraint, notNullValue());

    FilterType filter = constraint.getFilter();
    assertThat(filter, notNullValue());
    assertThat(filter.isSetComparisonOps(), is(true));
    assertThat(filter.isSetLogicOps(), is(false));
    assertThat(filter.isSetSpatialOps(), is(false));

    assertThat(request.getService(), is(CswConstants.CSW));
    assertThat(request.getVersion(), is(CswConstants.VERSION_2_0_2));
    assertThat(request.isVerbose(), is(false));
  }

  @Test
  public void testReadMultipleUpdatesFrom() throws IOException, ParseException {
    TransactionMessageBodyReader reader =
        new TransactionMessageBodyReader(
            cswRecordConverter, CswQueryFactoryTest.getCswMetacardType(), registry);
    CswTransactionRequest request =
        reader.readFrom(
            CswTransactionRequest.class,
            null,
            null,
            null,
            null,
            IOUtils.toInputStream(MULTIPLE_UPDATES_REQUEST_XML));
    assertThat(request, notNullValue());
    assertThat(request.getInsertActions().size(), is(0));
    assertThat(request.getDeleteActions().size(), is(0));
    assertThat(request.getUpdateActions().size(), is(2));

    UpdateAction firstUpdateAction = request.getUpdateActions().get(0);
    assertThat(firstUpdateAction, notNullValue());
    assertThat(firstUpdateAction.getMetacard(), notNullValue());

    Metacard metacard = firstUpdateAction.getMetacard();
    assertThat(metacard.getId(), is("123"));
    assertThat(metacard.getTitle(), is("Aliquam fermentum purus quis arcu"));

    SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd");
    Date date = simpleDateFormat.parse("2008-08-10");
    assertThat(metacard.getModifiedDate(), is(date));

    assertThat(
        metacard.getLocation(), is("POLYGON ((1.0 2.0, 3.0 2.0, 3.0 4.0, 1.0 4.0, 1.0 2.0))"));

    assertThat(firstUpdateAction.getHandle(), is("handle1"));
    assertThat(firstUpdateAction.getTypeName(), is(CswConstants.CSW_RECORD));

    UpdateAction secondUpdateAction = request.getUpdateActions().get(1);
    assertThat(secondUpdateAction, notNullValue());
    assertThat(secondUpdateAction.getMetacard(), nullValue());

    Map<String, Serializable> recordProperties = secondUpdateAction.getRecordProperties();
    assertThat(recordProperties, notNullValue());
    assertThat(recordProperties.size(), is(1));

    Serializable newSubject = recordProperties.get("topic.category");
    assertThat(newSubject, is("foo"));

    QueryConstraintType constraint = secondUpdateAction.getConstraint();
    assertThat(constraint, notNullValue());
    assertThat(constraint.getCqlText().trim(), is("title = 'bar'"));

    assertThat(secondUpdateAction.getHandle(), is("handle2"));
    assertThat(secondUpdateAction.getTypeName(), is(CswConstants.CSW_RECORD));

    assertThat(request.getService(), is(CswConstants.CSW));
    assertThat(request.getVersion(), is(CswConstants.VERSION_2_0_2));
    assertThat(request.isVerbose(), is(false));
  }

  @Test(expected = ConversionException.class)
  public void testConversionExceptionWhenNoNameInUpdateRecordProperty() throws IOException {
    TransactionMessageBodyReader reader =
        new TransactionMessageBodyReader(
            mock(Converter.class), CswQueryFactoryTest.getCswMetacardType(), registry);
    reader.readFrom(
        CswTransactionRequest.class,
        null,
        null,
        null,
        null,
        IOUtils.toInputStream(UPDATE_REQUEST_NO_RECORDPROPERTY_NAME_XML));
  }

  @Test(expected = ConversionException.class)
  public void testConversionExceptionWhenNoConstraintInUpdate() throws IOException {
    TransactionMessageBodyReader reader =
        new TransactionMessageBodyReader(
            mock(Converter.class), CswQueryFactoryTest.getCswMetacardType(), registry);
    reader.readFrom(
        CswTransactionRequest.class,
        null,
        null,
        null,
        null,
        IOUtils.toInputStream(UPDATE_REQUEST_NO_CONSTRAINT_XML));
  }

  @Test(expected = ForbiddenClassException.class)
  public void testForbiddenDeserialization() throws IOException {
    TransactionMessageBodyReader reader =
        new TransactionMessageBodyReader(
            mock(Converter.class), CswQueryFactoryTest.getCswMetacardType(), registry);
    reader.readFrom(
        CswTransactionRequest.class,
        null,
        null,
        null,
        null,
        IOUtils.toInputStream(DYNAMIC_PROXY_SERIALIZED_XML));
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
