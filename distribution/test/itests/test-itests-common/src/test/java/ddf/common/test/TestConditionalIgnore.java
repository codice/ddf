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
package ddf.common.test;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.codice.ddf.itests.common.annotations.ConditionalIgnoreRule;
import org.codice.ddf.itests.common.annotations.ConditionalIgnoreRule.ConditionalIgnore;
import org.codice.ddf.itests.common.annotations.SkipUnstableTest;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.notification.Failure;
import org.junit.runner.notification.RunListener;

public class TestConditionalIgnore {

  public static class AnnotatedTest {

    @Rule public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

    @Test
    public void stableTest() {}

    @Test
    @ConditionalIgnore(condition = SkipUnstableTest.class)
    public void unstableTest() {}
  }

  private static class SkippedTestsCollector extends RunListener {
    List<Failure> skippedTests = new ArrayList<>();

    @Override
    public void testAssumptionFailure(Failure failure) {
      skippedTests.add(failure);
      super.testAssumptionFailure(failure);
    }
  }

  private JUnitCore core;

  private SkippedTestsCollector skippedTestsCollector;

  @Before
  public void setup() {
    core = new JUnitCore();
    skippedTestsCollector = new SkippedTestsCollector();
    core.addListener(skippedTestsCollector);
  }

  @Test
  public void testIncludeUnstableTestIsNotSet() {
    System.clearProperty("includeUnstableTests");

    Result result = core.run(AnnotatedTest.class);

    assertThat(result.wasSuccessful(), is(true));
    assertThat(result.getRunCount(), is(2));
    assertThat(result.getFailureCount(), is(0));
    assertThat(result.getIgnoreCount(), is(0));
    assertThat(skippedTestsCollector.skippedTests, hasSize(1));
    assertThat(
        skippedTestsCollector.skippedTests.get(0).getTestHeader(), startsWith("unstableTest"));
  }

  @Test
  public void testIncludeUnstableTestIsFalse() {
    System.setProperty("includeUnstableTests", "false");

    Result result = core.run(AnnotatedTest.class);

    assertThat(result.wasSuccessful(), is(true));
    assertThat(result.getRunCount(), is(2));
    assertThat(result.getFailureCount(), is(0));
    assertThat(result.getIgnoreCount(), is(0));
    assertThat(skippedTestsCollector.skippedTests, hasSize(1));
    assertThat(
        skippedTestsCollector.skippedTests.get(0).getTestHeader(), startsWith("unstableTest"));
  }

  @Test
  public void testIncludeUnstableTestIsTrue() {
    System.setProperty("includeUnstableTests", "true");

    Result result = core.run(AnnotatedTest.class);

    assertThat(result.wasSuccessful(), is(true));
    assertThat(result.getRunCount(), is(2));
    assertThat(result.getFailureCount(), is(0));
    assertThat(result.getIgnoreCount(), is(0));
    assertThat(skippedTestsCollector.skippedTests, hasSize(0));
  }
}
