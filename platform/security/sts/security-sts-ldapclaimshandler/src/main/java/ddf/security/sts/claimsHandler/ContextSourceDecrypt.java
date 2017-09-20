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
package ddf.security.sts.claimsHandler;

import ddf.security.encryption.EncryptionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class is only meant to decrypt an encrypted password that is set to the ldap/roles claims
 * handler. The decrypt code was taken from OracleCatalogProvider
 *
 * @author kimjs1
 */
public class ContextSourceDecrypt extends org.springframework.ldap.core.support.LdapContextSource {

  private static final Logger LOGGER = LoggerFactory.getLogger(ContextSourceDecrypt.class);

  private EncryptionService encryptionService;

  public void setEncryptionService(EncryptionService encryptionService) {
    this.encryptionService = encryptionService;
  }

  /**
   * Set the password (credentials) to use for getting authenticated contexts.
   *
   * @param password the password.
   */
  @Override
  public void setPassword(String password) {
    if (encryptionService == null) {
      LOGGER.debug(
          "The ContextSourceDecrypt has a null Encryption Service.  Unable to attempt to decrypt the encrypted password: [HIDDEN].");
      this.password = password;
    } else {
      this.password = encryptionService.decryptValue(password);
    }
  }
}
