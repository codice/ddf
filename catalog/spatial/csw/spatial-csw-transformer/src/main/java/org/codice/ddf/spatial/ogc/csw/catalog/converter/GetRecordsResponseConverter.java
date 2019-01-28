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

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import java.util.List;
import javax.measure.IncommensurableException;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Converts a {@link org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection} into a
 * {@link net.opengis.cat.csw.v_2_0_2.GetRecordsResponseType} with CSW records
 */
public class GetRecordsResponseConverter implements Converter {

  private static final Logger LOGGER = LoggerFactory.getLogger(GetRecordsResponseConverter.class);

  private Converter transformProvider;

  /**
   * Creates a new GetRecordsResponseConverter Object
   *
   * @param transformProvider The converter which will transform a {@link Metacard} to a the
   *     appropriate XML format and vice versa.
   */
  public GetRecordsResponseConverter(Converter transformProvider) {
    this.transformProvider = transformProvider;
  }

  @Override
  public boolean canConvert(Class type) {
    boolean canConvert = CswRecordCollection.class.isAssignableFrom(type);
    LOGGER.debug("Can convert? {}", canConvert);
    return canConvert;
  }

  @Override
  public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
    throw new UnsupportedOperationException();
  }

  /**
   * Parses GetRecordsResponse XML of this form:
   *
   * <p>
   *
   * <pre>{@code
   * <csw:GetRecordsResponse xmlns:csw="http://www.opengis.net/cat/csw">
   *     <csw:SearchStatus status="subset" timestamp="2013-05-01T02:13:36+0200"/>
   *     <csw:SearchResults elementSet="full" nextRecord="11"
   *         numberOfRecordsMatched="479" numberOfRecordsReturned="10"
   *         recordSchema="csw:Record">
   *         <csw:Record xmlns:csw="http://www.opengis.net/cat/csw">
   *         ...
   *         </csw:Record>
   *         <csw:Record xmlns:csw="http://www.opengis.net/cat/csw">
   *         ...
   *         </csw:Record>
   *
   * }</pre>
   */
  @Override
  public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
    if (transformProvider == null) {
      throw new IncommensurableException(
          "Unable to locate Converter for outputSchema: " + CswConstants.CSW_OUTPUT_SCHEMA);
    }
    CswRecordCollection cswRecords = new CswRecordCollection();
    List<Metacard> metacards = cswRecords.getCswRecords();

    XStreamAttributeCopier.copyXmlNamespaceDeclarationsIntoContext(reader, context);
    while (reader.hasMoreChildren()) {
      reader.moveDown();

      if (reader.getNodeName().contains("SearchResults")) {
        setSearchResults(reader, cswRecords);

        // Loop through the <SearchResults>, converting each
        // <csw:Record> into a Metacard
        while (reader.hasMoreChildren()) {
          reader.moveDown(); // move down to the <csw:Record> tag
          String name = reader.getNodeName();
          LOGGER.debug("node name = {}", name);
          Metacard metacard =
              (Metacard) context.convertAnother(null, MetacardImpl.class, transformProvider);
          metacards.add(metacard);

          // move back up to the <SearchResults> parent of the
          // <csw:Record> tags
          reader.moveUp();
        }
      }
      reader.moveUp();
    }

    LOGGER.debug("Unmarshalled {} metacards", metacards.size());
    if (LOGGER.isTraceEnabled()) {
      int index = 1;
      for (Metacard m : metacards) {
        LOGGER.trace("metacard {}: ", index);
        LOGGER.trace("    id = {}", m.getId());
        LOGGER.trace("    title = {}", m.getTitle());

        // Some CSW services return an empty bounding box, i.e., no lower
        // and/or upper corner positions
        Attribute boundingBoxAttr = m.getAttribute("BoundingBox");
        if (boundingBoxAttr != null) {
          LOGGER.trace("    bounding box = {}", boundingBoxAttr.getValue());
        }
        index++;
      }
    }

    return cswRecords;
  }

  private void setSearchResults(HierarchicalStreamReader reader, CswRecordCollection cswRecords) {

    String numberOfRecordsMatched = reader.getAttribute("numberOfRecordsMatched");
    LOGGER.debug("numberOfRecordsMatched = {}", numberOfRecordsMatched);
    String numberOfRecordsReturned = reader.getAttribute("numberOfRecordsReturned");
    LOGGER.debug("numberOfRecordsReturned = {}", numberOfRecordsReturned);
    cswRecords.setNumberOfRecordsMatched(Long.parseLong(numberOfRecordsMatched));
    cswRecords.setNumberOfRecordsReturned(Long.parseLong(numberOfRecordsReturned));
  }
}
