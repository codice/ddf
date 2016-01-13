/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.transformer.generic.xml.impl;

import java.util.Map;

import ddf.catalog.transformer.generic.xml.SaxEventHandler;
import ddf.catalog.transformer.generic.xml.SaxEventHandlerFactory;

public class XMLSaxEventHandlerFactoryImpl implements SaxEventHandlerFactory {

    private static final String VERSION = "1.0";

    private static final String ID = "XML_Handler";

    private static final String TITLE = "XMLSaxEventHandler Factory";

    private static final String DESCRIPTION = "Factory that returns a SaxEventHandler to help parse XML Metacards";

    private static final String ORGANIZATION = "Codice";

    @Override
    public SaxEventHandler getNewSaxEventHandler() {
        return new XMLSaxEventHandlerImpl();
    }

    public SaxEventHandler getNewSaxEventHandler(Map<String, String> xmlToMetacardMap) {
        return new XMLSaxEventHandlerImpl(xmlToMetacardMap);
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
}
