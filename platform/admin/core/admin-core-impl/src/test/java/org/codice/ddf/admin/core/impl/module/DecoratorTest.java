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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codice.ddf.ui.admin.api.module.AdminModule;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class DecoratorTest {

    private AdminModule module;

    @Before
    public void setUp() throws Exception {
        module = Mockito.mock(AdminModule.class);
    }

    @Test
    public void testAdminModuleAdminModuleDecorator() {
        doReturn("test").when(module)
                .getName();
        doReturn("0").when(module)
                .getId();
        ValidationDecorator proxy = new ValidationDecorator(module);
        assertThat("values are proxied correctly", proxy.getName(), is(module.getName()));
        assertThat("values are proxied correctly", proxy.getId(), is(module.getId()));
    }

    @Test
    public void testToMap() {
        doReturn("test").when(module)
                .getName();
        Decorator proxy = new ValidationDecorator(module);
        String name = (String) proxy.toMap()
                .get("name");
        assertThat("hash map gets constructed correctly", name, is("test"));
    }

    @Test
    public void testURLToMap() throws URISyntaxException {
        String uri = "js/app.js";
        doReturn(new URI(uri)).when(module)
                .getJSLocation();
        Decorator proxy = new ValidationDecorator(module);
        String jsLocation = (String) proxy.toMap()
                .get("jsLocation");
        assertThat("URLs get mapped to strings", jsLocation, is(uri));
    }

    @Test
    public void testNullURLToMap() {
        Decorator proxy = new ValidationDecorator(module);
        String jsLocation = (String) proxy.toMap()
                .get("jsLocation");
        assertThat("null URLs get mapped to empty strings", jsLocation, is(""));
    }

    @Test
    public void testCompareTo() {
        doReturn("test").when(module)
                .getName();
        Decorator proxy = new ValidationDecorator(module);
        assertThat("modules are the same if the have the same name",
                proxy.compareTo(module),
                is(0));
    }

    @Test
    public void testSort() {
        AdminModule hello = Mockito.mock(AdminModule.class);
        doReturn("hello").when(hello)
                .getName();
        AdminModule world = Mockito.mock(AdminModule.class);
        doReturn("world").when(world)
                .getName();

        List<AdminModule> list = new ArrayList<AdminModule>();
        list.add(world);
        list.add(hello);

        List<ValidationDecorator> modules = ValidationDecorator.wrap(list);
        Collections.sort(modules);

        assertThat("modules are sorted lexographically by name",
                modules.get(0)
                        .getName(),
                is("hello"));
        assertThat("modules are sorted lexographically by name",
                modules.get(1)
                        .getName(),
                is("world"));
    }
}