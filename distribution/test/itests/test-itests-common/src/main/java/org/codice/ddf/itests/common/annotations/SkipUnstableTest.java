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
package org.codice.ddf.itests.common.annotations;

import org.codice.ddf.itests.common.annotations.ConditionalIgnoreRule.IgnoreCondition;

/**
 * Condition used by the {@link ConditionalIgnoreRule.IgnoreCondition} to indicates that a test
 * should only be run if the {@value INCLUDE_UNSTABLE_TESTS_PROPERTY} system property is set to
 * {@code true}.
 *
 * <p>Note that you need the {@link ConditionalIgnoreRule} at the beginning of your test class in
 * order for the annotation to take effect.
 *
 * <p>Example:
 *
 * <pre>
 *   @Rule
 *   public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();
 *   // ...
 *   @Test
 *   @ConditionalIgnore(condition = SkipUnstableTest.class)
 *   public void unstableTestMethod() throws Exception {
 *       // ...
 *   }
 * </pre>
 */
public class SkipUnstableTest implements IgnoreCondition {
  public static final String INCLUDE_UNSTABLE_TESTS_PROPERTY = "includeUnstableTests";

  @Override
  public boolean isSatisfied() {
    return !System.getProperty(INCLUDE_UNSTABLE_TESTS_PROPERTY, "false").equals("true");
  }
}
