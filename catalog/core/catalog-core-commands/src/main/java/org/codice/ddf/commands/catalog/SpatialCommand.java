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
package org.codice.ddf.commands.catalog;

import java.io.PrintStream;

import org.apache.felix.gogo.commands.Argument;
import org.apache.felix.gogo.commands.Command;
import org.apache.felix.gogo.commands.Option;

import org.codice.ddf.commands.catalog.facade.CatalogFacade;
import org.codice.ddf.commands.util.SpatialOperations;

@Command(scope = CatalogCommands.NAMESPACE, name = "spatial", description = "Searches spatially the catalog provider.")
public class SpatialCommand extends CatalogCommands {

    private static final String ID = "ID ";

    private static final String TITLE = "Title ";

    private static final String DATE = "Date ";

    private static final int MAX_LENGTH = 30;

    private static final String FORMAT = "%1$-33s %2$-26s %3$-" + MAX_LENGTH + "s %4$-50s%n";

    private static final Object WKT = "WKT";

    @Argument(name = "Operation", description = "An operation from the set {CONTAINS,INTERSECTS,EQUALS,DISJOINT,TOUCHES,CROSSES,WITHIN,OVERLAPS,RADIUS,NN}", index = 0, multiValued = false, required = true)
    String operation = null;

    @Argument(name = "PointX", description = "X coordinate of point of reference", index = 1, multiValued = false, required = true)
    String pointX = null;

    @Argument(name = "PointY", description = "Y coordinate of point of reference", index = 2, multiValued = false, required = true)
    String pointY = null;

    @Argument(name = "Radius", description = "Radius for a Point-Radius search {RADIUS}", index = 3, multiValued = false, required = false)
    String radius = "10000";

    @Option(name = "items-returned", required = false, aliases = {"-n"}, multiValued = false, description = "Number of the items returned.")
    int numberOfItems = DEFAULT_NUMBER_OF_ITEMS;

    @Override
    protected Object doExecute() throws Exception {
        PrintStream console = System.out;

        CatalogFacade catalogProvider = getCatalog();

        switch (SpatialOperations.valueOf(operation.toUpperCase())) {
        case RADIUS:
            doRadiusQuery(console, catalogProvider);
            break;

        case NN:
            doNNQuery(console, catalogProvider);
            break;

        default:
            doOperationsQuery(console, catalogProvider);
        }

        return null;
    }

    protected void doRadiusQuery(PrintStream console, CatalogFacade catalogFacade) // throws
                                                                                   // CatalogException
    {
        // SpatialDistanceCriterion criterion = new SpatialDistanceCriterion() {
        //
        // public String getGeometryAsWKT() {
        // return "POINT(" + pointX + " " + pointY+ ")";
        // }
        //
        // public CriterionType getType() {
        // return CriterionType.SPATIAL_DISTANCE;
        // }
        //
        // public double getDistanceInMeters() {
        // return new Double(radius).doubleValue();
        // }
        // };
        //
        //
        // executeQuery(console, catalogProvider, criterion);
    }

    protected void doNNQuery(PrintStream console, CatalogFacade catalogFacade) // throws
                                                                               // CatalogException
    {
        // SpatialNearestNeighborCriterion criterion = new SpatialNearestNeighborCriterion() {

        // public CriterionType getType() {
        // return CriterionType.SPATIAL_NEAREST_NEIGHBOR;
        // }
        //
        // public String getGeometryAsWKT() {
        // return "POINT(" + pointX + " " + pointY+ ")";
        // }
        // };
        //
        //
        // executeQuery(console, catalogProvider, criterion);
    }

    private void doOperationsQuery(PrintStream console, CatalogFacade catalogFacade) // throws
                                                                                     // CatalogException
    {
        // SpatialOperationCriterion criterion = new SpatialOperationCriterion() {
        //
        // public String getGeometryAsWKT() {
        // return "POINT(" + pointX + " " + pointY+ ")";
        // }
        //
        // public CriterionType getType() {
        //
        // return CriterionType.SPATIAL_OPERATION;
        // }
        // public Operation getOperation() {
        //
        // return Operation.valueOf(operation.toUpperCase());
        // }
        // } ;
        //
        // executeQuery(console, catalogProvider, criterion);
    }

    // protected void executeQuery(PrintStream console,
    // CatalogProvider catalogProvider, Criterion criterion )
    // throws CatalogException
    // {
    // TermImpl filterNodeTerm = new TermImpl(null, criterion, true, null);
    //
    // SortPolicyImpl sortPolicy = new SortPolicyImpl(true, Constants.MTS_SORT_QUALIFIER,
    // Constants.SORT_POLICY_VALUE_DISTANCE, Order.ASCENDING) ;
    //
    // QueryImpl query = new QueryImpl(filterNodeTerm, null, 0, numberOfItems,sortPolicy, null,
    // null, true, 0) ;
    //
    // long start = System.currentTimeMillis() ;
    // BlockingQueue<Response<Result>> responseQ = catalogProvider
    // .query(query);
    // long end = System.currentTimeMillis() ;
    //
    // printToConsole(console, responseQ, start, end);
    // }

    // protected void printToConsole(PrintStream console,
    // BlockingQueue<Response<Result>> responseQ, long start, long end) {
    // console.println() ;
    // console.printf("%s %d%s result(s) in %3.3f seconds"
    // ,Ansi.ansi().fg(Ansi.Color.CYAN).toString(),(responseQ.size()-1),Ansi.ansi().reset().toString(),(end-start)/1000.0);
    // console.printf(FORMAT, "", "", "", "");
    // console.print(Ansi.ansi().fg(Ansi.Color.CYAN).toString()) ;
    // console.printf(FORMAT, ID, DATE, TITLE, WKT);
    // console.print(Ansi.ansi().reset().toString()) ;
    // while (!responseQ.peek().isTerminator()) {
    // Result result = responseQ.poll().getContent();
    //
    // String title = "" ;
    // String postedDate = "" ;
    // String wkt = "" ;
    //
    // if(result.getTitles() != null && !result.getTitles().isEmpty()) {
    // for(String item : result.getTitles()) {
    // title = item;
    // break;
    // }
    // }
    //
    // if(result.getGeospatialCoverageWKTs() != null &&
    // !result.getGeospatialCoverageWKTs().isEmpty()) {
    // for(String item : result.getGeospatialCoverageWKTs()) {
    // wkt = item ;
    // break;
    // }
    // }
    //
    // if(result.getPostedDate() != null ) {
    // postedDate = new DateTime(result.getPostedDate().getTime()).toString(fmt) ;
    // }
    //
    // console.printf(
    // FORMAT, result.getMetacardId(),
    // postedDate,
    // title.substring(0, Math.min(title.length(), MAX_LENGTH)),
    // wkt.substring(0, Math.min(wkt.length(), MAX_LENGTH+20))) ;
    // }
    // }

}
