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
package ddf.catalog.data;

import java.io.Serializable;
import java.util.List;

public class StubAttribute implements Attribute {
    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;

    private String name;

    private Serializable value;

    private transient List<Serializable> vals;

    public StubAttribute(String n, Serializable val, List<Serializable> values) {
        name = n;
        value = val;
        vals = values;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Serializable getValue() {
        return value;
    }

    @Override
    public List<Serializable> getValues() {
        return vals;
    }

}