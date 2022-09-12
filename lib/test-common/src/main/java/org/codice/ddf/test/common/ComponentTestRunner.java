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
package org.codice.ddf.test.common;

import org.junit.runner.notification.RunNotifier;
import org.junit.runners.model.InitializationError;
import org.ops4j.pax.exam.junit.PaxExam;

/**
 * JUnit test runner that dumps extra debug information to the console when a Pax Exam test
 * container fails to start.
 */
public class ComponentTestRunner extends PaxExam {
  public ComponentTestRunner(Class<?> testClass) throws InitializationError {
    super(testClass);
  }

  @Override
  public void run(RunNotifier notifier) {
    notifier.addFirstListener(new KarafContainerFailureLogger());
    super.run(notifier);
  }
}
