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
package org.codice.ddf.spatial.ogc.wcs.catalog.impl;

import java.util.Arrays;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import net.opengis.wcs.v_1_0_0.CoverageDescription;
import net.opengis.wcs.v_1_0_0.DescribeCoverage;
import net.opengis.wcs.v_1_0_0.GetCapabilities;
import net.opengis.wcs.v_1_0_0.GetCoverage;
import net.opengis.wcs.v_1_0_0.WCSCapabilitiesType;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.interceptor.LoggingInInterceptor;
import org.apache.cxf.interceptor.LoggingOutInterceptor;
import org.apache.cxf.jaxrs.client.JAXRSClientFactoryBean;
import org.codice.ddf.spatial.ogc.wcs.catalog.DescribeCoverageRequest;
import org.codice.ddf.spatial.ogc.wcs.catalog.GetCapabilitiesRequest;
import org.codice.ddf.spatial.ogc.wcs.catalog.GetCoverageRequest;
import org.codice.ddf.spatial.ogc.wcs.catalog.GetCoverageResponse;
import org.codice.ddf.spatial.ogc.wcs.catalog.Wcs;
import org.codice.ddf.spatial.ogc.wcs.catalog.WcsConfiguration;
import org.codice.ddf.spatial.ogc.wcs.catalog.WcsException;
import org.codice.ddf.spatial.ogc.wcs.catalog.reader.GetCoverageMessageBodyReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A client to a WCS 1.0.0 Service. This class uses the {@link Wcs} interface to create a client
 * proxy from the {@link JAXRSClientBeanFactory}.
 * 
 * @author rodgersh
 * 
 */
public class RemoteWcs implements Wcs {

    private static final Logger LOGGER = LoggerFactory.getLogger(RemoteWcs.class);

    private Wcs wcs;

    public RemoteWcs(WcsConfiguration wcsConfiguration) {
        if (StringUtils.isEmpty(wcsConfiguration.getWcsUrl())) {
            final String errMsg = "RemoteWcs was called without a wcsUrl.  RemoteWcs will not be able to connect.";
            LOGGER.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }

        JAXRSClientFactoryBean bean = createJAXRSClientBean(wcsConfiguration);

        // Additionally, set the username and password for Basic Auth
        if ((!StringUtils.isEmpty(wcsConfiguration.getUsername()))
                && (!StringUtils.isEmpty(wcsConfiguration.getPassword()))) {
            bean.setUsername(wcsConfiguration.getUsername());
            bean.setPassword(wcsConfiguration.getPassword());
        }

        wcs = bean.create(Wcs.class);
    }

    private JAXRSClientFactoryBean createJAXRSClientBean(WcsConfiguration wcsConfiguration) {

        JAXRSClientFactoryBean bean = new JAXRSClientFactoryBean();
        bean.setServiceClass(Wcs.class);
        bean.setAddress(wcsConfiguration.getWcsUrl());
        bean.getInInterceptors().add(new LoggingInInterceptor());
        bean.getOutInterceptors().add(new LoggingOutInterceptor());

        bean.setProviders(Arrays.asList(new GetCoverageMessageBodyReader()));

        return bean;
    }

    @Override
    @GET
    @Consumes({"text/xml", "application/xml"})
    @Produces({"text/xml", "application/xml"})
    public WCSCapabilitiesType getCapabilities(@QueryParam("")
    GetCapabilitiesRequest request) throws WcsException {

        return wcs.getCapabilities(request);
    }

    @Override
    @POST
    @Consumes({"text/xml", "application/xml"})
    @Produces({"text/xml", "application/xml"})
    public WCSCapabilitiesType getCapabilities(GetCapabilities request) throws WcsException {

        return wcs.getCapabilities(request);
    }

    @Override
    @GET
    @Consumes({"text/xml", "application/xml"})
    @Produces({"text/xml", "application/xml"})
    public CoverageDescription describeCoverage(@QueryParam("")
    DescribeCoverageRequest request) throws WcsException {

        return wcs.describeCoverage(request);
    }

    @Override
    @POST
    @Consumes({"text/xml", "application/xml"})
    @Produces({"text/xml", "application/xml"})
    public CoverageDescription describeCoverage(DescribeCoverage request) throws WcsException {

        return wcs.describeCoverage(request);
    }

    @Override
    @GET
    @Consumes({"text/xml", "application/xml"})
    public GetCoverageResponse getCoverage(@QueryParam("")
    GetCoverageRequest request) throws WcsException {

        return wcs.getCoverage(request);
    }

    @Override
    @POST
    @Consumes({"text/xml", "application/xml"})
    public GetCoverageResponse getCoverage(GetCoverage request) throws WcsException {

        return wcs.getCoverage(request);
    }

}
