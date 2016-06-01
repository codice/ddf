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
package org.codice.ddf.registry.schemabindings.converter.web;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.JAXBElement;

import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;
import org.codice.ddf.registry.common.RegistryConstants;
import org.codice.ddf.registry.schemabindings.helper.SlotTypeHelper;
import org.codice.ddf.registry.schemabindings.helper.WebMapHelper;

import net.opengis.cat.wrs.v_1_0_2.AnyValueType;
import net.opengis.cat.wrs.v_1_0_2.ValueListType;
import net.opengis.gml.v_3_1_1.DirectPositionType;
import net.opengis.gml.v_3_1_1.PointType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;

public class SlotWebConverter {
    public static final String NAME = "name";

    public static final String POINT_KEY = "Point";

    public static final String POSITION = "pos";

    public static final String SLOT_TYPE = "slotType";

    public static final String SRS_DIMENSION = "srsDimension";

    public static final String SRS_NAME = "srsName";

    public static final String VALUE = "value";

    private static final SlotTypeHelper SLOT_TYPE_HELPER = new SlotTypeHelper();

    private WebMapHelper webMapHelper = new WebMapHelper();

    /**
     * This method creates a Map<String, Object> representation of the SlotType1 provided.
     * The following keys will be added to the map (Taken from EbrimConstants):
     * <p>
     * SLOT_TYPE = "slotType";
     * NAME = "name";
     * POINT_KEY = "Point";
     * SRS_DIMENSION = "srsDimension";
     * SRS_NAME = "srsName";
     * POSITION = "pos";
     * VALUE = "value";
     *
     * @param slot the SlotType1 to be converted into a map, null returns empty Map
     * @return Map<String, Object> representation of the SlotType1 provided
     */
    public Map<String, Object> convert(SlotType1 slot) {
        Map<String, Object> slotMap = new HashMap<>();

        String slotType = slot.getSlotType();
        if (StringUtils.isNotBlank(slotType)) {
            slotMap.put(SLOT_TYPE, slotType);

            if (slotType.contains(RegistryConstants.XML_GEO_TYPE)) {
                webMapHelper.putAllIfNotEmpty(slotMap, getSlotGeoMap(slot));

            }
        }

        if (StringUtils.isBlank(slotType) || !slotType.contains(RegistryConstants.XML_GEO_TYPE)) {
            List<String> stringValues = SLOT_TYPE_HELPER.getStringValues(slot);

            webMapHelper.putIfNotEmpty(slotMap, VALUE, stringValues);
        }

        webMapHelper.putIfNotEmpty(slotMap, NAME, slot.getName());

        return slotMap;
    }

    private Map<String, Object> getSlotGeoMap(SlotType1 slot) {
        Map<String, Object> map = new HashMap<>();
        if (!slot.isSetValueList()) {
            return map;
        }

        ValueListType valueList = (ValueListType) slot.getValueList()
                .getValue();

        for (AnyValueType anyValue : valueList.getAnyValue()) {
            anyValue.getContent()
                    .stream()
                    .filter(content -> content instanceof JAXBElement)
                    .forEach(content -> {
                        JAXBElement jaxbElement = (JAXBElement) content;

                        if (jaxbElement.getValue() instanceof PointType) {
                            Map<String, Object> pointMap =
                                    getPointMapFromPointType((PointType) jaxbElement.getValue());
                            if (MapUtils.isNotEmpty(pointMap)) {
                                Map<String, Object> valueMap = new HashMap<>();
                                valueMap.put(POINT_KEY, pointMap);
                                map.put(VALUE, valueMap);
                            }
                        }
                    });
        }

        return map;
    }

    private static Map<String, Object> getPointMapFromPointType(PointType point) {
        Map<String, Object> pointMap = new HashMap<>();

        if (point.isSetSrsDimension()) {
            pointMap.put(SRS_DIMENSION,
                    point.getSrsDimension()
                            .intValue());
        }

        if (point.isSetSrsName()) {
            pointMap.put(SRS_NAME, point.getSrsName());
        }

        if (point.isSetPos()) {
            DirectPositionType directPosition = point.getPos();
            List<String> pointValues = new ArrayList<>();

            pointValues.addAll(directPosition.getValue()
                    .stream()
                    .map(String::valueOf)
                    .collect(Collectors.toList()));

            String position = String.join(" ", pointValues);

            if (StringUtils.isNotBlank(position)) {
                pointMap.put(POSITION, position);
            }
        }

        return pointMap;
    }

}
