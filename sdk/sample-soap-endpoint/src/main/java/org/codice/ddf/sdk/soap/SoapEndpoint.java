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
package org.codice.ddf.sdk.soap;

import ddf.security.SubjectUtils;
import org.apache.shiro.SecurityUtils;
import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sdk.ddf.soap.hello.HelloWorld;
import sdk.ddf.soap.hello.HelloWorldResponse;

import javax.jws.WebService;

@WebService(
        serviceName = "HelloWorldService",
        portName = "HelloWorldServicePort",
        targetNamespace = "http://ddf.sdk/soap/hello",
        endpointInterface = "sdk.ddf.soap.hello.HelloWorldServicePort"
)
public class SoapEndpoint {
    private static final Logger LOGGER = LoggerFactory.getLogger(SoapEndpoint.class);
    public HelloWorldResponse helloWorldOp(HelloWorld helloWorld) {

        String name = null;
        try {
            Subject subject = SecurityUtils.getSubject();
            name = SubjectUtils.getName(subject);
        } catch (Exception e) {
            LOGGER.debug("Unable to retrieve user from request.", e);
        }

        HelloWorldResponse helloWorldResponse = new HelloWorldResponse();
        helloWorldResponse.setResult("Hello " + name);
        return helloWorldResponse;
    }
}
