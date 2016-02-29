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
package org.codice.ddf.stanag4559.server.client;

import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.PackageElement;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.GIAS.Query;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UCO.DAG;
import org.codice.ddf.spatial.ogc.stanag4559.catalog.common.UID.Product;

public class Client {

    public static void main(String args[]) throws Exception {

        if (args.length != 1) {
            System.out.println("Unable to obtain IOR File :  No port specified.");
        }

        int port = Integer.parseInt(args[0]);

        org.omg.CORBA.ORB orb = org.omg.CORBA.ORB.init(args, null);

        StanagClient stanagClient = new StanagClient(orb);

        // Get IOR File
        String iorFile = stanagClient.getIorTextFile(port);

        // Initialize Corba Library
        stanagClient.initLibrary(iorFile);

        // Get the Managers from the Library
        stanagClient.getManagerTypes();
        stanagClient.initManagers();

        // CatalogMgr
        Query query = new Query("NSIL_ALL_VIEW", "");
        stanagClient.getHitCount(query);
        DAG[] results = stanagClient.submit_query(query);
        stanagClient.processAndPrintResults(results);

        // OrderMgr
        stanagClient.validate_order(orb);
        PackageElement[] packageElements = stanagClient.order(orb);

        // ProductMgr
        // For each packageElement in the order response, get the parameters and
        // related files for the product.
        for(PackageElement packageElement : packageElements) {
            Product product = packageElement.prod;
            stanagClient.get_parameters(orb, product);
            stanagClient.get_related_file_types(product);
            stanagClient.get_related_files(orb, product);
        }

        orb.shutdown(true);
        System.out.println("Done. ");
        System.exit(0);
    }
}
