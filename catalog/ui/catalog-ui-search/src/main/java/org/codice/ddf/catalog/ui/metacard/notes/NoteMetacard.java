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

import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.types.Core;

public class NoteMetacard extends MetacardImpl {

  private static final MetacardType TYPE = new NoteMetacardType();

  /**
   * @param id The parent ID of the note
   * @param user the owner of the note
   * @param comment The comment itself
   */
  public NoteMetacard(final String id, final String user, final String comment) {
    super(TYPE);
    this.setAttribute(NoteConstants.PARENT_ID, id);
    this.setAttribute(Core.METACARD_OWNER, user);
    this.setAttribute(NoteConstants.COMMENT, comment);
    this.setAttribute(Core.METACARD_TAGS, "note");
    this.setAttribute(Core.DESCRIPTION, "Note for a Metacard with ID " + id);
  }
}
