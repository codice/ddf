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
package org.codice.ddf.transformer.xml.streaming.impl;

import org.codice.ddf.transformer.xml.streaming.Gml3ToWkt;
import org.codice.ddf.transformer.xml.streaming.SaxEventHandler;
import org.codice.ddf.transformer.xml.streaming.SaxEventHandlerFactory;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.io.gml2.GMLHandler;
import org.xml.sax.ErrorHandler;

public class GmlHandlerFactory implements SaxEventHandlerFactory {

  private static final String VERSION = "1.0";

  private static final String ID = "gml-handler";

  private static final String TITLE = "GML Sax Event Handler Factory";

  private static final String DESCRIPTION =
      "Factory that returns a SaxEventHandler to help parse GML portions of Metacards";

  private static final String ORGANIZATION = "Codice";

  private Gml3ToWkt gml3ToWkt;

  @Override
  public SaxEventHandler getNewSaxEventHandler() {
    return new GmlHandler(
        new GMLHandler(new GeometryFactory(), (ErrorHandler) null), this.gml3ToWkt);
  }

  @Override
  public String getVersion() {
    return VERSION;
  }

  @Override
  public String getId() {
    return ID;
  }

  @Override
  public String getTitle() {
    return TITLE;
  }

  @Override
  public String getDescription() {
    return DESCRIPTION;
  }

  @Override
  public String getOrganization() {
    return ORGANIZATION;
  }

  public void setGml3ToWkt(Gml3ToWkt gml3ToWkt) {
    this.gml3ToWkt = gml3ToWkt;
  }
}
