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
package org.codice.ddf.spatial.ogc.wfs.catalog.endpoint;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.core.UriInfo;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import ogc.schema.opengis.filter.v_1_0_0.FeatureIdType;
import ogc.schema.opengis.filter.v_1_0_0.FilterType;
import ogc.schema.opengis.filter.v_1_0_0.LiteralType;
import ogc.schema.opengis.filter.v_1_0_0.PropertyIsLikeType;
import ogc.schema.opengis.filter.v_1_0_0.PropertyNameType;
import ogc.schema.opengis.wfs.v_1_0_0.DescribeFeatureTypeType;
import ogc.schema.opengis.wfs.v_1_0_0.GetCapabilitiesType;
import ogc.schema.opengis.wfs.v_1_0_0.GetFeatureType;
import ogc.schema.opengis.wfs.v_1_0_0.ObjectFactory;
import ogc.schema.opengis.wfs.v_1_0_0.QueryType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.WFSCapabilitiesType;

import org.apache.commons.lang.StringUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaExternal;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.DescribeFeatureTypeRequest;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsException;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsQnameBuilder;
import org.codice.ddf.spatial.ogc.wfs.catalog.endpoint.utils.ServicePropertiesMap;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.opengis.filter.Filter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.Result;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.federation.FederationException;
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.SourceInfoRequest;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.impl.QueryImpl;
import ddf.catalog.operation.impl.QueryRequestImpl;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.catalog.source.impl.SourceDescriptorImpl;

public class TestWfsEndpoint {

    private static UriInfo mockUriInfo = mock(UriInfo.class);

    private static WfsEndpoint wfs;

    private static GeotoolsFilterBuilder filterBuilder = new GeotoolsFilterBuilder();

    private static BundleContext mockContext = mock(BundleContext.class);

    private static FeatureTypeSchemaCache cache;

    private static CatalogFramework catalogFramework = mock(CatalogFramework.class);

    private ogc.schema.opengis.wfs.v_1_0_0.ObjectFactory wfsObjectFactory = new ogc.schema.opengis.wfs.v_1_0_0.ObjectFactory();

    private static SourceInfoResponse mockSourceInfoResponse = mock(SourceInfoResponse.class);

    private static ServiceReference mockServiceRef = mock(ServiceReference.class);

    private static ServicePropertiesMap<MetacardType> mockServiceList = new ServicePropertiesMap<MetacardType>();

    private static final String CONTENT_TYPE = "contentType";

    private ogc.schema.opengis.filter.v_1_0_0.ObjectFactory filterObjectFactory = new ogc.schema.opengis.filter.v_1_0_0.ObjectFactory();

    private ogc.schema.opengis.wfs_capabilities.v_1_0_0.ObjectFactory wfsCapabilityObjectFactory = new ogc.schema.opengis.wfs_capabilities.v_1_0_0.ObjectFactory();

    private static final Integer MAX_FEATURES = 10;

    private static final QName MOCK_QNAME = new QName("test:Cities");

    private static final String METACARD_LOCATION = "POINT(4 5)";

    @BeforeClass
    public static void setup() throws URISyntaxException, SourceUnavailableException,
        UnsupportedQueryException, FederationException {
        URI mockUri = new URI("http://example.com/services/wfs");
        when(mockUriInfo.getBaseUri()).thenReturn(mockUri);
        when(mockServiceRef.getProperty(Metacard.CONTENT_TYPE)).thenReturn(CONTENT_TYPE);
        when(mockContext.getService(any(ServiceReference.class)))
                .thenReturn(new MockMetacardType());
        mockServiceList.bindService(new MockMetacardType(), MockMetacardType.PROPERTIES);
        when(catalogFramework.getSourceInfo(any(SourceInfoRequest.class))).thenReturn(
                mockSourceInfoResponse);

        Set<SourceDescriptor> sourceDescriptors = new HashSet<SourceDescriptor>();
        Set<ContentType> contentTypes = new HashSet<ContentType>();
        contentTypes.add(new ContentTypeImpl(CONTENT_TYPE, CONTENT_TYPE));
        contentTypes.add(new ContentTypeImpl(MockMetacardType.IMAGE, MockMetacardType.IMAGE));
        contentTypes.add(new ContentTypeImpl(MockMetacardType.VIDEO, MockMetacardType.VIDEO));
        sourceDescriptors.add(new SourceDescriptorImpl("sourceId", contentTypes));
        when(mockSourceInfoResponse.getSourceInfo()).thenReturn(sourceDescriptors);
        cache = new FeatureTypeSchemaCache(mockContext, mockServiceList, catalogFramework);
        wfs = new WfsEndpoint(catalogFramework, filterBuilder, mockUriInfo, cache);
    }

