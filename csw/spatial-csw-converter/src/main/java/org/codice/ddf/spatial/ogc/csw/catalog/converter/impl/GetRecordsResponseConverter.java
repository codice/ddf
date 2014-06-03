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
package org.codice.ddf.spatial.ogc.csw.catalog.converter.impl;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.measure.converter.ConversionException;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;

import net.opengis.cat.csw.v_2_0_2.ElementSetType;
import net.opengis.cat.csw.v_2_0_2.GetRecordsType;
import net.opengis.cat.csw.v_2_0_2.QueryType;

import org.apache.commons.lang.StringUtils;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswConstants;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordCollection;
import org.codice.ddf.spatial.ogc.csw.catalog.common.CswRecordMetacardType;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.RecordConverter;
import org.codice.ddf.spatial.ogc.csw.catalog.converter.RecordConverterFactory;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.Metacard;
import ddf.catalog.data.impl.MetacardImpl;

/**
 * Converts a {@link CSWRecordCollection} into a {@link GetRecordsResponse} with CSW records
 */

public class GetRecordsResponseConverter implements Converter {

    private static final Logger LOGGER = LoggerFactory.getLogger(GetRecordsResponseConverter.class);

    private RecordConverter unmarshalRecordConverter;

    private List<RecordConverterFactory> converterFactories;

    private DefaultCswRecordMap defaultCswRecordMap = new DefaultCswRecordMap();

    private String outputSchema = CswConstants.CSW_OUTPUT_SCHEMA;

    private Map<String, String> metacardAttributeMap;

    private String productRetrievalMethod;

    private String resourceUriMapping;

    private String thumbnailMapping;

    private boolean isLonLatOrder;

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
     * @param recordConverter
     *            The converter which will transform a {@link Metacard} to a CSWRecord
     */
    public GetRecordsResponseConverter(List<RecordConverterFactory> factories) {
        this.converterFactories = factories;
    }

    @Override
    public boolean canConvert(Class type) {
        boolean canConvert = CswRecordCollection.class.isAssignableFrom(type);
        LOGGER.debug("Can convert? {}", canConvert);
        return canConvert;
    }

    @Override
    public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
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
        String elementSet = "full";
        String recordSchema = CswConstants.CSW_OUTPUT_SCHEMA;

        GetRecordsType request = cswRecordCollection.getRequest();
        QueryType query = null;
        List<QName> elementsToWrite = null;
        ElementSetType elementSetType = ElementSetType.FULL;

        if (request != null) {
            if (request.isSetStartPosition()) {
                start = request.getStartPosition().longValue();
            }
            if (request.isSetAbstractQuery()
                    && request.getAbstractQuery().getValue() instanceof QueryType) {
                query = (QueryType) request.getAbstractQuery().getValue();
            }
        }

        if (StringUtils.isNotBlank(cswRecordCollection.getOutputSchema())) {
            recordSchema = cswRecordCollection.getOutputSchema();

        }
        RecordConverterFactory converterFactory = null;
        for (RecordConverterFactory factory : converterFactories) {
            LOGGER.debug("Factory recordSchema == {}", factory.getOutputSchema());
            if (StringUtils.equals(factory.getOutputSchema(), recordSchema)) {
                converterFactory = factory;
            }
        }

        if (converterFactory == null) {
            throw new ConversionException("Unable to locate converter for outputSchema.");
        }

        if (cswRecordCollection.getElementSetType() != null) {
            elementSetType = cswRecordCollection.getElementSetType();
            elementSet = elementSetType.value();
            switch (elementSetType) {
            case BRIEF:
                elementsToWrite = CswRecordMetacardType.BRIEF_CSW_RECORD_FIELDS;
                break;
            case SUMMARY:
                elementsToWrite = CswRecordMetacardType.SUMMARY_CSW_RECORD_FIELDS;
                break;
            case FULL:
            default:
                elementsToWrite = CswRecordMetacardType.FULL_CSW_RECORD_FIELDS;
                break;
            }
        } else if (query != null && query.isSetElementName()) {
            elementsToWrite = query.getElementName();
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
            writer.addAttribute(NUMBER_OF_RECORDS_RETURNED_ATTRIBUTE,
                    Long.toString(cswRecordCollection.getNumberOfRecordsReturned()));

            writer.addAttribute(NEXT_RECORD_ATTRIBUTE, Long.toString(nextRecord));
            writer.addAttribute(RECORD_SCHEMA_ATTRIBUTE, recordSchema);
            writer.addAttribute(ELEMENT_SET_ATTRIBUTE, elementSet);
        }

