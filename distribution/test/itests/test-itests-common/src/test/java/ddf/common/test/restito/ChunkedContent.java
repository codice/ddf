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

import static java.lang.Thread.sleep;
import static com.xebialabs.restito.semantics.Action.composite;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.custom;
import static com.xebialabs.restito.semantics.Action.header;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.util.HttpStatus;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Function;


/**
 * Encompasses the response for a product retrieval for a Restito stub server.
 * Allows for the simple setup of specific responses, slow sending, and simulated
 * network failure retry testing.
 */
public class ChunkedContent {
    private static final XLogger LOGGER =
            new XLogger(LoggerFactory.getLogger(ChunkedContent.class));

    /**
     * Returns the minimum headers required for sending a chunked data response, but does not
     * include the response function itself.
     *
     * @return the minimum headers required for sending a chunked data response
     */
    private static Action getHeaders() {
        return composite(contentType("text/plain"),
                header("Transfer-Encoding", "chunked"),
                header("content-type", "text/plain"));
    }

    /**
     * Constructor for a response that has a delay and a number of planned failures.
     *
     * @param responseMessage  Message to be sent in response.
     * @param messageDelay     Time to wait between sending each character of the message.
     * @param numberOfFailures Number of times to fail (simulate network disconnect) after sending
     *                         the first character. Once this number is reached, the message will
     *                         send successfully.
     *
     * @return Action that sends a response message based on the given parameters.
     */
    public static Action chunkedContent(String responseMessage, Duration messageDelay,
            int numberOfFailures) {
        return custom(new ChunkedContentFunction(responseMessage, messageDelay, numberOfFailures));

    }

    /**
     * Constructor for a response that has a delay and a number of planned failures.
     *
     * @param responseMessage  Message to be sent in response.
     * @param messageDelay     Time to wait between sending each character of the message.
     * @param numberOfFailures Number of times to fail (simulate network disconnect) after sending
     *                         the first character. Once this number is reached, the message will
     *                         send successfully.
     * @param getRequestHeaders Function that returns a map with the request's headers
     *
     * @return Action that sends a response message based on the given parameters.
     */
    public static Action chunkedContent(String responseMessage, Duration messageDelay,
            int numberOfFailures, Function<Void, Map<String, String>> getRequestHeaders) {
        return custom(new ChunkedContentFunction(responseMessage,
                messageDelay,
                numberOfFailures,
                getRequestHeaders));

    }

    /**
     * Returns a composite action that holds the headers required for a plain text response as well
     * as the response function itself. Unless the headers included are not wanted, this is the
     * preferred way to set the Restito response actions.
     * <p>
     * Note that additional headers may also be needed for specific endpoint functionality. This
     * method returns a composite action so that additional headers can be set alongside this action.
     *
     * @param responseMessage  Message to be sent in response.
     * @param messageDelay     Time to wait between sending each character of the message.
     * @param numberOfFailures Number of times to fail (simulate network disconnect) after sending
     *                         the first character. Once this number is reached, the message will
     *                         send successfully.
     *
     * @return composite action that holds the headers required for a plain text response as well
     * as the response function itself.
     */
    public static Action chunkedContentWithHeaders(String responseMessage, Duration messageDelay,
            int numberOfFailures) {
        return composite(getHeaders(),
                chunkedContent(responseMessage, messageDelay, numberOfFailures));
    }

    /**
     * Returns a composite action that holds the headers required for a plain text response as well
     * as the response function itself. Unless the headers included are not wanted, this is the
     * preferred way to set the Restito response actions.
     * <p>
     * Note that additional headers may also be needed for specific endpoint functionality. This
     * method returns a composite action so that additional headers can be set alongside this action.
     *
     * @param responseMessage  Message to be sent in response.
     * @param messageDelay     Time to wait between sending each character of the message.
     * @param numberOfFailures Number of times to fail (simulate network disconnect) after sending
     *                         the first character. Once this number is reached, the message will
     *                         send successfully.
     * @param requestHeaders   Function that returns a map with the request's headers
     *
     * @return composite action that holds the headers required for a plain text response as well
     * as the response function itself.
     */
    public static Action chunkedContentWithHeaders(String responseMessage, Duration messageDelay,
            int numberOfFailures, Function<Void, Map<String, String>> requestHeaders) {
        return composite(getHeaders(),
                chunkedContent(responseMessage, messageDelay, numberOfFailures, requestHeaders));
    }

    /**
     * Private inner class representing the response function
     */
    private static class ChunkedContentFunction implements Function<Response, Response>{
        private char[] responseMessage;

        private long messageDelayMs;

        private int numberOfFailures;

        private int numberOfRetries = 0;

        private Function<Void, Map<String, String>> getRequestHeaders = (Void) -> null;

