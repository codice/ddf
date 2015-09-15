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
package org.codice.security.policy.context.attributes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.codice.ddf.security.policy.context.attributes.DefaultContextAttributeMapping;
import org.junit.Before;
import org.junit.Test;

import junit.framework.Assert;

import ddf.security.permission.KeyValueCollectionPermission;
import ddf.security.permission.KeyValuePermission;

/**
 * Test for the default ContextAttributeMapping implementation.
 */
public class DefaultContextAttributeMappingTest {

    KeyValueCollectionPermission userPermissions;

    DefaultContextAttributeMapping roleMapping;

    DefaultContextAttributeMapping roleMapping2;

    DefaultContextAttributeMapping controlsMapping;

    DefaultContextAttributeMapping controlMapping;

    @Before
    public void setup() {
        List<KeyValuePermission> userPerms = new ArrayList<KeyValuePermission>();
        userPerms.add(new KeyValuePermission("role", Arrays.asList("admin")));
        userPerms.add(new KeyValuePermission("controls", Arrays.asList("Foo", "Bar")));
        userPerms.add(new KeyValuePermission("control", Arrays.asList("Foo")));
        userPermissions = new KeyValueCollectionPermission("context", userPerms);

        roleMapping = new DefaultContextAttributeMapping("context", "role", "admin");
        roleMapping2 = new DefaultContextAttributeMapping("context", "role", "charlie");

        controlsMapping = new DefaultContextAttributeMapping("context", "controls", "Foo");
        controlMapping = new DefaultContextAttributeMapping("context", "control", "Bar");
    }

    @Test
    public void testIsPermitted() {
        boolean roleImply = userPermissions.implies(roleMapping.getAttributePermission());

        boolean roleImply2 = userPermissions.implies(roleMapping2.getAttributePermission());

        boolean controlsImply = userPermissions.implies(controlsMapping.getAttributePermission());

        boolean controlImply = userPermissions.implies(controlMapping.getAttributePermission());

        Assert.assertEquals(true, roleImply);

        Assert.assertEquals(false, roleImply2);

        Assert.assertEquals(true, controlsImply);

        Assert.assertEquals(false, controlImply);
    }
}
