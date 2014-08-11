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

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.Response;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.opengis.ows.v_1_1_0.ExceptionReport;
import net.opengis.ows.v_1_1_0.ExceptionType;

import org.apache.commons.lang.StringUtils;
import org.apache.cxf.common.util.CollectionUtils;
import org.apache.cxf.helpers.IOUtils;
import org.apache.cxf.jaxrs.client.ResponseExceptionMapper;
import org.apache.cxf.jaxrs.provider.JAXBElementProvider;
import org.codice.ddf.spatial.ogc.wfs.catalog.common.WfsException;

public class WfsResponseExceptionMapper implements ResponseExceptionMapper<WfsException> {

    public WfsException fromResponse(Response response) {

        WfsException wfsEx = null;

        if (response != null) {
            if (response.getEntity() instanceof InputStream) {
                String msg = null;
                try {
                    InputStream is = (InputStream) response.getEntity();
                    is.reset();
                    msg = IOUtils.toString(is);
                } catch (IOException e) {
                    wfsEx = new WfsException("Error reading Response"
                            + (msg != null ? ": " + msg : ""), e);
                }
                if (msg != null) {
                    try {
                        JAXBElementProvider<ExceptionReport> provider = new JAXBElementProvider<ExceptionReport>();
                        Unmarshaller um = provider.getJAXBContext(ExceptionReport.class,
                                ExceptionReport.class).createUnmarshaller();
                        ExceptionReport report = (ExceptionReport) um
                                .unmarshal(new StringReader(msg));
                        wfsEx = convertToWfsException(report);
                    } catch (JAXBException e) {
                        wfsEx = new WfsException("Error parsing Response: " + msg, e);
                    }
                }
            } else {
                wfsEx = new WfsException("Error reading response, entity type not understood: "
                        + response.getEntity().getClass().getName());
            }
            wfsEx.setHttpStatus(response.getStatus());
        } else {
            wfsEx = new WfsException("Error handling response, response is null");
        }
        return wfsEx;
    }

    private WfsException convertToWfsException(ExceptionReport report) {

        WfsException wfsException = null;
        List<ExceptionType> list = new ArrayList<ExceptionType>(
                report.getException());

        if (list.size() > 0) {
            Collections.reverse(list);
            for (ExceptionType exceptionType : list) {
                String exceptionCode = exceptionType.getExceptionCode();
                String locator = exceptionType.getLocator();
                List<String> exceptionText = exceptionType.getExceptionText();
                StringBuilder exceptionMsg = new StringBuilder();

                // Exception code is required per CSW schema, but check it anyway
                if (StringUtils.isNotBlank(exceptionCode)) {
                    exceptionMsg.append("exceptionCode = " + exceptionCode + "\n");
                } else {
                    exceptionMsg.append("exceptionCode = UNSPECIFIED");
                }

                // Locator and exception text(s) are both optional
                if (StringUtils.isNotBlank(locator)) {
                    exceptionMsg.append("locator = " + locator + "\n");
                }

                if (!CollectionUtils.isEmpty(exceptionText)) {
                    for (String text : exceptionText) {
                        exceptionMsg.append(text);
                    }
                }
                wfsException = new WfsException(exceptionMsg.toString());
            }
            
            if (null == wfsException) {
                wfsException = new WfsException("Empty Service Exception Report (version = "
                        + report.getVersion() + ")");
            }
        } else {
            wfsException = new WfsException("Empty Service Exception Report (version = "
                    + report.getVersion() + ")");
        }

        return wfsException;
    }
}
