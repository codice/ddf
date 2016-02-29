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
package org.codice.ddf.stanag4559.server.impl.requests;

import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.CB.Callback;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.DelayEstimate;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.DeliveryManifestHolder;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.OrderRequestPOA;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.RequestManager;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS._RequestManagerStub;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.InvalidInputParameter;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.ProcessingFault;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.RequestDescription;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.State;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.Status;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.SystemFault;
import org.codice.ddf.stanag4559.server.data.DeliveryManifestGenerator;

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

