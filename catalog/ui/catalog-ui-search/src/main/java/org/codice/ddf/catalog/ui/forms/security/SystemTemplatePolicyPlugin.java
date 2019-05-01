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
package org.codice.ddf.catalog.ui.forms.security;

import static org.codice.ddf.catalog.ui.security.Constants.SYSTEM_TEMPLATE;

import ddf.catalog.data.Metacard;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.plugin.impl.PolicyResponseImpl;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import org.codice.ddf.catalog.ui.security.accesscontrol.AccessControlSecurityConfiguration;

public class SystemTemplatePolicyPlugin extends AbstractPolicyPlugin {
  private final AccessControlSecurityConfiguration config;

  private final Predicate<Metacard> isSystemTemplateMetacard =
      (metacard) ->
          metacard != null
              && metacard.getTags() != null
              && metacard.getTags().contains(SYSTEM_TEMPLATE);

  public SystemTemplatePolicyPlugin(AccessControlSecurityConfiguration config) {
    this.config = config;
  }

  @Override
  public PolicyResponse processPreCreate(Metacard input, Map<String, Serializable> properties)
      throws StopProcessingException {
    if (isSystemTemplateMetacard.test(input)) {
      return new PolicyResponseImpl(
          Collections.singletonMap(
              config.getSystemUserAttribute(),
              Collections.singleton(config.getSystemUserAttributeValue())),
          Collections.emptyMap());
    }
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreDelete(
      List<Metacard> metacards, Map<String, Serializable> properties)
      throws StopProcessingException {
    if (metacards.stream().anyMatch(isSystemTemplateMetacard)) {
      return new PolicyResponseImpl(
          Collections.singletonMap(
              config.getSystemUserAttribute(),
              Collections.singleton(config.getSystemUserAttributeValue())),
          Collections.emptyMap());
    }
    return new PolicyResponseImpl();
  }
}
