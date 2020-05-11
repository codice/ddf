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
package org.codice.ddf.commands.catalog;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import ddf.catalog.transform.InputTransformer;
import ddf.security.audit.SecurityLogger;
import java.io.InputStream;
import java.util.ArrayList;
import org.apache.commons.lang3.SystemUtils;
import org.codice.ddf.commands.util.DigitalSignature;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;
import org.osgi.framework.ServiceReference;

/** Tests the {@link IngestCommand} output. */
public class IngestCommandTest extends CommandCatalogFrameworkCommon {

  @Rule public TemporaryFolder testFolder = new TemporaryFolder();

  IngestCommand ingestCommand;

  private DigitalSignature verifier;

  @Before
  public void setup() throws Exception {
    this.verifier = mock(DigitalSignature.class);

    when(verifier.createDigitalSignature(
            any(InputStream.class), any(String.class), any(String.class)))
        .thenReturn(new byte[0]);
    doReturn(true)
        .when(verifier)
        .verifyDigitalSignature(any(InputStream.class), any(InputStream.class), any(String.class));

    ingestCommand = new IngestCommand(verifier);
    ingestCommand.securityLogger = mock(SecurityLogger.class);
    ingestCommand.catalogFramework = givenCatalogFramework(getResultList("id1", "id2"));

    BundleContext bundleContext = mock(BundleContext.class);
    try {
      when(bundleContext.getServiceReferences(anyString(), anyString()))
          .thenReturn(new ServiceReference[] {mock(ServiceReference.class)});
      InputTransformer inputTransformer = mock(InputTransformer.class);
      when(bundleContext.getService(anyObject())).thenReturn(inputTransformer);
    } catch (InvalidSyntaxException e) {
      // ignore
    }
    ingestCommand.bundleContext = bundleContext;

    ingestCommand.transformerId = CatalogCommands.SERIALIZED_OBJECT_ID;
    ingestCommand.filePath = testFolder.getRoot().getAbsolutePath();
  }

  /**
   * Test empty folder
   *
   * @throws Exception
   */
  @Test
  public void testNoFiles() throws Exception {
    // when
    ingestCommand.executeWithSubject();

    // then
    String expectedIngested = "0 file(s) ingested";
    assertThat(consoleOutput.getOutput(), containsString(expectedIngested));
  }

  /**
   * Check expected output and ingested,failed counts
   *
   * @throws Exception
   */
  @Test
  public void testExpectedCounts() throws Exception {
    // given
    testFolder.newFile("somefile1.txt");
    testFolder.newFile("somefile2.jpg");
    testFolder.newFile("somefile3.txt");
    testFolder.newFile("somefile4.jpg");
    testFolder.newFile("somefile5.txt");

    // when
    ingestCommand.executeWithSubject();

    // then
    String expectedIngested = "0 file(s) ingested";
    String expectedFailed = "5 file(s) failed";
    assertThat(consoleOutput.getOutput(), containsString(expectedIngested));
    assertThat(consoleOutput.getOutput(), containsString(expectedFailed));
  }

  /**
   * Check expected output and ingested,ignored,failed counts for ignore command
   *
   * @throws Exception
   */
  @Test
  public void testExpectedCountsWithIgnore() throws Exception {
    // given
    testFolder.newFile("somefile1.txt");
    testFolder.newFile("somefile2.jpg");
    testFolder.newFile("somefile3.txt");
    testFolder.newFile("somefile4.jpg");
    testFolder.newFile("somefile5.txt");

    ArrayList<String> ignoreList = new ArrayList<>();
    ignoreList.add(".txt");
    ingestCommand.ignoreList = ignoreList;

    // when
    ingestCommand.executeWithSubject();

    // then
    String expectedIngested = "0 file(s) ingested";
    String expectedIgnored = "3 file(s) ignored";
    String expectedFailed = "2 file(s) failed";
    assertThat(consoleOutput.getOutput(), containsString(expectedIngested));
    assertThat(consoleOutput.getOutput(), containsString(expectedFailed));
    assertThat(consoleOutput.getOutput(), containsString(expectedIgnored));
  }

  /**
   * Check expected output for hidden files
   *
   * @throws Exception
   */
  @Test
  public void testIgnoreHiddenFiles() throws Exception {
    assumeFalse(SystemUtils.IS_OS_WINDOWS);

    // given
    testFolder.newFile(".somefile1");
    testFolder.newFile(".somefile2");
    testFolder.newFile(".somefile3");
    testFolder.newFile(".somefile4");
    testFolder.newFile("somefile5");

    // when
    ingestCommand.executeWithSubject();

    // then
    String expectedIngested = "0 file(s) ingested";
    String expectedFailed = "1 file(s) failed";

    String firstOutput = consoleOutput.getOutput();
    String secondOutput = consoleOutput.getOutput();

    assertThat(firstOutput, containsString(expectedIngested));
    assertThat(secondOutput, containsString(expectedFailed));
    assertThat(consoleOutput.getOutput(), not(containsString("ignored")));
  }

  @Test
  public void testIncludeContentNonZipFile() throws Exception {
    // given
    ingestCommand.includeContent = true;

    // when
    ingestCommand.executeWithSubject();

    // then
    String expectedMessage = "must be a zip file";
    assertThat(consoleOutput.getOutput(), containsString(expectedMessage));
  }
}
