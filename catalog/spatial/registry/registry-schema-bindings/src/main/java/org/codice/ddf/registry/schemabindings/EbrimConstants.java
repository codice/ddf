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
package org.codice.ddf.registry.schemabindings;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.ObjectFactory;

/** This class consists of constants used by the ebrim schema bindings. */
public final class EbrimConstants {

  public static final net.opengis.gml.v_3_1_1.ObjectFactory GML_FACTORY =
      new net.opengis.gml.v_3_1_1.ObjectFactory();

  public static final net.opengis.ogc.ObjectFactory OGC_FACTORY =
      new net.opengis.ogc.ObjectFactory();

  public static final ObjectFactory RIM_FACTORY = new ObjectFactory();

  public static final net.opengis.cat.wrs.v_1_0_2.ObjectFactory WRS_FACTORY =
      new net.opengis.cat.wrs.v_1_0_2.ObjectFactory();

  private EbrimConstants() {}
}
