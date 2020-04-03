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
package ddf.security.command;

import ddf.security.encryption.EncryptionService;
import org.apache.karaf.shell.api.action.Action;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.fusesource.jansi.Ansi;

@Service
@Command(scope = "security", name = "encrypt", description = "Encrypts a plain text value.")
public class EncryptCommand implements Action {
  @Argument(
      name = "plainTextValue",
      description = "The plain text value to be encrypted.",
      index = 0,
      multiValued = false,
      required = true)
  private String plainTextValue = null;

  @Reference private EncryptionService encryptionService = null;

  /** Called to execute the security:encrypt console command. */
  @Override
  public Object execute() throws Exception {
    if (plainTextValue == null) {
      return null;
    }

    String encryptedValue = encryptionService.encryptValue(plainTextValue);
    System.out.print(Ansi.ansi().fg(Ansi.Color.YELLOW).toString());
    System.out.println(encryptedValue);
    System.out.print(Ansi.ansi().reset().toString());

    return null;
  }

  public void setEncryptionService(EncryptionService encryptionService) {
    this.encryptionService = encryptionService;
  }
}
