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

import static junit.framework.TestCase.fail;
import static org.assertj.core.api.Assertions.assertThat;
import static org.ops4j.pax.exam.CoreOptions.bundle;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;

import org.codice.ddf.test.common.DependencyVersionResolver;
import org.codice.ddf.test.common.annotations.AfterExam;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.codice.ddf.test.common.annotations.PaxExamRule;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.junit.runner.notification.Failure;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

public class PaxExamRuleIT {

  private static final String FAILING_TEST_MESSAGE = "test failed";

  private static final String BEFORE_EXAM_EXCEPTION_MESSAGE =
      "java.lang.RuntimeException: BeforeExam exception";

  private static final String AFTER_EXAM_EXCEPTION_MESSAGE =
      "java.lang.RuntimeException: AfterExam exception";

  private static final String EXPECTED_BEFORE_EXAM_ERROR_MESSAGE =
      String.format(
          PaxExamRule.BEFORE_EXAM_FAILURE_MESSAGE, FailingBeforeExamTest.class.getSimpleName());

  private static final String EXPECTED_AFTER_EXAM_ERROR_MESSAGE =
      String.format(
          PaxExamRule.AFTER_EXAM_FAILURE_MESSAGE, FailingAfterExamTest.class.getSimpleName());

  public static class SuperDummyTest {

    @Rule public PaxExamRule paxExamRule = new PaxExamRule(this);

    @Configuration
    public Option[] config() {
      return options(
          junitBundles(),
          bundle("file:target/test-common-" + System.getProperty("ddf.version") + ".jar"),
          wrappedBundle(
              mavenBundle("org.assertj", "assertj-core")
                  .version(DependencyVersionResolver.resolver())));
    }

    @Test
    public void superTest() {}
  }

  public static class DummyTest extends SuperDummyTest {

    @Test
    public void passingTest() {}

    @Test
    public void secondPassingTest() {}

    @Test
    public void failingTest() {
      fail(FAILING_TEST_MESSAGE);
    }

    @Test
    @Ignore
    @SuppressWarnings("squid:S1607")
    public void ignoredTest() {}
  }

  @RunWith(PaxExam.class)
  @ExamReactorStrategy(PerClass.class)
  public static class PassingBeforeExamAndAfterExamTest extends DummyTest {

    private static boolean ranBeforeExam;

    private static boolean ranAfterExam;

    @BeforeExam
    public void beforeExam() {
      assertThat(ranBeforeExam).isFalse();
      assertThat(ranAfterExam).isFalse();
      ranBeforeExam = true;
    }

    @Before
    public void before() {
      assertThat(ranBeforeExam).isTrue();
      assertThat(ranAfterExam).isFalse();
    }

    @After
    public void after() {
      assertThat(ranBeforeExam).isTrue();
      assertThat(ranAfterExam).isFalse();
    }

    @AfterExam
    public void afterExam() {
      assertThat(ranBeforeExam).isTrue();
      assertThat(ranAfterExam).isFalse();
      ranAfterExam = true;
    }
  }

  @RunWith(PaxExam.class)
  @ExamReactorStrategy(PerClass.class)
  public static class FailingBeforeExamTest extends DummyTest {

    @BeforeExam
    public void beforeExam() {
      throw new RuntimeException(BEFORE_EXAM_EXCEPTION_MESSAGE);
    }
  }

  @RunWith(PaxExam.class)
  @ExamReactorStrategy(PerClass.class)
  public static class FailingAfterExamTest extends DummyTest {

    @AfterExam
    public void afterExam() {
      throw new RuntimeException(AFTER_EXAM_EXCEPTION_MESSAGE);
    }
  }

  private Result result;

  // Rule used to print the test result stack traces when a test fails. Useful to debug
  // container startup failures.
  @Rule
  public TestWatcher exceptionLogger =
      new TestWatcher() {
        @Override
        protected void failed(Throwable e, Description description) {
          result.getFailures().stream()
              .map(Failure::getException)
              .forEach(Throwable::printStackTrace);
        }
      };

  @Test
  public void validBeforeAndAfter() {
    JUnitCore core = new JUnitCore();
    result = core.run(PassingBeforeExamAndAfterExamTest.class);

    assertThat(result.getFailures()).extracting("message").contains(FAILING_TEST_MESSAGE);

    assertResultCounts(result, 1);
  }

  @Test
  public void failingBeforeExam() {
    JUnitCore core = new JUnitCore();
    result = core.run(FailingBeforeExamTest.class);

    boolean examFail =
        result.getFailures().stream()
            .anyMatch(
                failure -> failure.getMessage().equals(PaxExamRule.EXAM_SETUP_FAILED_MESSAGE));
    boolean beforeExamFail =
        result.getFailures().stream()
            .anyMatch(failure -> failure.getMessage().contains(EXPECTED_BEFORE_EXAM_ERROR_MESSAGE));

    assertThat(examFail && beforeExamFail).isTrue();
    assertResultCounts(result, 4);
  }

  @Test
  public void failingAfterExam() {
    JUnitCore core = new JUnitCore();
    result = core.run(FailingAfterExamTest.class);

    boolean examFail =
        result.getFailures().stream()
            .anyMatch(failure -> failure.getMessage().equals(FAILING_TEST_MESSAGE));
    boolean afterExamFail =
        result.getFailures().stream()
            .anyMatch(failure -> failure.getMessage().contains(EXPECTED_AFTER_EXAM_ERROR_MESSAGE));

    assertThat(examFail && afterExamFail).isTrue();
    assertResultCounts(result, 2);
  }

  private void assertResultCounts(Result result, int failureCount) {
    assertThat(result.getRunCount()).as("Check run count").isEqualTo(4);
    assertThat(result.getIgnoreCount()).as("Check ignore count").isEqualTo(1);
    assertThat(result.getFailureCount()).as("Check failure count").isEqualTo(failureCount);
  }
}
