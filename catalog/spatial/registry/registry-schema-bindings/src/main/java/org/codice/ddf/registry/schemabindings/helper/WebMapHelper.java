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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang.StringUtils;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.InternationalStringType;
import oasis.names.tc.ebxml_regrep.xsd.rim._3.VersionInfoType;

public class WebMapHelper {
    private InternationalStringTypeHelper internationalStringTypeHelper =
            new InternationalStringTypeHelper();

    public void putIfNotEmpty(Map<String, Object> map, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            map.put(key, value);
        }
    }

    public void putIfNotEmpty(Map<String, Object> map, String key, Boolean value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    public void putIfNotEmpty(Map<String, Object> map, String key, List values) {
        if (CollectionUtils.isNotEmpty(values)) {
            map.put(key, values);
        }
    }

    public void putIfNotEmpty(Map<String, Object> map, String key, Map valueMap) {
        if (MapUtils.isNotEmpty(valueMap)) {
            map.put(key, valueMap);
        }
    }

    public void putIfNotEmpty(Map<String, Object> map, String key,
            VersionInfoType versionInfoType) {
        if (versionInfoType != null) {
            String versionName = versionInfoType.getVersionName();
            if (StringUtils.isNotBlank(versionName)) {
                map.put(key, versionName);
            }
        }
    }

    public void putIfNotEmpty(Map<String, Object> map, String key,
            InternationalStringType internationalStringType) {
        if (internationalStringType != null) {

            String value = internationalStringTypeHelper.getString(internationalStringType);
            if (StringUtils.isNotBlank(value)) {
                map.put(key, value);
            }
        }
    }

    public void putAllIfNotEmpty(Map<String, Object> map, Map valueMap) {
        if (MapUtils.isNotEmpty(valueMap)) {
            map.putAll(valueMap);
        }
    }

    public List<String> getStringListFromMap(Map<String, Object> map, String key) {
        List<String> values = new ArrayList<>();
        if (MapUtils.isEmpty(map) || !map.containsKey(key)) {
            return values;
        }

        if (map.get(key) instanceof String) {
            values.add(MapUtils.getString(map, key));
        } else if (map.get(key) instanceof List) {
            values.addAll((List<String>) map.get(key));
        }

        return values;
    }
}
