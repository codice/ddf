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
package org.codice.ddf.catalog.ui.forms.builder;

import java.util.List;
import java.util.function.Function;

/**
 * {@link NodeReducer}s are {@link Function}s that take a {@link List} of filter nodes and reduce
 * them down to a single filter node. This paradigm is recurring throughout the entire filter data
 * structure:
 *
 * <ul>
 *   <li>A single binary logic node consists of a list of child nodes
 *   <li>Most terminal filters consist of a list of two expressions
 *   <li>Function nodes consist of an arbitrary number of arguments
 * </ul>
 *
 * This marker interface makes recognizing these key relationships much easier.
 *
 * @param <T> the type being used to represent a node in the filter tree.
 */
@FunctionalInterface
interface NodeReducer<T> extends Function<List<T>, T> {}
