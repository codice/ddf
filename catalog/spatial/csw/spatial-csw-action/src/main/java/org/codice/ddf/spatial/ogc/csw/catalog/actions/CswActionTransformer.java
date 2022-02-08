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
package org.codice.ddf.spatial.ogc.csw.catalog.actions;

import java.util.Set;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b>
 *
 * <p>A CswActionTransformer is used to modify CSW transactions before they are run. Specifically,
 * it modifies {@link org.codice.ddf.spatial.ogc.csw.catalog.actions.UpdateAction}, {@link
 * org.codice.ddf.spatial.ogc.csw.catalog.actions.DeleteAction}, and {@link
 * org.codice.ddf.spatial.ogc.csw.catalog.actions.InsertAction} objects before they are processed by
 * the framework.
 */
public interface CswActionTransformer {

  /**
   * Transforms an {@link org.codice.ddf.spatial.ogc.csw.catalog.actions.UpdateAction}.
   *
   * @param updateAction the UpdateAction to transform.
   * @return the transformed UpdateAction.
   */
  UpdateAction transform(UpdateAction updateAction);

  /**
   * Transforms a {@link org.codice.ddf.spatial.ogc.csw.catalog.actions.DeleteAction}.
   *
   * @param deleteAction the DeleteAction to transform.
   * @return the transformed DeleteAction.
   */
  DeleteAction transform(DeleteAction deleteAction);

  /**
   * Transforms an {@link org.codice.ddf.spatial.ogc.csw.catalog.actions.InsertAction}.
   *
   * @param insertAction the InsertAction to transform.
   * @return the transformed InsertAction.
   */
  InsertAction transform(InsertAction insertAction);

  /**
   * Returns a set of typenames this transformer can be applied to.
   *
   * @return a set of typenames this transformer can be applied to.
   */
  Set<String> getTypeNames();
}
