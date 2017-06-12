/**
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
 */
package ddf.catalog.util.impl;

import org.junit.Test;

// TODO: 6/12/17 Fix `expects` part of each unit test method name to match eventual assert
public class QueryResultPaginatorTest {

    @Test
    /**
     * Only has 100 remaining items to retrieve, but request size is 500
     * Assert only returns 100
     */
    public void testRequest500Only100RemaingExpectsReturn100() {

    }

    @Test
    public void testRequest500Get500ExpectSuccess() {

    }

    @Test
    /**
     * Assert that more than one call is made to fulfill request
     */
    public void testRequest500GetLessExpectAnotherCall() {

    }

    @Test
    public void testPaginatorStartIndexZeroThrowsException() {

    }

    @Test
    public void testPaginatorMaxRequestSizeReachedExpectCompleted() {

    }

    @Test
    public void testPaginatorStartIndexGreaterThanCeilingValueThrowsException() {

    }

}