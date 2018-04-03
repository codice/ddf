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
package org.codice.spock.extension;

import java.lang.annotation.ElementType
import java.lang.annotation.Inherited
import java.lang.annotation.Retention
import java.lang.annotation.RetentionPolicy
import java.lang.annotation.Target
import org.codice.spock.extension.builtin.ClearInterruptionsExtension
import org.spockframework.runtime.extension.ExtensionAnnotation

/**
 * The <code>ClearInterruptions</code> annotation can be used to clear any interruption states from
 * the current thread after testing.
 *
 * <p>Applying this annotation to a Spock specification class has the same effect as applying it to all
 * its feature methods.
 */
@Inherited
@ExtensionAnnotation(ClearInterruptionsExtension.class)
@Retention(RetentionPolicy.RUNTIME)
@Target([ElementType.TYPE, ElementType.METHOD])
@interface ClearInterruptions {}