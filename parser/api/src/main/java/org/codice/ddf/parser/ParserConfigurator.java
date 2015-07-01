/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.parser;

import java.util.List;
import java.util.Map;

import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;

public interface ParserConfigurator {

    ParserConfigurator setContextPath(List<String> contextPath);

    List<String> getContextPath();

    ParserConfigurator setClassLoader(ClassLoader loader);

    ClassLoader getClassLoader();

    ParserConfigurator setHandler(ValidationEventHandler handler);

    ValidationEventHandler getHandler();

    ParserConfigurator setAdapter(XmlAdapter adapter);

    XmlAdapter getAdapter();

    ParserConfigurator addProperty(String key, Object val);

    ParserConfigurator addProperties(Map<String, Object> properties);

    Map<String, Object> getProperties();
}