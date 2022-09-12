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
package org.codice.ddf.test.common.rules;

import java.lang.reflect.Field;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.codice.ddf.test.common.annotations.MockOsgiService;
import org.junit.rules.MethodRule;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Test rule used to create mock OSGi services for all fields annotated with {@link
 * MockOsgiService}.
 */
public class ServiceRegistrationRule implements MethodRule {

  private static final Logger LOGGER = LoggerFactory.getLogger(ServiceRegistrationRule.class);

  @Override
  public Statement apply(Statement statement, FrameworkMethod method, Object target) {
    LOGGER.trace("apply");

    List<Field> fieldsWithAnnotation =
        FieldUtils.getFieldsListWithAnnotation(target.getClass(), MockOsgiService.class);

    List<MockOsgiServiceField> mockOsgiServiceFields =
        fieldsWithAnnotation.stream()
            .map(field -> new MockOsgiServiceField(target, field))
            .collect(Collectors.toList());

    return new Statement() {
      @Override
      public void evaluate() throws Throwable {
        try {
          LOGGER.debug("Setting up mock services for test {}", method.getName());
          mockOsgiServiceFields.forEach(MockOsgiServiceField::setup);
          statement.evaluate();
        } finally {
          mockOsgiServiceFields.forEach(MockOsgiServiceField::cleanup);
        }
      }
    };
  }
}
