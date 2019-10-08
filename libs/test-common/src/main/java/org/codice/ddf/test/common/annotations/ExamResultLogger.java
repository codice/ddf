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
package org.codice.ddf.test.common.annotations;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExamResultLogger extends TestWatcher {
  protected static final Logger LOGGER = LoggerFactory.getLogger(ExamResultLogger.class);

  @Override
  protected void failed(Throwable e, Description description) {
    LOGGER.info("FAILURE: {} failed.", description.getMethodName());
  }

  @Override
  protected void succeeded(Description description) {
    LOGGER.info("SUCCESS: {} passed.", description.getMethodName());
  }
}