    @Test
    public void testGetCapabiltiesHttpGet() {
        WFSCapabilitiesType capabilities = null;
        try {
            capabilities = wfs.getCapabilities(new GetCapabilitiesRequest());
        } catch (WfsException e) {
            fail(e.getMessage());
        }
        assertNotNull(capabilities);
        // Marshal the capabilities
        String response = marshalXmlObject(capabilities);
        assertFalse(StringUtils.isEmpty(response));
    }

    @Test
    public void testGetCapabiltiesHttpPost() {
        GetCapabilitiesType request = new GetCapabilitiesType();
        request.setService(WfsConstants.WFS);
        request.setVersion(WfsConstants.VERSION_1_0_0);
        WFSCapabilitiesType capabilities = null;
        try {
            capabilities = wfs.getCapabilities(request);
        } catch (WfsException e) {
            fail(e.getMessage());

        }
        assertNotNull(capabilities);
        // Marshal the capabilities
        String response = marshalXmlObject(capabilities);
        assertFalse(StringUtils.isEmpty(response));
    }

    @Test(expected = WfsException.class)
    public void testGetCapabiltiesInvalidVersion() throws WfsException {
        GetCapabilitiesType request = new GetCapabilitiesType();
        request.setService(WfsConstants.WFS);
        request.setVersion("1.1.0");
        wfs.getCapabilities(request);
    }

    @Test(expected = WfsException.class)
    public void testGetCapabiltiesInvalidService() throws WfsException {
        GetCapabilitiesType request = new GetCapabilitiesType();
        request.setService("SOMETHING");
        request.setVersion(WfsConstants.VERSION_1_0_0);
        wfs.getCapabilities(request);
    }

    @Test
    public void testDescribeFeatureTypeHttpGet() throws WfsException {
        DescribeFeatureTypeRequest request = new DescribeFeatureTypeRequest();
        request.setTypeName(CONTENT_TYPE);
        XmlSchema schema = wfs.describeFeatureType(request);
        assertNotNull(schema);
    }

    @Test
    public void testDescribeFeatureTypeNoneSpecifiedHttpGet() throws WfsException {
        DescribeFeatureTypeRequest request = new DescribeFeatureTypeRequest();
        XmlSchema schema = wfs.describeFeatureType(request);
        assertNotNull(schema);
    }

    @Test
    public void testDescribeMultipleFeatureTypesHttpGet() throws WfsException,
        UnsupportedEncodingException {
        DescribeFeatureTypeRequest request = new DescribeFeatureTypeRequest();
        request.setTypeName(MockMetacardType.IMAGE + "," + MockMetacardType.VIDEO);
        XmlSchema schema = wfs.describeFeatureType(request);
        assertNotNull(schema);
        StringWriter writer = new StringWriter();
        schema.write(writer);
        for (XmlSchemaExternal external : schema.getExternals()) {
            XmlSchemaImport importSchema = (XmlSchemaImport) external;
            assertTrue(WfsQnameBuilder.buildQName(MockMetacardType.NAME, MockMetacardType.IMAGE)
                    .getNamespaceURI().equals(importSchema.getNamespace())
                    || WfsQnameBuilder.buildQName(MockMetacardType.NAME, MockMetacardType.VIDEO)
                            .getNamespaceURI().equals(importSchema.getNamespace()));

        }
    }

    @Test
    public void testDescribeFeatureTypeWithNamespacePrefix() throws WfsException {
        DescribeFeatureTypeRequest request = new DescribeFeatureTypeRequest();
        QName qname = WfsQnameBuilder.buildQName(MetacardType.DEFAULT_METACARD_TYPE_NAME,
                CONTENT_TYPE);
        request.setTypeName(qname.getPrefix() + ":"
                + qname.getLocalPart());
        XmlSchema schema = wfs.describeFeatureType(request);
        assertNotNull(schema);
    }

