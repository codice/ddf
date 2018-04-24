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
package org.codice.ddf.catalog.ui.security;

import com.google.common.collect.ImmutableSet;
import java.util.Set;
import org.codice.ddf.catalog.ui.forms.data.AttributeGroupType;
import org.codice.ddf.catalog.ui.forms.data.QueryTemplateType;
import org.codice.ddf.catalog.ui.metacard.workspace.WorkspaceAttributes;

public class Constants {

  public static final Set<String> SHAREABLE_TAGS =
      ImmutableSet.of(
          AttributeGroupType.ATTRIBUTE_GROUP_TAG,
          WorkspaceAttributes.WORKSPACE_TAG,
          QueryTemplateType.QUERY_TEMPLATE_TAG);

  public static final String IS_SHAREABLE = "security.tag.is-shareable";

  public static final String ROLES_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

  public static final String EMAIL_ADDRESS_CLAIM_URI =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress";
}
