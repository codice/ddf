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
 **/
package ddf.catalog.content.data.impl;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.junit.Test;

import ddf.catalog.content.data.ContentItem;

public class ContentItemValidatorTest {
    
    private static final String ILLEGAL_QUALIFIER = "bad.txt";

    private static final String VALID_FILENAME = "good.txt";

    private static final String VALID_QUALIFIER = "good-qualifier";

    @Test
    public void testValidFilename() throws Exception {
        ContentItem item = new ContentItemImpl(null, "", VALID_FILENAME, null);
        ContentItemValidator.validate(item);
        assertThat(item.getFilename(), is(VALID_FILENAME));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInvalidQualifier() throws Exception {
        ContentItemValidator.validate(new ContentItemImpl("", ILLEGAL_QUALIFIER, null, "", null));
    }

    @Test
    public void testValidQualifier() throws Exception {
        ContentItem item = new ContentItemImpl("123456789", VALID_QUALIFIER, null, "", null);
        ContentItemValidator.validate(item);
        assertThat(item.getQualifier(), is(VALID_QUALIFIER));
    }
}