    @Test(expected = WfsException.class)
    public void testDescribeFeatureTypeNoMatchingFeature() throws WfsException {
        DescribeFeatureTypeRequest request = new DescribeFeatureTypeRequest();
        request.setTypeName("FAKE");
        XmlSchema schema = wfs.describeFeatureType(request);
        assertNotNull(schema);
    }

    @Test
    public void testDescribeFeatureTypeHttpPost() throws WfsException {
        DescribeFeatureTypeType request = new DescribeFeatureTypeType();
        request.getTypeName().add(
                new QName(WfsConstants.NAMESPACE_URN_ROOT + MetacardType.DEFAULT_METACARD_TYPE_NAME
                        + "." + CONTENT_TYPE, CONTENT_TYPE));
        XmlSchema schema = wfs.describeFeatureType(request);
        assertNotNull(schema);
    }

    @Test
    public void testDescribeMultipleFeatureTypesHttpPost() throws WfsException {
        DescribeFeatureTypeType request = new DescribeFeatureTypeType();
        request.getTypeName().add(
                WfsQnameBuilder.buildQName(MetacardType.DEFAULT_METACARD_TYPE_NAME, CONTENT_TYPE));
        request.getTypeName().add(
                WfsQnameBuilder.buildQName(MockMetacardType.NAME, MockMetacardType.IMAGE));
        request.getTypeName().add(
                WfsQnameBuilder.buildQName(MockMetacardType.NAME, MockMetacardType.VIDEO));
        XmlSchema schema = wfs.describeFeatureType(request);
        assertNotNull(schema);
    }

    @Test
    public void testDescribeFeatureTypeNoneSpecifiedHttpPost() throws WfsException {
        DescribeFeatureTypeType request = new DescribeFeatureTypeType();
        XmlSchema schema = wfs.describeFeatureType(request);
        assertNotNull(schema);
    }

    @Test
    public void testGetFeaturePropertyIsLike() throws UnsupportedQueryException,
        SourceUnavailableException, FederationException, WfsException, URISyntaxException {

        QueryType queryType = wfsObjectFactory.createQueryType();
        queryType.setFilter(getPropertyIsLikeFilter());
        queryType.setTypeName(MOCK_QNAME);

        ArgumentCaptor<QueryRequestImpl> captor = ArgumentCaptor.forClass(QueryRequestImpl.class);

        CatalogFramework cf = getCatalogFrameworkForQuery();
        getWfsEndpoint(cf).getFeature(getGetFeatureType(queryType));

        verify(cf).query(captor.capture());

        QueryRequestImpl queryRequest = captor.getValue();
        QueryImpl queryImpl = (QueryImpl) queryRequest.getQuery();
        assert (queryImpl.getPageSize() == MAX_FEATURES);
        assert (queryImpl.getFilter() instanceof Filter);
        // can we do anymore with the filter here?

    }

    @Test
    public void testGetFeatureEmptyFilter() throws WfsException, UnsupportedQueryException,
        SourceUnavailableException, FederationException, URISyntaxException {

        QueryType queryType = wfsObjectFactory.createQueryType();
        queryType.setFilter(new FilterType());
        queryType.setTypeName(MOCK_QNAME);

        ArgumentCaptor<QueryRequestImpl> captor = ArgumentCaptor.forClass(QueryRequestImpl.class);
        CatalogFramework cf = getCatalogFrameworkForQuery();

        getWfsEndpoint(cf).getFeature(getGetFeatureType(queryType));

        verify(cf).query(captor.capture());

        QueryRequestImpl queryRequest = captor.getValue();
        QueryImpl queryImpl = (QueryImpl) queryRequest.getQuery();

        assert (queryImpl.getFilter() instanceof Filter);
    }

