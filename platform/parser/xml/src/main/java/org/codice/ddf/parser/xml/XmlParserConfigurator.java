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
package org.codice.ddf.parser.xml;

import com.google.common.base.Predicates;
import com.google.common.collect.Maps;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import org.codice.ddf.parser.ParserConfigurator;

public class XmlParserConfigurator implements ParserConfigurator {
  private List<String> contextPath;

  private ClassLoader loader;

  private ValidationEventHandler handler;

  private XmlAdapter adapter;

  private Map<String, Object> properties = new HashMap<>();

  @Override
  public ParserConfigurator setContextPath(List contextPath) {
    this.contextPath = contextPath;
    return this;
  }

  @Override
  public List<String> getContextPath() {
    return contextPath;
  }

  @Override
  public ParserConfigurator setClassLoader(ClassLoader loader) {
    this.loader = loader;
    return this;
  }

  @Override
  public ClassLoader getClassLoader() {
    return loader;
  }

  @Override
  public ParserConfigurator setHandler(ValidationEventHandler handler) {
    this.handler = handler;
    return this;
  }

  @Override
  public ValidationEventHandler getHandler() {
    return handler;
  }

  @Override
  public ParserConfigurator setAdapter(XmlAdapter adapter) {
    this.adapter = adapter;
    return this;
  }

  @Override
  public XmlAdapter getAdapter() {
    return adapter;
  }

  @Override
  public ParserConfigurator addProperty(String key, Object val) {
    if (key != null) {
      properties.put(key, val);
    }
    return this;
  }

  @Override
  public ParserConfigurator addProperties(Map<String, Object> properties) {
    this.properties.putAll(Maps.filterKeys(properties, Predicates.notNull()));
    return this;
  }

  @Override
  public Map<String, Object> getProperties() {
    return Collections.unmodifiableMap(properties);
  }
}