        // TODO - need to pass good values here.
        RecordConverter recordConverter = converterFactory.createConverter(null,
                defaultCswRecordMap.getPrefixToUriMapping(), null, null, null,
                true);
        recordConverter.setFieldsToWrite(elementsToWrite);
        for (Metacard mc : cswRecordCollection.getCswRecords()) {
            String rootElementName = recordConverter.getRootElementName(elementSetType.toString());
            if (StringUtils.isNotBlank(rootElementName)) {
                writer.startNode(rootElementName);
                context.convertAnother(mc, recordConverter);
                writer.endNode();
            } else {
                context.convertAnother(mc, recordConverter);
            }
        }
        if (!cswRecordCollection.isById()) {
            writer.endNode();
        }
    }

    /**
     * Parses GetRecordsResponse XML of this form:
     * 
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

        unmarshalRecordConverter = createUnmarshalConverter();
        if (unmarshalRecordConverter == null) {
            throw new ConversionException("Unable to locate Converter for outputSchema: "
                    + outputSchema);
        }
        CswRecordCollection cswRecords = new CswRecordCollection();
        List<Metacard> metacards = cswRecords.getCswRecords();

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
                            unmarshalRecordConverter);
                    metacards.add(metacard);

                    // move back up to the <SearchResults> parent of the
                    // <csw:Record> tags
                    reader.moveUp();
                }
            }
            reader.moveUp();
        }

        LOGGER.debug("Unmarshalled {} metacards", metacards.size());
        if (LOGGER.isDebugEnabled()) {
            int index = 1;
            for (Metacard m : metacards) {
                LOGGER.debug("metacard {}: ", index);
                LOGGER.debug("    id = {}", m.getId());
                LOGGER.debug("    title = {}", m.getTitle());
    
                // Some CSW services return an empty bounding box, i.e., no lower
                // and/or upper corner positions
                Attribute boundingBoxAttr = m.getAttribute("BoundingBox");
                if (boundingBoxAttr != null) {
                    LOGGER.debug("    bounding box = {}", boundingBoxAttr.getValue());
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
        cswRecords.setNumberOfRecordsMatched(Long.valueOf(numberOfRecordsMatched));
        cswRecords.setNumberOfRecordsReturned(Long.valueOf(numberOfRecordsReturned));

    }

    public void setUnmarshalConverterSchema(String schema,
            Map<String, String> metacardAttributeMap, String productRetrievalMethod,
            String resourceUriMapping, String thumbnailMapping, boolean isLonLatOrder) {
        if (StringUtils.isNotBlank(schema)) {
            this.outputSchema = schema;
        }
        this.metacardAttributeMap = metacardAttributeMap;
        this.productRetrievalMethod = productRetrievalMethod;
        this.resourceUriMapping = resourceUriMapping;
        this.thumbnailMapping = thumbnailMapping;
        this.isLonLatOrder = isLonLatOrder;
        this.unmarshalRecordConverter = createUnmarshalConverter();
    }

    private RecordConverter createUnmarshalConverter() {
        if (unmarshalRecordConverter == null) {
            for (RecordConverterFactory converterFactory : converterFactories) {
                if (StringUtils.equals(converterFactory.getOutputSchema(), outputSchema)) {
                    return converterFactory.createConverter(metacardAttributeMap,
                            productRetrievalMethod, resourceUriMapping, thumbnailMapping,
                            isLonLatOrder);
                }
            }
            return null;
        }
        return unmarshalRecordConverter;
    }
}
