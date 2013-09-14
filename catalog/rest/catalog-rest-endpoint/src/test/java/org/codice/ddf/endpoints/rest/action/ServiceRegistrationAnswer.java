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
package org.codice.ddf.endpoints.rest.action;

import static org.mockito.Mockito.mock;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.osgi.framework.ServiceRegistration;

/**
 * Used only for Mock purposes. This class provides a way of verifying input parameters and also
 * provides mock ServiceRegistration objects.
 * 
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 * 
 */
class ServiceRegistrationAnswer implements Answer<ServiceRegistration> {

    private Object[] arguments;

    public Object[] getArguments() {
        return arguments;
    }

    public List<ServiceRegistration> issued = new ArrayList<ServiceRegistration>();

    @Override
    public ServiceRegistration answer(InvocationOnMock invocation) throws Throwable {
        this.arguments = invocation.getArguments();

        ServiceRegistration registration = mock(ServiceRegistration.class);

        issued.add(registration);

        return registration;

    }

    public List<ServiceRegistration> getIssuedServiceRegistrations() {
        return Collections.unmodifiableList(issued);
    }

}