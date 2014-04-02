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
package org.codice.ddf.spatial.ogc.catalog.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map.Entry;

import javax.ws.rs.core.MultivaluedMap;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.jaxrs.ext.ResourceComparator;
import org.apache.cxf.jaxrs.ext.xml.XMLSource;
import org.apache.cxf.jaxrs.model.ClassResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.model.OperationResourceInfoComparator;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom {@link ResourceComparator} to determine which method to call on the {@link WfsEndpoint}
 * and {@link CswEndpoint} based on the incoming request. By default JAX-RS only uses the URI path,
 * HTTP method, Consumes Media Type, and Produces Media Type to determine which method to execute.
 * The {@link WfsEndpoint} and {@link CswEndpoint} require the decision to made based on the
 * "request" query parameter or the type of XML document.
 * 
 */
public class EndpointOperationInfoResourceComparator extends OperationResourceInfoComparator
        implements ResourceComparator {

    public static final String HTTP_GET = "GET";

    public static final String HTTP_POST = "POST";

    public static final String QUERY_PARAM_DELIMITER = "&";

    public static final String REQUEST_PARAM = "request";

    public static final String SERVICE_PARAM = "service";
    
    public static final String UNKNOWN_SERVICE = "unknownService";

    public static final String UNKNOWN_OPERATION = "unknownOperation";

    private static final Logger LOGGER = LoggerFactory
            .getLogger(EndpointOperationInfoResourceComparator.class);

    private String serviceName;
    
    public EndpointOperationInfoResourceComparator() {
        super(null, null);
        
        this.serviceName = null;
    }

    public EndpointOperationInfoResourceComparator(String serviceName) {
        super(null, null);

        this.serviceName = serviceName;
    }

    @Override
    public int compare(ClassResourceInfo cri1, ClassResourceInfo cri2, Message message) {
        // Leave Class selection to CXF
        return 0;
    }

    @Override
    public int compare(OperationResourceInfo oper1, OperationResourceInfo oper2, Message message) {
        if (null == oper1 || null == oper2 || null == message) {
            LOGGER.warn("Found NULL parameters in the compare method.");
            return 0;
        }
        String httpMethod = (String) message.get(Message.HTTP_REQUEST_METHOD);
        LOGGER.debug("HTTP METHOD = {}", httpMethod);

        String requestName = null;
        String requestedService = null;

        if (HTTP_GET.equalsIgnoreCase(httpMethod)) {
            String queryString = (String) message.get(Message.QUERY_STRING);
            if (StringUtils.isNotEmpty(queryString)) {

                MultivaluedMap<String, String> allQueryParams = JAXRSUtils.getStructuredParams(
                        queryString, QUERY_PARAM_DELIMITER, false, false);
                // Loop through the keys and do a case insensitive check
                for (Entry<String, List<String>> queryParam : allQueryParams.entrySet()) {
                    if ((REQUEST_PARAM.equalsIgnoreCase(queryParam.getKey()))
                            && (!queryParam.getValue().isEmpty()) && requestName == null) {
                        // We should never have more than one "request" query
                        // param so ignore them if we do
                        requestName = queryParam.getValue().get(0);
                        LOGGER.debug("Request Query Param = {}", requestName);
                    }
                    if ((SERVICE_PARAM.equalsIgnoreCase(queryParam.getKey()))
                            && (!queryParam.getValue().isEmpty()) && requestedService == null) {
                        // We should never have more than one "service" query
                        // param so ignore them if we do
                        requestedService = queryParam.getValue().get(0);
                        LOGGER.debug("Service Query Param = {}", requestedService);
                    }
                }
            }
        } else if (HTTP_POST.equalsIgnoreCase(httpMethod)) {
            // Get the payload
            InputStream is = message.getContent(InputStream.class);
            if (is != null) {
                CachedOutputStream bos = new CachedOutputStream();
                try {
                    // We need to make a copy and put it back for later
                    // processing
                    IOUtils.copy(is, bos);
                    bos.flush();
                    is.close();
                    message.setContent(InputStream.class, bos.getInputStream());
                    XMLSource xml = new XMLSource(bos.getInputStream());
                    xml.setBuffering();
                    // The request name will be the root node name

                    requestName = xml.getValue("local-name(/*)");
                    requestedService = xml.getValue("/*/@service");
                    
                    LOGGER.debug("ROOT NODE = {}", requestName);
                    LOGGER.debug("Service Name = {}", requestedService);
                    bos.close();
                } catch (IOException ioe) {
                    LOGGER.warn("Unable to read message contents", ioe);
                }
            }

        } else {
            LOGGER.warn("Got unknown HTTP Method {}", httpMethod);
            return 0;
        }

        if (StringUtils.isEmpty(requestName)) {
            LOGGER.warn("Unable to determine the request name");
            return 0;
        }

        int op1Rank = getOperationRank(oper1, requestName, requestedService);
        int op2Rank = getOperationRank(oper2, requestName, requestedService);

        return (op1Rank == op2Rank) ? 0 : (op1Rank < op2Rank) ? 1 : -1;
    }

    private int getOperationRank(OperationResourceInfo operation, String requestName,
            String requestedService) {
        if (serviceName != null && (!serviceName.equalsIgnoreCase(requestedService))
                && operation.getMethodToInvoke().getName().equalsIgnoreCase(UNKNOWN_SERVICE)) {
            return 3;
        }

        return operation.getMethodToInvoke().getName().equalsIgnoreCase(requestName) ? 1
                : (operation.getMethodToInvoke().getName().equalsIgnoreCase(UNKNOWN_OPERATION) ? 0
                        : -1);
    }

}
