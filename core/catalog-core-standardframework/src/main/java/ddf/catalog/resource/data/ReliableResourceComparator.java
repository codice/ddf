/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.resource.data;

import java.util.Comparator;
import java.util.Map;


@SuppressWarnings("rawtypes")  //suppressing these because Hazelcast specifically requires a Comparator<Map.Entry>
public class ReliableResourceComparator implements Comparator<Map.Entry> {

    @Override
    public int compare(Map.Entry rr1, Map.Entry rr2) {
        ReliableResource mapEntry1 = (ReliableResource) rr1.getValue();
        ReliableResource mapEntry2 = (ReliableResource) rr2.getValue();
        return (mapEntry1.getLastTouchedMillis() < mapEntry2.getLastTouchedMillis()) ? -1 : ((mapEntry1.getLastTouchedMillis() > mapEntry2.getLastTouchedMillis()) ? 1:0);
    }

}
