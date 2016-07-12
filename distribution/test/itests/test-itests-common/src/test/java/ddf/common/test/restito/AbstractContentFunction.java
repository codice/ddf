/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.common.test.restito;

import java.util.Collections;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xebialabs.restito.semantics.Function;

/**
 * Boiler-plate processing logic for the mock csw server response function.
 */
public abstract class AbstractContentFunction implements Function<Response, Response> {
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractContentFunction.class);

    protected char[] responseMessage;

    protected HeaderCapture headerCapture;

    public AbstractContentFunction(String responseMessage, HeaderCapture headerCapture) {
        this.responseMessage = responseMessage.toCharArray();
        this.headerCapture = headerCapture;
    }

    /**
     * Implementation of the Function interface's apply method. This class can also be used as a
     * Function<Response, Response> directly in the Restito response if custom actions are needed.
     *
     * @param response Response object correlating to the incoming request. Used to write data
     *                 back to the requesting client.
     * @return Response New state of the response object correlating to the incoming request.
     */
    @Override
    public Response apply(Response response) {
        return respond(response);
    }

    /**
     * Sends message to the client one character at a time. Appropriate headers must be set on
     * the Response object before calling this method.
     *
     * @param response  Response object containing an output stream to the client
     * @param byteRange Object containing the range of bytes to send to the client
     */
    protected abstract Response send(Response response, ByteRange byteRange);

    /**
     * Default response logic for all restito csw mocked responses.
     */
    private Response respond(Response response) {
        Map<String, String> requestHeaders = Collections.emptyMap();
        if (headerCapture != null) {
            requestHeaders = headerCapture.getHeaders();
            LOGGER.debug("ChunkedContentResponse: extracted request headers [{}]", requestHeaders);
        }

        //  If range header is present, return 206 - Partial Content status
        // Set Content-Range header if byte offset is specified
        ByteRange byteRange;
        if (StringUtils.isNotBlank(requestHeaders.get("range"))) {
            byteRange = new ByteRange(requestHeaders.get("range"),
                    responseMessage.length,
                    responseMessage);
            response.setStatus(HttpStatus.PARTIAL_CONTENT_206);
            response.setHeader("Content-Range", byteRange.contentRangeValue());
            LOGGER.debug("ChunkedContentResponse: Response range header set to [Content-Range: {}]",
                    byteRange.contentRangeValue());
        } else {
            response.setStatus(HttpStatus.OK_200);
            byteRange = new ByteRange(0, responseMessage.length - 1, responseMessage);
            LOGGER.debug("ChunkedContentResponse: set response status to 200");
        }

        return send(response, byteRange);
    }
}
