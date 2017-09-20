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
package ddf.lib;

import static ddf.lib.OwaspDiffRunner.GIT_DIFF_NAME_COMMAND;
import static ddf.lib.OwaspDiffRunner.GIT_SHORT_BRANCH_NAME_COMMAND;
import static ddf.lib.OwaspDiffRunner.MAVEN_SETTINGS_COMMAND;
import static ddf.lib.OwaspDiffRunner.MAVEN_VERSION_COMMAND;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.matches;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

public class TestOwaspDiffRunner {

  @Rule public TemporaryFolder fakeRepo;

  @Rule public ExpectedException expectedEx = ExpectedException.none();

  public final Runtime runtime = mock(Runtime.class);

  public final Process mavenVersionCommandProcess = mock(Process.class);

  public final Process mavenSettingCommandProcess = mock(Process.class);

  public final Process gitShortBranchNameCommandProcess = mock(Process.class);

  public final Process gitDiffNameCommandProcess = mock(Process.class);

  public final InvocationResult mavenBuildResult = mock(InvocationResult.class);

  public final Invoker invoker = mock(Invoker.class);

  private static OwaspDiffRunner owaspDiffRunner;

  @Before
  public void before() throws Exception {
    fakeRepo = new TemporaryFolder();
    fakeRepo.create();
    File fakeChangedPom = fakeRepo.newFile("pom.xml");
    System.setProperty("maven.repo.local", fakeChangedPom.getParent());
    //Set command line returns
    when(mavenVersionCommandProcess.getInputStream())
        .thenReturn(
            new ByteArrayInputStream(
                ("Maven home: " + fakeRepo.getRoot().getPath()).getBytes(StandardCharsets.UTF_8)));

    when(mavenSettingCommandProcess.getInputStream())
        .thenReturn(
            new ByteArrayInputStream(
                ("<localRepository>" + fakeRepo.getRoot().getPath() + "</localRepository>")
                    .getBytes(StandardCharsets.UTF_8)));

    when(gitShortBranchNameCommandProcess.getInputStream())
        .thenReturn(new ByteArrayInputStream("test-branch".getBytes(StandardCharsets.UTF_8)));

    when(gitDiffNameCommandProcess.getInputStream())
        .thenReturn(
            new ByteArrayInputStream(fakeChangedPom.getPath().getBytes(StandardCharsets.UTF_8)));

    //Set runtime when executing commands
    when(runtime.exec(MAVEN_VERSION_COMMAND)).thenReturn(mavenVersionCommandProcess);

    when(runtime.exec(MAVEN_SETTINGS_COMMAND)).thenReturn(mavenSettingCommandProcess);

    when(runtime.exec(GIT_SHORT_BRANCH_NAME_COMMAND)).thenReturn(gitShortBranchNameCommandProcess);

    when(runtime.exec(matches(GIT_DIFF_NAME_COMMAND + ".*"))).thenReturn(gitDiffNameCommandProcess);

    //Set maven executor
    when(mavenBuildResult.getExitCode()).thenReturn(0);

    when(invoker.execute(any())).thenReturn(mavenBuildResult);

    owaspDiffRunner = new OwaspDiffRunner(runtime, invoker);
  }

  @Test
  public void passOnNoOwaspFindings() throws OwaspDiffRunnerException {
    owaspDiffRunner.main(null);
  }

  @Test
  public void failOnOwaspFailure() throws OwaspDiffRunnerException {
    expectedEx.expect(OwaspDiffRunnerException.class);
    expectedEx.expectMessage(OwaspDiffRunnerException.FOUND_VULNERABILITIES);
    when(mavenBuildResult.getExitCode()).thenReturn(1);
    owaspDiffRunner.main(null);
  }

  @Test
  public void invalidMavenRepo() throws OwaspDiffRunnerException {
    expectedEx.expect(OwaspDiffRunnerException.class);
    expectedEx.expectMessage(OwaspDiffRunnerException.UNABLE_TO_RETRIEVE_LOCAL_MAVEN_REPO);

    System.setProperty("maven.repo.local", "not-a-real-repo");
    owaspDiffRunner.main(null);
  }

  @Test
  public void noChangedPoms() throws OwaspDiffRunnerException, IOException {
    when(gitDiffNameCommandProcess.getInputStream())
        .thenReturn(new ByteArrayInputStream("".getBytes(StandardCharsets.UTF_8)));
    owaspDiffRunner.main(null);
  }

  @Test
  public void changedRootProjectPom() throws OwaspDiffRunnerException, IOException {
    when(gitDiffNameCommandProcess.getInputStream())
        .thenReturn(new ByteArrayInputStream("pom.xml".getBytes(StandardCharsets.UTF_8)));
    owaspDiffRunner.main(null);
  }
}
