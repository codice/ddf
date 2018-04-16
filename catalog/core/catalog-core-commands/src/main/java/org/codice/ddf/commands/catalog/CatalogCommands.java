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
package org.codice.ddf.commands.catalog;

import ddf.catalog.CatalogFramework;
import ddf.catalog.cache.SolrCacheMBean;
import ddf.catalog.filter.FilterBuilder;
import ddf.catalog.source.CatalogProvider;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanServer;
import javax.management.MBeanServerInvocationHandler;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Reference;
import org.codice.ddf.catalog.transform.Transform;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.commands.catalog.facade.Framework;
import org.codice.ddf.commands.catalog.facade.Provider;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.osgi.framework.BundleContext;
import org.osgi.framework.InvalidSyntaxException;

/**
 * Parent object to all Catalog Commands. Provides common methods and instance variables as well as
 * the extension of {@link org.apache.karaf.shell.console.OsgiCommandSupport} and {@link
 * SubjectCommands} that Catalog Commands can use.
 *
 * @author Ashraf Barakat
 * @author ddf.isgs@lmco.com
 */
public abstract class CatalogCommands extends SubjectCommands {

  public static final DateTimeFormatter DATETIME_FORMATTER =
      DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZZ");

  public static final String NAMESPACE = "catalog";

  protected static final int DEFAULT_NUMBER_OF_ITEMS = 15;

  protected static final String ESCAPE = "\\";

  protected static final String SINGLE_WILDCARD = "?";

  protected static final String WILDCARD = "*";

  protected static final String DEFAULT_TRANSFORMER_ID = "xml";

  protected static final String SERIALIZED_OBJECT_ID = "ser";

  // DDF-535: remove "-provider" alias in DDF 3.0
  @Option(
    name = "--provider",
    required = false,
    aliases = {"-p", "-provider"},
    multiValued = false,
    description =
        "Interacts with the Provider directly "
            + "instead of the framework. NOTE: This option picks the first Provider."
  )
  protected boolean isProvider = false;

  @Reference protected CatalogProvider catalogProvider;

  @Reference protected CatalogFramework catalogFramework;

  @Reference protected BundleContext bundleContext;

  @Reference protected FilterBuilder filterBuilder;

  @Reference protected Transform transform;

  protected SolrCacheMBean getCacheProxy()
      throws IOException, MalformedObjectNameException, InstanceNotFoundException {
    ObjectName solrCacheObjectName = new ObjectName(SolrCacheMBean.OBJECT_NAME);
    MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
    return MBeanServerInvocationHandler.newProxyInstance(
        mBeanServer, solrCacheObjectName, SolrCacheMBean.class, false);
  }

  // TODO Optional
  protected CatalogFacade getCatalog() {
    if (isProvider) {
      return new Provider(catalogProvider);
    } else {
      // otherwise use default
      return new Framework(catalogFramework);
    }
  }

  protected <T> Optional<T> getServiceByFilter(Class<T> clazz, String filter)
      throws InvalidSyntaxException {
    return bundleContext
        .getServiceReferences(clazz, filter)
        .stream()
        .map(ref -> bundleContext.getService(ref))
        .filter(Objects::nonNull)
        .findFirst();
  }

  protected String getFormattedDuration(Instant start) {
    return getFormattedDuration(Duration.between(start, Instant.now()));
  }

  protected String getFormattedDuration(Duration duration) {
    return duration.toString().substring(2).replaceAll("(\\d[HMS])(?!$)", "$1 ").toLowerCase();
  }

  protected Transform getTransform() {
    return transform;
  }
}
