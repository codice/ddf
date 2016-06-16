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

import org.glassfish.grizzly.http.server.Response;
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
     */
    public static Action chunkedContent(String responseMessage, Duration messageDelay,
            int numberOfFailures) {
        return custom(new ChunkedContentFunction(responseMessage, messageDelay, numberOfFailures));

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
     * @return composite action that holds the headers required for a plain text response as well
     * as the response function itself.
     */
    public static Action chunkedContentWithHeaders(String responseMessage, Duration messageDelay,
            int numberOfFailures) {
        return composite(getHeaders(),
                chunkedContent(responseMessage, messageDelay, numberOfFailures));
    }

    /**
     * Private inner class representing the response function
     */
    private static class ChunkedContentFunction implements Function<Response, Response> {
        private String responseMessage;

        private long messageDelayMs;

        private int numberOfFailures;

        private int numberOfRetries = 0;

        /**
         * Constructor for a response that has a delay and a number of planned failures.
         *
         * @param responseMessage  Message to be sent in response.
         * @param messageDelay     Time to wait between sending each character of the message.
         * @param numberOfFailures Number of times to fail (simulate network disconnect) after sending
         *                         the first character. Once this number is reached, the message will
         *                         send successfully.
         */
        private ChunkedContentFunction(String responseMessage, Duration messageDelay,
                int numberOfFailures) {
            this.responseMessage = responseMessage;
            this.messageDelayMs = messageDelay.toMillis();
            this.numberOfFailures = numberOfFailures;
        }

        /**
         * Implementation of the Function interface's apply method. This class can also be used as a
         * Function<Response, Response> directly in the Restito response if custom actions are needed.
         *
         * @param response
         * @return
         */
        @Override
        public Response apply(Response response) {
            return respond(response);
        }

        private Response respond(Response response) {
            for (char c : responseMessage.toCharArray()) {
                try {
                    response.getNIOWriter()
                            .write(c);
                    response.flush();
                    sleep(messageDelayMs);

                    // fail download by ungracefully closing the output buffer to simulate connection lost
                    if (numberOfRetries < numberOfFailures) {
                        response.getOutputBuffer()
                                .recycle();
                        numberOfRetries++;
                        break;
                    }
                } catch (IOException | InterruptedException e) {
                    LOGGER.error("Error", e);
                    break;
                }
            }
            response.finish();

            return response;
        }
    }
}
