/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free Software Foundation, either
 * version 3 of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package ddf.catalog.federation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.data.Metacard;
import ddf.catalog.data.Result;
import ddf.catalog.federation.AbstractFederationStrategy;
import ddf.catalog.operation.ProcessingDetails;
import ddf.catalog.operation.ProcessingDetailsImpl;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryResponseImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.source.Source;
import ddf.catalog.util.DistanceResultComparator;
import ddf.catalog.util.RelevanceResultComparator;
import ddf.catalog.util.TemporalResultComparator;

/**
 * This class represents a {@link FederationStrategy} based on sorting {@link Metacard}s. The sorting is based 
 * on the {@link Query}'s {@link SortBy} propertyName. The possible sorting values are
 * {@link Metacard.EFFECTIVE}, {@link Result.TEMPORAL}, {@link Result.DISTANCE}, or {@link Result.RELEVANCE}.
 * The supported ordering includes {@link SortOrder.DESCENDING} and {@link SortOrder.ASCENDING}. For this class to function 
 * properly a sort value and sort order must be provided.
 * 
 * @author ddf.isgs@lmco.com
 *  
 * @see Metacard
 * @see Query
 * @see SortBy
 * @deprecated - As of DDF v2.1.0.ALPHA1. SortedFederationStrategy has been moved to /ddf/trunk/catalog/federation, federation-impl is now a separate bundle containing federation strategy implementations
 */
public class SortedFederationStrategy extends AbstractFederationStrategy {
   
    /** The default comparator for sorting by {@link Result.RELEVANCE}, {@link SortOrder.DESCENDING} */ 
    protected static final Comparator<Result> DEFAULT_COMPARATOR = new RelevanceResultComparator( SortOrder.DESCENDING );

    /** The default comparator for sorting by {@link Result.RELEVANCE}, {@link SortOrder.DESCENDING} 
     * @deprecated*/ 
    protected static final Comparator<Result> defaultComparator = new RelevanceResultComparator( SortOrder.DESCENDING );
    
    private static XLogger logger = new XLogger( LoggerFactory.getLogger( SortedFederationStrategy.class ) );

    
    /**
     * Instantiates a {@code SortedFederationStrategy} with the provided {@link ExecutorService}.
     * 
     * @param queryExecutorService the {@link ExecutorService} for queries
     */
    public SortedFederationStrategy( ExecutorService queryExecutorService ) {
        super( queryExecutorService );
    }

    @Override
    protected Runnable createMonitor( final ExecutorService pool, final Map<Source, Future<SourceResponse>> futures,
            final QueryResponseImpl returnResults, final Query query ) {

        return new SortedQueryMonitor( pool, futures, returnResults, query );
    }

    private class SortedQueryMonitor implements Runnable {

        private QueryResponseImpl returnResults;
        private Map<Source, Future<SourceResponse>> futures;
        private Query query;

        public SortedQueryMonitor( ExecutorService pool, Map<Source, Future<SourceResponse>> futuress,
                QueryResponseImpl returnResults, Query query ) {

            this.returnResults = returnResults;
            this.query = query;
            this.futures = futuress;
        }

        @Override
        public void run() {
            String methodName = "run";
            logger.entry( methodName );

            SortBy sortBy = query.getSortBy();
            // Prepare the Comparators that we will use
            Comparator<Result> coreComparator = DEFAULT_COMPARATOR;
            
            if ( sortBy != null && sortBy.getPropertyName() != null ) {
        	PropertyName sortingProp = sortBy.getPropertyName();
                String sortType = sortingProp.getPropertyName();
                SortOrder sortOrder = ( sortBy.getSortOrder() == null ) ? SortOrder.DESCENDING : sortBy.getSortOrder();
                logger.debug( "Sorting by type: " + sortType );
                logger.debug( "Sorting by Order: " + sortBy.getSortOrder() );

                //Temporal searches are currently sorted by the effective time
                if ( Metacard.EFFECTIVE.equals( sortType ) || Result.TEMPORAL.equals(sortType)) {
                    coreComparator = new TemporalResultComparator( sortOrder );
                } else if ( Result.DISTANCE.equals( sortType ) ) {
                    coreComparator = new DistanceResultComparator( sortOrder );
                } else if ( Result.RELEVANCE.equals( sortType ) ) {
                    coreComparator = new RelevanceResultComparator( sortOrder );
                } 
            }


            List<Result> resultList = new ArrayList<Result>();
            long totalHits = 0;
            Set<ProcessingDetails> processingDetails = returnResults.getProcessingDetails();
            
            long deadline = System.currentTimeMillis() + query.getTimeoutMillis();
            
            Map<String, Serializable> returnProperties = returnResults.getProperties();
            for ( final Entry<Source,Future<SourceResponse>> entry : futures.entrySet() ) {
                Source site = entry.getKey();
                try {
                
                    SourceResponse sourceResponse = query.getTimeoutMillis() < 1 ? 
                            entry.getValue().get() : 
                            entry.getValue().get( getTimeRemaining( deadline ), TimeUnit.MILLISECONDS ); 
                            
                    resultList.addAll( sourceResponse.getResults() );
                    totalHits += sourceResponse.getHits();
                    
                    // TODO: for now add all properties into outgoing response's properties.
                    // this is not the best idea because we could get properties from records that 
                    // get eliminated by the max results enforcement done below.  See DDF-1183 for 
                    // a possible solution.
                    Map<String, Serializable> properties = sourceResponse.getProperties();
                    returnProperties.putAll(properties);
                    
                } catch ( InterruptedException e ) {
                    logger.warn( "Couldn't get results from completed federated query on site with ShortName "
                            + site.getId(), e );
                    processingDetails.add(new ProcessingDetailsImpl(site.getId(), e));
                } catch ( ExecutionException e ) {
                    logger.warn( "Couldn't get results from completed federated query on site "
                            + site.getId(), e );
                    if ( logger.isDebugEnabled() ) {
                        logger.debug( "Adding exception to response." );
                    }
                    processingDetails.add(new ProcessingDetailsImpl(site.getId(), e));
                } catch ( TimeoutException e ) {
                    logger.warn( "search timed out: " + new Date() + " on site " + site.getId() );
                    processingDetails.add(new ProcessingDetailsImpl(site.getId(), e));
                }
            }
            logger.debug( "all sites finished returning results: " + resultList.size() );

            Collections.sort( resultList, coreComparator );

            returnResults.setHits( totalHits );
            int maxResults = query.getPageSize() > 0 ? query.getPageSize() : Integer.MAX_VALUE;
            
            returnResults.addResults( resultList.size() > maxResults ? resultList.subList( 0, maxResults ) : resultList, true );
        }

        private long getTimeRemaining( long deadline ) {
            long timeleft;
            if ( System.currentTimeMillis() > deadline ) {
                timeleft = 0;
            } else {
                timeleft = deadline - System.currentTimeMillis();
            }
            return timeleft;
        }

    }
}