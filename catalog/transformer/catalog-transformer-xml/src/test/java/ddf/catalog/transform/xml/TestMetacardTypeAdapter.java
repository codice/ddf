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
package ddf.catalog.transform.xml;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static ddf.catalog.data.impl.BasicTypes.BASIC_METACARD;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.MetacardType;
import ddf.catalog.data.MetacardTypeRegistry;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.transform.CatalogTransformerException;
import ddf.catalog.transformer.xml.adapter.MetacardTypeAdapter;

public class TestMetacardTypeAdapter {
    private static final String NULL_TYPE_NAME = null;

    private static final String EMPTY_TYPE_NAME = "";

    private static final String DDF_METACARD_TYPE_NAME = BASIC_METACARD.getName();

    private static final String UNKNOWN_TYPE_NAME = "unknownTypeName.metacard";

    private static final String KNOWN_TYPE_NAME = "knownTypeName.metacard";

    private MetacardTypeAdapter metacardTypeAdapter;

    private MetacardTypeRegistry registry;

    @Before
    public void setUp() {
        registry = mock(MetacardTypeRegistry.class);

        metacardTypeAdapter = new MetacardTypeAdapter(registry);
    }

    private void setUpRegistry(Set<String> metacardTypeNames) {
        when(registry.lookup(anyString())).thenAnswer(invocationOnMock -> {
            String metacardTypeName = (String) invocationOnMock.getArguments()[0];
            if (metacardTypeNames.contains(metacardTypeName)) {
                return Optional.of(new MetacardTypeImpl(metacardTypeName,
                        BASIC_METACARD.getAttributeDescriptors()));
            }
            return Optional.empty();
        });
    }

    @Test
    public void testUnmarshalWithNullTypeName() throws CatalogTransformerException {
        setUpRegistry(Collections.singleton(BASIC_METACARD.getName()));
        MetacardType metacardType = metacardTypeAdapter.unmarshal(NULL_TYPE_NAME);
        assertThat(metacardType.getName(), is(BASIC_METACARD.getName()));
    }

    @Test
    public void testUnmarshalWithEmptyTypeName() throws CatalogTransformerException {
        setUpRegistry(Collections.singleton(BASIC_METACARD.getName()));
        MetacardType metacardType = metacardTypeAdapter.unmarshal(EMPTY_TYPE_NAME);
        assertThat(metacardType.getName(), is(BASIC_METACARD.getName()));
    }

    @Test(expected = CatalogTransformerException.class)
    public void testUnmarshalWithUnknownTypeName() throws CatalogTransformerException {
        setUpRegistry(Collections.emptySet());
        metacardTypeAdapter.unmarshal(UNKNOWN_TYPE_NAME);
    }

    @Test
    public void testUnmarshalWithKnownMetacardType() throws CatalogTransformerException {
        setUpRegistry(Collections.singleton(KNOWN_TYPE_NAME));
        MetacardType metacardType = metacardTypeAdapter.unmarshal(KNOWN_TYPE_NAME);
        assertThat(metacardType.getName(), is(KNOWN_TYPE_NAME));
    }
}
