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
package ddf.common.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

import org.apache.shiro.subject.Subject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.security.common.util.Security;

/**
 * Runs the ServiceManager methods as the system subject
 */
public class ServiceManagerProxy implements InvocationHandler {

    private ServiceManager serviceManager;

    private static final Logger LOGGER = LoggerFactory.getLogger(ServiceManagerProxy.class);

    public ServiceManagerProxy(ServiceManager serviceManager) {
        this.serviceManager = serviceManager;
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

        Subject subject = org.codice.ddf.security.common.Security.runAsAdmin(() -> Security.getSystemSubject());
        return subject.execute(() -> {
            try {
                return method.invoke(serviceManager, args);
            } catch (Exception e) {
                LOGGER.error("Unable to run method {} as system subject", method.getName());
                return null;
            }
        });
    }
}
