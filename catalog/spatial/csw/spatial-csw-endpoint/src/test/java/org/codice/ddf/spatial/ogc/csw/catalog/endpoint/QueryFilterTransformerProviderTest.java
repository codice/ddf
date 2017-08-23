/*
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
 */

package org.codice.ddf.spatial.ogc.csw.catalog.endpoint;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import javax.xml.namespace.QName;

import org.junit.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;

import ddf.catalog.transform.QueryFilterTransformer;

public class QueryFilterTransformerProviderTest extends QueryFilterTransformerProvider {
    private BundleContext bundleContext = mock(BundleContext.class);

    @Test
    public void testAddingTransformer() {
        QueryFilterTransformer transformer = mock(QueryFilterTransformer.class);
        ServiceReference<QueryFilterTransformer> serviceReference = mock(ServiceReference.class);

        String namespace = "{namespace}test";
        when(serviceReference.getProperty("id")).thenReturn(namespace);
        when(bundleContext.getService(serviceReference)).thenReturn(transformer);

        bind(serviceReference);

        Optional<QueryFilterTransformer> result = getTransformer(QName.valueOf(namespace));
        assertThat(result.isPresent(), equalTo(true));
        assertThat(result.get(), equalTo(transformer));
    }

    @Test
    public void testRemovingTransformer() {
        QueryFilterTransformer transformer = mock(QueryFilterTransformer.class);
        ServiceReference<QueryFilterTransformer> serviceReference = mock(ServiceReference.class);

        String namespace = "{namespace}test";
        when(serviceReference.getProperty("id")).thenReturn(namespace);
        when(bundleContext.getService(serviceReference)).thenReturn(transformer);

        bind(serviceReference);
        unbind(serviceReference);

        Optional<QueryFilterTransformer> result = getTransformer(QName.valueOf(namespace));
        assertThat(result.isPresent(), equalTo(false));
    }

    @Test(expected = Test.None.class)
    public void testNullServiceReferenceOnBind() {
        bind(null);
    }

    @Test(expected = Test.None.class)
    public void testNullServiceReferenceOnUnbind() {
        unbind(null);
    }

    @Test
    public void testGettingBadTransformer() {
        Optional<QueryFilterTransformer> result = getTransformer(QName.valueOf("fake"));
        assertThat(result.isPresent(), equalTo(false));
    }

    @Test
    public void testNullQName() {
        Optional<QueryFilterTransformer> result = getTransformer(null);
        assertThat(result.isPresent(), equalTo(false));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullServiceReferenceId() {
        ServiceReference<QueryFilterTransformer> serviceReference = mock(ServiceReference.class);
        when(serviceReference.getProperty("id")).thenReturn(null);
        bind(serviceReference);
    }

    @Test
    public void testListOfServiceReferenceIds() {
        QueryFilterTransformer transformer = mock(QueryFilterTransformer.class);
        ServiceReference<QueryFilterTransformer> serviceReference = mock(ServiceReference.class);

        List<String> namespaces = Arrays.asList("{namespace}one",
                "{namespace}two",
                "{namespace}three");
        when(serviceReference.getProperty("id")).thenReturn(namespaces);
        when(bundleContext.getService(serviceReference)).thenReturn(transformer);

        bind(serviceReference);

        for (String namespace : namespaces) {
            Optional<QueryFilterTransformer> result = getTransformer(QName.valueOf(namespace));
            assertThat(result.isPresent(), equalTo(true));
            assertThat(result.get(), equalTo(transformer));
        }
    }

    @Test(expected = IllegalStateException.class)
    public void testNullTransformer() {
        ServiceReference<QueryFilterTransformer> serviceReference = mock(ServiceReference.class);

        String namespace = "{namespace}test";
        when(serviceReference.getProperty("id")).thenReturn(namespace);
        when(bundleContext.getService(serviceReference)).thenReturn(null);

        bind(serviceReference);
    }

    @Override
    BundleContext getBundleContext() {
        return bundleContext;
    }
}