        /**
         * Constructor for a response that has a delay and a number of planned failures.
         * Does not support range headers.
         *
         * @param responseMessage  Message to be sent in response.
         * @param messageDelay     Time to wait between sending each character of the message.
         * @param numberOfFailures Number of times to fail (simulate network disconnect) after sending
         *                         the first character. Once this number is reached, the message will
         *                         send successfully.
         */
        private ChunkedContentFunction(String responseMessage, Duration messageDelay,
                int numberOfFailures) {
            this.responseMessage = responseMessage.toCharArray();
            this.messageDelayMs = messageDelay.toMillis();
            this.numberOfFailures = numberOfFailures;
            this.getRequestHeaders = (Void) -> null;
        }
        /**
         * Constructor for a response that has a delay and a number of planned failures.
         * Supports range headers.
         *
         * @param responseMessage  Message to be sent in response.
         * @param messageDelay     Time to wait between sending each character of the message.
         * @param numberOfFailures Number of times to fail (simulate network disconnect) after sending
         *                         the first character. Once this number is reached, the message will
         *                         send successfully.
         * @param getRequestHeaders Function that returns headers on the incoming request.
         */
        private ChunkedContentFunction(String responseMessage, Duration messageDelay,
                int numberOfFailures, Function<Void, Map<String, String>> getRequestHeaders) {
            this.responseMessage = responseMessage.toCharArray();
            this.messageDelayMs = messageDelay.toMillis();
            this.numberOfFailures = numberOfFailures;
            this.getRequestHeaders = getRequestHeaders;
        }

        /**
         * Implementation of the Function interface's apply method. This class can also be used as a
         * Function<Response, Response> directly in the Restito response if custom actions are needed.
         * @param response   Response object correlating to the incoming request. Used to write data
         *                   back to the requesting client.
         *
         * @return Response New state of the response object correlating to the incoming request.
         */
        @Override
        public Response apply(Response response) {
            return respond(response);
        }

        private Response respond(Response response) {
            Map<String, String> requestHeaders = getRequestHeaders.apply(null);
            LOGGER.debug("StubServer: extracted request headers [{}]", requestHeaders);

            // if range header is present, return 206 - Partial Content status and set Content-Range header if byte Offset is specified
            ByteRange byteRange;
            if (requestHeaders != null && StringUtils.isNotBlank(requestHeaders.get("range"))) {
                byteRange = new ByteRange(requestHeaders.get("range"), responseMessage.length);
                response.setStatus(HttpStatus.PARTIAL_CONTENT_206);
                response.setHeader("Content-Range", byteRange.contentRangeValue());
                LOGGER.debug("StubServer: Response range header set to [Content-Range: {}]",
                        byteRange.contentRangeValue());
            } else {
                response.setStatus(HttpStatus.OK_200); //TODO: change this to a constant
                byteRange = new ByteRange(0, responseMessage.length - 1);
                LOGGER.debug("StubServer: set response status to 200");
            }

            // send each character, respecting the range header
            for (int i = byteRange.start; i <= byteRange.end; i++) {
                try {
                    LOGGER.debug("StubServer: Sending character [{}]", responseMessage[i]);
                    response.getNIOWriter()
                            .write(responseMessage[i]);
                    response.flush();
                    sleep(messageDelayMs);

                    // fail download by ungracefully closing the output buffer to simulate connection lost
                    if (numberOfRetries < numberOfFailures) {
                        response.getOutputBuffer()
                                .recycle();
                        numberOfRetries++;
                        return response;
                    }
                } catch (IOException | InterruptedException e) {
                    LOGGER.error("Error", e);
                    break;
                }
            }
            response.finish();

            return response;
        }

        /**
         * Holds byte offset data specified by the request's range header and any related parsing
         * methods.
         */
        private class ByteRange {
            public int start;
            public int end;

            public ByteRange(int start, int end){
                this.start = start;
                this.end = end;
            }

            /**
             * Tokenizes the starting and ending bytes values from a range header. The stub server only
             * advertising supporting byte offsets specifically, so this function assumes that byte
             * ranges are provided and does not evaluate the unit of measurement
             * @param rangeHeaderValue          Value of the range header sent in the request
             * @param totalSizeOfProductInBytes Total size of the message to be returned. This is
             *                                  not the partial content size, but the FULL size of
             *                                  the product.
             */
            public ByteRange(String rangeHeaderValue, int totalSizeOfProductInBytes){
                // extract bytes
                String startToken = StringUtils.substringBetween(rangeHeaderValue, "=", "-");
                String endToken = StringUtils.substringAfter(rangeHeaderValue, "-");

                // range offsets can be blank to indicate "until beginning" or "until end" of data.
                if (StringUtils.isBlank(startToken)) {
                    start = 0;
                } else {
                    start = Integer.parseInt(startToken);
                }
                if (StringUtils.isBlank(endToken)) {
                    end = totalSizeOfProductInBytes - 1;
                } else {
                    end = Integer.parseInt(endToken);
                }
            }

            public String contentRangeValue() {
                return "bytes " + start + "-" + end + "/" + responseMessage.length;
            }
        }
    }
}
