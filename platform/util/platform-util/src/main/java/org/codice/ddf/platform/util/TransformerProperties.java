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
package org.codice.ddf.platform.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import javax.xml.transform.ErrorListener;

/**
 * Properties for transforming XML to a String using a Transformer
 */
public class TransformerProperties {
    private Map<String, String> transformProperties;

    private ErrorListener errorListener;

    public TransformerProperties() {
        this.transformProperties = new HashMap<>();
    }

    public void addOutputProperty(String propertyType, String propertyValue) {
        this.transformProperties.put(propertyType, propertyValue);
    }

    public void setErrorListener(ErrorListener errorListener) {
        this.errorListener = errorListener;
    }

    public Set<String> getTransformTypes() {
        return this.transformProperties.keySet();
    }

    public String getTransformValue(String propertyType) {
        return transformProperties.get(propertyType);
    }

    public ErrorListener getErrorListener() {
        return this.errorListener;
    }
}
