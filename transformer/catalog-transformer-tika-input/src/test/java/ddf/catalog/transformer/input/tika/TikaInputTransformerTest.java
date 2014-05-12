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
package ddf.catalog.transformer.input.tika;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.FileUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.data.Metacard;
import ddf.catalog.transform.CatalogTransformerException;

public class TikaInputTransformerTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(TikaInputTransformerTest.class);

    private static final String TEST_DATA_PATH = "src/test/resources/";

    @Test
    public void testNullInput() throws Exception {
        TikaInputTransformer tikaInputTransformer = new TikaInputTransformer();
        try {
            tikaInputTransformer.transform(null);
            fail("Did not get expected CatalogTransformerException");
        } catch (CatalogTransformerException e) {
        }
    }

    @Test
    public void testPDF() throws Exception {
        // transform(TEST_DATA_PATH + "DDF-Administrators-Guide.pdf");
        transform(TEST_DATA_PATH + "testPDF.pdf");
    }

    @Test
    public void testWordDoc() throws Exception {
        transform(TEST_DATA_PATH + "testWORD.docx");
        transform(TEST_DATA_PATH + "testWORD.doc");
    }

    @Test
    public void testPowerPoint() throws Exception {
        // transform(TEST_DATA_PATH + "Federated_Events.pptx");
        transform(TEST_DATA_PATH + "testPPT.ppt");
        transform(TEST_DATA_PATH + "testPPT.pptx");
    }

    @Test
    public void testExcel() throws Exception {
        transform(TEST_DATA_PATH + "testEXCEL.xls");
        transform(TEST_DATA_PATH + "testEXCEL.xlsx");
    }

    @Test
    public void testOpenOffice() throws Exception {
        transform(TEST_DATA_PATH + "testOpenOffice2.odt");
    }

    private static void transform(String filename) throws Exception {
        LOGGER.info("--------  File:  {}  -------------\n", filename);

        File file = new File(filename);
        FileInputStream fis = FileUtils.openInputStream(file);
        TikaInputTransformer tikaInputTransformer = new TikaInputTransformer();
        Metacard metacard = tikaInputTransformer.transform(fis);

        LOGGER.info("-------------------------------------------------\n\n");

        assertNotNull(metacard);
        assertNotNull(metacard.getCreatedDate());
        assertNotNull(metacard.getModifiedDate());
    }
}
