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
package org.codice.ddf.catalog.transform;

/**
 * A ListMultiInputTransformer transformers an InputStream into a list of zero or more Metacards.
 * The interface adds the contractual requirement that the transformer arguments include {@link
 * #LIST_TYPE}. If the transformer cannot transform an item within the InputStream to the type
 * indicated by LIST_TYPE, then it should skip that item.
 */
public interface ListMultiInputTransformer extends MultiInputTransformer {

  String LIST_TYPE = "List-Type";
}
