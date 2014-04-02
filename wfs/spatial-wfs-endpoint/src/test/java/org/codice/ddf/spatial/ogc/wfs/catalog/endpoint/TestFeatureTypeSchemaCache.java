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
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.xml.namespace.QName;

import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaType;
import org.apache.ws.commons.schema.constants.Constants;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsQnameBuilder;
import org.codice.ddf.spatial.ogc.wfs.catalog.endpoint.utils.ServicePropertiesMap;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.ContentTypeImpl;
import ddf.catalog.operation.SourceInfoRequest;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;
import ddf.catalog.source.impl.SourceDescriptorImpl;

public class TestFeatureTypeSchemaCache {

    private FeatureTypeSchemaCache cache;

    private static final String REPORT = "report";

    private static final String THING = "thing";

    private static BundleContext mockContext = mock(BundleContext.class);

    private static ServicePropertiesMap<MetacardType> mockServiceList = new ServicePropertiesMap<MetacardType>();

    private static CatalogFramework catalogFramework = mock(CatalogFramework.class);

    private static SourceInfoResponse mockSourceInfoResponse = mock(SourceInfoResponse.class);

    private Set<ContentType> contentTypes = new HashSet<ContentType>();

    private List<String> basicNames = getBasicAttributeNames();

    @BeforeClass
    public static void beforeClass() {
        when(mockContext.getService(any(ServiceReference.class)))
                .thenReturn(new MockMetacardType());
    }

    @Before
    public void setUp() throws SourceUnavailableException {
        mockServiceList.bindService(new MockMetacardType(), MockMetacardType.PROPERTIES);
        when(catalogFramework.getSourceInfo(any(SourceInfoRequest.class))).thenReturn(
                mockSourceInfoResponse);
        Set<SourceDescriptor> sourceDescriptors = new HashSet<SourceDescriptor>();
        contentTypes.add(new ContentTypeImpl(MockMetacardType.IMAGE, MockMetacardType.IMAGE));
        contentTypes.add(new ContentTypeImpl(MockMetacardType.VIDEO, MockMetacardType.VIDEO));
        sourceDescriptors.add(new SourceDescriptorImpl("sourceId", contentTypes));
        when(mockSourceInfoResponse.getSourceInfo()).thenReturn(sourceDescriptors);
        cache = new FeatureTypeSchemaCache(mockContext, mockServiceList, catalogFramework);
    }

    @Test
    public void testGetSchemaByQnameMockMetacard() throws UnsupportedEncodingException {
        XmlSchema schema = cache.getSchemaByQname(WfsQnameBuilder.buildQName(MockMetacardType.NAME,
                MockMetacardType.IMAGE));

        StringWriter writer = new StringWriter();
        schema.write(writer);
        assertNotNull(schema);

        // Get the root element
        XmlSchemaElement rootElement = schema.getElementByName(MockMetacardType.IMAGE);
        assertNotNull(rootElement);
        // Validate the type its defined as exists
        XmlSchemaType rootType = schema.getTypeByName(rootElement.getSchemaTypeName());
        assertNotNull(rootType);
        assertTrue(rootType instanceof XmlSchemaComplexType);
        XmlSchemaComplexType complexType = (XmlSchemaComplexType) rootType;
        // Validate the Complex Type is formed correctly
        assertTrue(complexType.getContentModel() instanceof XmlSchemaComplexContent);
        XmlSchemaComplexContent complexContent = (XmlSchemaComplexContent) complexType
                .getContentModel();
        assertTrue(complexContent.getContent() instanceof XmlSchemaComplexContentExtension);
        XmlSchemaSequence sequence = (XmlSchemaSequence) ((XmlSchemaComplexContentExtension) complexContent
                .getContent()).getParticle();
        assertNotNull(sequence);
        List<XmlSchemaSequenceMember> elements = sequence.getItems();

        // Check each of the AttributeDescriptors were translated as expected.
        Set<AttributeDescriptor> descriptors = new MockMetacardType().getAttributeDescriptors();
        for (AttributeDescriptor descriptor : descriptors) {
            // We cannot translate "OBJECT"
            if (descriptor.getType().getAttributeFormat() != AttributeFormat.OBJECT) {
                Boolean found = false;
                for (XmlSchemaSequenceMember xmlSchemaSequenceMember : elements) {
                    XmlSchemaElement element = (XmlSchemaElement) xmlSchemaSequenceMember;
                    if (descriptor.getName().equals(element.getName())) {
                        assertTrue(compareDescriptorTypeToElementType(descriptor.getType()
                                .getAttributeFormat(), element.getSchemaTypeName()));
                        assertNotBasicAttribute(element);
                        found = true;
                    }
                }

            }
        }
    }

