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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentHashMap;

import javax.xml.namespace.QName;

import org.apache.cxf.common.util.StringUtils;
import org.apache.ws.commons.schema.XmlSchema;
import org.apache.ws.commons.schema.XmlSchemaCollection;
import org.apache.ws.commons.schema.XmlSchemaComplexContent;
import org.apache.ws.commons.schema.XmlSchemaComplexContentExtension;
import org.apache.ws.commons.schema.XmlSchemaComplexType;
import org.apache.ws.commons.schema.XmlSchemaElement;
import org.apache.ws.commons.schema.XmlSchemaForm;
import org.apache.ws.commons.schema.XmlSchemaImport;
import org.apache.ws.commons.schema.XmlSchemaSequence;
import org.apache.ws.commons.schema.XmlSchemaSequenceMember;
import org.apache.ws.commons.schema.XmlSchemaSerializer;
import org.apache.ws.commons.schema.constants.Constants;
import org.apache.ws.commons.schema.utils.NamespaceMap;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.AttributeDescriptorComparator;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsConstants;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsQnameBuilder;
import org.codice.ddf.spatial.ogc.wfs.catalog.endpoint.utils.ServicePropertiesMap;
import org.osgi.framework.BundleContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.CatalogFramework;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeType.AttributeFormat;
import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.operation.SourceInfoResponse;
import ddf.catalog.operation.impl.SourceInfoRequestEnterprise;
import ddf.catalog.source.SourceDescriptor;
import ddf.catalog.source.SourceUnavailableException;

/**
 * Translates MetacardTypes registered in the OSGi Service Registry into an XmlSchema that adheres
 * to the WFS v1.0.0 specification. These schemas are used in response to a "DescribeFeatureType"
 * request.
 */
public class FeatureTypeSchemaCache {

    private Map<MetacardType, Map<String, Object>> services;

    private CatalogFramework framework;

    private Map<QName, XmlSchema> contentTypeSchemas = new ConcurrentHashMap<QName, XmlSchema>();

    private static final Logger LOGGER = LoggerFactory.getLogger(FeatureTypeSchemaCache.class);

    // Protected for unit test
    protected static final QName GML_GEO_PROPERTY_TYPE = new QName(WfsConstants.GML_NAMESPACE,
            "GeometryPropertyType");

    private static final QName GML_FEATURE = new QName(WfsConstants.GML_NAMESPACE, "_Feature");

    private static final QName ABSTRACT_FEATURE_TYPE = new QName(WfsConstants.GML_NAMESPACE,
            "AbstractFeatureType");

    public FeatureTypeSchemaCache(BundleContext context, ServicePropertiesMap services,
            CatalogFramework ddf) {
        this.services = services;
        this.framework = ddf;
        updateAvailableTypesFromFramework();
    }

    /**
     * Retrieves a schema based on its {@link QName}.
     * 
     * @param qname
     *            - the {@link QName} of the type.
     * @return - the schema that describes the type.
     */
    public XmlSchema getSchemaByQname(final QName qname) {
        updateAvailableTypesFromFramework();
        if (null == qname) {
            return null;
        }
        // If no namespace is provided check against the "localPart" of the
        // QName
        if (StringUtils.isEmpty(qname.getNamespaceURI())) {
            QName foundQname = getQnamefromLocalPart(qname.getLocalPart());
            if (null != foundQname) {
                return contentTypeSchemas.get(foundQname);
            }
        }
        return contentTypeSchemas.get(qname);
    }

    /**
     * Returns a list of {@link QName}s of all the cached types.
     * 
     * @return - the {@link Set} of {@link QName}s of all the cached types.
     */
    public Set<QName> getFeatureTypeQnames() {
        updateAvailableTypesFromFramework();
        return contentTypeSchemas.keySet();
    }

    public QName getQnamefromLocalPart(String localPart) {
        QName foundQname = null;
        for (QName key : contentTypeSchemas.keySet()) {
            if (key.getLocalPart().equals(localPart)) {
                if (null == foundQname) {
                    foundQname = key;
                } else {
                    // If we found this type more than once we don't know
                    // which to return, so
                    // return null
                    return null;
                }
            }
        }
        return foundQname;
    }

