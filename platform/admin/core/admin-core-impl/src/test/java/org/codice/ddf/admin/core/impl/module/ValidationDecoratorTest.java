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
 */
package org.codice.ddf.admin.core.impl.module;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.doReturn;

import java.net.URI;
import java.net.URISyntaxException;

import org.codice.ddf.ui.admin.api.module.AdminModule;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class ValidationDecoratorTest {

    private AdminModule module;

    @Before
    public void setUp() throws Exception {
        module = Mockito.mock(AdminModule.class);
    }

    @Test
    public void testValidAdminModule() throws URISyntaxException {
        doReturn(new URI("js/app.js")).when(module)
                .getJSLocation();
        ValidationDecorator proxy = new ValidationDecorator(module);
        assertThat("relative paths are valid", proxy.isValid(), is(true));
    }

    @Test
    public void testInvalidAdminModule() throws URISyntaxException {
        doReturn(new URI("http://test/js/app.js")).when(module)
                .getJSLocation();
        ValidationDecorator proxy = new ValidationDecorator(module);
        assertThat("absolute paths are not valid", proxy.isValid(), is(false));
    }

    @Test
    public void testPartialInvalidAdminModule() throws URISyntaxException {
        doReturn(new URI("js/app.js")).when(module)
                .getJSLocation();
        doReturn(new URI("/css/styles.css")).when(module)
                .getCSSLocation();
        ValidationDecorator proxy = new ValidationDecorator(module);
        assertThat("any absolute paths are not valid", proxy.isValid(), is(false));
    }
}