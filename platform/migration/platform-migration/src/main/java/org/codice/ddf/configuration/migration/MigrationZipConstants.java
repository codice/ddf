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
package org.codice.ddf.configuration.migration;

import com.google.common.annotations.VisibleForTesting;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class MigrationZipConstants {

  @VisibleForTesting static final String CHECKSUM_EXTENSION = ".sha256";
  @VisibleForTesting static final String KEY_EXTENSION = ".key";

  @VisibleForTesting
  static final String KEY_DECODE_ERROR = "could not decode key from file [%s] [%s].";

  @VisibleForTesting
  static final String KEY_INVALID_ERROR = "invalid key file used for import [%s] [%s].";

  @VisibleForTesting static final String KEY_ALGORITHM = "AES";

  @VisibleForTesting static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";

  @VisibleForTesting
  static final byte[] CIPHER_IV = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

  @VisibleForTesting
  static final String CHECKSUM_INVALID_ALGORITHM_ERROR =
      "Invalid algorithm used for checksum digest [%s] [%s].";

  @VisibleForTesting static final String CHECKSUM_DIGEST_ALGORITHM = "SHA-256";

  @VisibleForTesting static final String FILE_NOT_EXIST = "File [%s] does not exist. [%s].";

  @VisibleForTesting static final String FILE_IO_ERROR = "Could not read file [%s] [%s].";

  private MigrationZipConstants() {}

  /**
   * Computes the default key location based on the location of the zip file
   *
   * @param zipPath path to the zip file
   * @return the default location of the key
   */
  public static Path getDefaultKeyPathFor(Path zipPath) {
    return Paths.get(zipPath.toString() + MigrationZipConstants.KEY_EXTENSION);
  }

  /**
   * Computes the default checksum file location based on the location of the zip file
   *
   * @param zipPath path to the zip file
   * @return the default location of the checksum file
   */
  public static Path getDefaultChecksumPathFor(Path zipPath) {
    return Paths.get(zipPath.toString() + MigrationZipConstants.CHECKSUM_EXTENSION);
  }
}
