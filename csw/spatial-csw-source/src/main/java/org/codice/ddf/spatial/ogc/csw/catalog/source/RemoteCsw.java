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
package org.codice.ddf.spatial.ogc.csw.catalog.source;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.xml.namespace.QName;

import com.thoughtworks.xstream.converters.Converter;
import net.opengis.cat.csw.v_2_0_2.CapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.DescribeRecordResponseType;
import net.opengis.cat.csw.v_2_0_2.DescribeRecordType;
import net.opengis.cat.csw.v_2_0_2.GetCapabilitiesType;
import net.opengis.cat.csw.v_2_0_2.GetRecordByIdType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsResponseType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.TransactionResponseType;
import net.opengis.cat.csw.v_2_0_2.TransactionType;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.Bus;
import org.apache.cxf.configuration.jsse.TLSClientParameters;
import org.apache.cxf.jaxrs.client.Client;
import org.apache.cxf.jaxrs.client.ClientConfiguration;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.trust.STSClient;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.codice.ddf.spatial.ogc.catalog.common.TrustedRemoteSource;
import org.codice.ddf.spatial.ogc.csw.catalog.common.Csw;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswJAXBElementProvider;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswSourceConfiguration;
import org.codice.ddf.spatial.ogc.csw.catalog.common.DescribeRecordRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordByIdRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.common.GetRecordsRequest;
import org.codice.ddf.spatial.ogc.csw.catalog.source.reader.GetRecordsMessageBodyReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.Subject;
import ddf.security.sts.client.configuration.STSClientConfiguration;

/**
 * A client to a CSW 2.0.2 Service. This class uses the {@link Csw} interface to create a client
 * proxy from the {@link JAXRSClientFactoryBean}.
 */
