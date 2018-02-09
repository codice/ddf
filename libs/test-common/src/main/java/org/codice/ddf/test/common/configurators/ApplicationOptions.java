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
package org.codice.ddf.test.common.configurators;

import java.util.function.Supplier;
import org.ops4j.pax.exam.Option;

/**
 * Interface implemented by Pax Exam test classes to provide application specific {@link Option}s.
 *
 * @see org.codice.ddf.test.common.AbstractComponentTest#getApplicationOptions(PortFinder)
 */
public interface ApplicationOptions extends Supplier<Option> {

  /**
   * Gets the Pax Exam configuration {@link Option}s for this application. Implementers should use
   * Pax Exam's {@link org.ops4j.pax.exam.CoreOptions#composite(Option...)} to combine multiple
   * options into one if needed.
   *
   * @return Pax Exam configuration {@link Option}s
   */
  Option get();
}
