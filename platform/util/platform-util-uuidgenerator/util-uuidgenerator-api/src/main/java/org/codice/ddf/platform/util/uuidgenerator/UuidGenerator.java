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
package org.codice.ddf.platform.util.uuidgenerator;

/**
 * <b> This code is experimental. While this interface is functional and tested, it may change or be
 * removed in a future version of the library. </b> The {@code UuidGenerator} generates {@link
 * java.util.UUID}s
 */
public interface UuidGenerator {

  /**
   * Returns a generated uuid for a Metacard
   *
   * @return
   */
  String generateUuid();

  /**
   * @param metacardTag tag of metacard to generate ID for
   * @param userId user the metacard is being created for
   * @param additionalInput any additional properties
   * @return UUID
   */
  String generateKnownId(String metacardTag, String userId, String... additionalInput);

  /**
   * Returns true if the UUID format is correct
   *
   * @param uuid - the given UUID
   * @return
   */
  boolean validateUuid(String uuid);

  /** Returns true if the generated UUIDs contain hyphens or not */
  boolean useHyphens();
}
