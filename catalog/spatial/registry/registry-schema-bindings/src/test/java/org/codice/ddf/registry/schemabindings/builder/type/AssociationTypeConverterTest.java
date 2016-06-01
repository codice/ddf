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
package org.codice.ddf.registry.schemabindings.builder.type;

import static org.codice.ddf.registry.schemabindings.converter.web.AssociationWebConverter.ASSOCIATION_TYPE;
import static org.codice.ddf.registry.schemabindings.converter.web.AssociationWebConverter.SOURCE_OBJECT;
import static org.codice.ddf.registry.schemabindings.converter.web.AssociationWebConverter.TARGET_OBJECT;
import static org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectWebConverter.HOME_KEY;
import static org.codice.ddf.registry.schemabindings.converter.web.RegistryObjectWebConverter.ID_KEY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsNull.notNullValue;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.codice.ddf.registry.schemabindings.converter.type.AssociationTypeConverter;
import org.junit.Before;
import org.junit.Test;

import oasis.names.tc.ebxml_regrep.xsd.rim._3.AssociationType1;

public class AssociationTypeConverterTest {

    private Map<String, Object> associationMap = new HashMap<>();

    @Before
    public void setup() {
        associationMap.put(ID_KEY, "ID");
        associationMap.put(HOME_KEY, "HOME");
        associationMap.put(ASSOCIATION_TYPE, "ASSOCIATIONTYPE");
        associationMap.put(SOURCE_OBJECT, "SOURCEOBJECT");
        associationMap.put(TARGET_OBJECT, "TARGETOBJECT");
    }

    @Test
    public void testBuildAssociation() throws Exception {
        AssociationTypeConverter atBuilder = new AssociationTypeConverter();

        Optional<AssociationType1> optionalAssociation = atBuilder.convert(associationMap);
        assertThat(optionalAssociation, notNullValue());
    }

}
