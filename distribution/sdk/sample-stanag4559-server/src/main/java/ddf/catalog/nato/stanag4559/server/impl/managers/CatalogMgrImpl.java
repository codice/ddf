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
package ddf.catalog.nato.stanag4559.server.impl.managers;

import java.nio.charset.Charset;

import ddf.catalog.nato.stanag4559.common.GIAS.CatalogMgrPOA;
import ddf.catalog.nato.stanag4559.common.GIAS.HitCountRequest;
import ddf.catalog.nato.stanag4559.common.GIAS.HitCountRequestHelper;
import ddf.catalog.nato.stanag4559.common.GIAS.Library;
import ddf.catalog.nato.stanag4559.common.GIAS.Query;
import ddf.catalog.nato.stanag4559.common.GIAS.Request;
import ddf.catalog.nato.stanag4559.common.GIAS.SortAttribute;
import ddf.catalog.nato.stanag4559.common.GIAS.SubmitQueryRequest;
import ddf.catalog.nato.stanag4559.common.GIAS.SubmitQueryRequestHelper;
import ddf.catalog.nato.stanag4559.common.UCO.InvalidInputParameter;
import ddf.catalog.nato.stanag4559.common.UCO.NameValue;
import ddf.catalog.nato.stanag4559.common.UCO.ProcessingFault;
import ddf.catalog.nato.stanag4559.common.UCO.SystemFault;
import ddf.catalog.nato.stanag4559.server.impl.requests.HitCountRequestImpl;
import ddf.catalog.nato.stanag4559.server.impl.requests.SubmitQueryRequestImpl;

import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.PortableServer.POA;
import org.omg.PortableServer.POAPackage.ObjectAlreadyActive;
import org.omg.PortableServer.POAPackage.ServantAlreadyActive;
import org.omg.PortableServer.POAPackage.WrongPolicy;

public class CatalogMgrImpl extends CatalogMgrPOA {

    private static final String ENCODING = "UTF-8";

    private static final int DEFAULT_TIMEOUT = 1;

    private POA poa_;

    public CatalogMgrImpl(POA poa) {
        this.poa_ = poa;
    }

    @Override
    public Request[] get_active_requests() throws ProcessingFault, SystemFault {
        return new Request[0];
    }

    @Override
    public int get_default_timeout() throws ProcessingFault, SystemFault {
        return DEFAULT_TIMEOUT;
    }

    @Override
    public void set_default_timeout(int new_default)
            throws ProcessingFault, InvalidInputParameter, SystemFault {
        return;
    }

    @Override
    public int get_timeout(Request aRequest)
            throws ProcessingFault, InvalidInputParameter, SystemFault {
        return DEFAULT_TIMEOUT;
    }

    @Override
    public void set_timeout(Request aRequest, int new_lifetime)
            throws ProcessingFault, InvalidInputParameter, SystemFault {
        return;
    }

    @Override
    public void delete_request(Request aRequest)
            throws ProcessingFault, InvalidInputParameter, SystemFault {
        return;
    }

    @Override
    public SubmitQueryRequest submit_query(Query aQuery, String[] result_attributes,
            SortAttribute[] sort_attributes, NameValue[] properties)
            throws ProcessingFault, InvalidInputParameter, SystemFault {

        SubmitQueryRequestImpl submitQueryRequest = new SubmitQueryRequestImpl();

        try {
            poa_.activate_object_with_id("submit_query".getBytes(Charset.forName(ENCODING)), submitQueryRequest);
        } catch (ServantAlreadyActive | ObjectAlreadyActive | WrongPolicy e) {
            System.out.println("submit_query : Unable to activate submitQueryRequest object.");
        }

        org.omg.CORBA.Object obj = poa_.create_reference_with_id("submit_query".getBytes(Charset.forName(ENCODING)),
                SubmitQueryRequestHelper.id());
        SubmitQueryRequest queryRequest = SubmitQueryRequestHelper.narrow(obj);

        return queryRequest;
    }

    @Override
    public HitCountRequest hit_count(Query aQuery, NameValue[] properties)
            throws ProcessingFault, InvalidInputParameter, SystemFault {
        HitCountRequestImpl hitCountRequest = new HitCountRequestImpl();

        try {
            poa_.activate_object_with_id("hit_count".getBytes(Charset.forName(ENCODING)), hitCountRequest);
        } catch (ServantAlreadyActive | ObjectAlreadyActive | WrongPolicy e) {
            System.out.println("hit_count : Unable to activate hitCountRequest object.");
        }

        org.omg.CORBA.Object obj = poa_.create_reference_with_id("hit_count".getBytes(Charset.forName(ENCODING)),
                HitCountRequestHelper.id());
        HitCountRequest queryRequest = HitCountRequestHelper.narrow(obj);

        return queryRequest;
    }

    // LibraryMgr
    @Override
    public String[] get_property_names() throws ProcessingFault, SystemFault {
        throw new NO_IMPLEMENT();
    }

    @Override
    public NameValue[] get_property_values(String[] desired_properties)
            throws ProcessingFault, InvalidInputParameter, SystemFault {
        throw new NO_IMPLEMENT();
    }

    @Override
    public Library[] get_libraries() throws ProcessingFault, SystemFault {
        throw new NO_IMPLEMENT();
    }
}
