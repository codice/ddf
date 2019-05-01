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

import com.google.common.annotations.VisibleForTesting;
import ddf.catalog.data.MetacardType;
import java.util.HashSet;
import java.util.Set;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccessControlTags {
  private static final Logger LOGGER = LoggerFactory.getLogger(AccessControlTags.class);

  private static final String ACCESS_CONTROLLED_TAG = "access-controlled-tag";

  private final Set<String> accessControlledTags;

  public AccessControlTags() {
    this.accessControlledTags = new HashSet<>();
  }

  AccessControlTags(final Set<String> accessControlledTags) {
    this.accessControlledTags = accessControlledTags;
  }

  public Set<String> getAccessControlledTags() {
    return accessControlledTags;
  }

  // Invoked by listener in blueprint
  public void bindTag(ServiceReference metacardTypeRef) {
    if (metacardTypeRef == null) {
      LOGGER.trace("New reference to metacard type was null");
      return;
    }
    Object tag = metacardTypeRef.getProperty(ACCESS_CONTROLLED_TAG);
    if (tag == null) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(
            "Registered metacard type {} is not access-controlled", typeName(metacardTypeRef));
      }
      return;
    }
    if (!(tag instanceof String)) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "The service property 'access-controlled-tag' is not valid for metacard type {}, "
                + "verify the blueprint XML document is correct",
            typeName(metacardTypeRef));
      }
      return;
    }
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(
          "Found access-controlled metacard type '{}', caching tag '{}'",
          typeName(metacardTypeRef),
          tag);
    }
    accessControlledTags.add((String) tag);
  }

  // Invoked by listener in blueprint
  public void unbindTag(ServiceReference metacardTypeRef) {
    if (metacardTypeRef == null) {
      LOGGER.trace("Reference to old metacard type was null");
      return;
    }
    Object tag = metacardTypeRef.getProperty(ACCESS_CONTROLLED_TAG);
    if (tag == null) {
      if (LOGGER.isTraceEnabled()) {
        LOGGER.trace(
            "Deregistered metacard type {} is not access-controlled", typeName(metacardTypeRef));
      }
      return;
    }
    if (!(tag instanceof String)) {
      if (LOGGER.isWarnEnabled()) {
        LOGGER.warn(
            "The service property 'access-controlled-tag' is not valid for metacard type {}, "
                + "verify the blueprint XML document is correct",
            typeName(metacardTypeRef));
      }
      return;
    }
    if (LOGGER.isTraceEnabled()) {
      LOGGER.trace(
          "Found access-controlled metacard type '{}', removing cached tag '{}'",
          typeName(metacardTypeRef),
          tag);
    }
    accessControlledTags.remove(tag);
  }

  @SuppressWarnings(
      "unchecked" /* We only ever inject references to metacard types in this class */)
  @VisibleForTesting
  String typeName(ServiceReference metacardTypeRef) {
    BundleContext context =
        FrameworkUtil.getBundle(AccessControlPreQueryPlugin.class).getBundleContext();
    MetacardType type = (MetacardType) context.getService(metacardTypeRef);
    return type.getName();
  }
}
