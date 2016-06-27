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
package org.codice.ddf.registry.schemabindings.converter.type;

import static org.codice.ddf.registry.schemabindings.EbrimConstants.GML_FACTORY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;
import static org.codice.ddf.registry.schemabindings.EbrimConstants.WRS_FACTORY;
import static org.codice.ddf.registry.schemabindings.converter.web.SlotWebConverter.ENVELOPE_KEY;
import static org.codice.ddf.registry.schemabindings.converter.web.SlotWebConverter.LOWER_CORNER;
import static org.codice.ddf.registry.schemabindings.converter.web.SlotWebConverter.NAME;
import static org.codice.ddf.registry.schemabindings.converter.web.SlotWebConverter.POINT_KEY;
import static org.codice.ddf.registry.schemabindings.converter.web.SlotWebConverter.POSITION;
import static org.codice.ddf.registry.schemabindings.converter.web.SlotWebConverter.SLOT_TYPE;
import static org.codice.ddf.registry.schemabindings.converter.web.SlotWebConverter.SRS_DIMENSION;
import static org.codice.ddf.registry.schemabindings.converter.web.SlotWebConverter.SRS_NAME;
import static org.codice.ddf.registry.schemabindings.converter.web.SlotWebConverter.UPPER_CORNER;
import static org.codice.ddf.registry.schemabindings.converter.web.SlotWebConverter.VALUE;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.schemabindings.helper.MapToSchemaElement;
import org.codice.ddf.registry.schemabindings.helper.WebMapHelper;

import net.opengis.cat.wrs.v_1_0_2.AnyValueType;
import net.opengis.gml.v_3_1_1.DirectPositionType;
import net.opengis.gml.v_3_1_1.EnvelopeType;
import net.opengis.gml.v_3_1_1.PointType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ValueListType;

public class SlotTypeConverter {

    private MapToSchemaElement<SlotType1> mapToSchemaElement =
            new MapToSchemaElement<>(RIM_FACTORY::createSlotType1);

    private WebMapHelper webMapHelper = new WebMapHelper();

    /**
     * This method creates an SlotType1 from the values in the provided map.
     * The following keys are expected in the provided map (Taken from EbrimConstants):
     * <p>
     * SLOT_TYPE = "slotType";
     * NAME = "name";
     * POINT_KEY = "Point";
     * SRS_DIMENSION = "srsDimension";
     * SRS_NAME = "srsName";
     * POSITION = "pos";
     * VALUE = "value";
     *
     * @param map the Map representation of the SlotType1 to generate, null returns empty Optional
     * @return Optional SlotType1 created from the values in the map
     */
    public Optional<SlotType1> convert(Map<String, Object> map) {
        Optional<SlotType1> optionalSlot = Optional.empty();

        String slotType = MapUtils.getString(map, SLOT_TYPE);
        if (StringUtils.isNotBlank(slotType)) {

            if (slotType.contains(RegistryConstants.XML_GEO_TYPE) || slotType.contains(
                    RegistryConstants.XML_BOUNDS_TYPE)) {
                Optional<net.opengis.cat.wrs.v_1_0_2.ValueListType> optionalValueList =
                        getWrsValueList(map);
                if (optionalValueList.isPresent()) {
                    if (!optionalSlot.isPresent()) {
                        optionalSlot = Optional.of(mapToSchemaElement.getObjectFactory()
                                .get());
                    }

                    optionalSlot.get()
                            .setValueList(RIM_FACTORY.createValueList(optionalValueList.get()));
                }
            }

            if (!optionalSlot.isPresent()) {
                optionalSlot = Optional.of(mapToSchemaElement.getObjectFactory()
                        .get());
            }
            optionalSlot.get()
                    .setSlotType(slotType);
        }
        if (StringUtils.isBlank(slotType) || !slotType.contains(RegistryConstants.XML_GEO_TYPE)) {
            Optional<ValueListType> optionalValueList = getValueList(map);
            if (optionalValueList.isPresent()) {
                if (!optionalSlot.isPresent()) {
                    optionalSlot = Optional.of(mapToSchemaElement.getObjectFactory()
                            .get());
                }

                optionalSlot.get()
                        .setValueList(RIM_FACTORY.createValueList(optionalValueList.get()));
            }
        }

        optionalSlot = mapToSchemaElement.populateStringElement(map,
                NAME,
                optionalSlot,
                (valueToPopulate, slot) -> slot.setName(valueToPopulate));

        return optionalSlot;
    }

    private Optional<ValueListType> getValueList(Map<String, Object> map) {
        Optional<ValueListType> optionalValueList = Optional.empty();
        if (MapUtils.isEmpty(map)) {
            return optionalValueList;
        }

        List<String> values = webMapHelper.getStringListFromMap(map, VALUE);
        if (CollectionUtils.isNotEmpty(values)) {
            optionalValueList = Optional.of(RIM_FACTORY.createValueListType());
            optionalValueList.get()
                    .getValue()
                    .addAll(values);
        }

        return optionalValueList;
    }

