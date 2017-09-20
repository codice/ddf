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
package ddf.catalog.data;

/**
 * Manages {@link InjectableAttribute} services and injects the appropriate {@link
 * AttributeDescriptor}s into {@link MetacardType}s and into {@link Metacard}s.
 *
 * <p><b>This code is experimental. While this interface is functional and tested, it may change or
 * be removed in a future version of the library.</b>
 */
public interface AttributeInjector {
  /**
   * 'Injects' {@link AttributeDescriptor}s (represented by 'global' {@link InjectableAttribute}s
   * and {@link InjectableAttribute}s specific to {@code original}) into the given {@link
   * MetacardType} by returning a new {@link MetacardType} with the same name as {@code original}
   * and containing the additional injected {@link AttributeDescriptor}s.
   *
   * <p>If there are no attributes to inject for the given {@link MetacardType}, {@code original} is
   * returned.
   *
   * @param original the original {@link MetacardType}, cannot be null
   * @return a new {@link MetacardType} with the same name as {@code original} and containing the
   *     additional injected {@link AttributeDescriptor}s, or {@code original} if there are no
   *     attributes to inject
   * @throws IllegalArgumentException if {@code original} is null
   */
  MetacardType injectAttributes(MetacardType original);

  /**
   * 'Injects' {@link AttributeDescriptor}s (represented by 'global' {@link InjectableAttribute}s
   * and {@link InjectableAttribute}s specific to {@code original}'s metacard type) into the given
   * {@link Metacard} by returning a new {@link Metacard} with the same data as {@code original} but
   * with a new {@link MetacardType} containing the additional injected {@link
   * AttributeDescriptor}s.
   *
   * <p>If there are no attributes to inject for the given {@link Metacard}'s metacard type, {@code
   * original} is returned.
   *
   * @param original the original {@link Metacard}, cannot be null
   * @return a new {@link Metacard} containing the same data as {@code original} but with a new
   *     {@link MetacardType} containing the additional injected {@link AttributeDescriptor}s, or
   *     {@code original} if there are no attributes to inject
   * @throws IllegalArgumentException if {@code original} is null
   */
  Metacard injectAttributes(Metacard original);
}
