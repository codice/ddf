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
package ddf.catalog;

import java.util.Date;
import java.util.Set;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.MetacardType;
import ddf.catalog.operation.CreateRequest;
import ddf.catalog.operation.CreateResponse;
import ddf.catalog.operation.DeleteRequest;
import ddf.catalog.operation.DeleteResponse;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.operation.UpdateRequest;
import ddf.catalog.operation.UpdateResponse;
import ddf.catalog.source.CatalogProvider;
import ddf.catalog.source.IngestException;

/**
 * Throws a runtime exception for all of the performing operations. This is used to test the catalog
 * framework and verify that runtime exceptions are being caught and not thrown back to the
 * endpoint.
 * 
 * @author LMCO
 * 
 */
public class MockExceptionProvider extends MockSource implements CatalogProvider {

    public MockExceptionProvider(String shortName, String title, String version,
            String organization, Set<ContentType> catalogTypes, boolean isAvailable,
            Date lastAvailability) {
        super(shortName, title, version, organization, catalogTypes, isAvailable, lastAvailability);
    }

    @Override
    public SourceResponse query(QueryRequest query) {
        throw new RuntimeException("RUNTIME EXCEPTION IN QUERY");
    }

    @Override
    public CreateResponse create(CreateRequest create) throws IngestException {
        throw new RuntimeException("RUNTIME EXCEPTION IN CREATE");
    }

    @Override
    public UpdateResponse update(UpdateRequest update) throws IngestException {
        throw new RuntimeException("RUNTIME EXCEPTION IN UDPATE");

    }

    @Override
    public DeleteResponse delete(DeleteRequest delete) throws IngestException {
        throw new RuntimeException("RUNTIME EXCEPTION IN DELETE");

    }

    public void maskId(String sourceId) {

    }

    @Override
    public Set<ContentType> getContentTypes() {
        throw new RuntimeException("RUNTIME EXCEPTION getContentTypes");
    }

}
