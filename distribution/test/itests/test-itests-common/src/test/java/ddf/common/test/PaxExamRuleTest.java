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
package ddf.common.test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.wrappedBundle;
import static junit.framework.TestCase.fail;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.JUnitCore;
import org.junit.runner.Result;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

public class PaxExamRuleTest {

    public static final String FAILING_TEST_MESSAGE = "test failed";

    public static final String BEFORE_EXAM_EXCEPTION_MESSAGE = "BeforeExam exception";

    public static final String AFTER_EXAM_EXCEPTION_MESSAGE = "AfterExam exception";

    public static final String EXPECTED_BEFORE_EXAM_ERROR_MESSAGE = String
            .format(PaxExamRule.BEFORE_EXAM_FAILURE_MESSAGE,
                    FailingBeforeExamTest.class.getSimpleName(), BEFORE_EXAM_EXCEPTION_MESSAGE);

    public static final String EXPECTED_AFTER_EXAM_ERROR_MESSAGE = String
            .format(PaxExamRule.AFTER_EXAM_FAILURE_MESSAGE,
                    FailingAfterExamTest.class.getSimpleName(), AFTER_EXAM_EXCEPTION_MESSAGE);

    public static class SuperDummyTest {

        @Rule
        public PaxExamRule paxExamRule = new PaxExamRule(this);

        @Configuration
        public Option[] config() {
            return options(junitBundles(),
                    wrappedBundle(mavenBundle("org.assertj", "assertj-core").versionAsInProject()));
        }

        @Test
        public void superTest() {

        }
    }

    public static class DummyTest extends SuperDummyTest {

        @Test
        public void passingTest() {

        }

        @Test
        public void secondPassingTest() {

        }

        @Test
        public void failingTest() {
            fail(FAILING_TEST_MESSAGE);
        }

        @Test
        @Ignore
        public void ignoredTest() {

        }

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

    @Test
    public void validBeforeAndAfter() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(PassingBeforeExamAndAfterExamTest.class);

        assertThat(result.getFailures()).extracting("message")
                .contains(FAILING_TEST_MESSAGE);

        assertResultCounts(result, 1);
    }

    @Test
    public void failingBeforeExam() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(FailingBeforeExamTest.class);

        assertThat(result.getFailures()).extracting("message")
                .containsOnly(EXPECTED_BEFORE_EXAM_ERROR_MESSAGE,
                        PaxExamRule.EXAM_SETUP_FAILED_MESSAGE);

        assertResultCounts(result, 4);
    }

    @Test
    public void failingAfterExam() {
        JUnitCore core = new JUnitCore();
        Result result = core.run(FailingAfterExamTest.class);

        assertThat(result.getFailures()).extracting("message")
                .contains(EXPECTED_AFTER_EXAM_ERROR_MESSAGE, FAILING_TEST_MESSAGE);

        assertResultCounts(result, 2);
    }

    private void assertResultCounts(Result result, int failureCount) {
        assertThat(result.getRunCount()).as("Check run count").isEqualTo(4);
        assertThat(result.getIgnoreCount()).as("Check ignore count").isEqualTo(1);
        assertThat(result.getFailureCount()).as("Check failure count").isEqualTo(failureCount);
    }

}
