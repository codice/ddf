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
package org.codice.ddf.spatial.ogc.wcs.catalog.resource.reader;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import net.opengis.gml.profiles.gml4wcs.v_1_0_0.CodeListType;
import net.opengis.gml.profiles.gml4wcs.v_1_0_0.CodeType;
import net.opengis.gml.profiles.gml4wcs.v_1_0_0.DirectPositionType;
import net.opengis.gml.profiles.gml4wcs.v_1_0_0.EnvelopeType;
import net.opengis.gml.profiles.gml4wcs.v_1_0_0.ObjectFactory;
import net.opengis.wcs.v_1_0_0.CoverageDescription;
import net.opengis.wcs.v_1_0_0.CoverageOfferingType;
import net.opengis.wcs.v_1_0_0.DescribeCoverage;
import net.opengis.wcs.v_1_0_0.DomainSubsetType;
import net.opengis.wcs.v_1_0_0.GetCapabilities;
import net.opengis.wcs.v_1_0_0.GetCoverage;
import net.opengis.wcs.v_1_0_0.OutputType;
import net.opengis.wcs.v_1_0_0.SpatialDomainType;
import net.opengis.wcs.v_1_0_0.SpatialSubsetType;
import net.opengis.wcs.v_1_0_0.TimeSequenceType;
import net.opengis.wcs.v_1_0_0.WCSCapabilitiesType;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.wcs.catalog.GetCoverageResponse;
import org.codice.ddf.spatial.ogc.wcs.catalog.WcsConfiguration;
import org.codice.ddf.spatial.ogc.wcs.catalog.WcsException;
import org.codice.ddf.spatial.ogc.wcs.catalog.impl.RemoteWcs;
import org.geotools.geometry.Envelope2D;
import org.opengis.geometry.DirectPosition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.impl.ResourceResponseImpl;
import ddf.catalog.resource.Resource;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.resource.ResourceReader;
import ddf.catalog.resource.impl.ResourceImpl;
import ddf.mime.MimeTypeMapper;
import ddf.mime.MimeTypeResolutionException;

public class WcsResourceReader {

    private static final Logger LOGGER = LoggerFactory.getLogger(WcsResourceReader.class);

    private static final String WCS_SCHEME = "wcs";

    private static final String URI_PARTS_DELIMITER = ":";

    private static final String TITLE = "WCS Resource Reader";

    private static final String ORIGINAL_PRODUCT_FORMAT = "AS_IS;rrds:0";

    private static final String FILENAME_STR = "filename=";

    protected WcsConfiguration wcsConfiguration;

    protected RemoteWcs remoteWcs;

    /** Mapper for file extensions-to-mime types (and vice versa) */
    private MimeTypeMapper mimeTypeMapper;

    private WCSCapabilitiesType capabilities;

    private static final JAXBContext jaxbContext = initJaxbContext();

    public WcsResourceReader() {
        wcsConfiguration = new WcsConfiguration();
    }

    /**
     * Constructor for unit testing only.
     * 
     * @param remoteWcs
     */
    public WcsResourceReader(RemoteWcs remoteWcs, String id, String wcsUrl,
            MimeTypeMapper mimeTypeMapper) {
        this();
        this.remoteWcs = remoteWcs;
        wcsConfiguration.setId(id);
        wcsConfiguration.setWcsUrl(wcsUrl);
        setMimeTypeMapper(mimeTypeMapper);
        configureWcs();
    }

    /**
     * Initializes the WcsResourceReader by connecting to the WCS Server
     */
    public void init() {
        connectToRemoteWcs();
    }

    /**
     * Clean-up when shutting down the Wcs
     */
    public void destroy() {
    }

    private void connectToRemoteWcs() {

        LOGGER.debug("Connecting to remote WCS Server " + wcsConfiguration.getWcsUrl());

        try {
            remoteWcs = new RemoteWcs(wcsConfiguration);
            configureWcs();
        } catch (IllegalArgumentException iae) {
            LOGGER.error("Unable to create RemoteWcs.", iae);
            remoteWcs = null;
        }
    }

