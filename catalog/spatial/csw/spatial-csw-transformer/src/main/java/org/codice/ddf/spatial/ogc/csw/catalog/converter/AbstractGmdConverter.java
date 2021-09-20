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
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.naming.NoNameCoder;
import com.thoughtworks.xstream.io.path.Path;
import com.thoughtworks.xstream.io.path.PathTracker;
import com.thoughtworks.xstream.io.path.PathTrackingWriter;
import com.thoughtworks.xstream.io.xml.Xpp3Driver;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;
import javax.xml.datatype.DatatypeConfigurationException;
import javax.xml.datatype.DatatypeFactory;
import org.apache.commons.lang.NotImplementedException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractGmdConverter implements Converter {

  protected static final DatatypeFactory XSD_FACTORY;

  protected static final TimeZone UTC_TIME_ZONE = TimeZone.getTimeZone("UTC");

  private static final String XPATH_SEPARATOR = "/";

  private static final Logger LOGGER = LoggerFactory.getLogger(AbstractGmdConverter.class);

  static {
    DatatypeFactory factory = null;

    try {
      factory = DatatypeFactory.newInstance();
    } catch (DatatypeConfigurationException e) {
      LOGGER.debug("Failed to create xsdFactory", e);
    }

    XSD_FACTORY = factory;
  }

  protected AbstractGmdConverter() {

    XStream xstream = new XStream(new Xpp3Driver(new NoNameCoder()));

    xstream.setClassLoader(xstream.getClass().getClassLoader());

    xstream.allowTypesByWildcard(new String[] {"ddf.**", "org.codice.**"});

    xstream.registerConverter(this);
    getXstreamAliases().forEach(name -> xstream.alias(name, Metacard.class));
  }

  /**
   * Get a list of aliases to be passed to {@link XStream#alias(String, Class)}.
   *
   * @return list of aliases
   */
  protected abstract List<String> getXstreamAliases();

  @Override
  public final boolean canConvert(Class clazz) {
    return Metacard.class.isAssignableFrom(clazz);
  }

  @Override
  public final Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    throw new NotImplementedException();
  }

  @Override
  public final void marshal(
      Object source, HierarchicalStreamWriter inWriter, MarshallingContext context) {
    if (!(source instanceof Metacard)) {
      LOGGER.debug("Failed to marshal Metacard: {}", source);
      return;
    }
    MetacardImpl metacard = new MetacardImpl((Metacard) source);

    PathTracker tracker = new PathTracker();
    PathTrackingWriter trackingWriter = new PathTrackingWriter(inWriter, tracker);

    XstreamPathValueTracker pathValueTracker = buildPaths(metacard);
    XmlTree tree = buildTree(pathValueTracker.getPaths());

    tree.accept(new XstreamTreeWriter(trackingWriter, tracker, pathValueTracker));
  }

  /**
   * Builds up the xml paths and values to write. Order matters! Paths should be added in the order
   * they must be written.
   *
   * @param metacard must be non-null
   * @return XstreamPathValueTracker containing XML paths and values to write
   */
  protected abstract XstreamPathValueTracker buildPaths(MetacardImpl metacard);

  /**
   * Get the name of the root name.
   *
   * @return root node
   */
  protected abstract String getRootNodeName();

  protected final XmlTree buildTree(Set<Path> paths) {

    XmlTree gmdTree = new XmlTree(getRootNodeName());

    for (Path path : paths) {
      String tree = path.toString();
      XmlTree current = gmdTree;

      tree = StringUtils.substringAfter(tree, getRootNodeName());
      for (String data : tree.split(XPATH_SEPARATOR)) {
        if (StringUtils.isNotEmpty(data)) {
          current = current.addChild(data);
        }
      }
    }
    return gmdTree;
  }
}
