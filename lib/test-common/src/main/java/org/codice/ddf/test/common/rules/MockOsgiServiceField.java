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

import static org.mockito.Mockito.mock;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.stream.Collectors;
import org.codice.ddf.configuration.DictionaryMap;
import org.codice.ddf.test.common.annotations.MockOsgiService;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Class used by the {@link ServiceRegistrationRule} to manage the mock service associated with a
 * field annotated with {@link MockOsgiService}.
 */
class MockOsgiServiceField {
  private static final Logger LOGGER = LoggerFactory.getLogger(MockOsgiServiceField.class);

  private final Object testInstance;

  private final Field field;

  private ServiceRegistration<?> serviceRegistration;

  /**
   * Default constructor.
   *
   * @param testInstance reference to the test class instance being executed
   * @param field field annotated
   */
  MockOsgiServiceField(Object testInstance, Field field) {
    this.testInstance = testInstance;
    this.field = field;
  }

  /**
   * Creates the mock service for the annotated field, registers it with the service registry and
   * assigns it to the field.
   */
  void setup() {
    BundleContext bundleContext =
        FrameworkUtil.getBundle(testInstance.getClass()).getBundleContext();

    try {
      MockOsgiService annotation = field.getAnnotation(MockOsgiService.class);
      Dictionary<String, Object> serviceProperties = getServiceProperties(annotation);
      Object mockService = createMock(annotation, serviceProperties);
      serviceRegistration =
          bundleContext.registerService(field.getType().getName(), mockService, serviceProperties);
      assignToField(mockService);
    } catch (RuntimeException | IllegalAccessException e) {
      String errorMessage =
          String.format("Failed to create mock service for field %s", field.getName());
      LOGGER.error(errorMessage, e);
      throw new UnsupportedOperationException(errorMessage, e);
    }
  }

  /** Cleans up the mock service registration. */
  void cleanup() {
    serviceRegistration.unregister();
  }

  private Dictionary<String, Object> getServiceProperties(MockOsgiService annotation) {
    return new Hashtable<>(
        Arrays.stream(annotation.properties())
            .collect(
                Collectors.toMap(
                    MockOsgiService.Property::key,
                    MockOsgiService.Property::value,
                    (v1, v2) -> v2,
                    DictionaryMap::new)));
  }

  private Object createMock(
      MockOsgiService annotation, Dictionary<String, Object> serviceProperties) {
    LOGGER.debug(
        "Creating mock for service {} with answer {} and properties {}",
        field.getType().getName(),
        annotation.answer(),
        serviceProperties);
    return mock(field.getType(), annotation.answer().get());
  }

  private void assignToField(Object mockService) throws IllegalAccessException {
    field.setAccessible(true);
    field.set(testInstance, mockService);
  }
}
