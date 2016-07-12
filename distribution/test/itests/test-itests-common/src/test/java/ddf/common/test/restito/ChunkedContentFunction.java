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

import java.io.IOException;
import java.time.Duration;

import org.glassfish.grizzly.http.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Time-delimited chunked content function for returning data from restito mocks.
 */
class ChunkedContentFunction extends AbstractContentFunction {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChunkedContentFunction.class);

    private long messageDelayMs;

    private int numberOfFailures;

    private int numberOfRetries;

    /**
     * Constructor for a response that has a delay and a number of planned failures.
     * Supports range headers.
     *
     * @param responseMessage  Message to be sent in response.
     * @param messageDelay     Time to wait between sending each character of the message.
     * @param numberOfFailures Number of times to fail (simulate network disconnect) after sending
     *                         the first character. Once this number is reached, the message will
     *                         send successfully.
     * @param headerCapture    HeaderCapture object that contains the request's headers.
     */
    public ChunkedContentFunction(String responseMessage, Duration messageDelay,
            int numberOfFailures, HeaderCapture headerCapture) {
        super(responseMessage, headerCapture);
        this.messageDelayMs = messageDelay.toMillis();
        this.numberOfFailures = numberOfFailures;
    }

    @Override
    protected Response send(Response response, ByteRange byteRange) {
        // send each character, respecting the range header
        for (int i = byteRange.start; i <= byteRange.end; i++) {
            try {
                LOGGER.debug("ChunkedContentResponse: Sending character [{}]", responseMessage[i]);
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
}