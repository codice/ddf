/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.test.itests.platform;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import java.io.File;
import java.io.FilenameFilter;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.test.common.LoggingUtils;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.junit.After;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class TestSolrCommands extends AbstractIntegrationTest {

  private static final String BACKUP_COMMAND = "solr:backup";

  private static final String DDF_HOME_PROPERTY = "ddf.home";

  private static final String BACKUP_SUCCESS_MESSAGE = "Backup of [catalog] complete";

  private static final String BACKUP_ERROR_MESSAGE_FORMAT = "Error backing up Solr core: [%s]";

  private static final String CATALOG_CORE_NAME = "catalog";

  @BeforeExam
  public void beforeExam() throws Exception {
    try {
      basePort = getBasePort();
      getServiceManager().startFeature(true, getDefaultRequiredApps());
      getServiceManager().waitForAllBundles();
      getCatalogBundle().waitForCatalogProvider();
      getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query");

      configureRestForGuest();
      getSecurityPolicy().waitForGuestAuthReady(REST_PATH.getUrl() + "?_wadl");
    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed in @BeforeExam: ");
    }
  }

  @After
  public void tearDown() {
    cleanUpBackups(CATALOG_CORE_NAME);
  }

  // TODO: Turn on this test once DDF-3340 is complete
  @Ignore
  @Test
  public void testSolrBackupCommand() {
    String output = console.runCommand(BACKUP_COMMAND);
    assertThat(output, containsString(BACKUP_SUCCESS_MESSAGE));
  }

  // TODO: Turn on this test once DDF-3340 is complete
  @Ignore
  @Test
  public void testSolrBackupBadCoreName() {
    String coreName = "blah";
    String command = BACKUP_COMMAND + " -c " + coreName;
    String output = console.runCommand(command);

    assertThat(output, containsString(String.format(BACKUP_ERROR_MESSAGE_FORMAT, coreName)));
  }

  // TODO: Turn on this test once DDF-3340 is complete
  @Ignore
  @Test
  public void testSolrBackupNumToKeep() throws InterruptedException {
    int numToKeep = 2;

    String command = BACKUP_COMMAND + " --numToKeep " + numToKeep;

    // Run this 3 times to make sure 2 backups are kept
    // On run 1, backup A is created.
    console.runCommand(command);
    Set<File> firstBackupDirSet = waitForBackupDirsToBeCreated(CATALOG_CORE_NAME, 1, 1);

    // On run 2, backup B is created (2 backups now: A and B).
    console.runCommand(command);
    Set<File> secondBackupDirSet = waitForBackupDirsToBeCreated(CATALOG_CORE_NAME, 2, 2);
    assertTrue(
        "Unexpected backup directories found on pass 2.",
        secondBackupDirSet.containsAll(firstBackupDirSet));

    // On run 3, backup C is created (backup A is deleted and backups B and C remain).
    console.runCommand(command);
    // Wait for the 3rd backup to replace the 1st backup
    Set<File> thirdBackupDirSet =
        waitForFirstBackupDirToBeDeleted(CATALOG_CORE_NAME, firstBackupDirSet);

    assertThat(
        "Wrong number of backup directories kept. Number of backups found in "
            + getSolrDataPath(CATALOG_CORE_NAME).getAbsolutePath()
            + " is : ["
            + thirdBackupDirSet.size()
            + "]; Expected: [2].",
        thirdBackupDirSet,
        hasSize(2));

    secondBackupDirSet.removeAll(firstBackupDirSet);
    assertTrue(
        "Unexpected backup directories found on pass 3.",
        thirdBackupDirSet.containsAll(secondBackupDirSet));
  }

  private Set<File> waitFor(String coreName, Predicate<Set<File>> predicate)
      throws InterruptedException {
    final long timeout = TimeUnit.SECONDS.toMillis(10);
    int currentWaitTime = 0;

    Set<File> backupDirs = getBackupDirectories(coreName);

    while (predicate.apply(backupDirs) && (currentWaitTime < timeout)) {
      TimeUnit.MILLISECONDS.sleep(250);
      currentWaitTime += 250;
      backupDirs = getBackupDirectories(coreName);
    }

    if (currentWaitTime >= timeout) {
      fail(
          "Timed out after "
              + timeout
              + " seconds waiting for correct number of backups in "
              + getSolrDataPath(CATALOG_CORE_NAME).getAbsolutePath());
    }

    return backupDirs;
  }

  private Set<File> waitForFirstBackupDirToBeDeleted(
      String coreName, final Set<File> firstBackupDirSet) throws InterruptedException {
    return waitFor(
        coreName,
        new Predicate<Set<File>>() {
          @Override
          public boolean apply(Set<File> backupDirs) {
            return backupDirs.containsAll(firstBackupDirSet) && (backupDirs.size() != 2);
          }
        });
  }

  private Set<File> waitForBackupDirsToBeCreated(String coreName, final int numberOfDirs, int pass)
      throws InterruptedException {
    Set<File> backupDirs =
        waitFor(
            coreName,
            new Predicate<Set<File>>() {
              @Override
              public boolean apply(Set<File> backupDirs) {
                return backupDirs.size() < numberOfDirs;
              }
            });

    assertThat(
        "Wrong number of backup directories created on pass: ["
            + pass
            + "].  Found: "
            + backupDirs
            + ". Expected: ["
            + numberOfDirs
            + "].",
        backupDirs,
        hasSize(numberOfDirs));
    return backupDirs;
  }

  private Set<File> getBackupDirectories(String coreName) {
    File solrDir = getSolrDataPath(coreName);

    File[] backupDirs;

    backupDirs =
        solrDir.listFiles(
            new FilenameFilter() {
              @Override
              public boolean accept(File dir, String name) {
                // Only match on snapshot.<timestamp> directories, filter out snapshot_metadata
                return name.startsWith("snapshot.");
              }
            });

    return Sets.newHashSet(backupDirs);
  }

  private File getSolrDataPath(String coreName) {
    String home = System.getProperty(DDF_HOME_PROPERTY);
    File dir = Paths.get(home + "/data/solr/" + coreName + "/data").toFile();
    return dir;
  }

  private void cleanUpBackups(String coreName) {
    Set<File> backupDirs = getBackupDirectories(coreName);
    for (File backupDir : backupDirs) {
      FileUtils.deleteQuietly(backupDir);
    }
  }
}
