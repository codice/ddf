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
package org.codice.ddf.catalog.ui.security;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceMetacardImpl;
import org.junit.Test;

import ddf.catalog.operation.impl.CreateRequestImpl;
import ddf.catalog.plugin.StopProcessingException;

public class WorkspacePreIngestPluginTest {

    private static WorkspacePreIngestPlugin makePlugin(String email, String name) {
        return new WorkspacePreIngestPlugin() {
            protected String getSubjectEmail() {
                return email;
            }

            protected String getSubjectName() {
                return name;
            }

            protected void warn(String message) {
            }
        };
    }

    private static WorkspacePreIngestPlugin makePlugin() {
        return makePlugin("admin@localhost", "admin");
    }

    @Test
    public void testSuccessfulIngest() throws Exception {
        WorkspacePreIngestPlugin wpip = makePlugin();
        WorkspaceMetacardImpl workspace = new WorkspaceMetacardImpl();

        wpip.process(new CreateRequestImpl(workspace));
        assertThat(workspace.getOwner(), is("admin@localhost"));
    }

    @Test(expected = StopProcessingException.class)
    public void testUnsuccessfulIngest() throws Exception {
        WorkspacePreIngestPlugin wpip = makePlugin(null, null);
        wpip.process(new CreateRequestImpl(new WorkspaceMetacardImpl()));
    }
}