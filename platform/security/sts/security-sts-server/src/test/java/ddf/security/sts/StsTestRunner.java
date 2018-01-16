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
package ddf.security.sts;

import ddf.security.sts.StsIssueTestMethods.StsPortTypes;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class StsTestRunner {

  private static final Logger LOGGER = LoggerFactory.getLogger(StsTestRunner.class);

  private StsTestRunner() {}

  /**
   * @param args
   * @throws Exception
   */
  public static void main(String[] args) {
    StsIssueTestMethods sit = new StsIssueTestMethods();

    // Test the Transport Port
    try {
      sit.testBearerUsernameTokenSaml2(StsPortTypes.TRANSPORT);
      sit.testBearerWebSsoTokenSaml2(StsPortTypes.TRANSPORT);
    } catch (Exception e) {
      LOGGER.warn("An Error Occurred Calling the STS", e);
    }
  }
}
