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
package org.codice.ddf.parser;

import java.util.List;
import java.util.Map;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;

/**
 * Configuration helper for setting up necessary environment for underlying parser implementations.
 *
 * <p>This is designed as a fluent interface for ease of use, so implementing classes should respect
 * that and return {@code this} and not a new instance of {@code ParserConfigurator} on all {@code
 * setXXX} methods.
 */
public interface ParserConfigurator {

  /**
   * Sets the list of paths that implementations can search in order to find binding information.
   *
   * @param contextPath a list of paths that implementations can search in order to find binding
   *     information
   * @return a configuration with the specified context path
   */
  ParserConfigurator setContextPath(List<String> contextPath);

  /**
   * Returns the list of paths to be searched for binding information.
   *
   * @return the list of paths to be searched for binding information.
   */
  List<String> getContextPath();

  /**
   * Sets the classloader to be used by underlying implementations.
   *
   * @param loader the classloader for the parser to use
   * @return a configuration with the specified classloader
   */
  ParserConfigurator setClassLoader(ClassLoader loader);

  /**
   * Returns the classloader to be used by the underlying parser.
   *
   * @return the classloader
   */
  ClassLoader getClassLoader();

  /**
   * Sets XML-specific validation handler.
   *
   * <p><em>This is one of two XML-specific settings on the configurator. As XML is a ubiquitous
   * externalizable format, these concessions have been made to provide first-class support to its
   * parsers.</em>
   *
   * @param handler the validation handler to pass to the underlying parser
   * @return a configurator with the specified event handler
   */
  ParserConfigurator setHandler(ValidationEventHandler handler);

  /**
   * Gets XML-specific validation handler.
   *
   * @return the validation handler associated with the underlying parser
   */
  ValidationEventHandler getHandler();

  /**
   * Sets XML-specific adapter.
   *
   * <p><em>This is one of two XML-specific settings on the configurator. As XML is a ubiquitous
   * externalizable format, these concessions have been made to provide first-class support to its
   * parsers.</em>
   *
   * @param adapter the adapter to pass to the underlying parser
   * @return a configurator with the specified adapter
   */
  ParserConfigurator setAdapter(XmlAdapter adapter);

  /**
   * Gets XML-specific adapter.
   *
   * @return the adapter associated with the underlying parser
   */
  XmlAdapter getAdapter();

  /**
   * Adds a property with the given key and value for the underlying parser to use.
   *
   * @param key the unique key for the entry
   * @param val the value of the property
   * @return a configurator with the specified property set
   */
  ParserConfigurator addProperty(String key, Object val);

  /**
   * Adds a collection of properties with the given key-value mappings for the underlying parser.
   *
   * @param properties a collection of key-value objects to set
   * @return a configurator with the specified properties set
   */
  ParserConfigurator addProperties(Map<String, Object> properties);

  /**
   * Returns the properties associated with the underlying parser.
   *
   * @return the associated properties
   */
  Map<String, Object> getProperties();
}