    protected void configureWcs() {
        try {
            if (remoteWcs != null) {
                GetCapabilities request = new GetCapabilities();
                this.capabilities = remoteWcs.getCapabilities(request);
            }
        } catch (WcsException e) {
            LOGGER.warn("Error getting capabilities of WCS service", e);
        }
    }

    public String getWcsUrl() {
        return wcsConfiguration.getWcsUrl();
    }

    public void setWcsUrl(String wcsUrl) {
        wcsConfiguration.setWcsUrl(wcsUrl);
    }

    public String getUsername() {
        return wcsConfiguration.getUsername();
    }

    public void setUsername(String username) {
        wcsConfiguration.setUsername(username);
    }

    public String getPassword() {
        return wcsConfiguration.getPassword();
    }

    public void setPassword(String password) {
        wcsConfiguration.setPassword(password);
    }

    public void setId(String id) {
        wcsConfiguration.setId(id);
    }

    public String getId() {
        return wcsConfiguration.getId();
    }

    /**
     * Retrieves a {@link Resource} based on a {@link URI} and provided arguments. A connection is
     * made to the {@link URI} to obtain the {@link Resource}'s {@link InputStream} and build a
     * {@link ResourceResponse} from that. The {@link URI}'s scheme must be "wcs" and the remainder
     * of the {@link URI} is the coverage ID for the {@link Resource} to be retrieved using the Web
     * Coverage Service (WCS) URL configured for this reader.
     * 
     * @param resourceURI
     *            A WCS {@link URI} that defines what {@link Resource} to retrieve. WCS URI is of
     *            the form {@code}wcs:coverageId{@code}
     * @param properties
     *            Any additional arguments that should be passed to the {@link ResourceReader}
     * @return A {@link ResourceResponse} containing the retrieved {@link Resource}
     * 
     * @throws IOException
     * @throws ResourceNotFoundException
     * @throws ResourceNotSupportedException
     */
    public ResourceResponse retrieveResource(URI resourceURI, Map<String, Serializable> properties)
        throws IOException, ResourceNotFoundException, ResourceNotSupportedException {

        if (resourceURI == null) {
            LOGGER.warn("Resource URI was null");
            throw new ResourceNotFoundException("Unable to find resource");
        }

        if (resourceURI.getScheme().equals(WCS_SCHEME)) {
            String resourceUriString = resourceURI.toASCIIString();
            String[] uriParts = resourceUriString.split(URI_PARTS_DELIMITER);
            if (uriParts.length != 2) {
                String msg = "WCS resource URI did not have 2 parts (scheme:coverageId): "
                        + resourceUriString;
                LOGGER.error(msg);
                throw new ResourceNotFoundException("Unable to find resource: " + msg);
            } else {
                String coverageId = uriParts[1];
                LOGGER.debug("coverageId = {}", coverageId);
                LOGGER.debug("Retrieving product from WCS: {}", this.wcsConfiguration.getWcsUrl());
                return retrieveProduct(coverageId);
            }
        } else {
            ResourceNotFoundException ce = new ResourceNotFoundException("Resource qualifier ( "
                    + resourceURI.getScheme() + " ) not valid. " + WcsResourceReader.TITLE
                    + " requires a qualifier of " + WCS_SCHEME);
            LOGGER.error("", ce);
            throw ce;
        }

    }

