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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.mockito.Answers;

/**
 * Annotation used to mock OSGi services using Mockito. To mock an OSGi service, add the {@link
 * org.codice.ddf.test.common.rules.ServiceRegistrationRule} to the Pax Exam test class and annotate
 * any OSGi service field that needs to be mocked.
 *
 * <p>Service properties can be assigned to the service using the {@link #properties()} attribute,
 * and all supported Mockito {@link Answers} modes can be specified using the {@link #answer()}
 * attribute.
 *
 * <p>For instance, the following code would mock the Catalog Framework and Mime Type Resolver
 * services in a Pax Exam test class:
 *
 * <pre>
 * public class TestClass {
 *    {@literal @}Rule
 *     public final ServiceRegistrationRule serviceRegistrationRule = new ServiceRegistrationRule();
 *
 *    {@literal @}MockOsgiService
 *     private CatalogFramework catalogFramework;
 *
 *    {@literal @}MockOsgiService(properties = {
 *        {@literal @}Property(key = "name", value = "tikaMimeTypeResolver")})
 *     private MimeTypeResolver mimeTypeResolver;
 *
 *     // ...
 * }
 * </pre>
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface MockOsgiService {
  /** Answer mode to use for the mock */
  Answers answer() default Answers.RETURNS_DEFAULTS;

  /** Properties to assign to the service when registering the mock with OSGi. */
  Property[] properties() default {};

  /** Annotation used to assign a property to an OSGi service */
  @interface Property {
    /** Service property key */
    String key();

    /** Service property value */
    String value();
  }
}
