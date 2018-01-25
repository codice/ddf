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
package org.codice.ddf.spatial.ogc.csw.catalog.common.transaction;

import java.util.ArrayList;
import java.util.List;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.DeleteAction;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.InsertAction;
import org.codice.ddf.spatial.ogc.csw.catalog.actions.UpdateAction;

/**
 * Represents a single CSW transaction request that can contain multiple insert, update, and delete
 * actions.
 */
public class CswTransactionRequest {

  private String version;

  private String service;

  private boolean verbose;

  private final List<InsertAction> insertActions = new ArrayList<>();

  private final List<DeleteAction> deleteActions = new ArrayList<>();

  private final List<UpdateAction> updateActions = new ArrayList<>();

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public String getService() {
    return service;
  }

  public void setService(String service) {
    this.service = service;
  }

  public boolean isVerbose() {
    return verbose;
  }

  public void setVerbose(boolean verbose) {
    this.verbose = verbose;
  }

  public List<InsertAction> getInsertActions() {
    return insertActions;
  }

  public List<DeleteAction> getDeleteActions() {
    return deleteActions;
  }

  public List<UpdateAction> getUpdateActions() {
    return updateActions;
  }
}