    // Called to update the cache of schemas whenever the service registry
    // changes.
    private Map<QName, XmlSchema> updateSchemas() {
        Map<QName, XmlSchema> schemas = new HashMap<QName, XmlSchema>();
        List<String> metacardTypes = new ArrayList<String>();
        for (Entry<MetacardType, Map<String, Object>> entry : services.entrySet()) {
            // Get the Content Types this MetacardType supports
            Object propertyValue = entry.getValue().get(Metacard.CONTENT_TYPE);
            if (propertyValue instanceof String[]) {
                MetacardType metacardType = entry.getKey();
                final String metacardTypeName = metacardType.getName();
                List<String> contentTypes = Arrays.asList((String[]) propertyValue);
                LOGGER.debug("The CONTENT_TYPE property was set to: {}",
                        Arrays.toString(contentTypes.toArray()));
                for (String contentTypeName : contentTypes) {
                    // Build the QName to uniquely identify this content type
                    QName qname = WfsQnameBuilder.buildQName(metacardTypeName, contentTypeName);
                    if (null == contentTypeSchemas.get(qname)) {
                        // Add the schema to the map.
                        schemas.put(qname, buildSchemaFromMetacardType(metacardType, qname));
                    }
                    metacardTypes.add(qname.getPrefix());
                }

            }
        }
        // Remove the schemas associated to metacards that no longer exist
        removeInvalidMetacardTypes(metacardTypes);

        return schemas;
    }

    // Helper method to see if a schema is already registered with this name
    // TODO - What about duplicates???
    private void createSchemaByLocalName(final String typeName) {
        boolean found = false;
        for (QName qname : contentTypeSchemas.keySet()) {
            if (qname.getLocalPart().equals(typeName)) {
                found = true;
            }
        }

        // If we did not find the schema then we need to create one based on
        // BASIC_METACARD and add it to the map
        if (!found) {
            LOGGER.debug("Creating a new schema from BASIC_METACARD for: {}", typeName);
            QName newQname = WfsQnameBuilder.buildQName(BasicTypes.BASIC_METACARD.getName(),
                    typeName);
            XmlSchema schema = buildSchemaFromMetacardType(BasicTypes.BASIC_METACARD, newQname);
            contentTypeSchemas.put(newQname, schema);
        }
    }

    private void removeInvalidMetacardTypes(List<String> validNames) {
        Set<QName> qnames = contentTypeSchemas.keySet();
        Set<QName> missingTypes = new HashSet<QName>();
        for (QName qname : qnames) {
            if (!qname.getPrefix().startsWith(BasicTypes.BASIC_METACARD.getName())) {
                if (!validNames.contains(qname.getPrefix())) {
                    missingTypes.add(qname);
                }
            }
        }
        if (!missingTypes.isEmpty()) {
            LOGGER.debug("Removing the following from the schema cache: {}",
                    Arrays.toString(missingTypes.toArray()));
            qnames.removeAll(missingTypes);
        }
    }

    private XmlSchema buildSchemaFromMetacardType(MetacardType metacardType, QName qname) {
        final String typeName = qname.getLocalPart();
        XmlSchema schema = new XmlSchema(qname.getNamespaceURI(), new XmlSchemaCollection());
        schema.setElementFormDefault(XmlSchemaForm.QUALIFIED);

        // Add the GML Namespace
        NamespaceMap nsMap = new NamespaceMap();
        nsMap.add("", XmlSchemaSerializer.XSD_NAMESPACE);
        nsMap.add(WfsConstants.GML_PREFIX, WfsConstants.GML_NAMESPACE);
        nsMap.add(qname.getPrefix(), qname.getNamespaceURI());
        schema.setNamespaceContext(nsMap);

        // Add the import GML statement so the schema will validate
        XmlSchemaImport gmlSchemaImport = new XmlSchemaImport(schema);
        gmlSchemaImport.setNamespace(WfsConstants.GML_NAMESPACE);
        gmlSchemaImport.setSchemaLocation(WfsConstants.GML_SCHEMA_LOCATION);
        schema.getExternals().add(gmlSchemaImport);

        // Add the metacardType name as the root Element name
        XmlSchemaElement rootElement = new XmlSchemaElement(schema, true);
        rootElement.setName(typeName);

        XmlSchemaSequence rootSequence = new XmlSchemaSequence();
        // Add the translated attributes
        // Set<AttributeDescriptor> attributeDescriptors = new
        // HashSet<AttributeDescriptor>();
        // attributeDescriptors.addAll(metacardType.getAttributeDescriptors());
        rootSequence.getItems().addAll(
                translateAttributeDescriptors(metacardType.getAttributeDescriptors(), schema));

        XmlSchemaComplexContentExtension contentExtension = new XmlSchemaComplexContentExtension();
        contentExtension.setBaseTypeName(ABSTRACT_FEATURE_TYPE);
        contentExtension.setParticle(rootSequence);

        XmlSchemaComplexType rootComplexType = new XmlSchemaComplexType(schema, true);
        rootComplexType.setName(typeName + "Type");
        XmlSchemaComplexContent complexContent = new XmlSchemaComplexContent();
        complexContent.setContent(contentExtension);
        rootComplexType.setContentModel(complexContent);
        rootElement.setSchemaTypeName(rootComplexType.getQName());
        rootElement.setSubstitutionGroup(GML_FEATURE);

        // Add the Element to the Schema
        schema.getElements().put(rootElement.getQName(), rootElement);

        return schema;
    }

