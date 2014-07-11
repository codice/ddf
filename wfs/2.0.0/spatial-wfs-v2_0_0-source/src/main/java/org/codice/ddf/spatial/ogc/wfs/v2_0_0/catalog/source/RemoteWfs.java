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
package org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.source;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;

import net.opengis.wfs.v_2_0_0.DescribeFeatureTypeType;
import net.opengis.wfs.v_2_0_0.GetCapabilitiesType;
import net.opengis.wfs.v_2_0_0.GetFeatureType;
import net.opengis.wfs.v_2_0_0.GetPropertyValueType;
import net.opengis.wfs.v_2_0_0.ValueCollectionType;
import net.opengis.wfs.v_2_0_0.WFSCapabilitiesType;

import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.ws.commons.schema.XmlSchema;
import org.codice.ddf.spatial.ogc.catalog.common.TrustedRemoteSource;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsException;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.source.MarkableStreamInterceptor;
import org.codice.ddf.spatial.ogc.wfs.catalog.source.WfsResponseExceptionMapper;
import org.codice.ddf.spatial.ogc.wfs.catalog.source.reader.FeatureCollectionMessageBodyReader;
import org.codice.ddf.spatial.ogc.wfs.catalog.source.reader.XmlSchemaMessageBodyReader;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.DescribeFeatureTypeRequest;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.GetPropertyValueRequest;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.Wfs;
import org.codice.ddf.spatial.ogc.wfs.v2_0_0.catalog.common.WfsConstants;

/**
 * A client to a WFS 2.0.0 Service. This class uses the {@link Wfs} interface to create a client
 * proxy from the {@link JAXRSClientFactory}.
 * 
 */
public class RemoteWfs extends TrustedRemoteSource implements Wfs {

    private Wfs wfs;

    private FeatureCollectionMessageBodyReader featureCollectionReader;

    public RemoteWfs(String wfsServerUrl, String username, String password,
            boolean disableSSLCertVerification) {
        wfs = createClientBean(Wfs.class, wfsServerUrl, username, password,
                disableSSLCertVerification, initProviders(), getClass().getClassLoader(),
                new MarkableStreamInterceptor());
    }

    private List<? extends Object> initProviders() {
        // We need to tell the JAXBElementProvider to marshal the GetFeatureType
        // class as an element
        // because it is are missing the @XmlRootElement Annotation
        JAXBElementProvider<GetFeatureType> provider = new JAXBElementProvider<GetFeatureType>();
        Map<String, String> jaxbClassMap = new HashMap<String, String>();

        // Ensure a namespace is used when the GetFeature request is generated
        String expandedName = new QName(WfsConstants.WFS_2_0_NAMESPACE, WfsConstants.GET_FEATURE)
                .toString();
        jaxbClassMap.put(GetFeatureType.class.getName(), expandedName);
        provider.setJaxbElementClassMap(jaxbClassMap);
        provider.setMarshallAsJaxbElement(true);

        featureCollectionReader = new FeatureCollectionMessageBodyReader();
        return Arrays.asList(provider, new WfsResponseExceptionMapper(),
                new XmlSchemaMessageBodyReader(), featureCollectionReader);
    }

    public FeatureCollectionMessageBodyReader getFeatureCollectionReader() {
        return featureCollectionReader;
    }

    @Override
    @GET
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public WFSCapabilitiesType getCapabilities(@QueryParam("")
    GetCapabilitiesRequest request) throws WfsException {
        return this.wfs.getCapabilities(request);
    }

    @Override
    @GET
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public XmlSchema describeFeatureType(@QueryParam("")
    DescribeFeatureTypeRequest request) throws WfsException {
        return this.wfs.describeFeatureType(request);
    }

    @Override
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public WfsFeatureCollection getFeature(GetFeatureType getFeature) throws WfsException {
        return this.wfs.getFeature(getFeature);
    }

    @Override
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public WFSCapabilitiesType getCapabilities(GetCapabilitiesType getCapabilitesRequest)
        throws WfsException {
        return this.wfs.getCapabilities(getCapabilitesRequest);
    }

    @Override
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public XmlSchema describeFeatureType(DescribeFeatureTypeType describeFeatureRequest)
        throws WfsException {
        return this.wfs.describeFeatureType(describeFeatureRequest);
    }

    /**
     * GetPropertyValueType - HTTP GET
     */
    @GET
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public ValueCollectionType getPropertyValueType(@QueryParam("")
    GetPropertyValueRequest request) throws WfsException {
            return this.wfs.getPropertyValueType(request);
    }

    /**
     * GetPropertyValueType - HTTP POST
     */
    @POST
    @Consumes({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    @Produces({MediaType.TEXT_XML, MediaType.APPLICATION_XML})
    public XmlSchema getPropertyValueType(GetPropertyValueType propertyValueTypeRequest) 
            throws WfsException {
                return this.wfs.getPropertyValueType(propertyValueTypeRequest);
    }
}
