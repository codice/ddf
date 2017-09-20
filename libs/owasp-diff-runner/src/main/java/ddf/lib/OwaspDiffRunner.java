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

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;
import org.apache.commons.io.IOUtils;
import org.apache.maven.shared.invoker.DefaultInvocationRequest;
import org.apache.maven.shared.invoker.DefaultInvoker;
import org.apache.maven.shared.invoker.InvocationRequest;
import org.apache.maven.shared.invoker.InvocationResult;
import org.apache.maven.shared.invoker.Invoker;
import org.apache.maven.shared.invoker.MavenInvocationException;
import org.codehaus.plexus.util.StringUtils;

public class OwaspDiffRunner {

  public static final String MAVEN_VERSION_COMMAND = "mvn -version";

  public static final String MAVEN_SETTINGS_COMMAND = "mvn help:effective-settings";

  public static final String GIT_DIFF_NAME_COMMAND = "git diff --name-only ";

  public static final String GIT_SHORT_BRANCH_NAME_COMMAND = "git rev-parse --abbrev-ref HEAD";

  private static final String DASHES = StringUtils.repeat("-", 25);

  public static final String BEGIN_OWASP_AUDIT = DASHES + " BEGIN OWASP DIFF AUDIT " + DASHES;

  public static final String END_OWASP_AUDIT = DASHES + " END OWASP DIFF AUDIT " + DASHES;

  public static final String PROJECT_ROOT =
      System.getProperty("user.dir").split("/libs/owasp-diff-runner")[0];

  private static String mavenHome;

  private static String localRepo;

  private static Runtime runTime = Runtime.getRuntime();

  private static Invoker invoker = new DefaultInvoker();

  public OwaspDiffRunner(Runtime runTime, Invoker invoker) {
    this.runTime = runTime;
    this.invoker = invoker;
  }

  public static void main(String[] args) throws OwaspDiffRunnerException {
    System.out.println(BEGIN_OWASP_AUDIT);

    try {
      mavenHome = getMavenHome();
      localRepo = getLocalRepo();
      String modulesOfChangedPoms = getModulesOfChangedPoms();
      if (modulesOfChangedPoms.isEmpty()) {
        System.out.println("No changed poms.");
        return;
      }

      InvocationRequest request = new DefaultInvocationRequest();
      request.setPomFile(new File(PROJECT_ROOT + File.separator + "pom.xml"));
      request.setBaseDirectory(new File(PROJECT_ROOT));
      request.setLocalRepositoryDirectory(new File(localRepo));

      request.setGoals(
          Arrays.asList(
              "dependency-check:check", "--quiet", "-pl", modulesOfChangedPoms, "-Powasp"));
      invoker.setMavenHome(new File(mavenHome));
      System.out.println("-- Maven home: " + mavenHome);
      System.out.println("-- Local Maven repo: " + localRepo);

      InvocationResult mavenBuildResult;

      try {
        mavenBuildResult = invoker.execute(request);
      } catch (MavenInvocationException e) {
        throw new OwaspDiffRunnerException(
            OwaspDiffRunnerException.UNABLE_TO_RUN_MAVEN + modulesOfChangedPoms, e);
      }
      if (mavenBuildResult.getExitCode() != 0) {
        throw new OwaspDiffRunnerException(OwaspDiffRunnerException.FOUND_VULNERABILITIES);
      }
    } finally {
      System.out.println(END_OWASP_AUDIT);
    }
  }

  private static String getModulesOfChangedPoms() throws OwaspDiffRunnerException {
    String changedFiles;
    String currentBranchName;

    try {
      currentBranchName =
          IOUtils.toString(runTime.exec(GIT_SHORT_BRANCH_NAME_COMMAND).getInputStream())
              .replace(File.separator, "")
              .replace(System.getProperty("line.separator"), "");

      changedFiles =
          IOUtils.toString(
              runTime
                  .exec(GIT_DIFF_NAME_COMMAND + currentBranchName + "..master")
                  .getInputStream());
    } catch (IOException e) {
      throw new OwaspDiffRunnerException(OwaspDiffRunnerException.UNABLE_TO_RETRIEVE_GIT_INFO, e);
    }

    System.out.println(
        "Comparing commits of branch " + currentBranchName + " to master. Changed poms: ");

    return Arrays.stream(changedFiles.split(System.getProperty("line.separator")))
        .filter(path -> path.endsWith("pom.xml"))
        .peek(System.out::println)
        .map(
            path ->
                path.endsWith(File.separator + "pom.xml")
                    ? path.replace(File.separator + "pom.xml", "")
                    : path.replace(
                        "pom.xml",
                        File.separator)) // Special case for the root pom, change path pom.xml -> /
        .collect(Collectors.joining(","));
  }

  private static String getMavenHome() throws OwaspDiffRunnerException {
    String mavenHome;
    String mavenVersionInfo;

    try {
      mavenVersionInfo = IOUtils.toString(runTime.exec(MAVEN_VERSION_COMMAND).getInputStream());

      // parsing console response, confirmed to work with at least maven version 3.3.9
      mavenHome =
          Arrays.stream(mavenVersionInfo.split(System.getProperty("line.separator")))
              .filter(str -> str.contains("Maven home:"))
              .findFirst()
              .get()
              .split("Maven home:")[1]
              .trim();

    } catch (Exception e) {
      throw new OwaspDiffRunnerException(OwaspDiffRunnerException.UNABLE_TO_RETRIEVE_MAVEN_HOME, e);
    }

    return mavenHome;
  }

  private static String getLocalRepo() throws OwaspDiffRunnerException {
    String mavenHelpInfo;
    String localRepoProperty = System.getProperty("maven.repo.local");

    if (StringUtils.isNotEmpty(localRepoProperty)) {
      if (!new File(localRepoProperty).exists()) {
        throw new OwaspDiffRunnerException(
            OwaspDiffRunnerException.UNABLE_TO_RETRIEVE_LOCAL_MAVEN_REPO + localRepoProperty);
      }
      return localRepoProperty;
    }

    try {
      mavenHelpInfo = IOUtils.toString(runTime.exec(MAVEN_SETTINGS_COMMAND).getInputStream());

      // parsing console response, confirmed to work with at least maven version 3.3.9
      return mavenHelpInfo.split("<localRepository(.*\")?>")[1].split("</localRepository>")[0];

    } catch (Exception e) {
      throw new OwaspDiffRunnerException(
          OwaspDiffRunnerException.UNABLE_TO_RETRIEVE_LOCAL_MAVEN_REPO, e);
    }
  }
}
