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
package org.codice.ddf.catalog.ui.security.accesscontrol;

import org.codice.ddf.catalog.ui.security.Constants;

public class AccessControlSecurityConfiguration {

  private String systemUserAttribute = Constants.ROLES_CLAIM_URI;

  private String systemUserAttributeValue = "system-user";

  private boolean policyToFilterEnabled = true;

  public String getSystemUserAttribute() {
    return systemUserAttribute;
  }

  public void setSystemUserAttribute(String systemUserAttribute) {
    this.systemUserAttribute = systemUserAttribute.trim();
  }

  public boolean isPolicyToFilterEnabled() {
    return policyToFilterEnabled;
  }

  public String getSystemUserAttributeValue() {
    return systemUserAttributeValue;
  }

  public void setSystemUserAttributeValue(String systemUserAttributeValue) {
    this.systemUserAttributeValue = systemUserAttributeValue.trim();
  }

  public void setPolicyToFilterEnabled(boolean policyToFilterEnabled) {
    this.policyToFilterEnabled = policyToFilterEnabled;
  }
}
