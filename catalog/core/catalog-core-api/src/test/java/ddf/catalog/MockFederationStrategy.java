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
package ddf.catalog;

import java.util.List;

import org.apache.log4j.Logger;

import ddf.catalog.data.Result;
import ddf.catalog.federation.FederationException;
import ddf.catalog.federation.FederationStrategy;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.QueryResponse;
import ddf.catalog.operation.QueryResponseImpl;
import ddf.catalog.source.Source;
import ddf.catalog.source.UnsupportedQueryException;

public class MockFederationStrategy implements FederationStrategy {
	
	private static Logger LOGGER = Logger.getLogger(CatalogFrameworkImpl.class);

	@Override
	public QueryResponse federate(List<Source> sources, QueryRequest query)
			throws FederationException {
		LOGGER.debug("entry");
		QueryResponseImpl resp = new QueryResponseImpl(query);
		
	    for(Source src : sources){
			try {
				resp.addResults(src.query(query).getResults(), false);
			} catch (UnsupportedQueryException e) {
				LOGGER.error("Doh!", e);
			}
		}
	    resp.closeResultQueue();
	    List<Result> results = resp.getResults(); 
	    resp.setHits(results.size());
	    LOGGER.debug("exit");
		return resp;
	}

}