    private List<XmlSchemaSequenceMember> translateAttributeDescriptors(
            Set<AttributeDescriptor> attributeDescriptors, XmlSchema parent) {
        // Sort the AttributeDescriptors
        SortedSet<AttributeDescriptor> sortedADs = new TreeSet<AttributeDescriptor>(
                new AttributeDescriptorComparator());
        sortedADs.addAll(attributeDescriptors);
        List<XmlSchemaSequenceMember> members = new ArrayList<XmlSchemaSequenceMember>();

        for (AttributeDescriptor descriptor : sortedADs) {
            if (null != descriptor && descriptor.isStored()) {
                QName xsdType = getXsdTypeFromAttributeFormat(descriptor.getType()
                        .getAttributeFormat());
                if (null != xsdType) {
                    XmlSchemaElement simpleElement = new XmlSchemaElement(parent, false);
                    simpleElement.setName(descriptor.getName());
                    simpleElement.setSchemaTypeName(xsdType);
                    simpleElement.setMinOccurs(0);
                    if (descriptor.isMultiValued()) {
                        // Unbounded
                        simpleElement.setMaxOccurs(Long.MAX_VALUE);
                    }
                    if (!Metacard.ID.equalsIgnoreCase(descriptor.getName())) {
                        simpleElement.setMinOccurs(0);
                    }
                    members.add(simpleElement);
                }
            }
        }
        return members;
    }

    private QName getXsdTypeFromAttributeFormat(AttributeFormat format) {
        switch (format) {
        case STRING:
            return Constants.XSD_STRING;
        case XML:
            return Constants.XSD_ANYTYPE;
        case BOOLEAN:
            return Constants.XSD_BOOLEAN;
        case DATE:
            return Constants.XSD_DATETIME;
        case SHORT:
            return Constants.XSD_SHORT;
        case INTEGER:
            return Constants.XSD_INTEGER;
        case LONG:
            return Constants.XSD_LONG;
        case FLOAT:
            return Constants.XSD_FLOAT;
        case DOUBLE:
            return Constants.XSD_DOUBLE;
        case GEOMETRY:
            return GML_GEO_PROPERTY_TYPE;
        case BINARY:
            return Constants.XSD_BASE64;
        case OBJECT:
        default:
            // No translation can be made.
            return null;
        }

    }

    private void updateAvailableTypesFromFramework() {
        List<String> contentTypes = new ArrayList<String>();
        SourceInfoResponse response = null;
        try {
            // Get info on all the configured Sources
            response = framework.getSourceInfo(new SourceInfoRequestEnterprise(true));
        } catch (SourceUnavailableException e) {
            // Ignore this. We should never get this exception since we are
            // doing an enterprise
            // request.
            LOGGER.error("Found Source unavailable: {}", e);
        }
        if (response != null) {
            Set<SourceDescriptor> sourceDescriptors = response.getSourceInfo();
            // Get the Content Types for each Source.
            for (SourceDescriptor sourceDescriptor : sourceDescriptors) {
                Set<ContentType> types = sourceDescriptor.getContentTypes();
                for (ContentType contentType : types) {
                    contentTypes.add(contentType.getName());
                }
            }
        }

        // First check if any of the serviceRefernces have been updated.
        contentTypeSchemas.putAll(updateSchemas());
        if (contentTypes != null) {
            for (String name : contentTypes) {
                // This will determine if the schema exists or create it if it
                // doesn't.
                createSchemaByLocalName(name);
            }
        }

        // Remove the content types that are no longer available
        Set<QName> missingTypes = new HashSet<QName>();
        for (QName qname : contentTypeSchemas.keySet()) {
            if (!contentTypes.contains(qname.getLocalPart())) {
                missingTypes.add(qname);
            }
        }
        if (!missingTypes.isEmpty()) {
            LOGGER.debug("Removing the following from the schema cache: {}",
                    Arrays.toString(missingTypes.toArray()));
            contentTypeSchemas.keySet().removeAll(missingTypes);
        }
    }
}
