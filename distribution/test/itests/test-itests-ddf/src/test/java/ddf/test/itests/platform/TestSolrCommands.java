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
package ddf.test.itests.platform;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;

import ddf.common.test.BeforeExam;
import ddf.common.test.KarafConsole;
import ddf.test.itests.AbstractIntegrationTest;

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
        try {
            basePort = getBasePort();
            getAdminConfig().setLogLevels();
            getServiceManager().waitForRequiredApps(getDefaultRequiredApps());
            getServiceManager().waitForAllBundles();
            getCatalogBundle().waitForCatalogProvider();
            getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query");
            console = new KarafConsole(getServiceManager().getBundleContext(), features, sessionFactory);
        } catch (Exception e) {
            LOGGER.error("Failed in @BeforeExam: ", e);
            fail("Failed in @BeforeExam: " + e.getMessage());
        }
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
    public void testSolrBackupNumToKeep() throws InterruptedException {
        int numToKeep = 3;
        String coreName = "catalog";

        String command = BACKUP_COMMAND + " --numToKeep " + numToKeep;

        // Run this three times to make sure only 2 are kept
        console.runCommand(command);
        Set<File> firstBackupFileSet = waitForBackupFilesToBeCreated(coreName, 1);

        console.runCommand(command);
        Set<File> secondBackupFileSet = waitForBackupFilesToBeCreated(coreName, 2);
        assertTrue("Unexpected backup files found",
                secondBackupFileSet.containsAll(firstBackupFileSet));

        console.runCommand(command);

        secondBackupFileSet.removeAll(firstBackupFileSet);
        Set<File> thirdBackupFileSet = waitForFirstFileToBeDeleted(coreName, firstBackupFileSet);

        assertThat("Wrong number of backup files created", thirdBackupFileSet, hasSize(3));
        assertTrue("Unexpected backup files found",
                thirdBackupFileSet.containsAll(secondBackupFileSet));
    }

    private Set<File> waitFor(String coreName, Predicate<Set<File>> predicate)
            throws InterruptedException {
        final long timeout = TimeUnit.SECONDS.toMillis(10);
        int currentWaitTime = 0;

        Set<File> backupFiles = getBackupFiles(coreName);

        while (predicate.apply(backupFiles) && (currentWaitTime < timeout)) {
            TimeUnit.MILLISECONDS.sleep(250);
            currentWaitTime += 250;
            backupFiles = getBackupFiles(coreName);
        }

        return backupFiles;
    }

    private Set<File> waitForFirstFileToBeDeleted(String coreName,
            final Set<File> firstBackupFileSet) throws InterruptedException {
        return waitFor(coreName, new Predicate<Set<File>>() {
            @Override
            public boolean apply(Set<File> backupFiles) {
                return backupFiles.containsAll(firstBackupFileSet) && (backupFiles.size() != 2);
            }
        });
    }

    private Set<File> waitForBackupFilesToBeCreated(String coreName, final int numberOfFiles)
            throws InterruptedException {
        Set<File> backupFiles = waitFor(coreName, new Predicate<Set<File>>() {
            @Override
            public boolean apply(Set<File> backupFiles) {
                return backupFiles.size() < numberOfFiles;
            }
        });

        assertThat("Wrong number of backup files created", backupFiles, hasSize(numberOfFiles));
        return backupFiles;
    }

    private Set<File> getBackupFiles(String coreName) {
        File solrDir = getSolrDataPath(coreName);

        File[] backupFiles;

        backupFiles = solrDir.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.contains("snapshot");
            }
        });

        return Sets.newHashSet(backupFiles);
    }

    private File getSolrDataPath(String coreName) {
        String home = System.getProperty(DDF_HOME_PROPERTY);
        File file = Paths.get(home + "/data/solr/" + coreName + "/data")
                .toFile();
        return file;
    }
}
