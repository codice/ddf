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
package org.codice.ddf.catalog.plugin.metacard.backup.storage.filestorage;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.apache.camel.CamelContext;
import org.apache.camel.impl.DefaultCamelContext;
import org.junit.Before;
import org.junit.Test;

public class MetacardBackupFileStorageTest {

    private static final String OUTPUT_PATH_TEMPLATE =
            File.separator + "tmp" + File.separator + "test-output" + File.separator;

    private CamelContext camelContext = new DefaultCamelContext();

    private MetacardFileStorageRoute fileStorageProvider =
            new MetacardFileStorageRoute(camelContext);

    @Before
    public void setUp() throws Exception {
        fileStorageProvider.setOutputPathTemplate(OUTPUT_PATH_TEMPLATE);
    }

    @Test
    public void testOutputPathTemplate() {
        assertThat(fileStorageProvider.getOutputPathTemplate(), is(OUTPUT_PATH_TEMPLATE));
    }

    @Test
    public void testRefresh() throws Exception {
        String newBackupDir = "target" + File.separator + "temp";
        boolean backupInvalidCards = false;
        boolean keepDeletedMetacards = false;
        String metacardTransformerId = "testTransformer";

        Map<String, Object> properties = new HashMap<>();
        properties.put("outputPathTemplate", newBackupDir);
        properties.put("backupInvalidMetacards", backupInvalidCards);
        properties.put("keepDeletedMetacards", keepDeletedMetacards);
        properties.put("metacardTransformerId", metacardTransformerId);

        fileStorageProvider.refresh(properties);
        assertThat(fileStorageProvider.getOutputPathTemplate(), is(newBackupDir));
        assertThat(fileStorageProvider.isBackupInvalidMetacards(), is(backupInvalidCards));
        assertThat(fileStorageProvider.isKeepDeletedMetacards(), is(keepDeletedMetacards));
        assertThat(fileStorageProvider.getMetacardTransformerId(), is(metacardTransformerId));
    }

    @Test
    public void testRefreshBadValues() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put("outputPathTemplate", 2);
        properties.put("id", 5);
        fileStorageProvider.refresh(properties);
        assertThat(fileStorageProvider.getOutputPathTemplate(), is(OUTPUT_PATH_TEMPLATE));
    }

    @Test
    public void testRefreshEmptyStrings() throws Exception {
        Map<String, Object> properties = new HashMap<>();
        properties.put("outputPathTemplate", "");
        fileStorageProvider.refresh(properties);
        assertThat(fileStorageProvider.getOutputPathTemplate(), is(OUTPUT_PATH_TEMPLATE));
    }
}