    @Test
    public void testGetSchemaByQnameInvalidQname() {
        XmlSchema schema = cache.getSchemaByQname(new QName("DNE"));
        assertNull(schema);
    }

    @Test
    public void testGetFeatureTypesByContentTypes() {
        // Have the mockService Reference return a different list.
        mockServiceList.unbindService(new MockMetacardType());
        contentTypes.clear();
        contentTypes.add(new ContentTypeImpl(THING, THING));
        contentTypes.add(new ContentTypeImpl(REPORT, REPORT));
        Set<QName> qnames = cache.getFeatureTypeQnames();
        assertEquals(2, qnames.size());
        for (QName qName : qnames) {
            assertTrue(REPORT.equals(qName.getLocalPart()) || THING.equals(qName.getLocalPart()));

        }
    }

    @Test
    public void testGetFeatureTypesByContentTypesNullList() {
        Set<QName> qnames = cache.getFeatureTypeQnames();
        assertEquals(2, qnames.size());
        for (QName qName : qnames) {

            assertTrue(MockMetacardType.IMAGE.equals(qName.getLocalPart())
                    || MockMetacardType.VIDEO.equals(qName.getLocalPart()));
            assertTrue(qName.getPrefix().startsWith(MockMetacardType.NAME));
        }
    }

    @Test
    public void testRegisterRemoveOldContentTypes() {
        assertEquals(contentTypes.size(), cache.getFeatureTypeQnames().size());
        ContentType ct = new ContentTypeImpl(REPORT, REPORT);
        contentTypes.add(ct);
        assertEquals(contentTypes.size(), cache.getFeatureTypeQnames().size());
        contentTypes.remove(ct);
        assertEquals(contentTypes.size(), cache.getFeatureTypeQnames().size());

    }

    private Boolean compareDescriptorTypeToElementType(AttributeFormat format, QName schemaType) {
        switch (format) {
        case STRING:
            return Constants.XSD_STRING.equals(schemaType);
        case XML:
            return Constants.XSD_ANYTYPE.equals(schemaType);
        case BOOLEAN:
            return Constants.XSD_BOOLEAN.equals(schemaType);
        case DATE:
            return Constants.XSD_DATETIME.equals(schemaType);
        case SHORT:
            return Constants.XSD_SHORT.equals(schemaType);
        case INTEGER:
            return Constants.XSD_INTEGER.equals(schemaType);
        case LONG:
            return Constants.XSD_LONG.equals(schemaType);
        case FLOAT:
            return Constants.XSD_FLOAT.equals(schemaType);
        case DOUBLE:
            return Constants.XSD_DOUBLE.equals(schemaType);
        case GEOMETRY:
            return FeatureTypeSchemaCache.GML_GEO_PROPERTY_TYPE.equals(schemaType);
        case BINARY:
            return Constants.XSD_BASE64.equals(schemaType);
        case OBJECT:
        default:
            return false;
        }
    }

    private boolean assertNotBasicAttribute(XmlSchemaElement element) {
        return !basicNames.contains(element.getName());
    }

    private List<String> getBasicAttributeNames() {
        List<String> basicNames = new ArrayList<String>(BasicTypes.BASIC_METACARD
                .getAttributeDescriptors().size());
        for (AttributeDescriptor ad : BasicTypes.BASIC_METACARD.getAttributeDescriptors()) {
            basicNames.add(ad.getName());
        }
        return basicNames;
    }

}
