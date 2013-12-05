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
package ddf.catalog.transform.xml;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;
import org.junit.Test;

import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.xml.adapter.MetacardTypeAdapter;

public class TestMetacardTypeAdapter {
    private static final Logger LOGGER = Logger.getLogger(TestMetacardTypeAdapter.class);

    private static final String NULL_TYPE_NAME = null;

    private static final String EMPTY_TYPE_NAME = "";

    private static final String DDF_METACARD_TYPE_NAME = BasicTypes.BASIC_METACARD.getName();

    private static final String UNKNOWN_TYPE_NAME = "unknownTypeName.metacard";

    private static final String KNOWN_TYPE_NAME = "knownTypeName.metacard";

    private static final List<MetacardType> NULL_METACARD_TYPES_LIST = null;

    private static final List<MetacardType> EMPTY_METACARD_TYPES_LIST = new ArrayList<MetacardType>(
            0);

    static {
        BasicConfigurator.configure();
    }

    @Test
    public void testUnmarshalWithNullTypeName() throws CatalogTransformerException {
        MetacardTypeAdapter metacardTypeAdpater = new MetacardTypeAdapter();
        MetacardType metacardType = metacardTypeAdpater.unmarshal(NULL_TYPE_NAME);
        assertThat(metacardType.getName(), is(BasicTypes.BASIC_METACARD.getName()));
    }

    @Test
    public void testUnmarshalWithEmptyTypeName() throws CatalogTransformerException {
        MetacardTypeAdapter metacardTypeAdpater = new MetacardTypeAdapter();
        MetacardType metacardType = metacardTypeAdpater.unmarshal(EMPTY_TYPE_NAME);
        assertThat(metacardType.getName(), is(BasicTypes.BASIC_METACARD.getName()));
    }

    @Test
    public void testUnmarshalWithDdfMetacardTypeName() throws CatalogTransformerException {
        MetacardType unknownMetacardType = new MetacardTypeImpl(KNOWN_TYPE_NAME, null);
        List<MetacardType> metacardTypes = new ArrayList<MetacardType>(1);
        metacardTypes.add(unknownMetacardType);
        MetacardTypeAdapter metacardTypeAdpater = new MetacardTypeAdapter(metacardTypes);
        MetacardType metacardType = metacardTypeAdpater.unmarshal(DDF_METACARD_TYPE_NAME);
        assertThat(metacardType.getName(), is(BasicTypes.BASIC_METACARD.getName()));
    }

    @Test
    public void testUnmarshalWithUnknownTypeName() throws CatalogTransformerException {
        MetacardType unknownMetacardType = new MetacardTypeImpl(KNOWN_TYPE_NAME, null);
        List<MetacardType> metacardTypes = new ArrayList<MetacardType>(1);
        metacardTypes.add(unknownMetacardType);
        MetacardTypeAdapter metacardTypeAdpater = new MetacardTypeAdapter(metacardTypes);
        MetacardType metacardType = metacardTypeAdpater.unmarshal(UNKNOWN_TYPE_NAME);
        assertThat(metacardType.getName(), is(BasicTypes.BASIC_METACARD.getName()));
    }

    @Test
    public void testUnmarshalWithNullRegisteredMetacardTypes() throws CatalogTransformerException {
        MetacardTypeAdapter metacardTypeAdpater = new MetacardTypeAdapter(NULL_METACARD_TYPES_LIST);
        MetacardType metacardType = metacardTypeAdpater.unmarshal(UNKNOWN_TYPE_NAME);
        assertThat(metacardType.getName(), is(BasicTypes.BASIC_METACARD.getName()));
    }

    @Test
    public void testUnmarshalWithEmptyRegisteredMetacardTypes() throws CatalogTransformerException {
        MetacardTypeAdapter metacardTypeAdpater = new MetacardTypeAdapter(EMPTY_METACARD_TYPES_LIST);
        MetacardType metacardType = metacardTypeAdpater.unmarshal(UNKNOWN_TYPE_NAME);
        assertThat(metacardType.getName(), is(BasicTypes.BASIC_METACARD.getName()));
    }

    @Test
    public void testUnmarshalWithKnownMetacardType() throws CatalogTransformerException {
        MetacardType knownMetacardType = new MetacardTypeImpl(KNOWN_TYPE_NAME, null);
        List<MetacardType> metacardTypes = new ArrayList<MetacardType>(1);
        metacardTypes.add(knownMetacardType);
        MetacardTypeAdapter metacardTypeAdpater = new MetacardTypeAdapter(metacardTypes);
        MetacardType metacardType = metacardTypeAdpater.unmarshal(KNOWN_TYPE_NAME);
        assertThat(metacardType.getName(), is(knownMetacardType.getName()));
    }
}
