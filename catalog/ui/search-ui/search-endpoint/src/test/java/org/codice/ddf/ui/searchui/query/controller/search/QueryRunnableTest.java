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
package org.codice.ddf.ui.searchui.query.controller.search;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.junit.Test;

public class QueryRunnableTest {
    @Test
    public void getShape() throws Exception {
        assertThat(QueryRunnable.getShape(
                "GEOMETRYCOLLECTION (MULTIPOLYGON (((56 9, 64 9, 60 14, 56 9)), ((61 9, 69 9, 65 14, 61 9)), ((51 9, 59 9, 55 14, 51 9))), LINESTRING (50 8, 50 15, 70 15, 70 8, 50 8), MULTIPOINT ((62.5 14), (67.5 14), (57.5 14), (52.5 14)))"),
                notNullValue());
    }

}