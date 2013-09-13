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
package ddf.catalog.pubsub;

import ddf.catalog.data.Metacard;
import ddf.catalog.event.DeliveryMethod;

public class MockDeliveryMethod implements DeliveryMethod {
    @Override
    public void created(Metacard metacard) {
    }

    @Override
    public void deleted(Metacard metacard) {
    }

    @Override
    public void updatedHit(Metacard newMetacard, Metacard oldMetacard) {
        // TODO Auto-generated method stub

    }

    @Override
    public void updatedMiss(Metacard newMetacard, Metacard oldMetacard) {
        // TODO Auto-generated method stub

    }

}