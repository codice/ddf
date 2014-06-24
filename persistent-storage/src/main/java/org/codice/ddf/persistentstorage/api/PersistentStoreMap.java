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
package org.codice.ddf.persistentstorage.api;

import java.util.HashMap;

import org.apache.commons.lang.StringUtils;

public class PersistentStoreMap extends HashMap<String, Object> {
    
    private static final long serialVersionUID = 6030726429622527480L;

    public static final String ID = "id_txt";
    
    public static final String TEXT_SUFFIX = "_txt";
    
    public static final String XML_SUFFIX = "_xml";
    
    public static final String INT_SUFFIX = "_int";
    
    public static final String LONG_SUFFIX = "_lng";
    
    // for Set<String>
    public static final String TEXT_SET_SUFFIX = "_txt_set";
    
    
    public void addIdProperty(Object value) {
        if (value != null) {
            put(ID, value);
        }
    }
    
    public void addTextProperty(String name, Object value) {
        addProperty(name, TEXT_SUFFIX, value);
    }
    
    public void addXmlProperty(String name, Object value) {
        addProperty(name, XML_SUFFIX, value);
    }
    
    public void addIntProperty(String name, Object value) {
        addProperty(name, INT_SUFFIX, value);
    }
    
    public void addLongProperty(String name, Object value) {
        addProperty(name, LONG_SUFFIX, value);
    }
    
    public void addTextSetProperty(String name, Object value) {
        addProperty(name, TEXT_SET_SUFFIX, value);
    }
    
    public void addProperty(String name, String suffix, Object value) {
        if (StringUtils.isNotBlank(name) && StringUtils.isNotBlank(suffix)) {
            put(name + suffix, value);
        }
    }
}
