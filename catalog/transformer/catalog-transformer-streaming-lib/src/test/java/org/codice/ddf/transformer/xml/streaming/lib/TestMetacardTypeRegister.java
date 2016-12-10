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

package org.codice.ddf.transformer.xml.streaming.lib;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;

import java.util.Set;

import org.codice.ddf.transformer.xml.streaming.SaxEventHandlerFactory;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.BundleContext;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.impl.BasicTypes;

public class TestMetacardTypeRegister {

    MetacardTypeRegister metacardTypeRegister;

    SaxEventHandlerFactory mockFactory;

    @Before
    public void setup() {
        //mock factory
        mockFactory = mock(SaxEventHandlerFactory.class);
        doReturn(BasicTypes.BASIC_METACARD.getAttributeDescriptors()).when(mockFactory)
                .getSupportedAttributeDescriptors();

        metacardTypeRegister = new MetacardTypeRegister() {
            @Override
            public BundleContext getContext() {
                return mock(BundleContext.class);
            }
        };
    }

    @Test
    public void testBind() throws Exception {
        metacardTypeRegister.bind(mockFactory);

        Set<AttributeDescriptor> results = metacardTypeRegister.getMetacardType()
                .getAttributeDescriptors();

        assertThat(results, is(BasicTypes.BASIC_METACARD.getAttributeDescriptors()));
    }
}
