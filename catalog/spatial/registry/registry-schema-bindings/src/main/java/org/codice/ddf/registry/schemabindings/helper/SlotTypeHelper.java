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
package org.codice.ddf.registry.schemabindings.helper;

import static org.codice.ddf.registry.schemabindings.EbrimConstants.RIM_FACTORY;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.SlotType1;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.ValueListType;

public class SlotTypeHelper {

    public SlotTypeHelper() {
    }

    /**
     * This is a convenience method that searches the List of SlotType1 provided and returns the
     * SlotType1 matching the provided name
     *
     * @param name  the name of the slot to be returned
     * @param slots the List of SlotType1 to be searched, not null
     * @return SlotType1 whose name matched the name provided
     * null if a match isn't found
     */
    public SlotType1 getSlotByName(String name, List<SlotType1> slots) {
        if (CollectionUtils.isNotEmpty(slots)) {
            for (SlotType1 slot : slots) {
                if (slot.getName()
                        .equals(name)) {
                    return slot;
                }
            }
        }

        return null;
    }

    /**
     * This is a convenience method that will iterate through the List of SlotType1 provided and return a SlotType1 name to SlotType1 mapping
     * If multiple slots share the same name, the last one will be stored in the map
     *
     * @param slots the list of SlotType1s to be mapped, null returns empty map
     * @return a mapping of SlotType1 name to SlotType1
     */
    public Map<String, SlotType1> getNameSlotMap(List<SlotType1> slots) {
        Map<String, SlotType1> slotMap = new HashMap<>();

        if (CollectionUtils.isNotEmpty(slots)) {
            for (SlotType1 slot : slots) {
                slotMap.put(slot.getName(), slot);
            }
        }

        return slotMap;
    }

    /**
     * This is a convenience method that will iterate through the List of SlotType1 provided and return a mapping of SlotType1 name to a List of SlotType1s
     * * If multiple slots share the same name they will be added to the list
     *
     * @param slots the list of SlotType1s to be mapped, null returns empty map
     * @return a mapping of SlotType1 name to List of SlotType1
     */
    public Map<String, List<SlotType1>> getNameSlotMapDuplicateSlotNamesAllowed(
            List<SlotType1> slots) {
        Map<String, List<SlotType1>> slotMap = new HashMap<>();

        if (CollectionUtils.isNotEmpty(slots)) {
            for (SlotType1 slot : slots) {
                slotMap.putIfAbsent(slot.getName(), new ArrayList<>());
                slotMap.get(slot.getName())
                        .add(slot);
            }
        }

        return slotMap;
    }

    /**
     * This is a convenience method that will get the values from a SlotType1
     *
     * @param slot the SlotType1 to get the values from, null returns empty List
     * @return a List of String values found in the SlotType1
     */
    public List<String> getStringValues(SlotType1 slot) {
        List<String> slotAttributes = new ArrayList<>();

        if (slot != null && slot.isSetValueList()) {
            ValueListType valueList = slot.getValueList()
                    .getValue();
            if (valueList.isSetValue()) {
                slotAttributes = valueList.getValue();
            }
        }

        return slotAttributes;
    }

    /**
     * This is a convenience method that will get the values from a SlotType1
     *
     * @param slot the SlotType1 to get the values from, null returns empty List
     * @return a List of Date values found in the SlotType1
     */
    public List<Date> getDateValues(SlotType1 slot) {
        return getStringValues(slot).stream()
                .map(dateString -> Date.from(ZonedDateTime.parse(dateString)
                        .toInstant()))
                .collect(Collectors.toList());
    }

    /**
     * This is a convenience method to create a SlotType1 object with the provided value
     *
     * @param slotName  the name of the slot, empty SlotType1 if null
     * @param slotValue the value to set
     * @param slotType  the slot type of the slot
     * @return
     */
    public SlotType1 create(String slotName, String slotValue, String slotType) {
        return create(slotName, Collections.singletonList(slotValue), slotType);
    }

    /**
     * This is a convenience method to create a SlotType1 object with the List of values
     *
     * @param slotName   the name of the slot, empty SlotType1 if null
     * @param slotValues the value to set
     * @param slotType   the slot type of the slot
     * @return
     */
    public SlotType1 create(String slotName, List<String> slotValues, String slotType) {
        SlotType1 slot = RIM_FACTORY.createSlotType1();

        if (StringUtils.isNotBlank(slotName)) {
            ValueListType valueList = RIM_FACTORY.createValueListType();
            valueList.getValue()
                    .addAll(slotValues);
            slot.setValueList(RIM_FACTORY.createValueList(valueList));
            slot.setSlotType(slotType);
            slot.setName(slotName);
        }

        return slot;
    }
}
