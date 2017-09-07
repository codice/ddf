/*
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package org.codice.ddf.spatial.kml.transformer;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.data.impl.types.AssociationsAttributes;
import ddf.catalog.data.impl.types.ContactAttributes;
import ddf.catalog.data.impl.types.CoreAttributes;
import ddf.catalog.data.impl.types.DateTimeAttributes;
import ddf.catalog.data.impl.types.LocationAttributes;
import ddf.catalog.data.impl.types.ValidationAttributes;

public class TestKmzInputTransformer {

    private KmzInputTransformer kmzInputTransformer;
    private KmlInputTransformer kmlInputTransformer;

    private static List<MetacardType> metacardTypes;

    static {
        metacardTypes = new ArrayList<>();
        metacardTypes.add(new AssociationsAttributes());
        metacardTypes.add(new ContactAttributes());
        metacardTypes.add(new CoreAttributes());
        metacardTypes.add(new DateTimeAttributes());
        metacardTypes.add(new LocationAttributes());
        metacardTypes.add(new ValidationAttributes());
    }

    @Before
    public void setUp() {
        kmlInputTransformer = new KmlInputTransformer(new MetacardTypeImpl("kmzMetacardType",
                metacardTypes));

        kmzInputTransformer = new KmzInputTransformer(kmlInputTransformer);
    }

    @Test
    public void testTransformPointKmz() throws Exception {
        InputStream stream = TestKmzInputTransformer.class.getResourceAsStream("/point.kmz");

        Metacard metacard = kmzInputTransformer.transform(stream);
        assertThat(metacard, is(notNullValue()));
    }
}
