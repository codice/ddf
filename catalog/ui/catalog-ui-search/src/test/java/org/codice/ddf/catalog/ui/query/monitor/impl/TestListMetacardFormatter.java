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
package org.codice.ddf.catalog.ui.query.monitor.impl;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.codice.ddf.catalog.ui.query.monitor.api.MetacardFormatter;
import org.junit.Before;
import org.junit.Test;

public class TestListMetacardFormatter {

    private MetacardFormatter childFormatter1;

    private MetacardFormatter childFormatter2;

    private String childFormatter1Result;

    private String childFormatter2Result;

    private String template;

    private Long hitCount;

    private WorkspaceMetacardImpl workspaceMetacard;

    private String result;

    @Before
    public void setup() {
        childFormatter1 = mock(MetacardFormatter.class);
        childFormatter2 = mock(MetacardFormatter.class);

        childFormatter1Result = "childFormatter1Result";
        childFormatter2Result = "childFormatter2Result";

        when(childFormatter1.format(any(), any(), any())).thenReturn(childFormatter1Result);
        when(childFormatter2.format(any(), any(), any())).thenReturn(childFormatter2Result);

        ListMetacardFormatter listMetacardFormatter = new ListMetacardFormatter(Arrays.asList(
                childFormatter1,
                childFormatter2));

        template = "template";
        hitCount = 1L;
        workspaceMetacard = mock(WorkspaceMetacardImpl.class);

        result = listMetacardFormatter.format(template, workspaceMetacard, hitCount);

    }

    @Test
    public void testFormatReturnsSecondChildResult() {
        assertThat(result, is(childFormatter2Result));
    }

    @Test
    public void testThatFirstChildIsCalled() {
        verify(childFormatter1).format(template, workspaceMetacard, hitCount);
    }

    @Test
    public void testThatSecondChildIsCalledWithFirstChildResult() {
        verify(childFormatter2).format(childFormatter1Result, workspaceMetacard, hitCount);
    }

}
