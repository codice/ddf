/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version. 
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package com.lmco.ddf.endpoints.rest.action;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;

import org.apache.log4j.Logger;
import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

import ddf.action.ActionProvider;

public class TestActionProviderRegistryProxy {

    private static final String SAMPLE_TRANSFORMER_ID = "sampleTransformerId";

    private static final Logger LOGGER = Logger
            .getLogger(TestActionProviderRegistryProxy.class);

    @Test
    public void testNoTransformerId() {
        // given
        ServiceRegistrationAnswer answer = new ServiceRegistrationAnswer();

        BundleContext bundleContext = givenBundleContext(answer);

        ActionProviderRegistryProxy proxy = new ActionProviderRegistryProxy(
                bundleContext);

        ServiceReference reference = mock(ServiceReference.class);

        // when
        proxy.bind(reference);

        // then
        verify(bundleContext, times(0)).registerService(isA(String.class),
                isA(Object.class), isA(Dictionary.class));

    }

    @Test
    public void testRegisterUnRegister() {

        // given
        ServiceRegistrationAnswer answer = new ServiceRegistrationAnswer();

        BundleContext bundleContext = givenBundleContext(answer);

        ActionProviderRegistryProxy proxy = new ActionProviderRegistryProxy(
                bundleContext);

        ServiceReference reference = mock(ServiceReference.class);

        when(reference.getProperty(isA(String.class))).thenReturn(
                SAMPLE_TRANSFORMER_ID);

        // when
        proxy.bind(reference);
        proxy.unbind(reference);

        // then
        verify(bundleContext, times(2)).registerService(isA(String.class),
                isA(Object.class), isA(Dictionary.class));

        Dictionary actionProperties = (Dictionary) (answer.getArguments()[2]);
        LOGGER.info("actionproperties:" + actionProperties);

        String actionProviderId = actionProperties.get(
                ddf.catalog.Constants.SERVICE_ID).toString();

        assertThat(actionProviderId,
                is(ActionProviderRegistryProxy.ACTION_ID_PREFIX
                        + SAMPLE_TRANSFORMER_ID));

        ServiceRegistration mockRegistration1 = answer
                .getIssuedServiceRegistrations().get(0);
        ServiceRegistration mockRegistration2 = answer
                .getIssuedServiceRegistrations().get(1);

        verify(mockRegistration1, times(1)).unregister();
        verify(mockRegistration2, times(1)).unregister();

    }

    private BundleContext givenBundleContext(ServiceRegistrationAnswer answer) {
        BundleContext bundleContext = mock(BundleContext.class);

        when(
                bundleContext.registerService(isA(String.class),
                        isA(Object.class), isA(Dictionary.class))).then(answer);

        return bundleContext;
    }

}
