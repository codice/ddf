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
package org.codice.ddf.platform.filter.delegate;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Dictionary;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.ServletContext;

import org.codice.ddf.platform.filter.delegate.FilterInjector;
import org.junit.Test;
import org.mockito.Matchers;
import org.mockito.Mockito;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.osgi.framework.ServiceRegistration;

/**
 * Tests the functionality of the filter injector to verify that all of the
 * methods function properly with adding and removing filters.
 * 
 */
public class FilterInjectorTest {

    ServiceReference<ServletContext> curReference;

    ServiceRegistration<Filter> curRegistration;

    ServletContext curContext;

    /**
     * Tests that the filter is registered when the injectFilter method is
     * called.
     */
    @Test
    public void testInjectFilter() {
        Filter filter = createMockFilter();
        FilterInjector injector = new FilterInjector(filter);
        updateMockReference();

        injector.injectFilter(curReference);

        verify(curContext).addFilter("delegating-filter", filter);

    }

    private Filter createMockFilter() {
        Filter filter = mock(Filter.class);
        return filter;
    }

    @SuppressWarnings("unchecked")
    private void updateMockReference() {
        curReference = mock(ServiceReference.class);
        Bundle bundle = mock(Bundle.class);
        BundleContext context = mock(BundleContext.class);
        curContext = mock(ServletContext.class);
        FilterRegistration.Dynamic filterReg = mock(FilterRegistration.Dynamic.class);
        when(curContext.addFilter(anyString(), any(Filter.class))).thenReturn(filterReg);
        curRegistration = mock(ServiceRegistration.class);
        when(
                context.registerService(eq(Filter.class), Mockito.any(Filter.class),
                        Matchers.<Dictionary<String, Object>> any())).thenReturn(curRegistration);
        when(context.getService(curReference)).thenReturn(curContext);
        when(bundle.getBundleContext()).thenReturn(context);
        when(bundle.getSymbolicName()).thenReturn("Mock Bundle.");
        when(curReference.getBundle()).thenReturn(bundle);
    }

}
