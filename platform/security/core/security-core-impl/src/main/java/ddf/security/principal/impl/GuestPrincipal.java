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
package ddf.security.principal.impl;

import java.io.Serializable;
import java.security.Principal;
import javax.annotation.Nullable;

/** Principal that designates a {@link ddf.security.Subject} as guest */
public class GuestPrincipal implements Principal, Serializable {

  public static final String GUEST_NAME_PREFIX = "Guest";

  public static final String NAME_DELIMITER = "@";

  private static final long serialVersionUID = -4630425142287155229L;

  private String name;

  public GuestPrincipal(@Nullable String name) {
    if (name != null && name.startsWith(GUEST_NAME_PREFIX + NAME_DELIMITER)) {
      this.name = name;
    } else {
      this.name = GUEST_NAME_PREFIX + NAME_DELIMITER + ((name != null) ? name : "");
    }
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String toString() {
    return getName();
  }
}
