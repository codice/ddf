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
/**
 * ***************************************************************************** Copyright (c)
 * 2013,2014 Rüdiger Herrmann All rights reserved. This program and the accompanying materials are
 * made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: Rüdiger Herrmann - initial API and implementation Matt Morrissette - allow to
 * use non-static inner IgnoreConditions
 * ****************************************************************************
 */
package org.codice.ddf.itests.common.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Modifier;
import org.junit.Assume;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConditionalIgnoreRule implements MethodRule {

  private static final Logger LOGGER = LoggerFactory.getLogger(ConditionalIgnoreRule.class);

  public interface IgnoreCondition {
    boolean isSatisfied();
  }

  @Retention(RetentionPolicy.RUNTIME)
  @Target({ElementType.METHOD})
  public @interface ConditionalIgnore {
    Class<? extends IgnoreCondition> condition();
  }

  @Override
  public Statement apply(Statement base, FrameworkMethod method, Object target) {
    Statement result = base;
    if (hasConditionalIgnoreAnnotation(method)) {
      IgnoreCondition condition = getIgnoreContition(target, method);
      if (condition.isSatisfied()) {
        LOGGER.warn(
            "IgnoreCondition {} applies, ignoring test {}",
            condition.getClass().getSimpleName(),
            method.getName());
        result = new IgnoreStatement(condition);
      }
    }
    return result;
  }

  private static boolean hasConditionalIgnoreAnnotation(FrameworkMethod method) {
    return method.getAnnotation(ConditionalIgnore.class) != null;
  }

  private static IgnoreCondition getIgnoreContition(Object target, FrameworkMethod method) {
    ConditionalIgnore annotation = method.getAnnotation(ConditionalIgnore.class);
    return new IgnoreConditionCreator(target, annotation).create();
  }

  private static class IgnoreConditionCreator {
    private final Object target;

    private final Class<? extends IgnoreCondition> conditionType;

    IgnoreConditionCreator(Object target, ConditionalIgnore annotation) {
      this.target = target;
      this.conditionType = annotation.condition();
    }

    IgnoreCondition create() {
      checkConditionType();
      try {
        return createCondition();
      } catch (RuntimeException re) {
        throw re;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }

    private IgnoreCondition createCondition() throws Exception {
      IgnoreCondition result;
      if (isConditionTypeStandalone()) {
        result = conditionType.newInstance();
      } else {
        result = conditionType.getDeclaredConstructor(target.getClass()).newInstance(target);
      }
      return result;
    }

    private void checkConditionType() {
      if (!isConditionTypeStandalone() && !isConditionTypeDeclaredInTarget()) {
        String msg =
            "Conditional class '%s' is a member class "
                + "but was not declared inside the test case using it.\n"
                + "Either make this class a static class, "
                + "standalone class (by declaring it in it's own file) "
                + "or move it inside the test case using it";
        throw new IllegalArgumentException(String.format(msg, conditionType.getName()));
      }
    }

    private boolean isConditionTypeStandalone() {
      return !conditionType.isMemberClass() || Modifier.isStatic(conditionType.getModifiers());
    }

    private boolean isConditionTypeDeclaredInTarget() {
      return target.getClass().isAssignableFrom(conditionType.getDeclaringClass());
    }
  }

  private static class IgnoreStatement extends Statement {
    private final IgnoreCondition condition;

    IgnoreStatement(IgnoreCondition condition) {
      this.condition = condition;
    }

    @Override
    public void evaluate() {
      Assume.assumeTrue("Ignored by " + condition.getClass().getSimpleName(), false);
    }
  }
}
