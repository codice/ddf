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
package ddf.catalog.test;


import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Paths;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import ddf.common.test.BeforeExam;
import ddf.common.test.KarafConsole;



@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestSolrCommands extends AbstractIntegrationTest {

    private static final String BACKUP_COMMAND = "solr:backup";

    private static final String DDF_HOME_PROPERTY = "ddf.home";

    private static final String BACKUP_SUCCESS_MESSAGE = "Backup of [catalog] complete";

    private static final String BACKUP_ERROR_MESSAGE_FORMAT = "Error backing up Solr core: [%s]";

    private static KarafConsole console;


    @BeforeExam
    public void beforeExam() throws Exception {
        setLogLevels();
        waitForAllBundles();
        waitForCatalogProvider();
        waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query");
        console = new KarafConsole(bundleCtx);
    }

    @Test
    public void testSolrBackupCommand() {
        String output = console.runCommand(BACKUP_COMMAND);
        assertThat(output, containsString(BACKUP_SUCCESS_MESSAGE));
    }

    @Test
    public void testSolrBackupBadCoreName() {
        String coreName = "blah";
        String command = BACKUP_COMMAND + " -c " + coreName;
        String output = console.runCommand(command);

        assertThat(output, containsString(String.format(BACKUP_ERROR_MESSAGE_FORMAT, coreName)));
    }

    @Test
    public void testSolrBackupNumToKeep() {
        int numToKeep = 2;
        String coreName = "catalog";

        String command = BACKUP_COMMAND + " -n " + numToKeep;
        //run this three times to make sure only 2 are kept
        console.runCommand(command);

        console.runCommand(command);

        console.runCommand(command);

        assertEquals(numToKeep, countBackupFiles(coreName));

    }

    private int countBackupFiles(String coreName) {
        File solrDir =  getSolrDataPath(coreName);

        File[] backupFiles = new File[0];

        if (solrDir != null && solrDir.exists()) {
            backupFiles = solrDir.listFiles(new FilenameFilter() {
                @Override
                public boolean accept(File dir, String name) {
                    return name.contains("snapshot");
                }
            });
        }

        return backupFiles.length;
    }

    private File getSolrDataPath(String coreName) {
        String home = System.getProperty(DDF_HOME_PROPERTY);
        File file =  Paths.get(home + "/data/solr/" + coreName + "/data").toFile();
        return file;
    }

}
