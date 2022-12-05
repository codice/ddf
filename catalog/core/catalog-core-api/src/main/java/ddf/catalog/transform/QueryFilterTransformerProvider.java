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
package ddf.catalog.transform;

import java.util.Optional;
import javax.xml.namespace.QName;

/**
 * Provider interface to obtain the appropriate {@link QueryFilterTransformer} given qName or
 * typeName
 */
@Deprecated
public interface QueryFilterTransformerProvider {

  /**
   * Look up the {@link QueryFilterTransformer} that can provide Query Filter transformation for the
   * given qName
   *
   * @param qName
   * @return {@link QueryFilterTransformer}
   */
  Optional<QueryFilterTransformer> getTransformer(QName qName);

  /**
   * Look up the {@link QueryFilterTransformer} that can provide Query Filter transformation for the
   * given typeName
   *
   * @param typeName
   * @return {@link QueryFilterTransformer}
   */
  Optional<QueryFilterTransformer> getTransformer(String typeName);
}