    /**
     * Retrieve product with specified coverage ID using remote WCS service. A DescribeCoverage
     * request is first sent to determine the spatial and temporal domains for the coverage ID -
     * these will be used in the GetCoverage request to further specify the product to download. All
     * products are retrieved AS_IS, meaning in their original form, i.e., GetCoverage request will
     * not specify an alternate output format identified in the DescribeCoverage response, nor will
     * the GetCoverage request include a RangeSubset or InterpolationMethod since only the original
     * product is retrieved.
     * 
     * The GetCoverage request that will be generated to retrieve the product will be similar to
     * this format:
     * 
     * <pre>
     *  {@code
     *  <ns3:GetCoverage service="WCS" version="1.0.0" xmlns:ns2="http://www.w3.org/1999/xlink" 
     *      xmlns:ns1="http://www.opengis.net/gml" xmlns:ns4="http://www.opengis.net/ows" 
     *      xmlns:ns3="http://www.opengis.net/wcs">
     *      <ns3:sourceCoverage>bc1fb5db-0768-47df-b17b-af8975fc59c9</ns3:sourceCoverage>
     *      <ns3:domainSubset>
     *          <ns3:spatialSubset>
     *              <ns1:Envelope srsName="EPSG:4326">
     *                  <ns1:pos>33.716944444444444 51.703611111111115</ns1:pos>
     *                  <ns1:pos>33.73166666666667 51.740833333333335</ns1:pos>
     *              </ns1:Envelope>
     *          </ns3:spatialSubset>
     *          <ns3:temporalSubset>
     *              <ns1:timePosition>2002-08-21T07:26:24Z</ns1:timePosition>
     *          </ns3:temporalSubset>
     *     </ns3:domainSubset>
     *     <ns3:output>
     *         <ns3:crs>EPSG:4326</ns3:crs>
     *         <ns3:format>AS_IS;rrds:0</ns3:format>
     *     </ns3:output>
     * </ns3:GetCoverage>
     *  }
     * </pre>
     * 
     * @param coverageId
     * @return
     */
    protected ResourceResponse retrieveProduct(String coverageId) {
        ResourceResponse resourceResponse = null;

        // The DescribeCoverage response will return the spatial and temporal
        // domains of the coverage, which when used in the GetCoverage request
        // will most accurately define the product to be retrieved
        DescribeCoverage describeCoverageRequest = new DescribeCoverage();
        describeCoverageRequest.setCoverage(Collections.singletonList(coverageId));
        CoverageDescription coverageDescription = null;
        try {
            coverageDescription = remoteWcs.describeCoverage(describeCoverageRequest);
        } catch (WcsException e1) {
            LOGGER.warn("Unable to describe coverage for ID {}, Exception {}", coverageId, e1);
            return null;
        }

        if (coverageDescription == null) {
            return null;
        }

        GetCoverage request = new GetCoverage();
        request.setSourceCoverage(coverageId);

        List<CoverageOfferingType> coverageOfferings = coverageDescription.getCoverageOffering();

        // Expect only one coverage offering since only one coverage ID specified in reuqest
        if (coverageOfferings.size() == 1) {
            CoverageOfferingType coverageOffering = coverageOfferings.get(0);
            List<JAXBElement<?>> domainSubsetContent = coverageOffering.getDomainSet().getContent();
            DomainSubsetType domainSubset = new DomainSubsetType();
            List<JAXBElement<?>> content = new ArrayList<JAXBElement<?>>();

            // Buffer the spatial and temporal domain coverage data from the DescribeCoverage
            // response into the GetCoverage request's JAXB objects
            for (JAXBElement<?> e : domainSubsetContent) {
                if (e.getValue() instanceof SpatialDomainType) {
                    LOGGER.debug("Detected SpatialDomainType");

                    SpatialDomainType spatialDomain = (SpatialDomainType) e.getValue();
                    List<JAXBElement<? extends EnvelopeType>> envelopes = spatialDomain
                            .getEnvelope();

                    for (JAXBElement<? extends EnvelopeType> envelope : envelopes) {
                        SpatialSubsetType spatialSubset = getSpatialSubset(envelope);

                        net.opengis.wcs.v_1_0_0.ObjectFactory wcsObjFactory = new net.opengis.wcs.v_1_0_0.ObjectFactory();
                        JAXBElement<SpatialSubsetType> spatialSubsetType = wcsObjFactory
                                .createSpatialSubset(spatialSubset);

                        content.add(spatialSubsetType);
                    }

                } else if (e.getValue() instanceof TimeSequenceType) {
                    LOGGER.debug("Detected TimeSequenceType");

                    TimeSequenceType temporalDomain = (TimeSequenceType) e.getValue();
                    if (temporalDomain.isSetTimePositionOrTimePeriod()) {
                        content.add(new net.opengis.wcs.v_1_0_0.ObjectFactory()
                                .createTemporalSubset(temporalDomain));
                    }
                }
            }
            domainSubset.setContent(content);
            request.setDomainSubset(domainSubset);

            // Only original product format retrieval is supported
            List<CodeListType> supportedFormats = coverageOffering.getSupportedFormats()
                    .getFormats();
            for (CodeListType supportedFormat : supportedFormats) {
                List<String> formats = supportedFormat.getValue();
                int index = formats.indexOf(ORIGINAL_PRODUCT_FORMAT);
                if (index > -1) {
                    OutputType output = getOutput(formats.get(index), coverageOffering
                            .getSupportedCRSs().getRequestResponseCRSs());
                    request.setOutput(output);
                    break;
                } else {
                    LOGGER.warn("Did not find format {}", ORIGINAL_PRODUCT_FORMAT);
                }
            }

            // RangeSubset and InterpolationMethod is omitted from GetCoverage request
            // since the product is only retrieved in its original format, i.e., no
            // chipping, resizing, etc.
            // request.setRangeSubset(value);
            // request.setInterpolationMethod(value);

        }

        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{}: GetCoverage request:\n {}", wcsConfiguration.getId(),
                        getGetCoverageAsXml(request));
            }


            GetCoverageResponse response = remoteWcs.getCoverage(request);
            String contentDisposition = response.getContentDisposition();

            String mimeType = null;
            String filename = parseFilename(contentDisposition);
            if (StringUtils.isBlank(filename)) {
                filename = coverageId;
            } else {
                mimeType = getMimeType(filename);
            }

            resourceResponse = new ResourceResponseImpl(new ResourceImpl(new BufferedInputStream(
                    response.getInputStream()), mimeType, filename));
        } catch (WcsException e) {
            LOGGER.warn("Unable to retrieve coverage for ID {}, Exception {}", coverageId, e);
            return null;
        }

        return resourceResponse;
    }

    protected SpatialSubsetType getSpatialSubset(JAXBElement<? extends EnvelopeType> envelope) {

        List<DirectPositionType> positions = envelope.getValue().getPos();
        String srsName = envelope.getValue().getSrsName();
        LOGGER.debug("srsName = {}", srsName);
        Envelope2D env2D = new Envelope2D();
        for (DirectPositionType pos : positions) {
            List<Double> coordinates = pos.getValue();
            LOGGER.debug("coordinates = {}", coordinates);
            env2D.include(coordinates.get(0), coordinates.get(1));
        }

        // Sort BBOX coordinates in min/max order (this order is
        // required by the GetCoverage request but is not guaranteed
        // in the DescribeCoverage response)
        DirectPosition lowerCorner = env2D.getLowerCorner();
        DirectPosition upperCorner = env2D.getUpperCorner();
        List<DirectPositionType> sortedPositions = new ArrayList<DirectPositionType>();

        DirectPositionType lc = new DirectPositionType();
        List<Double> lowerCornerCoords = new ArrayList<Double>();
        double[] lcCoords = lowerCorner.getCoordinate();
        lowerCornerCoords.add(lcCoords[0]);
        lowerCornerCoords.add(lcCoords[1]);
        lc.setValue(lowerCornerCoords);
        sortedPositions.add(lc);

        DirectPositionType uc = new DirectPositionType();
        List<Double> upperCornerCoords = new ArrayList<Double>();
        double[] ucCoords = upperCorner.getCoordinate();
        upperCornerCoords.add(ucCoords[0]);
        upperCornerCoords.add(ucCoords[1]);
        uc.setValue(upperCornerCoords);
        sortedPositions.add(uc);

        // Put sorted spatial data into SpatialSubsetType (because SpatialDomainType is
        // not the desired object type for the JAXB GetCoverage request!)
        SpatialSubsetType spatialSubset = new SpatialSubsetType();
        EnvelopeType env = new EnvelopeType();
        env.setSrsName(srsName);
        env.setPos(sortedPositions);
        List<JAXBElement<? extends EnvelopeType>> envs = new ArrayList<JAXBElement<? extends EnvelopeType>>();
        ObjectFactory objFactory = new ObjectFactory();
        JAXBElement<EnvelopeType> envType = objFactory.createEnvelope(env);
        envs.add(envType);
        spatialSubset.setEnvelope(envs);

        return spatialSubset;
    }

    protected OutputType getOutput(String format, List<CodeListType> requestResponseCRSs) {

        OutputType output = new OutputType();
        if (!requestResponseCRSs.isEmpty()) {
            List<String> crsList = requestResponseCRSs.get(0).getValue();
            if (!crsList.isEmpty()) {
                CodeType crsCodeType = new CodeType();
                crsCodeType.setValue(crsList.get(0));
                output.setCrs(crsCodeType);
            }
        }

        CodeType formatCodeType = new CodeType();
        LOGGER.debug("Setting output format to {}", format);
        formatCodeType.setValue(format);
        output.setFormat(formatCodeType);

        return output;
    }

    protected String parseFilename(String contentDisposition) {
        String filename = null;
        String contentHeader = StringUtils.stripEnd(contentDisposition, ";");
        if (StringUtils.isNotBlank(contentHeader)) {
            int nameStart = contentHeader.indexOf(FILENAME_STR);
            if (nameStart != -1) {
                nameStart += FILENAME_STR.length();
                // If filename is present starts with a double quote
                if (nameStart < contentHeader.length()) {
                    if (contentHeader.charAt(nameStart) == '\"') {
                        // Skip opening double quote and look for ending quote
                        nameStart++;
                        int nameEnd = contentHeader.indexOf("\"", nameStart);
                        if (nameEnd != -1) {
                            filename = contentHeader.substring(nameStart, nameEnd);
                        }
                    } else {
                        filename = contentHeader.substring(nameStart);
                    }
                }
                LOGGER.debug("Found content disposition header, changing resource name to {}",
                        filename);
            }
        }

        return filename;
    }

    protected String getMimeType(String filename) {

        String mimeType = null;
        if (mimeTypeMapper != null) {
            // Extract the file extension (if any) from the URL's filename
            int index = filename.lastIndexOf(".");

            // If there is a file extension, attempt to get mime type based on
            // the file extension, using the MimeTypeMapper so that any custom
            // MimeTypeResolvers are consulted
            if (index > -1) {
                String fileExtension = filename.substring(index + 1);
                LOGGER.debug("filename = {},   fileExtension = {}", filename, fileExtension);
                try {
                    mimeType = mimeTypeMapper.getMimeTypeForFileExtension(fileExtension);
                } catch (MimeTypeResolutionException e) {
                    LOGGER.warn("Unable to determine mime type from file extension {}",
                            fileExtension, e);
                }
            }
        }

        LOGGER.debug("mimeType set to: {}", mimeType);

        return mimeType;
    }

    public void setMimeTypeMapper(MimeTypeMapper mimeTypeMapper) {
        this.mimeTypeMapper = mimeTypeMapper;
    }

    private static JAXBContext initJaxbContext() {

        JAXBContext jaxbContext = null;

        try {
            jaxbContext = JAXBContext
                    .newInstance(
                            "net.opengis.wcs.v_1_0_0:net.opengis.gml.profiles.gml4wcs.v_1_0_0:net.opengis.ows.v_1_0_0",
                            WcsResourceReader.class.getClassLoader());
        } catch (JAXBException e) {
            LOGGER.error("Failed to initialize JAXBContext", e);
        }

        return jaxbContext;
    }

    protected String getGetCoverageAsXml(GetCoverage getCoverage) {
        Writer writer = new StringWriter();
        try {
            Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);

            JAXBElement<GetCoverage> jaxbElement = new JAXBElement<GetCoverage>(new QName(
                    "http://www.opengis.net/wcs", "GetCoverage"), GetCoverage.class, getCoverage);
            marshaller.marshal(jaxbElement, writer);
        } catch (JAXBException e) {
            LOGGER.error("{}: Unable to marshall {} to XML.  Exception {}", wcsConfiguration.getId(),
                    GetCoverage.class, e);
        }
        return writer.toString();
    }

}
