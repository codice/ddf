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
package ddf.catalog.validation.impl;

import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.Sets;

import ddf.catalog.validation.AttributeValidatorRegistry;
import ddf.catalog.validation.impl.validator.PastDateValidator;
import ddf.catalog.validation.impl.validator.PatternValidator;
import ddf.catalog.validation.impl.validator.SizeValidator;

public class AttributeValidatorRegistryImplTest {
    private AttributeValidatorRegistry registry;

    @Before
    public void setup() {
        registry = new AttributeValidatorRegistryImpl();
    }

    @Test
    public void testRegisterValidators() {
        registry.registerValidators("title",
                Sets.newHashSet(new SizeValidator(1, 10), new PatternValidator("\\d+")));
        assertThat(registry.getValidators("title"), hasSize(2));

        registry.registerValidators("title", Sets.newHashSet(PastDateValidator.getInstance()));
        assertThat(registry.getValidators("title"), hasSize(3));
    }

    @Test
    public void testDeregisterValidators() {
        registry.registerValidators("title",
                Sets.newHashSet(new SizeValidator(1, 10), new PatternValidator("\\d+")));
        assertThat(registry.getValidators("title"), hasSize(2));

        registry.deregisterValidators("title");
        assertThat(registry.getValidators("title"), empty());
    }

    @Test
    public void testRegisterValidatorsForMultipleAttributes() {
        registry.registerValidators("title",
                Sets.newHashSet(new SizeValidator(1, 10), new PatternValidator("\\d+")));
        registry.registerValidators("modified", Sets.newHashSet(PastDateValidator.getInstance()));
        assertThat(registry.getValidators("title"), hasSize(2));
        assertThat(registry.getValidators("modified"), hasSize(1));
    }

    @Test
    public void testRegisterDuplicateValidator() {
        registry.registerValidators("title", Sets.newHashSet(new SizeValidator(1, 10)));
        assertThat(registry.getValidators("title"), hasSize(1));

        registry.registerValidators("title", Sets.newHashSet(new SizeValidator(1, 10)));
        assertThat(registry.getValidators("title"), hasSize(1));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testNullAttributeName() {
        registry.registerValidators(null, Sets.newHashSet(PastDateValidator.getInstance()));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testRegisterNoValidators() {
        registry.registerValidators("title", Collections.emptySet());
    }
}
