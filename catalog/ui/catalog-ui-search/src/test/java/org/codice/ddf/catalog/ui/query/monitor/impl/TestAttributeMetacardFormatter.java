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

import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.junit.Test;

public class TestAttributeMetacardFormatter {

    @Test
    public void testFormatNonDefault() {

        String template = "%[attribute=id]";

        String id = "the-id";

        WorkspaceMetacardImpl workspaceMetacard = new WorkspaceMetacardImpl(id);

        AttributeMetacardFormatter attributeMetacardFormatter =
                new AttributeMetacardFormatter("n/a");

        String result = attributeMetacardFormatter.format(template, workspaceMetacard, 0L);

        assertThat(result, is(id));

    }

    @Test
    public void testFormatDefault() {

        String template = "%[attribute=xyz]";

        String defaultValue = "n/a";

        WorkspaceMetacardImpl workspaceMetacard = new WorkspaceMetacardImpl();

        AttributeMetacardFormatter attributeMetacardFormatter = new AttributeMetacardFormatter(
                defaultValue);

        String result = attributeMetacardFormatter.format(template, workspaceMetacard, 0L);

        assertThat(result, is(defaultValue));

    }

    @Test
    public void testComplex() {
        String template =
                "The workspace '%[attribute=title]' (id: %[attribute=id]) contains up to %[hitCount] query hits.";

        String id = "the-id";

        WorkspaceMetacardImpl workspaceMetacard = new WorkspaceMetacardImpl(id);
        workspaceMetacard.setAttribute("title", "the-title");

        AttributeMetacardFormatter attributeMetacardFormatter =
                new AttributeMetacardFormatter("n/a");

        String result = attributeMetacardFormatter.format(template, workspaceMetacard, 1L);

        assertThat(result,
                is("The workspace 'the-title' (id: the-id) contains up to %[hitCount] query hits."));

    }

}