    @Test
    public void testGetFeatureDWithinValidUnits() throws WfsException, UnsupportedQueryException,
        SourceUnavailableException, FederationException, JAXBException {

        String getFeatureXml = "<?xml version=\"1.0\"?>"
                + "<ns4:GetFeature version=\"1.0.0\" service=\"WFS\" maxFeatures=\"10\" "
                + "xmlns:ns2=\"http://www.opengis.net/gml\" "
                + "xmlns=\"http://www.opengis.net/ogc\" xmlns:ns4=\"http://www.opengis.net/wfs\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
                + "<ns4:Query typeName=\"cities\" xmlns=\"\" xmlns:ns5=\"http://www.opengis.net/ogc\">"
                + "<ns5:Filter>" + "<ns5:DWithin>"
                + "<ns5:PropertyName>msGeometry</ns5:PropertyName>" + "<ns2:Point>"
                + "<ns2:coordinates>-73.0,40.0</ns2:coordinates>" + "</ns2:Point>"
                + "<ns5:Distance units=\"METRE\">10000.0</ns5:Distance>" + "</ns5:DWithin>"
                + "</ns5:Filter>" + "</ns4:Query>" + "</ns4:GetFeature>";

        JAXBContext ctx = JAXBContext.newInstance("ogc.schema.opengis.wfs.v_1_0_0");
        JAXBElement<GetFeatureType> getFeatureType = (JAXBElement<GetFeatureType>) ctx
                .createUnmarshaller().unmarshal(new ByteArrayInputStream(getFeatureXml.getBytes()));

        ArgumentCaptor<QueryRequestImpl> captor = ArgumentCaptor.forClass(QueryRequestImpl.class);
        CatalogFramework cf = getCatalogFrameworkForQuery();

        getWfsEndpoint(cf).getFeature(getFeatureType.getValue());

        verify(cf).query(captor.capture());

        QueryRequestImpl queryRequest = captor.getValue();
        QueryImpl queryImpl = (QueryImpl) queryRequest.getQuery();
        assert (queryImpl.getFilter() instanceof Filter);
    }

    @Test(expected = WfsException.class)
    public void testGetFeatureDWithinInvalidUnits() throws JAXBException, WfsException,
        UnsupportedQueryException, SourceUnavailableException, FederationException {
        String getFeatureXml = "<?xml version=\"1.0\"?>"
                + "<ns4:GetFeature version=\"1.0.0\" service=\"WFS\" maxFeatures=\"10\" "
                + "xmlns:ns2=\"http://www.opengis.net/gml\" "
                + "xmlns=\"http://www.opengis.net/ogc\" xmlns:ns4=\"http://www.opengis.net/wfs\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
                + "<ns4:Query typeName=\"cities\" xmlns=\"\" xmlns:ns5=\"http://www.opengis.net/ogc\">"
                + "<ns5:Filter>" + "<ns5:DWithin>"
                + "<ns5:PropertyName>msGeometry</ns5:PropertyName>"
                + "<ns2:Point srsName=\"EPSG:4326\">"
                + "<ns2:coordinates>-73.0,40.0</ns2:coordinates>" + "</ns2:Point>"
                // units = "m" no good
                + "<ns5:Distance units=\"m\">10000.0</ns5:Distance>" + "</ns5:DWithin>"
                + "</ns5:Filter>" + "</ns4:Query>" + "</ns4:GetFeature>";

        JAXBContext ctx = JAXBContext.newInstance("ogc.schema.opengis.wfs.v_1_0_0");
        JAXBElement<GetFeatureType> getFeatureType = (JAXBElement<GetFeatureType>) ctx
                .createUnmarshaller().unmarshal(new ByteArrayInputStream(getFeatureXml.getBytes()));

        ArgumentCaptor<QueryRequestImpl> captor = ArgumentCaptor.forClass(QueryRequestImpl.class);
        CatalogFramework cf = getCatalogFrameworkForQuery();

        getWfsEndpoint(cf).getFeature(getFeatureType.getValue());

        verify(cf).query(captor.capture());

    }

