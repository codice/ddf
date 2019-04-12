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
package org.codice.ddf.catalog.plugin.metacard.backup.common;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.types.Core;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.apache.camel.CamelContext;
import org.apache.camel.Predicate;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.model.RoutesDefinition;
import org.apache.commons.lang3.BooleanUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

public abstract class MetacardStorageRoute extends RouteBuilder {
  public static final String METACARD_TRANSFORMER_ID_RTE_PROP = "transformerId";

  public static final String METACARD_BACKUP_INVALID_RTE_PROP = "shouldBackupInvalid";

  public static final String METACARD_BACKUP_KEEP_DELETED_RTE_PROP = "keepDeleted";

  public static final String TEMPLATED_STRING_HEADER_RTE_PROP = "templated_string";

  public static final String BACKUP_INVALID_PROPERTY = "backupInvalidMetacards";

  public static final String KEEP_DELETED_PROPERTY = "keepDeletedMetacards";

  public static final String TRANSFORMER_ID_PROPERTY = "metacardTransformerId";

  public static final String METACARD_BACKUP_TAGS_PROPERTY = "backupMetacardTags";

  private static final String INVALID_TAG = "INVALID";

  protected boolean backupInvalidMetacards;

  protected boolean keepDeletedMetacards;

  protected String metacardTransformerId;

  protected List<String> backupMetacardTags;

  private static final Logger LOGGER = LoggerFactory.getLogger(MetacardStorageRoute.class);

  public MetacardStorageRoute(CamelContext camelContext) {
    super(camelContext);
  }

  public void start() {
    try {
      getContext().addRoutes(this);
      getContext().start();
    } catch (Exception e) {
      LOGGER.error("Could not start route: {}", toString(), e);
    }
  }

  public void stop(int code) {
    try {
      List<RouteDefinition> routesToRemove = new ArrayList<>();
      CamelContext context = getContext();
      for (RouteDefinition routeDefinition : context.getRouteDefinitions()) {
        if (getRouteIds().contains(routeDefinition.getId())) {
          context.stopRoute(routeDefinition.getId());
          routesToRemove.add(routeDefinition);
          setRouteCollection(new RoutesDefinition());
        }
      }

      for (RouteDefinition routeDefinition : routesToRemove) {
        context.removeRouteDefinition(routeDefinition);
      }
    } catch (Exception e) {
      LOGGER.error("Could not stop route: {}", toString(), e);
    }
  }

  public abstract List<String> getRouteIds();

  public String getMetacardTransformerId() {
    return metacardTransformerId;
  }

  public boolean isBackupInvalidMetacards() {
    return backupInvalidMetacards;
  }

  public boolean isKeepDeletedMetacards() {
    return keepDeletedMetacards;
  }

  public void setBackupInvalidMetacards(boolean backupInvalidMetacards) {
    this.backupInvalidMetacards = backupInvalidMetacards;
  }

  public void setKeepDeletedMetacards(boolean keepDeletedMetacards) {
    this.keepDeletedMetacards = keepDeletedMetacards;
  }

  public void setMetacardTransformerId(String metacardTransformerId) {
    this.metacardTransformerId = metacardTransformerId;
  }

  public List<String> getBackupMetacardTags() {
    return backupMetacardTags;
  }

  public void setBackupMetacardTags(List<String> backupMetacardTags) {
    this.backupMetacardTags = backupMetacardTags;
  }

  public void refresh(Map<String, Object> properties) throws Exception {
    Object backupInvalidProp = properties.get(BACKUP_INVALID_PROPERTY);
    if (backupInvalidProp instanceof Boolean) {
      this.backupInvalidMetacards = (boolean) backupInvalidProp;
    }

    Object keepDeletedProp = properties.get(KEEP_DELETED_PROPERTY);
    if (keepDeletedProp instanceof Boolean) {
      this.keepDeletedMetacards = (Boolean) keepDeletedProp;
    }

    Object metacardTransformerIdProp = properties.get(TRANSFORMER_ID_PROPERTY);
    if (metacardTransformerIdProp instanceof String) {
      this.metacardTransformerId = (String) metacardTransformerIdProp;
    }

    Object metacardTagsProp = properties.get(METACARD_BACKUP_TAGS_PROPERTY);
    if (metacardTagsProp instanceof List) {
      this.backupMetacardTags = (List<String>) metacardTagsProp;
    }

    stop(0);
    configure();
    start();
  }

  public boolean shouldBackupMetacard(Metacard metacard, boolean backupInvalid) {
    if (!isTagAllowed(metacard)) {
      return false;
    }
    if (backupInvalid) {
      return true;
    } else {
      Attribute metacardTagsAttr = metacard.getAttribute(Core.METACARD_TAGS);
      if (metacardTagsAttr != null) {
        return metacardTagsAttr
            .getValues()
            .stream()
            .filter(String.class::isInstance)
            .map(String.class::cast)
            .noneMatch(INVALID_TAG::equalsIgnoreCase);
      }
      return true;
    }
  }

  protected Predicate getShouldBackupPredicate() {
    return exchange -> {
      Object bodyObj = exchange.getIn().getBody();
      if (bodyObj instanceof Metacard) {
        Metacard metacard = (Metacard) bodyObj;
        return shouldBackupMetacard(
            metacard, exchange.getIn().getHeader(METACARD_BACKUP_INVALID_RTE_PROP, Boolean.class));
      }
      return false;
    };
  }

  protected Predicate getCheckDeletePredicate() {
    return exchange -> {
      Boolean keepDeleted =
          exchange.getIn().getHeader(METACARD_BACKUP_KEEP_DELETED_RTE_PROP, Boolean.class);
      if (BooleanUtils.isTrue(keepDeleted)) {
        return false;
      }
      return true;
    };
  }

  private boolean isTagAllowed(Metacard metacard) {
    if (CollectionUtils.isEmpty(backupMetacardTags)) {
      return true;
    }

    Attribute metacardTagsAttr = metacard.getAttribute(Core.METACARD_TAGS);
    if (metacardTagsAttr != null) {
      List<String> metacardTagValues =
          metacardTagsAttr
              .getValues()
              .stream()
              .filter(String.class::isInstance)
              .map(String.class::cast)
              .collect(Collectors.toList());

      return CollectionUtils.containsAny(metacardTagValues, backupMetacardTags);
    } else {
      return false;
    }
  }
}
