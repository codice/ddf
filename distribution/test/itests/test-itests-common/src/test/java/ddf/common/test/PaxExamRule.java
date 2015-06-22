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
package ddf.common.test;

import static org.junit.Assert.fail;

import java.lang.annotation.Annotation;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.junit.runners.model.TestClass;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adds support for {@link BeforeExam} and {@link AfterExam} annotations with OPS4J Pax
 * Exam integration tests.
 *
 * <code>
 * @Rule
 * public PaxExamRule paxExamRule = new PaxExamRule(this);
 * </code>
 */
public class PaxExamRule implements TestRule {

    private static final Logger LOGGER = LoggerFactory.getLogger(PaxExamRule.class);

    public static final String BEFORE_EXAM_FAILURE_MESSAGE = "Failed to setup exam for %s: %s";

    public static final String AFTER_EXAM_FAILURE_MESSAGE = "Failed to run AfterExam annotation(s) for %s: %s";

    public static final String EXAM_SETUP_FAILED_MESSAGE = "Exam setup failed";

    private static boolean firstRun = true;

    private static boolean setupFailed = false;

    private static int testCount;

    private static int testsExecuted;

    private Object testInstance;

    public PaxExamRule(Object testInstance) {
        this.testInstance = testInstance;
    }

    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    starting(description);
                    base.evaluate();
                } finally {
                    finished(description);
                }
            }
        };
    }

    private void starting(Description description) {
        testsExecuted++;
        String testClassName = description.getTestClass().getSimpleName();

        if (setupFailed) {
            fail(EXAM_SETUP_FAILED_MESSAGE);
        }

        if (firstRun) {
            LOGGER.info("Starting test(s) for {}", testClassName);
            firstRun = false;

            TestClass testClass = new TestClass(description.getTestClass());
            testCount = getTestCount(testClass);
            try {
                runAnnotations(BeforeExam.class, testClass);
            } catch (Throwable throwable) {
                setupFailed = true;
                fail(String.format(BEFORE_EXAM_FAILURE_MESSAGE, testClassName, throwable.getMessage()));
            }
        }

        LOGGER.info("Starting {} ({}/{})", description.getMethodName(), testsExecuted, testCount);
    }

    private void finished(Description description) {
        LOGGER.info("Finished {} ({}/{})", description.getMethodName(), testsExecuted, testCount);

        if (testsExecuted == testCount) {
            resetStaticFields();

            String testClassName = description.getTestClass().getSimpleName();
            try {
                runAnnotations(AfterExam.class, new TestClass(description.getTestClass()));
            } catch (Throwable throwable) {
                fail(String.format(AFTER_EXAM_FAILURE_MESSAGE, testClassName, throwable.getMessage()));
            }
            LOGGER.info("Finished test(s) for {}", testClassName);
        }
    }

    private void resetStaticFields() {
        firstRun = true;
        setupFailed = false;
        testsExecuted = 0;
    }

    private void runAnnotations(Class<? extends Annotation> annotation, TestClass testClass)
            throws Throwable {
        if (testInstance != null) {
            for (FrameworkMethod annotatedMethod : testClass.getAnnotatedMethods(annotation)) {
                annotatedMethod.invokeExplosively(testInstance);
            }
        }
    }

    private int getTestCount(TestClass testClass) {
        int count = 0;
        for (FrameworkMethod method : testClass.getAnnotatedMethods(Test.class)) {
            if (method.getAnnotation(Ignore.class) == null) {
                count++;
            }
        }
        return count;
    }

}
