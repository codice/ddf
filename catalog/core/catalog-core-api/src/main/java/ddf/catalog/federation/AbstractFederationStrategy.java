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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.data.Result;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryImpl;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryRequestImpl;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.QueryResponseImpl;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.source.Source;


/**
 * This class serves as a base implementation of the {@link FederationStrategy}
 * interface. Other classes can extend this class to create specific attributes
 * to sort by.
 * 
 * @author ddf.isgs@lmco.com
 * 
 */
public abstract class AbstractFederationStrategy implements FederationStrategy
{

    private static XLogger logger = new XLogger(LoggerFactory.getLogger(AbstractFederationStrategy.class));
    private static final String CLASS_NAME = AbstractFederationStrategy.class.getName();
    private ExecutorService queryExecutorService;
    private static final int DEFAULT_MAX_START_INDEX = 50000;
    private int maxStartIndex;

    /**
     * Instantiates an {@code AbstractFederationStrategy} with the provided
     * {@link ExecutorService}.
     * 
     * @param queryExecutorService the {@link ExecutorService} for queries
     */
    public AbstractFederationStrategy( ExecutorService queryExecutorService )
    {
        this.queryExecutorService = queryExecutorService;
        this.maxStartIndex = DEFAULT_MAX_START_INDEX;
    }

    /**
     * Creates the monitor for federated queries.
     * 
     * @param queryExecutorService
     * @param futures
     * @param returnResults the query results
     * @param query
     * @return the {@link Runnable}
     */
    protected abstract Runnable createMonitor( ExecutorService queryExecutorService,
        Map<Source, Future<SourceResponse>> futures, QueryResponseImpl returnResults, Query query );

    @Override
    public QueryResponse federate( List<Source> sources, final QueryRequest queryRequest )
    {
        final String methodName = "federate";
        logger.entry(methodName);
        if (logger.isDebugEnabled())
        {
            for ( Source source : sources )
            {
                if (source != null)
                {
                    logger.debug("source to query: " + source.getId());
                }
            }
        }

        Query originalQuery = queryRequest.getQuery();
        // final long queryTimeoutMillis = query.getTimeoutMillis();

        int offset = originalQuery.getStartIndex();
        final int pageSize = originalQuery.getPageSize();

        // limit offset to max value
        if (offset > this.maxStartIndex)
        {
            offset = this.maxStartIndex;
        }

        final QueryResponseImpl queryResponseQueue = new QueryResponseImpl(queryRequest, null);

        Map<Source, Future<SourceResponse>> futures = new HashMap<Source, Future<SourceResponse>>();

        Query modifiedQuery = getModifiedQuery(originalQuery, sources.size(), offset, pageSize);

        // Do NOT call source.isAvailable() when checking sources
        for ( final Source source : sources )
        {
            if (source != null)
            {
                if (!futures.containsKey(source))
                {
                    logger.debug("running query on source: " + source.getId());

                    futures.put(source, queryExecutorService.submit(new CallableSourceResponse(source, modifiedQuery,
                        queryRequest.getProperties())));
                }
                else
                {
                    logger.warn("Duplicate source found with name " + source.getId() + ". Ignoring second one.");
                }
            }
        }

        QueryResponseImpl offsetResults = null;
        // If there are offsets and more than one source, we have to get all the
        // results back and then
        // transfer them into a different Queue. That is what the
        // OffsetResultHandler does.
        if (offset > 1 && sources.size() > 1)
        {
            offsetResults = new QueryResponseImpl(queryRequest, null);
            queryExecutorService.submit(new OffsetResultHandler(queryResponseQueue, offsetResults, pageSize, offset));
        }

        queryExecutorService.submit(createMonitor(queryExecutorService, futures, queryResponseQueue, modifiedQuery));

        if (offset > 1 && sources.size() > 1)
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("returning offsetResults: " + offsetResults);
                logger.exit(CLASS_NAME + "." + "federate");
            }

