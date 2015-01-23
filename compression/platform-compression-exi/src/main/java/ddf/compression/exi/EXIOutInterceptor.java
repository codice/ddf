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
package ddf.compression.exi;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.helpers.CastUtils;
import org.apache.cxf.interceptor.MessageSenderInterceptor;
import org.apache.cxf.io.CachedOutputStream;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

/**
 * Interceptor that converts message content to exi-encoding if supporting by calling client.
 */
public class EXIOutInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOGGER = LoggerFactory.getLogger(EXIOutInterceptor.class);

    private static final String EXI_ACCEPT_ENCODING = "x-exi";

    /**
     * Creates a new Interceptor that handles converting server responses to exi-encoding.
     */
    public EXIOutInterceptor() {
        super(Phase.PREPARE_SEND);
        addAfter(MessageSenderInterceptor.class.getName());
    }

    @Override
    public void handleMessage(Message message) {

        if (isRequestor(message)) {
            //client sending request
            LOGGER.trace("Not performing any EXI compression for initial request.");
        } else {
            //server sending back response
            Message request = message.getExchange().getInMessage();
            Map<String, List<String>> requestHeaders = CastUtils.cast((Map<?, ?>) request
                    .get(Message.PROTOCOL_HEADERS));
            if (requestHeaders != null) {
                String acceptEncodingHeader = StringUtils
                        .join(requestHeaders.get(HttpHeaders.ACCEPT_ENCODING), ",");
                if (StringUtils.isNotBlank(acceptEncodingHeader) && acceptEncodingHeader
                        .contains(EXI_ACCEPT_ENCODING)) {
                    LOGGER.debug("Sending back response message using EXI-encoding.");
                    OutputStream os = message.getContent(OutputStream.class);
                    EXIOutputStream cached = new EXIOutputStream(os);
                    message.setContent(OutputStream.class, cached);
                } else {
                    LOGGER.debug("EXI encoding not accepted by the client, skipping EXI encoding.");
                }
            } else {
                LOGGER.debug(
                        "No request headers were found in the incoming request. Cannot encode to exi.");
            }
        }
    }

    /**
     * OutputStream that caches data and on close will encode the data into an exi format and send it out on the outputstream
     * provided in the constructor.
     */
    private static class EXIOutputStream extends CachedOutputStream {

        private OutputStream outStream;

        /**
         * Create a new Exi-based output stream.
         *
         * @param outStream Stream to write the exi-ecoded data to.
         */
        public EXIOutputStream(OutputStream outStream) {
            super();
            this.outStream = outStream;
        }

        @Override
        protected void doClose() throws IOException {
            InputStream xmlStream = null;
            try {
                xmlStream = getInputStream();
                EXIEncoder.encode(xmlStream, outStream);
            } catch (Exception exp) {
                LOGGER.warn(
                        "Encountered exception when trying to encode outgoing response into EXI. Sending back uncompressed response.",
                        exp);
                resetOut(outStream, true);
            } finally {
                IOUtils.closeQuietly(xmlStream);
            }
        }

    }
}
