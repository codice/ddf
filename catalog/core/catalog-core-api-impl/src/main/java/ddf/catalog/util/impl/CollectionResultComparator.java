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
package ddf.catalog.util.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ddf.catalog.data.Result;

public class CollectionResultComparator implements Comparator<Result>, Serializable {

    private List<Comparator<Result>> comparators = new ArrayList<>();

    public void addComparator(Comparator<Result> resultComparator) {
        comparators.add(resultComparator);
    }

    @Override
    public int compare(Result o1, Result o2) {
        int result = -1;
        for (Comparator<Result> comparator : comparators) {
            result = comparator.compare(o1, o2);
            if (result != 0) {
                break;
            }
        }

        return result;
    }
}