            return offsetResults;
        }
        else
        {
            if (logger.isDebugEnabled())
            {
                logger.debug("returning returnResults: " + queryResponseQueue);
                logger.exit(CLASS_NAME + "." + "federate");
            }
            return queryResponseQueue;
        }

    }

    private Query getModifiedQuery( Query originalQuery, int numberOfSources, int offset, int pageSize )
    {

        Query query = null;

        // If offset is not specified, our offset is 1
        if (offset > 1 && numberOfSources > 1)
        {

            final int modifiedOffset = 1;
            int modifiedPageSize = computeModifiedPageSize(offset, pageSize);

            if (logger.isDebugEnabled())
            {
                logger.debug("Creating new query for federated sources to query each source from " + modifiedOffset
                        + " to " + modifiedPageSize + ".");
                logger.debug("original offset: " + offset);
                logger.debug("original page size: " + pageSize);
                logger.debug("modified offset: " + modifiedOffset);
                logger.debug("modified page size: " + modifiedPageSize);
            }

            /**
             * Federated sources always query from offset of 1. When all query
             * results are received from all federated sources and merged
             * together - then the offset is applied.
             * 
             */
            query = new QueryImpl(originalQuery, modifiedOffset, modifiedPageSize, originalQuery.getSortBy(),
                originalQuery.requestsTotalResultsCount(), originalQuery.getTimeoutMillis());
        }
        else
        {
            query = originalQuery;
        }

        return query;
    }

    /**
     * Base 1 offset, hence page size is one less.
     */
    private int computeModifiedPageSize( int offset, int pageSize )
    {
        return offset + pageSize - 1;
    }

    private class CallableSourceResponse implements Callable<SourceResponse>
    {

        private Query query = null;
        private Source source = null;
        private Map<String, Serializable> properties = null;

        public CallableSourceResponse( Source source, Query query, Map<String, Serializable> properties )
        {
            this.source = source;
            this.query = query;
            this.properties = properties;
        }

        @Override
        public SourceResponse call() throws Exception
        {
            return source.query(new QueryRequestImpl(query, properties));
        };
    }

    private class OffsetResultHandler implements Runnable
    {

        private QueryResponseImpl originalResults = null;
        private QueryResponseImpl offsetResultQueue = null;
        private int pageSize = 0;
        private int offset = 1;

        private OffsetResultHandler( QueryResponseImpl originalResults, QueryResponseImpl offsetResultQueue,
            int pageSize, int offset )
        {
            this.originalResults = originalResults;
            this.offsetResultQueue = offsetResultQueue;
            this.pageSize = pageSize;
            this.offset = offset;

        }

        @Override
        public void run()
        {
            int queryResultIndex = 1;
            int resultsSent = 0;
            Result result;

            while (resultsSent < pageSize && originalResults.hasMoreResults()
                    && (result = originalResults.take()) != null)
            {
                if (queryResultIndex >= offset)
                {
                    offsetResultQueue.addResult(result, false);
                    resultsSent++;
                }
                queryResultIndex++;
            }

            logger.debug("Closing Queue and setting the total count");
            offsetResultQueue.setHits(originalResults.getHits());
            offsetResultQueue.closeResultQueue();
        }
    }

    /**
     * To be set via Spring/Blueprint
     * 
     * @param maxStartIndex the new default max start index value
     */
    public void setMaxStartIndex( int maxStartIndex )
    {
        logger.debug("Current max start index: " + this.maxStartIndex);
        this.maxStartIndex = DEFAULT_MAX_START_INDEX;

        if (maxStartIndex > 0)
        {
        	this.maxStartIndex = maxStartIndex;
            logger.debug("New max start index: " + this.maxStartIndex);
        }
        else 
        {	
        	logger.debug("Invalid max start index input. Reset to default value: " + this.maxStartIndex );
        }
    }

}
