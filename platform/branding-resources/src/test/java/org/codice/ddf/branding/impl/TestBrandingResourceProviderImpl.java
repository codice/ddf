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
package org.codice.ddf.branding.impl;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;

import org.junit.Test;
import org.osgi.framework.Bundle;

public class TestBrandingResourceProviderImpl {

    public static final String TEST_PATH = "asdf";

    private class TestImpl extends BrandingResourceProviderImpl {
        private final Bundle bundle;

        private TestImpl(Bundle bundle) {
            this.bundle = bundle;
        }

        @Override
        Bundle getBundle(Class<?> aClass) {
            return this.bundle;
        }
    }

    @Test
    public void testEmptyArray() throws IOException {
        TestImpl impl = new TestImpl(mock(Bundle.class));
        assertThat(impl.getResourceAsBytes("asdf").length, is(equalTo(0)));
    }

    @Test
    public void testFullArray() throws IOException {
        Bundle bundle = mock(Bundle.class);
        URL url = this.getClass()
                .getResource("/logo.png");
        when(bundle.getEntry(TEST_PATH)).thenReturn(url);
        TestImpl impl = new TestImpl(bundle);
        assertThat(impl.getResourceAsBytes(TEST_PATH).length, is(equalTo(22490)));
    }
}