    @Test
    public void testGetFeature() throws JAXBException, WfsException, UnsupportedQueryException,
        SourceUnavailableException, FederationException {
        String getFeatureXml = "<wfs:GetFeature xmlns:wfs=\"http://www.opengis.net/wfs\" "
                + "      xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "      xmlns:gml=\"http://www.opengis.net/gml\" "
                + "      xmlns:ddf.metacard=\"urn:ddf.catalog.gml:ddf.metacard\" "
                + "      version=\"1.0.0\" service=\"WFS\"> "
                + "    <wfs:Query typeName=\"ddf.metacard:raster_entry\">" + "    </wfs:Query>"
                + "</wfs:GetFeature> ";

        JAXBContext ctx = JAXBContext.newInstance("ogc.schema.opengis.wfs.v_1_0_0");
        JAXBElement<GetFeatureType> getFeatureType = (JAXBElement<GetFeatureType>) ctx
                .createUnmarshaller().unmarshal(new ByteArrayInputStream(getFeatureXml.getBytes()));

        ArgumentCaptor<QueryRequestImpl> captor = ArgumentCaptor.forClass(QueryRequestImpl.class);
        CatalogFramework cf = getCatalogFrameworkForQuery();

        WfsFeatureCollection col = getWfsEndpoint(cf).getFeature(getFeatureType.getValue());
        assertEquals(col.getFeatureMembers().size(), 1);
        assertEquals(col.getFeatureMembers().get(0).getLocation(), METACARD_LOCATION);

        verify(cf).query(captor.capture());

    }

    @Test(expected = WfsException.class)
    public void testGetFeatureBeyondInvalidUnits() throws JAXBException, WfsException,
        UnsupportedQueryException, SourceUnavailableException, FederationException {
        String getFeatureXml = "<?xml version=\"1.0\"?>"
                + "<ns4:GetFeature version=\"1.0.0\" service=\"WFS\" maxFeatures=\"10\" "
                + "xmlns:ns2=\"http://www.opengis.net/gml\" "
                + "xmlns=\"http://www.opengis.net/ogc\" xmlns:ns4=\"http://www.opengis.net/wfs\" xmlns:ns3=\"http://www.w3.org/1999/xlink\">"
                + "<ns4:Query typeName=\"cities\" xmlns=\"\" xmlns:ns5=\"http://www.opengis.net/ogc\">"
                + "<ns5:Filter>" + "<ns5:Beyond>"
                + "<ns5:PropertyName>msGeometry</ns5:PropertyName>"
                + "<ns2:Point srsName=\"EPSG:4326\">"
                + "<ns2:coordinates>-73.0,40.0</ns2:coordinates>" + "</ns2:Point>"
                // units = "m" no good
                + "<ns5:Distance units=\"m\">10000.0</ns5:Distance>" + "</ns5:Beyond>"
                + "</ns5:Filter>" + "</ns4:Query>" + "</ns4:GetFeature>";

        JAXBContext ctx = JAXBContext.newInstance("ogc.schema.opengis.wfs.v_1_0_0");
        JAXBElement<GetFeatureType> getFeatureType = (JAXBElement<GetFeatureType>) ctx
                .createUnmarshaller().unmarshal(new ByteArrayInputStream(getFeatureXml.getBytes()));

        ArgumentCaptor<QueryRequestImpl> captor = ArgumentCaptor.forClass(QueryRequestImpl.class);
        CatalogFramework cf = getCatalogFrameworkForQuery();

        getWfsEndpoint(cf).getFeature(getFeatureType.getValue());

        verify(cf).query(captor.capture());

    }

    @Test
    public void testGetFeatureById() throws JAXBException, WfsException, UnsupportedQueryException,
        SourceUnavailableException, FederationException {
        FeatureIdType fidType = new FeatureIdType();

        fidType.setFid("123456");
        FilterType filterType = new FilterType();
        filterType.getFeatureId().add(fidType);

        FeatureIdType anotherFidType = new FeatureIdType();
        anotherFidType.setFid("654321");
        filterType.getFeatureId().add(anotherFidType);

        QueryType queryType = wfsObjectFactory.createQueryType();
        queryType.setFilter(filterType);
        queryType.setTypeName(MOCK_QNAME);

        ArgumentCaptor<QueryRequestImpl> captor = ArgumentCaptor.forClass(QueryRequestImpl.class);
        CatalogFramework cf = getCatalogFrameworkForQuery();

        getWfsEndpoint(cf).getFeature(getGetFeatureType(queryType));

        verify(cf).query(captor.capture());
        QueryRequestImpl queryRequestImpl = captor.getValue();
        QueryImpl query = (QueryImpl) queryRequestImpl.getQuery();

        assert (query.getFilter() instanceof Filter);

    }

