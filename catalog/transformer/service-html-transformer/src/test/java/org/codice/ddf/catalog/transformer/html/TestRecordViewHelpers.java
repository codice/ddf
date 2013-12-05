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
package org.codice.ddf.catalog.transformer.html;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestRecordViewHelpers {

    @Test
    public void testBuildMetadata() {
        String metadata = "<foo><bar></bar></foo>";
        String newLine = System.getProperty("line.separator");
        String expected = "<pre>&lt;foo&gt;" + newLine + "    &lt;bar/&gt;"
                + newLine + "&lt;/foo&gt;" + newLine + "</pre>";
        
        RecordViewHelpers helpers = new RecordViewHelpers();
        
        CharSequence actual = helpers.buildMetadata(metadata, null).toString();
        
        assertEquals(expected, actual);
    }
}
