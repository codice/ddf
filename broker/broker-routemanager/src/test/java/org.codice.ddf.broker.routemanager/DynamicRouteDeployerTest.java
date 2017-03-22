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
package org.codice.ddf.broker.routemanager;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Test;

public class DynamicRouteDeployerTest {

    @Test
    public void testInstall() throws Exception {
        CamelContext context = new DefaultCamelContext();
        DynamicRouteDeployer dynamicRouteDeployer = new DynamicRouteDeployer(context);
        dynamicRouteDeployer.install(new File(this.getClass()
                .getResource("/test-route.xml")
                .toURI()));
        assertThat(context.getRouteDefinitions()
                .size(), is(1));

    }

    @Test
    public void testUninstall() throws Exception {
        CamelContext context = new DefaultCamelContext();

        DynamicRouteDeployer dynamicRouteDeployer = new DynamicRouteDeployer(context);
        dynamicRouteDeployer.install(new File(this.getClass()
                .getResource("/test-route.xml")
                .toURI()));
        assertThat(context.getRouteDefinitions()
                .size(), is(1));
        dynamicRouteDeployer.uninstall(new File(this.getClass()
                .getResource("/test-route.xml")
                .toURI()));
        assertThat(context.getRouteDefinitions()
                .size(), is(0));

    }

    @Test
    public void testUpdate() throws Exception {
        DynamicRouteDeployer dynamicRouteDeployer = mock(DynamicRouteDeployer.class);
        doCallRealMethod().when(dynamicRouteDeployer)
                .update(any(File.class));
        File testFile = new File(this.getClass()
                .getResource("/test-route.xml")
                .toURI());
        dynamicRouteDeployer.update(testFile);
        verify(dynamicRouteDeployer, times(1)).install(testFile);
        verify(dynamicRouteDeployer, times(1)).uninstall(testFile);
    }
}
