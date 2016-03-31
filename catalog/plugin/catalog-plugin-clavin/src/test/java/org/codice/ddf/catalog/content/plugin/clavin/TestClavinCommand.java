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
package org.codice.ddf.catalog.content.plugin.clavin;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.IOException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mockito;

import ddf.catalog.plugin.PluginExecutionException;

public class TestClavinCommand {

    @Rule
    public ExpectedException expectedEx = ExpectedException.none();

    @Test
    public void executeThrowsPluginExecutionExceptionWhenClavinWrapperIsNull() throws Exception {
        expectedEx.expect(PluginExecutionException.class);
        expectedEx.expectMessage("Failed to create clavin index from: filename");

        ClavinCommand clavinCommand = new ClavinCommand();
        clavinCommand.setResource("filename");

        clavinCommand.doExecute();
    }

    @Test
    public void executeThrowsPluginExecutionExceptionWhenResourceFileIsNull() throws Exception {
        expectedEx.expect(PluginExecutionException.class);
        expectedEx.expectMessage("Failed to create clavin index from: ");

        ClavinCommand clavinCommand = new ClavinCommand();
        clavinCommand.doExecute();
    }

    @Test
    public void execute() throws Exception {
        expectedEx.expect(PluginExecutionException.class);
        expectedEx.expectMessage("Failed to create clavin index from: some_filename");

        ClavinCommand clavinCommand = new ClavinCommand();
        clavinCommand.setResource("some_filename");
        ClavinWrapper mockClavinWrapper = mock(ClavinWrapper.class);

        Mockito.doThrow(new IOException())
                .when(mockClavinWrapper)
                .createIndex(any(File.class));

        clavinCommand.setClavinWrapper(mockClavinWrapper);
        clavinCommand.doExecute();
    }

}