    private Optional<net.opengis.cat.wrs.v_1_0_2.ValueListType> getWrsValueList(
            Map<String, Object> map) {
        Optional<net.opengis.cat.wrs.v_1_0_2.ValueListType> optionalValueList = Optional.empty();
        if (MapUtils.isEmpty(map) || !map.containsKey(VALUE)) {
            return optionalValueList;
        }

        Map<String, Object> valueMap = (Map<String, Object>) map.get(VALUE);

        if (valueMap.containsKey(POINT_KEY)) {
            Optional<PointType> optionalPoint = getPoint((Map<String, Object>) valueMap.get(
                    POINT_KEY));
            if (optionalPoint.isPresent()) {
                AnyValueType anyValue = WRS_FACTORY.createAnyValueType();
                anyValue.getContent()
                        .add(GML_FACTORY.createPoint(optionalPoint.get()));

                net.opengis.cat.wrs.v_1_0_2.ValueListType valueList =
                        WRS_FACTORY.createValueListType();
                valueList.getAnyValue()
                        .add(anyValue);
                if (!optionalValueList.isPresent()) {
                    optionalValueList = Optional.of(WRS_FACTORY.createValueListType());
                }

                optionalValueList.get()
                        .getAnyValue()
                        .add(anyValue);
            }
        } else if (valueMap.containsKey(ENVELOPE_KEY)) {
            Optional<EnvelopeType> optionalEnvelope =
                    getEnvelope((Map<String, Object>) valueMap.get(ENVELOPE_KEY));
            if (optionalEnvelope.isPresent()) {
                AnyValueType anyValue = WRS_FACTORY.createAnyValueType();
                anyValue.getContent()
                        .add(GML_FACTORY.createEnvelope(optionalEnvelope.get()));

                net.opengis.cat.wrs.v_1_0_2.ValueListType valueList =
                        WRS_FACTORY.createValueListType();
                valueList.getAnyValue()
                        .add(anyValue);
                if (!optionalValueList.isPresent()) {
                    optionalValueList = Optional.of(WRS_FACTORY.createValueListType());
                }

                optionalValueList.get()
                        .getAnyValue()
                        .add(anyValue);
            }
        }

        return optionalValueList;
    }

    private Optional<EnvelopeType> getEnvelope(Map<String, Object> envelopeMap) {
        Optional<EnvelopeType> optionalEnvelope = Optional.empty();
        if (MapUtils.isEmpty(envelopeMap)) {
            return optionalEnvelope;
        }
        optionalEnvelope = Optional.of(GML_FACTORY.createEnvelopeType());
        String valueToPopulate = MapUtils.getString(envelopeMap, SRS_NAME);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            optionalEnvelope.get()
                    .setSrsName(valueToPopulate);
        }

        String upperCorner = MapUtils.getString(envelopeMap, UPPER_CORNER);
        String lowerCorner = MapUtils.getString(envelopeMap, LOWER_CORNER);
        if (StringUtils.isNotBlank(upperCorner)) {
            List<Double> values = Arrays.stream(StringUtils.split(upperCorner))
                    .map(e -> new Double(e))
                    .collect(Collectors.toList());
            DirectPositionType directPosition = GML_FACTORY.createDirectPositionType();
            directPosition.setValue(values);
            optionalEnvelope.get()
                    .setUpperCorner(directPosition);
        }

        if (StringUtils.isNotBlank(lowerCorner)) {
            List<Double> values = Arrays.stream(StringUtils.split(lowerCorner))
                    .map(e -> new Double(e))
                    .collect(Collectors.toList());
            DirectPositionType directPosition = GML_FACTORY.createDirectPositionType();
            directPosition.setValue(values);
            optionalEnvelope.get()
                    .setLowerCorner(directPosition);
        }
        return optionalEnvelope;
    }

    private Optional<PointType> getPoint(Map<String, Object> pointMap) {
        Optional<PointType> optionalPoint = Optional.empty();
        if (MapUtils.isEmpty(pointMap)) {
            return optionalPoint;
        }

        BigInteger dimension = getBigIntFromMap(SRS_DIMENSION, pointMap);
        if (dimension != null) {
            if (!optionalPoint.isPresent()) {
                optionalPoint = Optional.of(GML_FACTORY.createPointType());
            }
            optionalPoint.get()
                    .setSrsDimension(dimension);
        }

        String valueToPopulate = MapUtils.getString(pointMap, SRS_NAME);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            if (!optionalPoint.isPresent()) {
                optionalPoint = Optional.of(GML_FACTORY.createPointType());
            }
            optionalPoint.get()
                    .setSrsName(valueToPopulate);
        }

        valueToPopulate = MapUtils.getString(pointMap, POSITION);
        if (StringUtils.isNotBlank(valueToPopulate)) {
            String[] values = StringUtils.split(valueToPopulate);

            DirectPositionType directPosition = GML_FACTORY.createDirectPositionType();

            for (String value : values) {
                directPosition.getValue()
                        .add(Double.valueOf(value));
            }

            if (!optionalPoint.isPresent()) {
                optionalPoint = Optional.of(GML_FACTORY.createPointType());
            }
            optionalPoint.get()
                    .setPos(directPosition);
        }

        return optionalPoint;
    }

    private BigInteger getBigIntFromMap(String key, Map<String, Object> map) {
        BigInteger value = null;
        if (map.containsKey(key)) {
            value = new BigInteger(MapUtils.getString(map, key));
        }

        return value;
    }

}
