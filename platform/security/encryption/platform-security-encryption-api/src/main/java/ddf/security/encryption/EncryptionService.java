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
package ddf.security.encryption;

import org.apache.wss4j.common.crypto.PasswordEncryptor;

public interface EncryptionService extends PasswordEncryptor {

  /**
   * Decrypts the unwrapped encrypted value
   *
   * @param wrappedEncryptedValue
   * @return
   */
  String decryptValue(String wrappedEncryptedValue);

  /**
   * Unwraps an encrypted value with the "ENC(*)" format
   *
   * @param wrappedEncryptedValue
   */
  String unwrapEncryptedValue(String wrappedEncryptedValue);
}
