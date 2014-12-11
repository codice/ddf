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
package org.codice.ddf.spatial.ogc.wfs.catalog.source;

import com.google.common.io.FileBackedOutputStream;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class MarkableStreamInterceptor extends AbstractPhaseInterceptor<Message> {

    private static final Logger LOGGER = LoggerFactory.getLogger(MarkableStreamInterceptor.class);

    public MarkableStreamInterceptor() {
        super(Phase.PRE_STREAM);
    }

    @Override
    public void handleMessage(Message message) throws Fault {
        LOGGER.debug("Converting message input stream to a buffered stream");
        InputStream is = message.getContent(InputStream.class);
        FileBackedOutputStream fileBackedOutputStream = new FileBackedOutputStream(1000000);

        try {
            IOUtils.copy(is, fileBackedOutputStream);
        } catch (IOException e) {
            LOGGER.warn("Could not copy bytes of content message.", e);
        }
         
        try {
            message.setContent(InputStream.class, fileBackedOutputStream.asByteSource().openStream());
        } catch (IOException e) {
            LOGGER.warn("Failed to convert buffered stream");
        } catch (NullPointerException e) {
            LOGGER.warn("InputStream was null");
        }
    }
}
