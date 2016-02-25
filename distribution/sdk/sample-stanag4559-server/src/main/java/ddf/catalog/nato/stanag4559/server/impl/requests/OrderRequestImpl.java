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
package ddf.catalog.nato.stanag4559.server.impl.requests;

import ddf.catalog.nato.stanag4559.common.CB.Callback;
import ddf.catalog.nato.stanag4559.common.GIAS.DelayEstimate;
import ddf.catalog.nato.stanag4559.common.GIAS.DeliveryManifestHolder;
import ddf.catalog.nato.stanag4559.common.GIAS.OrderRequestPOA;
import ddf.catalog.nato.stanag4559.common.GIAS.RequestManager;
import ddf.catalog.nato.stanag4559.common.GIAS._RequestManagerStub;
import ddf.catalog.nato.stanag4559.common.UCO.InvalidInputParameter;
import ddf.catalog.nato.stanag4559.common.UCO.ProcessingFault;
import ddf.catalog.nato.stanag4559.common.UCO.RequestDescription;
import ddf.catalog.nato.stanag4559.common.UCO.State;
import ddf.catalog.nato.stanag4559.common.UCO.Status;
import ddf.catalog.nato.stanag4559.common.UCO.SystemFault;
import ddf.catalog.nato.stanag4559.server.data.DeliveryManifestGenerator;

public class OrderRequestImpl extends OrderRequestPOA {

    @Override
    public State complete(DeliveryManifestHolder prods) throws ProcessingFault, SystemFault {
        prods.value = DeliveryManifestGenerator.getManifest();
        return State.COMPLETED;
    }

    @Override
    public RequestDescription get_request_description()
            throws ProcessingFault, SystemFault {
        return new RequestDescription();
    }

    @Override
    public void set_user_info(String message)
            throws InvalidInputParameter, ProcessingFault, SystemFault {
        return;
    }

    @Override
    public Status get_status() throws ProcessingFault, SystemFault {
        return new Status();
    }

    @Override
    public DelayEstimate get_remaining_delay() throws ProcessingFault, SystemFault {
        return new DelayEstimate();
    }

    @Override
    public void cancel() throws ProcessingFault, SystemFault {
        return;
    }

    @Override
    public String register_callback(Callback acallback)
            throws InvalidInputParameter, ProcessingFault, SystemFault {
        return "";
    }

    @Override
    public void free_callback(String id)
            throws InvalidInputParameter, ProcessingFault, SystemFault {
        return;
    }

    @Override
    public RequestManager get_request_manager() throws ProcessingFault, SystemFault {
        return new _RequestManagerStub();
    }
}

