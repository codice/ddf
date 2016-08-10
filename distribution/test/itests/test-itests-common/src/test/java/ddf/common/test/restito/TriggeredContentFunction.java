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

import java.io.IOException;

import org.glassfish.grizzly.http.server.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Allows manually specifying when data should be sent from a restito mocked source.
 */
public class TriggeredContentFunction extends AbstractContentFunction {
    private static final Logger LOGGER = LoggerFactory.getLogger(TriggeredContentFunction.class);

    private final Object senderLock = new Object();

    private final Object revealerLock = new Object();

    private int revealCount;

    public TriggeredContentFunction(String responseMessage, HeaderCapture headerCapture) {
        super(responseMessage, headerCapture);
        revealCount = 0;
    }

    /**
     * This method allows {@param count} number of bytes to be revealed to the consumers. This
     * method will not return until those bytes have been revealed. If the number revealed extends
     * past the end of the stream, only the subset (remaining characters) are revealed.
     * <p>
     * This method is thread-safe for multiple revealers, which will queue up as needed to reveal
     * their count of bytes.
     *
     * @param count the number of bytes to reveal.
     */
    public synchronized void revealBytes(int count) {
        if (count < 1) {
            throw new IllegalArgumentException("Cannot reveal a negative or zero count of bytes");
        }

        revealCount = count;
        senderLock.notify();
        try {
            revealerLock.wait();
        } catch (InterruptedException e) {
            LOGGER.error("Caller of revealBytes was interrupted while waiting for the reveal");
        }
    }

    @Override
    protected Response send(Response response, ByteRange byteRange) {
        // send each character, respecting the range header
        for (int i = byteRange.start; i <= byteRange.end; i++) {
            try {
                senderLock.wait();
                for (int j = 0; j < revealCount && i <= byteRange.end; j++) {
                    LOGGER.debug("TriggeredContentResponse: Sending character [{}]",
                            responseMessage[i]);
                    response.getNIOWriter()
                            .write(responseMessage[i]);
                    response.flush();
                }
                revealerLock.notify();
            } catch (IOException | InterruptedException e) {
                LOGGER.error("Error", e);
                break;
            }
        }
        response.finish();
        return response;
    }
}