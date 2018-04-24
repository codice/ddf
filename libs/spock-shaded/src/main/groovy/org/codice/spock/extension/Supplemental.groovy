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
package org.codice.spock.extension

import org.codice.spock.extension.builtin.SupplementalExtension
import org.spockframework.runtime.extension.ExtensionAnnotation

import java.lang.annotation.*

@Inherited
@ExtensionAnnotation(SupplementalExtension)
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
/**
 * The <code>Supplemental</code> annotation can be added to any Spock specification class in order
 * to get additional methods added to specific classes while testing the specification.
 *
 * <p>The following methods are added:
 * <ul>
 *   <li><code>Method[] Class.getApiMethods()</code>
 *       <p>Gets all API methods (inherited or not) for the given class filtering away all non public
 *          methods and all methods defined by the {@link Object} class (e.g.
 * {@link Object#equals}, {@link Object#toString}, {@link Object#clone} ...).</li>
 *
 *   <li><code>Method[] Class.getProxyableMethods()</code>
 *       <p>Gets all public proxy-able methods (inherited or not) for the class filtering away all
 *          final methods and all methods defined by the {@link Object} class (e.g.
 * {@link Object#equals}, {@link Object#toString}, {@link Object#clone} ...).</li>
 *
 *   <li><code>Method Class.getMethodBySimplePrototype(String prototype) throws NoSuchMethodException</code>
 *       <p>Returns a {@code Method} object that reflects the specified public member method of the
 *          underlying class or interface that matches the given simple prototype.
 *
 *   <li><code>String Class.getNoSpockSimpleName()</code>
 *       <p>Returns the simple name of the underlying class as given in the source code stripping
 *       away any references to Spock mocks, stubs, or spies.</li>
 *
 *   <li><code>String Method.getSimplePrototype()</code>
 *       <p>Gets a simple prototype string to represent the method.</li>
 *
 *   <li><code>void Method.verifyInvocation(MockInvocation delegate, Object... parameters)</code>
 *       <p>Asserts if the mock invocation matches the specified method. Only the method name and
 *          parameter types/values are verified.
 *       <p>This method is meant to be invoked from within a closure associated with a stubbed
 *          interaction by passing it a reference to the closure's delegate and the expected
 *          parameters used when calling the method. Parameters are verified using identity check
 *          and not equality. No verification of parameters will occur if no expected parameters are
 *          specified.</li>
 *
 *   <li><code>System.setPropertyIfNotNull(String name, String value)</code>
 *       <p>Sets the system property indicated by the specified key only if the specified value is
 *          not <code>null</code> otherwise remove any mappings to the specified key.
 *
 *   <li><code>&lt;T&gt; T MockingApi.Dummy(Class<T> type)</code>
 *       <p>Creates a dummy value or stub for the specified type.</li>
 *
 *   <li><code>Object[] MockingApi.Dummies(Class<?>... types)</code>
 *       <p>Creates dummy values or stubs for the specified types.</li>
 * </ul>
 */
public @interface Supplemental {}