    @Test
    public void testGetFeatureNoFilter() throws WfsException, UnsupportedQueryException,
        SourceUnavailableException, FederationException, URISyntaxException {
        QueryType queryType = wfsObjectFactory.createQueryType();
        queryType.setTypeName(MOCK_QNAME);

        ArgumentCaptor<QueryRequestImpl> captor = ArgumentCaptor.forClass(QueryRequestImpl.class);
        CatalogFramework cf = getCatalogFrameworkForQuery();

        getWfsEndpoint(cf).getFeature(getGetFeatureType(queryType));

        verify(cf).query(captor.capture());

        QueryRequestImpl queryRequest = captor.getValue();
        QueryImpl queryImpl = (QueryImpl) queryRequest.getQuery();
        assert (queryImpl.getFilter() instanceof Filter);
    }

    @Test(expected = WfsException.class)
    public void testGetFeatureNoType() throws WfsException {
        QueryType queryType = wfsObjectFactory.createQueryType();
        queryType.setFilter(getPropertyIsLikeFilter());
        queryType.setTypeName(null);

        getWfsEndpoint(catalogFramework).getFeature(getGetFeatureType(queryType));

    }

    private String marshalXmlObject(WFSCapabilitiesType marshalMe) {
        StringWriter writer = new StringWriter();
        try {
            JAXBContext contextObj = JAXBContext.newInstance(WFSCapabilitiesType.class);

            Marshaller marshallerObj = contextObj.createMarshaller();
            marshallerObj.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);

            marshallerObj.marshal(wfsCapabilityObjectFactory.createWFSCapabilities(marshalMe),
                    writer);

        } catch (JAXBException e) {
            fail(e.getMessage());
        }
        return writer.toString();
    }

    private WfsEndpoint getWfsEndpoint(CatalogFramework cf) {
        return new WfsEndpoint(cf, filterBuilder, mockUriInfo, cache);
    }

    private FilterType getPropertyIsLikeFilter() {
        JAXBElement<PropertyIsLikeType> propIsLike = filterObjectFactory
                .createPropertyIsLike(new PropertyIsLikeType());
        propIsLike.getValue().setEscape(WfsConstants.ESCAPE);
        propIsLike.getValue().setSingleChar(WfsConstants.SINGLE_CHAR);
        propIsLike.getValue().setWildCard(WfsConstants.WILD_CARD);

        LiteralType literalType = new LiteralType();
        literalType.getContent().add("Denver");
        propIsLike.getValue().setLiteral(filterObjectFactory.createLiteral(literalType).getValue());

        PropertyNameType propertyNameType = new PropertyNameType();
        propertyNameType.setContent("NAME");
        propIsLike.getValue().setPropertyName(
                filterObjectFactory.createPropertyName(propertyNameType).getValue());

        FilterType filter = filterObjectFactory.createFilterType();
        filter.setComparisonOps(propIsLike);

        return filter;
    }

    private GetFeatureType getGetFeatureType(QueryType queryType) {
        GetFeatureType getFeature = new ObjectFactory().createGetFeatureType();
        getFeature.getQuery().add(queryType);
        getFeature.setMaxFeatures(BigInteger.valueOf(MAX_FEATURES));

        return getFeature;
    }

    private CatalogFramework getCatalogFrameworkForQuery() throws UnsupportedQueryException,
        SourceUnavailableException, FederationException {
        CatalogFramework cf = mock(CatalogFramework.class);
        // set up responses for catalog framework queries
        QueryResponse mockResponse = mock(QueryResponse.class);
        when(cf.query(any(QueryRequest.class))).thenReturn(mockResponse);

        List<Result> mockResults = new ArrayList<Result>();
        Result mockResult = mock(Result.class);
        MetacardImpl metacard = new MetacardImpl();
        metacard.setLocation(METACARD_LOCATION);
        when(mockResult.getMetacard()).thenReturn(metacard);
        mockResults.add(mockResult);
        when(mockResponse.getResults()).thenReturn(mockResults);

        return cf;
    }

}
