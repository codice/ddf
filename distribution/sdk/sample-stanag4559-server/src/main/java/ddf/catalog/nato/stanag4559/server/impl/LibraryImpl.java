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
package ddf.catalog.nato.stanag4559.server.impl;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import ddf.catalog.nato.stanag4559.common.GIAS.AccessCriteria;
import ddf.catalog.nato.stanag4559.common.GIAS.CatalogMgrHelper;
import ddf.catalog.nato.stanag4559.common.GIAS.DataModelMgrHelper;
import ddf.catalog.nato.stanag4559.common.GIAS.LibraryDescription;
import ddf.catalog.nato.stanag4559.common.GIAS.LibraryManager;
import ddf.catalog.nato.stanag4559.common.GIAS.LibraryManagerHelper;
import ddf.catalog.nato.stanag4559.common.GIAS.LibraryPOA;
import ddf.catalog.nato.stanag4559.common.GIAS.OrderMgrHelper;
import ddf.catalog.nato.stanag4559.common.GIAS.ProductMgrHelper;
import ddf.catalog.nato.stanag4559.common.UCO.InvalidInputParameter;
import ddf.catalog.nato.stanag4559.common.UCO.ProcessingFault;
import ddf.catalog.nato.stanag4559.common.UCO.SystemFault;
import ddf.catalog.nato.stanag4559.common.UCO.exception_details;
import ddf.catalog.nato.stanag4559.server.impl.managers.OrderMgrImpl;

import ddf.catalog.nato.stanag4559.server.impl.managers.CatalogMgrImpl;
import ddf.catalog.nato.stanag4559.server.impl.managers.DataModelMgrImpl;
import ddf.catalog.nato.stanag4559.server.impl.managers.ProductMgrImpl;
import org.omg.CORBA.NO_IMPLEMENT;
import org.omg.PortableServer.POA;

public class LibraryImpl extends LibraryPOA {

    private LibraryDescription libraryDescription = new LibraryDescription("localhost",
            "US|AFRL/IFEC",
            "NSILI|1.0");

    private List<String> manager = Arrays.asList("OrderMgr",
            "CatalogMgr",
            "ProductMgr",
            "DataModelMgr"
            /* Optional :
            "QueryOrderMgr",
            "StandingQueryMgr",
            "CreationMgr",
            "UpdateMgr" */);

    private static final String ENCODING = "UTF-8";

    private POA poa_;

    public LibraryImpl(POA poa_) {
        this.poa_ = poa_;
    }

    @Override
    public String[] get_manager_types() throws ProcessingFault, SystemFault {
        return (String[]) manager.toArray();
    }

    @Override
    public LibraryManager get_manager(String manager_type, AccessCriteria access_criteria)
            throws ProcessingFault, InvalidInputParameter, SystemFault {

        org.omg.CORBA.Object obj;

        switch (manager_type) {
        case "CatalogMgr":
            CatalogMgrImpl catalogMgr = new CatalogMgrImpl(poa_);
            try {
                poa_.activate_object_with_id(manager_type.getBytes(Charset.forName(ENCODING)), catalogMgr);
            } catch (Exception e) {
                // Ignore
            }

            obj = poa_.create_reference_with_id(manager_type.getBytes(Charset.forName(ENCODING)), CatalogMgrHelper.id());
            break;

        case "OrderMgr":
            OrderMgrImpl orderMgr = new OrderMgrImpl();
            try {
                poa_.activate_object_with_id(manager_type.getBytes(Charset.forName(ENCODING)), orderMgr);
            } catch (Exception e) {
                // Ignore
            }

            obj = poa_.create_reference_with_id(manager_type.getBytes(Charset.forName(ENCODING)), OrderMgrHelper.id());
            break;

        case "ProductMgr":
            ProductMgrImpl productMgr = new ProductMgrImpl();
            try {
                poa_.activate_object_with_id(manager_type.getBytes(Charset.forName(ENCODING)), productMgr);
            } catch (Exception e) {
                // Ignore
            }

            obj = poa_.create_reference_with_id(manager_type.getBytes(Charset.forName(ENCODING)), ProductMgrHelper.id());
            break;

        case "DataModelMgr":
            DataModelMgrImpl dataModelMgr = new DataModelMgrImpl();
            try {
                poa_.activate_object_with_id(manager_type.getBytes(Charset.forName(ENCODING)), dataModelMgr);
            } catch (Exception e) {
                // Ignore
            }

            obj = poa_.create_reference_with_id(manager_type.getBytes(Charset.forName(ENCODING)), DataModelMgrHelper.id());
            break;

        default:
            String[] bad_params = {manager_type};
            throw new InvalidInputParameter("UnknownMangerType", new exception_details(
                    "UnknownMangerType",
                    true,
                    manager_type), bad_params);

        }

        LibraryManager libraryManager = LibraryManagerHelper.narrow(obj);
        return libraryManager;
    }

    @Override
    public LibraryDescription get_library_description()
            throws ProcessingFault, SystemFault {
        return libraryDescription;
    }

    @Override
    public LibraryDescription[] get_other_libraries(AccessCriteria access_criteria)
            throws ProcessingFault, InvalidInputParameter, SystemFault {
        throw new NO_IMPLEMENT();
    }

}
