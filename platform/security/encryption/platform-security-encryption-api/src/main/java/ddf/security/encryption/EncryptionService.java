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
   * Decrypts a wrapped encrypted value in the "ENC(*)" format. Inputs that are not wrapped are
   * returned as a no-op.
   *
   * @param wrappedEncryptedValue a string of the form "ENC(", followed by an encrypted value, and
   *     terminated with ")".
   * @return a decryption of the given value after removing the leading "ENC(" and trailing ")".
   */
  String decryptValue(String wrappedEncryptedValue);

  /**
   * Unwraps an encrypted value in the "ENC(*)" format. Inputs that are not wrapped are returned as
   * a no-op.
   *
   * @param wrappedEncryptedValue a string of the form "ENC(", followed by an encrypted value, and
   *     terminated with ")".
   * @return the encrypted value <b>without</b> the leading "ENC(" and trailing ")".
   */
  String unwrapEncryptedValue(String wrappedEncryptedValue);
}
