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

import java.io.PrintStream;
import org.apache.karaf.shell.api.action.Argument;
import org.apache.karaf.shell.api.action.Command;
import org.apache.karaf.shell.api.action.Option;
import org.apache.karaf.shell.api.action.lifecycle.Service;
import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.commands.util.SpatialOperations;

// TODO DDF-1282 Implement Spatial queries in Catalog Commands
@Service
@Command(
    scope = CatalogCommands.NAMESPACE,
    name = "spatial",
    description = "Searches spatially the Catalog Provider.")
public class SpatialCommand extends CatalogCommands {

  private static final String ID = "ID ";

  private static final String TITLE = "Title ";

  private static final String DATE = "Date ";

  private static final int MAX_LENGTH = 30;

  private static final String FORMAT = "%1$-33s %2$-26s %3$-" + MAX_LENGTH + "s %4$-50s%n";

  private static final Object WKT = "WKT";

  @Argument(
      name = "Operation",
      description =
          "An operation from the set {CONTAINS,INTERSECTS,EQUALS,DISJOINT,TOUCHES,CROSSES,WITHIN,OVERLAPS,RADIUS,NN}",
      index = 0,
      multiValued = false,
      required = true)
  String operation = null;

  @Argument(
      name = "PointX",
      description = "X coordinate of point of reference",
      index = 1,
      multiValued = false,
      required = true)
  String pointX = null;

  @Argument(
      name = "PointY",
      description = "Y coordinate of point of reference",
      index = 2,
      multiValued = false,
      required = true)
  String pointY = null;

  @Argument(
      name = "Radius",
      description = "Radius for a Point-Radius search {RADIUS}",
      index = 3,
      multiValued = false,
      required = false)
  String radius = "10000";

  @Option(
      name = "items-returned",
      required = false,
      aliases = {"-n"},
      multiValued = false,
      description = "Number of the items returned.")
  int numberOfItems = DEFAULT_NUMBER_OF_ITEMS;

  @Override
  protected Object executeWithSubject() throws Exception {

    CatalogFacade catalogProvider = getCatalog();

    switch (SpatialOperations.valueOf(operation.toUpperCase())) {
      case RADIUS:
        doRadiusQuery(console, catalogProvider);
        break;

      case NN:
        doNNQuery(console, catalogProvider);
        break;

      default:
        doOperationsQuery(console, catalogProvider);
    }

    return null;
  }

  protected void doRadiusQuery(PrintStream console, CatalogFacade catalogFacade) {}

  protected void doNNQuery(PrintStream console, CatalogFacade catalogFacade) {}

  private void doOperationsQuery(PrintStream console, CatalogFacade catalogFacade) {}
}
