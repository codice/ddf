/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.cxf.paos;

import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.List;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.WriterInterceptor;
import javax.xml.stream.XMLStreamWriter;

import org.apache.cxf.interceptor.AbstractOutDatabindingInterceptor;
import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.jaxrs.client.ClientProviderFactory;
import org.apache.cxf.jaxrs.model.OperationResourceInfo;
import org.apache.cxf.jaxrs.utils.InjectionUtils;
import org.apache.cxf.jaxrs.utils.JAXRSUtils;
import org.apache.cxf.message.Message;
import org.apache.cxf.message.MessageContentsList;
import org.apache.cxf.phase.Phase;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
public class BodyWriter extends AbstractOutDatabindingInterceptor {

    private static final Logger LOGGER = LoggerFactory.getLogger(BodyWriter.class);

        /*
         * This class is straight out of CXF. This was private within their ClientProxyImpl and handles
         * converting the Object message content into a byte stream. We are just reusing it here because
         * it is not accessible outside of the CXF packages.
         */

    BodyWriter() {
        super(Phase.WRITE);
    }

    public void handleMessage(Message outMessage) throws Fault {

        MessageContentsList objs = MessageContentsList.getContentsList(outMessage);
        if (objs == null || objs.size() == 0) {
            return;
        }

        OutputStream os = outMessage.getContent(OutputStream.class);
        if (os == null) {
            XMLStreamWriter writer = outMessage.getContent(XMLStreamWriter.class);
            if (writer == null) {
                return;
            }
        }

        Object body = objs.get(0);
        Annotation[] customAnns = (Annotation[]) outMessage.get(Annotation.class.getName());
        Type t = outMessage.get(Type.class);
        doWriteBody(outMessage, body, t, customAnns, os);
    }

    void doWriteBody(Message outMessage, Object body, Type bodyType, Annotation[] customAnns,
            OutputStream os) throws Fault {

        OperationResourceInfo ori = outMessage.getContent(OperationResourceInfo.class);
        if (ori == null) {
            return;
        }

        Method method = ori.getMethodToInvoke();
        int bodyIndex = (Integer) outMessage.get("BODY_INDEX");

        Annotation[] anns =
                customAnns != null ? customAnns : getMethodAnnotations(ori.getAnnotatedMethod(),
                        bodyIndex);
        try {
            if (bodyIndex != -1) {
                Class<?> paramClass = method.getParameterTypes()[bodyIndex];
                Class<?> bodyClass =
                        paramClass.isAssignableFrom(body.getClass()) ? paramClass : body.getClass();
                Type genericType = method.getGenericParameterTypes()[bodyIndex];
                if (bodyType != null) {
                    genericType = bodyType;
                }
                genericType = InjectionUtils.processGenericTypeIfNeeded(ori.getClassResourceInfo()
                        .getServiceClass(), bodyClass, genericType);
                bodyClass = InjectionUtils.updateParamClassToTypeIfNeeded(bodyClass, genericType);
                writeBody(body, outMessage, bodyClass, genericType, anns, os);
            } else {
                Type paramType = body.getClass();
                if (bodyType != null) {
                    paramType = bodyType;
                }
                writeBody(body, outMessage, body.getClass(), paramType, anns, os);
            }
        } catch (Exception ex) {
            throw new Fault(ex);
        }

    }

    Annotation[] getMethodAnnotations(Method aMethod, int bodyIndex) {
        return aMethod == null || bodyIndex == -1 ?
                new Annotation[0] :
                aMethod.getParameterAnnotations()[bodyIndex];
    }

    <T> void writeBody(T o, Message outMessage, Class<?> cls, Type type, Annotation[] anns,
            OutputStream os) {

        if (o == null) {
            return;
        }
        @SuppressWarnings("unchecked")
        MultivaluedMap<String, Object> headers = (MultivaluedMap<String, Object>) outMessage.get(
                Message.PROTOCOL_HEADERS);

        @SuppressWarnings("unchecked")
        Class<T> theClass = (Class<T>) cls;

        MediaType contentType = JAXRSUtils.toMediaType(headers.getFirst("Content-Type")
                .toString());

        List<WriterInterceptor> writers = ClientProviderFactory.getInstance(outMessage)
                .createMessageBodyWriterInterceptor(theClass,
                        type,
                        anns,
                        contentType,
                        outMessage,
                        null);
        if (writers != null) {
            try {
                JAXRSUtils.writeMessageBody(writers,
                        o,
                        theClass,
                        type,
                        anns,
                        contentType,
                        headers,
                        outMessage);

                OutputStream realOs = outMessage.get(OutputStream.class);
                if (realOs != null) {
                    realOs.flush();
                }
            } catch (Exception ex) {
                LOGGER.debug("Unable to write message body for final ECP response.");
            }
        } else {
            LOGGER.debug("No writers available for final ECP response");
        }

    }
}
