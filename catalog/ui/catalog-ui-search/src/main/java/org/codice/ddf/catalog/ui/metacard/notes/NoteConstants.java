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
package org.codice.ddf.catalog.ui.metacard.notes;

import ddf.catalog.data.types.Associations;

public final class NoteConstants {

  /**
   * The metacard ID of the parent object. Uses original taxonomy to preserve backwards
   * compatibility.
   */
  public static final String PARENT_ID = Associations.RELATED;

  /** A comment against a result. */
  public static final String COMMENT = "ext.note.result-comment";

  private NoteConstants() {}
}
