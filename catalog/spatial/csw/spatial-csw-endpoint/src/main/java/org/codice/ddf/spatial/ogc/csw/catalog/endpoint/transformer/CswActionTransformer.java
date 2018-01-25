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
package org.codice.ddf.spatial.ogc.csw.catalog.endpoint.transformer;

import java.util.List;
import javax.xml.namespace.QName;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.DeleteAction;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.InsertAction;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.UpdateAction;

public interface CswActionTransformer {

  UpdateAction transform(UpdateAction updateAction);

  DeleteAction transform(DeleteAction deleteAction);

  InsertAction transform(InsertAction insertAction);

  List<String> getTypeNames();

  List<QName> getQNames();
}
