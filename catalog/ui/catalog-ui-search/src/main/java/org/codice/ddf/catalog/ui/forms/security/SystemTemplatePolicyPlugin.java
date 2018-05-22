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
import ddf.catalog.data.Result;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.ResourceRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.plugin.PolicyPlugin;
import ddf.catalog.plugin.PolicyResponse;
import ddf.catalog.plugin.StopProcessingException;
import ddf.catalog.plugin.impl.PolicyResponseImpl;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class SystemTemplatePolicyPlugin implements PolicyPlugin {
  private static final String TEMPLATES_ROLE = "system-templates";

  private static final String NOT_IMPLIABLE = "not-impliable";

  private static final String ROLE_CLAIM =
      "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

  private final Predicate<Metacard> isSystemTemplateMetacard =
      (metacard) ->
          metacard != null
              && metacard.getTags() != null
              && metacard.getTags().contains(SYSTEM_TEMPLATE);

  @Override
  public PolicyResponse processPreCreate(Metacard input, Map<String, Serializable> properties)
      throws StopProcessingException {
    if (isSystemTemplateMetacard.test(input)) {
      return new PolicyResponseImpl(
          Collections.singletonMap(ROLE_CLAIM, Collections.singleton(TEMPLATES_ROLE)),
          Collections.emptyMap());
    }
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreUpdate(Metacard newMetacard, Map<String, Serializable> properties)
      throws StopProcessingException {
    if (isSystemTemplateMetacard.test(newMetacard)) {
      return new PolicyResponseImpl(
          Collections.singletonMap(NOT_IMPLIABLE, Collections.singleton(NOT_IMPLIABLE)),
          Collections.singletonMap(NOT_IMPLIABLE, Collections.singleton(NOT_IMPLIABLE)));
    }
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreDelete(
      List<Metacard> metacards, Map<String, Serializable> properties)
      throws StopProcessingException {
    if (metacards.stream().anyMatch(isSystemTemplateMetacard)) {
      return new PolicyResponseImpl(
          Collections.singletonMap(ROLE_CLAIM, Collections.singleton(TEMPLATES_ROLE)),
          Collections.emptyMap());
    }
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPostDelete(Metacard input, Map<String, Serializable> properties)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreQuery(Query query, Map<String, Serializable> properties)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPostQuery(Result input, Map<String, Serializable> properties)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPreResource(ResourceRequest resourceRequest)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }

  @Override
  public PolicyResponse processPostResource(ResourceResponse resourceResponse, Metacard metacard)
      throws StopProcessingException {
    return new PolicyResponseImpl();
  }
}
