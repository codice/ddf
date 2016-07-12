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

import static com.xebialabs.restito.semantics.Action.composite;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.custom;
import static com.xebialabs.restito.semantics.Action.header;

import java.time.Duration;

import javax.annotation.Nullable;

import org.glassfish.grizzly.http.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xebialabs.restito.semantics.Action;
import com.xebialabs.restito.semantics.Function;

/**
 * Encompasses the response for a product retrieval for a Restito stub server.
 * Allows for the simple setup of specific responses, slow sending, and simulated
 * network failure retry testing.
 */
public class RestitoContent {
    protected static final Logger LOGGER = LoggerFactory.getLogger(RestitoContent.class);

    public static class ChunkedContentBuilder {
        private String message;

        private Duration delayBetweenChunks = Duration.ofMillis(0);

        private int numberOfFailures = 0;

        private HeaderCapture headerCapture = null;

        /**
         * Set message.
         *
         * @param message Message to be sent in the response.
         */
        public ChunkedContentBuilder(String message) {
            this.message = message;
        }

        /**
         * Set delay between sending each character of the message in milliseconds.
         *
         * @param delay Time to wait between sending each character of the message.
         * @return Builder object
         */
        public ChunkedContentBuilder delayBetweenChunks(Duration delay) {
            this.delayBetweenChunks = delay;
            return this;
        }

        /**
         * Number of times to fail (simulate network disconnect) after sending
         * the first character. Once this number is reached, the message will
         * send successfully.
         *
         * @param numberOfFailures Number of times to fail (simulate network disconnect) after sending
         *                         the first character. Once this number is reached, the message will
         *                         send successfully.
         * @return Builder object
         */
        public ChunkedContentBuilder fail(int numberOfFailures) {
            this.numberOfFailures = numberOfFailures;
            return this;
        }

        /**
         * Setting this will make the Response handle range headers properly. If this isn't set, then
         * the response will ignore range requests and return the entire message every time.
         *
         * @param headerCapture HeaderCapture object that contains the request's headers.
         * @return
         */
        public ChunkedContentBuilder allowPartialContent(HeaderCapture headerCapture) {
            this.headerCapture = headerCapture;
            return this;
        }

        /**
         * Builds Action.
         *
         * @return Action constructed from builder object.
         */
        public Action build() {
            return createChunkedContent(message,
                    delayBetweenChunks,
                    numberOfFailures,
                    headerCapture);
        }
    }

    private static Action getChunkedResponseHeaders() {
        return composite(contentType("text/plain"),
                header("Transfer-Encoding", "chunked"),
                header("content-type", "text/plain"));
    }

    private static Action getRangeSupportHeaders() {
        return header("Accept-Ranges", "bytes");
    }

    /**
     * Returns a composite action that holds the headers required for a plain text response as well
     * as the response function itself. Unless the headers included are not wanted, this is the
     * preferred way to set the Restito response actions.
     * <p>
     * Note that additional headers may also be needed for specific endpoint functionality. This
     * method returns a composite action so that additional headers can be set alongside this action.
     *
     * @param responseMessage    Message to be sent in response.
     * @param delayBetweenChunks Time to wait between sending each character of the message.
     * @param numberOfFailures   Number of times to fail (simulate network disconnect) after sending
     *                           the first character. Once this number is reached, the message will
     *                           send successfully.
     * @param headerCapture      Object that can be called to return the request's headers.
     * @return composite action that holds the headers required for a plain text response as well
     * as the response function itself.
     */
    private static Action createChunkedContent(String responseMessage, Duration delayBetweenChunks,
            int numberOfFailures, HeaderCapture headerCapture) {

        Action response = composite(getChunkedResponseHeaders(),
                custom(new ChunkedContentFunction(responseMessage,
                        delayBetweenChunks,
                        numberOfFailures,
                        headerCapture)));

        // adds the Accept-Ranges header for range-header support
        if (headerCapture != null) {
            response = composite(getRangeSupportHeaders(), response);
        }

        return response;
    }

    public static Action createContentWithCustomFunction(Function<Response, Response> function,
            @Nullable HeaderCapture headerCapture) {

        Action response = composite(getChunkedResponseHeaders(), custom(function));

        if (headerCapture != null) {
            response = composite(getRangeSupportHeaders(), response);
        }

        return response;
    }
}
