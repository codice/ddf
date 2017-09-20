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
package org.codice.ddf.platform.util;

/** This utility class defines methods useful to perform on throwable or exception objects. */
public final class Exceptions {

  private Exceptions() {
    // as a utility this should never be constructed, hence it's private
  }

  /**
   * Given a throwable, this traces back through the causes to construct a full message.
   *
   * @param th meant to be a nested throwable, but doesn't have to be
   * @return all messages in reverse order (i.e. root cause message is first!)
   */
  public static String getFullMessage(Throwable th) {
    StringBuilder message = new StringBuilder(th.getMessage());
    th = th.getCause();
    while (th != null) {
      message.insert(0, th.getMessage() + "\n");
      th = th.getCause();
    }
    return message.toString();
  }
}
