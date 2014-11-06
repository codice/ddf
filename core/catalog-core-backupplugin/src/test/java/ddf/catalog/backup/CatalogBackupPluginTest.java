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
package ddf.catalog.backup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static junit.framework.Assert.fail;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.Update;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.plugin.PluginExecutionException;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CatalogBackupPluginTest {

    @Rule
    public TemporaryFolder rootBackupDir = new TemporaryFolder();

    private String[] METACARD_IDS = {"6c3810b98a3b4208b51bc0a96d61e5", "1b6482b1b8f730e343a96d61e0e089"};

    private String BASE_OLD_TITLE = "oldTitle";

    private String BASE_NEW_TITLE = "newTitle";

    /**
     * Verify no NullPointerException
     */
    @Test
    public void testNullBackupDir() {
        CatalogBackupPlugin catalogBackupPlugin = new CatalogBackupPlugin();
        catalogBackupPlugin.setRootBackupDir(null);
    }

    @Test(expected = PluginExecutionException.class)
    public void testProcessCreateResponseRootBackupDirBlank() throws Exception {
        // Setup
        int subDirLevels = 3;
        CreateResponse mockCreateResponse = getMockCreateResponse(Arrays.asList(METACARD_IDS));

        CatalogBackupPlugin catalogBackupPlugin = new CatalogBackupPlugin();
        catalogBackupPlugin.setRootBackupDir(null);
        catalogBackupPlugin.setSubDirLevels(subDirLevels);

        // Perform Test
        catalogBackupPlugin.process(mockCreateResponse);
    }

    @Test
    public void testProcessCreateResponseSubdirectoryLevelsIsZero() throws Exception {
        // Setup
        CreateResponse mockCreateResponse = getMockCreateResponse(Arrays.asList(METACARD_IDS));

        int subDirLevels = 0;

        CatalogBackupPlugin catalogBackupPlugin = new CatalogBackupPlugin();
        catalogBackupPlugin.setRootBackupDir(rootBackupDir.getRoot().getAbsolutePath());
        catalogBackupPlugin.setSubDirLevels(subDirLevels);

        // Perform Test
        CreateResponse postPluginCreateResponse = catalogBackupPlugin.process(mockCreateResponse);

        // Verify
        assertThat(postPluginCreateResponse, is(notNullValue()));

        for(String metacardId : METACARD_IDS) {
            File backedupMetacard = new File(rootBackupDir.getRoot().getAbsolutePath() + getMetacardPath(metacardId, 0) + metacardId);
            assertThat(backedupMetacard.exists(), is(Boolean.TRUE));
        }
    }

    @Test
    public void testProcessCreateResponseSubdirectoryLevelsIsNegative() throws Exception {
        // Setup
        CreateResponse mockCreateResponse = getMockCreateResponse(Arrays.asList(METACARD_IDS));

        int subDirLevels = -5;

        CatalogBackupPlugin catalogBackupPlugin = new CatalogBackupPlugin();
        catalogBackupPlugin.setRootBackupDir(rootBackupDir.getRoot().getAbsolutePath());
        catalogBackupPlugin.setSubDirLevels(subDirLevels);

        // Perform Test
        CreateResponse postPluginCreateResponse = catalogBackupPlugin.process(mockCreateResponse);

        // Verify
        assertThat(postPluginCreateResponse, is(notNullValue()));

        for(String metacardId : METACARD_IDS) {
            File backedupMetacard = new File(rootBackupDir.getRoot().getAbsolutePath() + getMetacardPath(metacardId, 0) + metacardId);
            assertThat(backedupMetacard.exists(), is(Boolean.TRUE));
        }
    }

    /**
     * If too many subdirectories are specified (two characters from metacardId are used to name each subdirectory),
     * then the CatalogBackupPlugin makes as many subdirectories as it can for each metacardId.
     */
    @Test
    public void testProcessCreateResponseSubdirectoryLevelsTooBigForMetacardId() throws Exception {
        // Setup
        String[] metacardIds = {"6c38", "0283"};

        CreateResponse mockCreateResponse = getMockCreateResponse(Arrays.asList(metacardIds));

        int subDirLevels = 1000;

        CatalogBackupPlugin catalogBackupPlugin = new CatalogBackupPlugin();
        catalogBackupPlugin.setRootBackupDir(rootBackupDir.getRoot().getAbsolutePath());
        catalogBackupPlugin.setSubDirLevels(subDirLevels);

        // Perform Test
        CreateResponse postPluginCreateResponse = catalogBackupPlugin.process(mockCreateResponse);

        // Verify
        assertThat(postPluginCreateResponse, is(notNullValue()));

        for(String metacardId : metacardIds) {
            File backedupMetacard = new File(rootBackupDir.getRoot().getAbsolutePath() + getMetacardPath(metacardId, 2) + metacardId);
            assertThat(backedupMetacard.exists(), is(Boolean.TRUE));
        }
    }

    @Test
    public void testProcessCreateResponseCreateSuccessful() throws Exception {
        // Setup
        CreateResponse mockCreateResponse = getMockCreateResponse(Arrays.asList(METACARD_IDS));

        int subDirLevels = 3;

        CatalogBackupPlugin catalogBackupPlugin = new CatalogBackupPlugin();
        catalogBackupPlugin.setRootBackupDir(rootBackupDir.getRoot().getAbsolutePath());
        catalogBackupPlugin.setSubDirLevels(subDirLevels);

        // Perform Test
        CreateResponse postPluginCreateResponse = catalogBackupPlugin.process(mockCreateResponse);

        // Verify
        assertThat(postPluginCreateResponse, is(notNullValue()));

        for(String metacardId : METACARD_IDS) {
            File backedupMetacard = new File(rootBackupDir.getRoot().getAbsolutePath() + getMetacardPath(metacardId, 3) + metacardId);
            assertThat(backedupMetacard.exists(), is(Boolean.TRUE));
        }
    }

    @Test
    public void testProcessDeleteResponseDeleteSuccessful() throws Exception {
        //Setup
        CreateResponse mockCreateResponse = getMockCreateResponse(Arrays.asList(METACARD_IDS));

        DeleteResponse mockDeleteResponse = getDeleteResponse(Arrays.asList(METACARD_IDS));

        int subDirLevels = 3;

        CatalogBackupPlugin catalogBackupPlugin = new CatalogBackupPlugin();
        catalogBackupPlugin.setRootBackupDir(rootBackupDir.getRoot().getAbsolutePath());
        catalogBackupPlugin.setSubDirLevels(subDirLevels);

        catalogBackupPlugin.process(mockCreateResponse);

        // Perform Test
        DeleteResponse postPluginDeleteResponse = catalogBackupPlugin.process(mockDeleteResponse);

        // Verify
        assertThat(postPluginDeleteResponse, is(notNullValue()));

        for(String metacardId : METACARD_IDS) {
            File deletedMetacards = new File(rootBackupDir.getRoot().getAbsolutePath() + getMetacardPath(metacardId, 3) + metacardId);
            assertThat(deletedMetacards.exists(), is(Boolean.FALSE));
        }
    }

    @Test
    public void testProcessDeleteResponseFailToDeleteAllMetacards() {
        //Setup
        DeleteResponse mockDeleteResponse = getDeleteResponse(Arrays.asList(METACARD_IDS));

        int subDirLevels = 3;

        CatalogBackupPlugin catalogBackupPlugin = new CatalogBackupPlugin();
        catalogBackupPlugin.setRootBackupDir(rootBackupDir.getRoot().getAbsolutePath());
        catalogBackupPlugin.setSubDirLevels(subDirLevels);

        // Perform Test
        try {
            catalogBackupPlugin.process(mockDeleteResponse);
        } catch(PluginExecutionException e) {
            // Verify
            for(int i = 0; i < METACARD_IDS.length; i++) {
                assertThat(e.getMessage(), containsString(METACARD_IDS[i]));
            }
        }
    }

    @Test
    public void testProcessDeleteResponseFailToDeleteOneMetacard() {
        // Setup
        String[] createdMetacardIds = {METACARD_IDS[0]};

        CreateResponse mockCreateResponse = getMockCreateResponse(Arrays.asList(createdMetacardIds));

        int subDirLevels = 3;

        CatalogBackupPlugin catalogBackupPlugin = new CatalogBackupPlugin();
        catalogBackupPlugin.setRootBackupDir(rootBackupDir.getRoot().getAbsolutePath());
        catalogBackupPlugin.setSubDirLevels(subDirLevels);

        try {
            catalogBackupPlugin.process(mockCreateResponse);
        } catch(PluginExecutionException e) {
            fail();
        }

        DeleteResponse mockDeleteResponse = getDeleteResponse(Arrays.asList(METACARD_IDS));

        try {
            // Perform Test
            catalogBackupPlugin.process(mockDeleteResponse);
        } catch(PluginExecutionException e) {
            // Verify
            for(int i = 1; i < METACARD_IDS.length; i++) {
                assertThat(e.getMessage(), containsString(METACARD_IDS[i]));
            }
        }
    }

    @Test
    public void testProcessUpdateResponseUpdateSuccessful() throws Exception {
        // Setup
        CreateResponse mockCreateResponse = getMockCreateResponse(Arrays.asList(METACARD_IDS));

        int subDirLevels = 3;

        CatalogBackupPlugin catalogBackupPlugin = new CatalogBackupPlugin();
        catalogBackupPlugin.setRootBackupDir(rootBackupDir.getRoot().getAbsolutePath());
        catalogBackupPlugin.setSubDirLevels(subDirLevels);

        catalogBackupPlugin.process(mockCreateResponse);

        UpdateResponse mockUpdateResponse = getUpdateResponse(Arrays.asList(METACARD_IDS));

        // Perform Test
        UpdateResponse postPluginUpdateResponse = catalogBackupPlugin.process(mockUpdateResponse);

        // Verify
        assertThat(postPluginUpdateResponse, is(notNullValue()));

        int j = 0;
        for(Metacard oldMetacard : mockCreateResponse.getCreatedMetacards()) {
            File updatedMetacardFile = new File(rootBackupDir.getRoot().getAbsolutePath() + getMetacardPath(oldMetacard.getId(), subDirLevels) + oldMetacard.getId());
            Metacard updatedMetacard = readMetacard(updatedMetacardFile);
            assertThat(updatedMetacardFile.exists(), is(Boolean.TRUE));
            // Verify that the metacard id has not changed (from newMetacard to oldMetacard)
            assertThat(updatedMetacard.getId(), is(oldMetacard.getId()));
            // Verify that the metacard title has been updated to "newTitle" + j
            assertThat((String) updatedMetacard.getAttribute(Metacard.TITLE).getValue(), is(BASE_NEW_TITLE + j));

            j++;
        }
    }

    @Test
    public void testProcessUpdateUpdateFailsForAllMetacards() {
        // Setup
        int subDirLevels = 3;

        CatalogBackupPlugin catalogBackupPlugin = new CatalogBackupPlugin();
        catalogBackupPlugin.setRootBackupDir(rootBackupDir.getRoot().getAbsolutePath());
        catalogBackupPlugin.setSubDirLevels(subDirLevels);

        UpdateResponse mockUpdateResponse = getUpdateResponse(Arrays.asList(METACARD_IDS));

        // Perform Test
        try {
            catalogBackupPlugin.process(mockUpdateResponse);
        } catch(PluginExecutionException e) {
            // Verify
            for(int i = 0; i < METACARD_IDS.length; i++) {
                assertThat(e.getMessage(), containsString(METACARD_IDS[i]));
            }
        }
    }

    @Test
    public void testProcessUpdateUpdateFailsForOneMetacard() {
        // Setup
        String[] createdMetacardIds = {METACARD_IDS[0]};

        CreateResponse mockCreateResponse = getMockCreateResponse(Arrays.asList(createdMetacardIds));

        int subDirLevels = 3;

        CatalogBackupPlugin catalogBackupPlugin = new CatalogBackupPlugin();
        catalogBackupPlugin.setRootBackupDir(rootBackupDir.getRoot().getAbsolutePath());
        catalogBackupPlugin.setSubDirLevels(subDirLevels);

        try {
            catalogBackupPlugin.process(mockCreateResponse);
        } catch(PluginExecutionException e) {
            fail();
        }

        UpdateResponse mockUpdateResponse = getUpdateResponse(Arrays.asList(METACARD_IDS));

        // Perform Test
        try {
            catalogBackupPlugin.process(mockUpdateResponse);
        } catch(PluginExecutionException e) {
            // Verify
            for(int i = 1; i < METACARD_IDS.length; i++) {
                assertThat(e.getMessage(), containsString(METACARD_IDS[i]));
            }
        }
    }

    /**
     * Helper Methods
     */

    private CreateResponse getMockCreateResponse(List<String> metacardIds) {
        List<Metacard> createdMetacards = new ArrayList<Metacard>(metacardIds.size());
        for(String metacardId : metacardIds) {
            Metacard metacard = getMetacard(metacardId, BASE_OLD_TITLE);
            createdMetacards.add(metacard);
        }

        CreateResponse mockCreateResponse = mock(CreateResponse.class);
        when(mockCreateResponse.getCreatedMetacards()).thenReturn(createdMetacards);

        return mockCreateResponse;
    }

    private DeleteResponse getDeleteResponse(List<String> metacardIds) {
        MetacardType mockMetacardType = mock(MetacardType.class);
        when(mockMetacardType.getName()).thenReturn(MetacardType.DEFAULT_METACARD_TYPE_NAME);

        List<Metacard> deletedMetacards = new ArrayList<Metacard>(metacardIds.size());

        for(String metacardId : metacardIds) {
            Metacard mockMetacard = mock(Metacard.class);
            when(mockMetacard.getId()).thenReturn(metacardId);
            when(mockMetacard.getMetacardType()).thenReturn(mockMetacardType);

            deletedMetacards.add(mockMetacard);
        }

        DeleteResponse mockDeleteResponse = mock(DeleteResponse.class);
        when(mockDeleteResponse.getDeletedMetacards()).thenReturn(deletedMetacards);

        return mockDeleteResponse;
    }

    private UpdateResponse getUpdateResponse(List<String> oldMetacardIds) {
        List<Update> updatedMetacards = new ArrayList<Update>(oldMetacardIds.size());
        int i = 0;
        for(String oldMetacardId : oldMetacardIds) {
            Metacard oldMetacard = getMetacard(oldMetacardId, BASE_OLD_TITLE + i);
            Metacard newMetacard = getMetacard(oldMetacardId, BASE_NEW_TITLE + i);

            // Create UpdateResponse
            Update mockUpdate = mock(Update.class);
            when(mockUpdate.getOldMetacard()).thenReturn(oldMetacard);
            when(mockUpdate.getNewMetacard()).thenReturn(newMetacard);
            updatedMetacards.add(mockUpdate);

            i++;
        }

        UpdateResponse mockUpdateResponse = mock(UpdateResponse.class);
        when(mockUpdateResponse.getUpdatedMetacards()).thenReturn(updatedMetacards);

        return mockUpdateResponse;
    }

    private Metacard getMetacard(String metacardId, String title) {
        Metacard metacard = new MetacardImpl();
        metacard.setAttribute(new AttributeImpl(Metacard.ID, metacardId));
        metacard.setAttribute((new AttributeImpl(Metacard.TITLE, title)));

        return metacard;
    }

    private String getMetacardPath(String metacardId, int subDirLevels) {
        StringBuilder builder = new StringBuilder();

        if(subDirLevels < 0) {
            subDirLevels = 0;
        } else if(metacardId.length() == 1 || metacardId.length() < subDirLevels * 2) {
            subDirLevels = (int) Math.floor(metacardId.length()/2);
        }

        for (int i = 0; i < subDirLevels; i++) {
            builder.append(File.separator);
            builder.append(metacardId.substring(i * 2, i * 2 + 2));
        }

        builder.append(File.separator);

        return builder.toString();
    }

    private Metacard readMetacard(File file) throws ClassNotFoundException, IOException {
        Metacard metacard = null;


        try(FileInputStream fis = new FileInputStream(file); ObjectInputStream ois = new ObjectInputStream(fis)) {
            metacard = (Metacard) ois.readObject();
        }

        return metacard;
    }
}
