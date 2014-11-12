/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.spatial.ogc.csw.catalog.converter;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;
import net.opengis.cat.csw.v_2_0_2.ResultType;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.measure.converter.ConversionException;
import javax.xml.XMLConstants;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Converts a {@link org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection} into a
 * {@link net.opengis.cat.csw.v_2_0_2.GetRecordsResponseType} with CSW records
 */

public class GetRecordsResponseConverter implements Converter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetRecordsResponseConverter.class);

    private Converter transformProvider;

    private DefaultCswRecordMap defaultCswRecordMap = new DefaultCswRecordMap();

    private String outputSchema = CswConstants.CSW_OUTPUT_SCHEMA;

    private static final String SEARCH_STATUS_NODE_NAME = "SearchStatus";

    private static final String SEARCH_RESULTS_NODE_NAME = "SearchResults";

    private static final String VERSION_ATTRIBUTE = "version";

    private static final String TIMESTAMP_ATTRIBUTE = "timestamp";

    private static final String NUMBER_OF_RECORDS_MATCHED_ATTRIBUTE = "numberOfRecordsMatched";

    private static final String NUMBER_OF_RECORDS_RETURNED_ATTRIBUTE = "numberOfRecordsReturned";

    private static final String NEXT_RECORD_ATTRIBUTE = "nextRecord";

    private static final String RECORD_SCHEMA_ATTRIBUTE = "recordSchema";

    private static final String ELEMENT_SET_ATTRIBUTE = "elementSet";

    /**
     * Creates a new GetRecordsResponseConverter Object
     *
     * @param transformProvider The converter which will transform a {@link Metacard} to a the appropriate XML
     *                          format and vice versa.
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
    public void marshal(Object source, HierarchicalStreamWriter writer,
            MarshallingContext context) {
        LOGGER.debug("Entering GetRecordsResponseConverter.marshal()");
        if (source == null || !(source instanceof CswRecordCollection)) {
            LOGGER.warn("Failed to marshal CswRecordCollection: {}", source);
            return;
        }
        CswRecordCollection cswRecordCollection = (CswRecordCollection) source;

        for (Entry<String, String> entry : defaultCswRecordMap.getPrefixToUriMapping().entrySet()) {
            writer.addAttribute(XMLConstants.XMLNS_ATTRIBUTE + CswConstants.NAMESPACE_DELIMITER
                    + entry.getKey(), entry.getValue());
        }

        long start = 1;
        String elementSet = null;
        String recordSchema = CswConstants.CSW_OUTPUT_SCHEMA;

        if (cswRecordCollection.getStartPosition() > 0) {
            start = cswRecordCollection.getStartPosition();
        }

        if (StringUtils.isNotBlank(cswRecordCollection.getOutputSchema())) {
            recordSchema = cswRecordCollection.getOutputSchema();

        }
        context.put(CswConstants.OUTPUT_SCHEMA_PARAMETER, recordSchema);

        if (cswRecordCollection.getElementSetType() != null) {
            context.put(CswConstants.ELEMENT_SET_TYPE, cswRecordCollection.getElementSetType());

            elementSet = cswRecordCollection.getElementSetType().value();
        } else if (cswRecordCollection.getElementName() != null) {
            context.put(CswConstants.ELEMENT_NAMES, cswRecordCollection.getElementName());
        }

        long nextRecord = start + cswRecordCollection.getNumberOfRecordsReturned();
        if (nextRecord > cswRecordCollection.getNumberOfRecordsMatched()) {
            nextRecord = 0;
        }
        if (!cswRecordCollection.isById()) {
            writer.addAttribute(VERSION_ATTRIBUTE, CswConstants.VERSION_2_0_2);

            writer.startNode(CswConstants.CSW_NAMESPACE_PREFIX + CswConstants.NAMESPACE_DELIMITER
                    + SEARCH_STATUS_NODE_NAME);
            writer.addAttribute(TIMESTAMP_ATTRIBUTE,
                    ISODateTimeFormat.dateTime().print(new DateTime()));
            writer.endNode();

            writer.startNode(CswConstants.CSW_NAMESPACE_PREFIX + CswConstants.NAMESPACE_DELIMITER
                    + SEARCH_RESULTS_NODE_NAME);
            writer.addAttribute(NUMBER_OF_RECORDS_MATCHED_ATTRIBUTE,
                    Long.toString(cswRecordCollection.getNumberOfRecordsMatched()));
            if (!ResultType.HITS.equals(cswRecordCollection.getResultType())) {
                writer.addAttribute(NUMBER_OF_RECORDS_RETURNED_ATTRIBUTE,
                        Long.toString(cswRecordCollection.getNumberOfRecordsReturned()));
            } else {
                writer.addAttribute(NUMBER_OF_RECORDS_RETURNED_ATTRIBUTE, Long.toString(0));
            }

            writer.addAttribute(NEXT_RECORD_ATTRIBUTE, Long.toString(nextRecord));
            writer.addAttribute(RECORD_SCHEMA_ATTRIBUTE, recordSchema);
            if (StringUtils.isNotBlank(elementSet)) {
                writer.addAttribute(ELEMENT_SET_ATTRIBUTE, elementSet);
            }
        }
        context.put(CswConstants.WRITE_NAMESPACES, cswRecordCollection.isDoWriteNamespaces());

        if (!ResultType.HITS.equals(cswRecordCollection.getResultType())) {
            LOGGER.debug("Transforming individual metacards.");
            for (Metacard mc : cswRecordCollection.getCswRecords()) {
                context.convertAnother(mc, transformProvider);
            }
        }
        if (!cswRecordCollection.isById()) {
            writer.endNode();
        }

    }

    /**
     * Parses GetRecordsResponse XML of this form:
     * <p/>
     * <pre>
     * {@code
     *  <csw:GetRecordsResponse xmlns:csw="http://www.opengis.net/cat/csw">
     *      <csw:SearchStatus status="subset" timestamp="2013-05-01T02:13:36+0200"/>
     *      <csw:SearchResults elementSet="full" nextRecord="11"
     *          numberOfRecordsMatched="479" numberOfRecordsReturned="10"
     *          recordSchema="csw:Record">
     *          <csw:Record xmlns:csw="http://www.opengis.net/cat/csw">
     *          ...
     *          </csw:Record>
     *          <csw:Record xmlns:csw="http://www.opengis.net/cat/csw">
     *          ...
     *          </csw:Record>
     *  }
     * </pre>
     */
    @Override
    public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
        if (transformProvider == null) {
            throw new ConversionException("Unable to locate Converter for outputSchema: "
                    + outputSchema);
        }
        CswRecordCollection cswRecords = new CswRecordCollection();
        List<Metacard> metacards = cswRecords.getCswRecords();

        parseXmlNamespaceDeclarations(reader, context);
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
                    Metacard metacard = (Metacard) context.convertAnother(null, MetacardImpl.class,
                            transformProvider);
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

    private void parseXmlNamespaceDeclarations(HierarchicalStreamReader reader,
            UnmarshallingContext context) {
        Map<String, String> namespaces = new HashMap<>();
        Iterator<String> attributeNames = reader.getAttributeNames();
        while (attributeNames.hasNext()) {
            String name = attributeNames.next();
            if (StringUtils.startsWith(name, CswConstants.XMLNS)) {
                String attributeValue = reader.getAttribute(name);
                namespaces.put(name, attributeValue);
            }
        }
        if (!namespaces.isEmpty()) {
            context.put(CswConstants.WRITE_NAMESPACES, namespaces);
        }
    }

    private void setSearchResults(HierarchicalStreamReader reader, CswRecordCollection cswRecords) {

        String numberOfRecordsMatched = reader.getAttribute("numberOfRecordsMatched");
        LOGGER.debug("numberOfRecordsMatched = {}", numberOfRecordsMatched);
        String numberOfRecordsReturned = reader.getAttribute("numberOfRecordsReturned");
        LOGGER.debug("numberOfRecordsReturned = {}", numberOfRecordsReturned);
        cswRecords.setNumberOfRecordsMatched(Long.valueOf(numberOfRecordsMatched));
        cswRecords.setNumberOfRecordsReturned(Long.valueOf(numberOfRecordsReturned));

    }
}
