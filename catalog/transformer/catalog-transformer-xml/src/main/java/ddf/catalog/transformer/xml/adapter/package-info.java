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
@XmlJavaTypeAdapters({
    @XmlJavaTypeAdapter(value = MetacardTypeAdapter.class, type = MetacardType.class),

    @XmlJavaTypeAdapter(value = AttributeAdapter.class, type = Attribute.class),

    @XmlJavaTypeAdapter(value = BooleanAdapter.class, type = BooleanElement.class),
    @XmlJavaTypeAdapter(value = Base64BinaryAdapter.class, type = Base64BinaryElement.class),
    @XmlJavaTypeAdapter(value = DateTimeAdapter.class, type = DateTimeElement.class),
    @XmlJavaTypeAdapter(value = DoubleAdapter.class, type = DoubleElement.class),
    @XmlJavaTypeAdapter(value = FloatAdapter.class, type = FloatElement.class),
    @XmlJavaTypeAdapter(value = GeometryAdapter.class, type = GeometryElement.class),
    @XmlJavaTypeAdapter(value = IntAdapter.class, type = IntElement.class),
    @XmlJavaTypeAdapter(value = LongAdapter.class, type = LongElement.class),
    @XmlJavaTypeAdapter(value = ObjectAdapter.class, type = ObjectElement.class),
    @XmlJavaTypeAdapter(value = ShortAdapter.class, type = ShortElement.class),
    @XmlJavaTypeAdapter(value = StringAdapter.class, type = StringElement.class),
    @XmlJavaTypeAdapter(value = StringxmlAdapter.class, type = StringxmlElement.class)

})
package ddf.catalog.transformer.xml.adapter;

import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapters;

import ddf.catalog.data.Attribute;
import ddf.catalog.data.MetacardType;
import ddf.catalog.transformer.xml.binding.Base64BinaryElement;
import ddf.catalog.transformer.xml.binding.BooleanElement;
import ddf.catalog.transformer.xml.binding.DateTimeElement;
import ddf.catalog.transformer.xml.binding.DoubleElement;
import ddf.catalog.transformer.xml.binding.FloatElement;
import ddf.catalog.transformer.xml.binding.GeometryElement;
import ddf.catalog.transformer.xml.binding.IntElement;
import ddf.catalog.transformer.xml.binding.LongElement;
import ddf.catalog.transformer.xml.binding.ObjectElement;
import ddf.catalog.transformer.xml.binding.ShortElement;
import ddf.catalog.transformer.xml.binding.StringElement;
import ddf.catalog.transformer.xml.binding.StringxmlElement;