public class RemoteCsw extends TrustedRemoteSource implements Csw {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteCsw.class.getName());

    private CswJAXBElementProvider<GetRecordsType> getRecordsTypeProvider;

    private List<String> jaxbElementClassNames = new ArrayList<String>();

    private Map<String, String> jaxbElementClassMap = new HashMap<String, String>();

    protected Csw csw;

    /**
     * Instantiates a new RemoteCsw
     *
     * @param cswTransformProvider The reference to the the CSW Transform Provider
     * @param cswSourceConfiguration   The Csw Source Configuration
     */
    public RemoteCsw(Converter cswTransformProvider,
            CswSourceConfiguration cswSourceConfiguration) {
        csw = createClientBean(Csw.class, cswSourceConfiguration.getCswUrl(),
                cswSourceConfiguration.getUsername(), cswSourceConfiguration.getPassword(),
                cswSourceConfiguration.getDisableCnCheck(),
                initProviders(cswTransformProvider, cswSourceConfiguration), getClass()
                        .getClassLoader());
    }

    /**
     * Sets the TLS Parameters on the current client
     */
    protected void setTlsParameters() {
        Client client = WebClient.client(csw);
        setTlsParameters(client);
    }

    /**
     * Sets the timeouts to use for connection and receive requests.
     * @param connectionTimeout Time in milliseconds to allow a connection.
     * @param receiveTimeout Time in milliseconds to allow a receive.
     */
    public void setTimeouts(Integer connectionTimeout, Integer receiveTimeout) {
        this.configureTimeouts(WebClient.client(csw), connectionTimeout, receiveTimeout);
    }

    public void setSubject(Subject subject) {
        Client client = WebClient.client(csw);
        client.reset();
        RestSecurity.setSubjectOnClient(subject, client);
    }
    
    public void setSAMLAssertion(STSClientConfiguration stsClientConfig) {
        Client client = WebClient.client(csw);
        LOGGER.debug("ENTERING: setSAMLAssertion()");
        if (stsClientConfig == null || StringUtils.isBlank(stsClientConfig.getAddress())) {
            LOGGER.debug("STSClientConfiguration is either null or its address is blank - assuming no STS Client is configured, so no SAML assertion will get generated.");
            return;
        }
        ClientConfiguration clientConfig = WebClient.getConfig(client);
        Bus bus = clientConfig.getBus();
        STSClient stsClient = configureSTSClient(bus, stsClientConfig);
        stsClient.setTokenType(stsClientConfig.getAssertionType());
        stsClient.setKeyType(stsClientConfig.getKeyType());
        stsClient.setKeySize(Integer.valueOf(stsClientConfig.getKeySize()));
        try {
            SecurityToken securityToken = stsClient.requestSecurityToken(stsClientConfig.getAddress());
            org.w3c.dom.Element samlToken = securityToken.getToken();
            if (samlToken != null) {
                Cookie cookie = new Cookie(RestSecurity.SECURITY_COOKIE_NAME, RestSecurity.encodeSaml(
                        samlToken));
                client.reset();
                client.cookie(cookie);
            } else {
                LOGGER.debug("Attempt to retrieve SAML token resulted in null token - could not add token to request");
            }
        } catch (Exception e) {
            LOGGER.warn("Exception trying to get SAML assertion", e);
        }
        LOGGER.debug("EXITING: setSAMLAssertion()");
    }    

    protected List<? extends Object> initProviders(
            Converter cswTransformProvider,
            CswSourceConfiguration cswSourceConfiguration) {
        getRecordsTypeProvider = new CswJAXBElementProvider<GetRecordsType>();
        getRecordsTypeProvider.setMarshallAsJaxbElement(true);

        // Adding class names that need to be marshalled/unmarshalled to
        // jaxbElementClassNames list
        jaxbElementClassNames.add(GetRecordsType.class.getName());
        jaxbElementClassNames.add(CapabilitiesType.class.getName());
        jaxbElementClassNames.add(GetCapabilitiesType.class.getName());
        jaxbElementClassNames.add(GetRecordsResponseType.class.getName());

        getRecordsTypeProvider.setJaxbElementClassNames(jaxbElementClassNames);

        // Adding map entry of <Class Name>,<Qualified Name> to
        // jaxbElementClassMap
        String expandedName = new QName(CswConstants.CSW_OUTPUT_SCHEMA, CswConstants.GET_RECORDS)
                .toString();
        LOGGER.debug("{} expanded name: {}", CswConstants.GET_RECORDS, expandedName);
        jaxbElementClassMap.put(GetRecordsType.class.getName(), expandedName);

        String getCapsEpandedName = new QName(CswConstants.CSW_OUTPUT_SCHEMA,
                CswConstants.GET_CAPABILITIES).toString();
        LOGGER.debug("{} expanded name: {}", CswConstants.GET_CAPABILITIES, expandedName);
        jaxbElementClassMap.put(GetCapabilitiesType.class.getName(), getCapsEpandedName);

        String capsExpandedName = new QName(CswConstants.CSW_OUTPUT_SCHEMA,
                CswConstants.CAPABILITIES).toString();
        LOGGER.debug("{} expanded name: {}", CswConstants.CAPABILITIES, capsExpandedName);
        jaxbElementClassMap.put(CapabilitiesType.class.getName(), capsExpandedName);

        String caps201ExpandedName = new QName("http://www.opengis.net/cat/csw",
                CswConstants.CAPABILITIES).toString();
        LOGGER.debug("{} expanded name: {}", CswConstants.CAPABILITIES, caps201ExpandedName);
        jaxbElementClassMap.put(CapabilitiesType.class.getName(), caps201ExpandedName);

        getRecordsTypeProvider.setJaxbElementClassMap(jaxbElementClassMap);

        GetRecordsMessageBodyReader grmbr = new GetRecordsMessageBodyReader(
                cswTransformProvider, cswSourceConfiguration);
        return Arrays.asList(getRecordsTypeProvider, new CswResponseExceptionMapper(), grmbr);

    }

    @Override
    @GET
    @Consumes({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    public DescribeRecordResponseType describeRecord(DescribeRecordRequest request)
            throws CswException {
        return csw.describeRecord(request);
    }

    @Override
    @POST
    @Consumes({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    public DescribeRecordResponseType describeRecord(DescribeRecordType request)
            throws CswException {
        return csw.describeRecord(request);
    }

    @Override
    @GET
    @Consumes({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    public CapabilitiesType getCapabilities(GetCapabilitiesRequest request) throws CswException {
        return csw.getCapabilities(request);
    }

    @Override
    @POST
    @Consumes({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    public CapabilitiesType getCapabilities(GetCapabilitiesType request) throws CswException {
        return csw.getCapabilities(request);
    }

    @Override
    @GET
    @Consumes({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    public CswRecordCollection getRecordById(GetRecordByIdRequest request)
            throws CswException {
        return csw.getRecordById(request);
    }

    @Override
    @POST
    @Consumes({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    public CswRecordCollection getRecordById(GetRecordByIdType request) throws CswException {
        return csw.getRecordById(request);
    }

    @Override
    @GET
    @Consumes({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    public CswRecordCollection getRecords(GetRecordsRequest request) throws CswException {
        return csw.getRecords(request);
    }

    @Override
    @POST
    @Consumes({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    public CswRecordCollection getRecords(GetRecordsType request) throws CswException {
        return csw.getRecords(request);
    }

    @Override
    @POST
    @Consumes({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    @Produces({ MediaType.TEXT_XML, MediaType.APPLICATION_XML })
    public TransactionResponseType transaction(TransactionType request) throws CswException {
        return csw.transaction(request);
    }

    /*
     * Set the version to CSW 2.0.1. The schemas don't vary much between 2.0.2 and 2.0.1. The
     * largest difference is the namespace itself. This method tells CXF JAX-RS to transform
     * outgoing messages CSW namespaces to 2.0.1.
     */
    public void setCsw201() {
        Map<String, String> outTransformElements = new HashMap<String, String>();
        outTransformElements.put("{" + CswConstants.CSW_OUTPUT_SCHEMA + "}*",
                "{http://www.opengis.net/cat/csw}*");
        getRecordsTypeProvider.setOutTransformElements(outTransformElements);
    }
}
