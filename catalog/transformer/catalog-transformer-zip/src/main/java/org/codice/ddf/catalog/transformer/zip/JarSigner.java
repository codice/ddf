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
package org.codice.ddf.catalog.transformer.zip;

import java.io.File;
import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Project;
import org.apache.tools.ant.Target;
import org.apache.tools.ant.taskdefs.SignJar;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class extends Ant's SignJar class. SignJar executes the jarsigner binary that exists in JDK
 * in code. https://ant.apache.org/manual/Tasks/signjar.html
 */
public class JarSigner extends SignJar {

  private static final Logger LOGGER = LoggerFactory.getLogger(JarSigner.class);

  private static final String SIGN_JAR = "signJar";

  public JarSigner() {
    setProject(new Project());
    getProject().init();
    setTaskType(SIGN_JAR);
    setTaskName(SIGN_JAR);
    setOwningTarget(new Target());
  }

  /**
   * Signs the given jar with the given parameters. Gives up if any exception occurs during the
   * execution.
   *
   * @param jarToSign - a reference to the zip / jar to sign
   * @param alias - the alias to sign the zip / jar under
   * @param keypass - the password for the private key
   * @param keystore - the path to the keystore
   * @param storepass - the password for the keystore
   */
  public void signJar(
      File jarToSign, String alias, String keypass, String keystore, String storepass) {
    setJar(jarToSign);
    setAlias(alias);
    setKeypass(keypass);
    setKeystore(keystore);
    setStorepass(storepass);
    setSignedjar(jarToSign);
    try {
      execute();
    } catch (BuildException e) {
      LOGGER.warn("Unable to sign {}", jarToSign.getName(), e);
    }
  }
}
