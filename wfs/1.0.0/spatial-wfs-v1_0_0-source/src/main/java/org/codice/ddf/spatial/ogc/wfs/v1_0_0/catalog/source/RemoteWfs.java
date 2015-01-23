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
package org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.source;

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

import ogc.schema.opengis.wfs.v_1_0_0.DescribeFeatureTypeType;
import ogc.schema.opengis.wfs.v_1_0_0.GetCapabilitiesType;
import ogc.schema.opengis.wfs.v_1_0_0.GetFeatureType;
import ogc.schema.opengis.wfs_capabilities.v_1_0_0.WFSCapabilitiesType;

import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.JAXRSClientFactory;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.apache.ws.commons.schema.XmlSchema;
import org.codice.ddf.spatial.ogc.catalog.common.TrustedRemoteSource;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsException;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsFeatureCollection;
import org.codice.ddf.spatial.ogc.wfs.catalog.source.MarkableStreamInterceptor;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common.DescribeFeatureTypeRequest;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common.Wfs;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.common.Wfs10Constants;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.source.reader.FeatureCollectionMessageBodyReaderWfs10;
import org.codice.ddf.spatial.ogc.wfs.v1_0_0.catalog.source.reader.XmlSchemaMessageBodyReaderWfs10;

/**
 * A client to a WFS 1.0.0 Service. This class uses the {@link Wfs} interface to create a client
 * proxy from the {@link JAXRSClientFactory}.
 * 
 */
public class RemoteWfs extends TrustedRemoteSource implements Wfs {

    private Wfs wfs;

    private FeatureCollectionMessageBodyReaderWfs10 featureCollectionReader;

    public RemoteWfs(String wfsServerUrl, String username, String password,
            boolean disableCnCheck) {
        wfs = createClientBean(Wfs.class, wfsServerUrl, username, password,
                disableCnCheck, initProviders(), getClass().getClassLoader(),
                new MarkableStreamInterceptor());
    }

    private List<? extends Object> initProviders() {
        // We need to tell the JAXBElementProvider to marshal the GetFeatureType
        // class as an element
        // because it is are missing the @XmlRootElement Annotation
        JAXBElementProvider<GetFeatureType> provider = new JAXBElementProvider<GetFeatureType>();
        Map<String, String> jaxbClassMap = new HashMap<String, String>();

        // Ensure a namespace is used when the GetFeature request is generated
        String expandedName = new QName(Wfs10Constants.WFS_NAMESPACE, Wfs10Constants.GET_FEATURE).toString();
        jaxbClassMap.put(GetFeatureType.class.getName(), expandedName);
        provider.setJaxbElementClassMap(jaxbClassMap);
        provider.setMarshallAsJaxbElement(true);

        featureCollectionReader = new FeatureCollectionMessageBodyReaderWfs10();

        return Arrays.asList(provider, new WfsResponseExceptionMapper(),
                new XmlSchemaMessageBodyReaderWfs10(), featureCollectionReader);
    }

    /**
     * Sets the TLS Parameters on the current client
     */
    protected void setTlsParameters() {
        Client client = WebClient.client(wfs);
        setTlsParameters(client);
    }

    /**
     * Sets the timeouts to use for connection and receive requests.
     *
     * @param connectionTimeout
     *            Time in milliseconds to allow a connection.
     * @param receiveTimeout
     *            Time in milliseconds to allow a receive.
     */
    public void setTimeouts(Integer connectionTimeout, Integer receiveTimeout) {
        this.configureTimeouts(WebClient.client(wfs), connectionTimeout, receiveTimeout);
    }

    public FeatureCollectionMessageBodyReaderWfs10 getFeatureCollectionReader() {
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
}
