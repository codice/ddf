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
package ddf.catalog.operation;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import ddf.catalog.source.SourceDescriptor;

/**
 * The SourceInfoResponseImpl provides a default implementation of a {@link SourceInfoResponse}.
 * 
 * @see SourceDescriptor
 * @see Source
 * @see SoruceInfoRequest
 */
public class SourceInfoResponseImpl extends ResponseImpl<SourceInfoRequest> implements SourceInfoResponse {

	protected Set<SourceDescriptor> sourceInfos;

	/**
	 * Instantiates a new SourceInfoResponseImpl
	 *
	 * @param request - the original {@link SourceInfoRequest} request
	 * @param properties - the properties associated with the operation
	 * @param sourceInfos - the source information for each requested {@link Source} 
	 */
	public SourceInfoResponseImpl(SourceInfoRequest request, Map<String, Serializable> properties, Set<SourceDescriptor> sourceInfos ) {
		super( request, properties );
		this.sourceInfos = sourceInfos;
	}
	
	/**
	 * Instantiates a new SourceInfoResponseImpl
	 *
	 * @param request - the original {@link SourceInfoRequest} request
	 * @param properties - the properties associated with the operation
	 * @param sourceInfo - the source information for each a single {@link Source} 
	 */
	public SourceInfoResponseImpl(SourceInfoRequest request, Map<String, Serializable> properties, SourceDescriptor sourceInfo ) {
        this( request, properties, getSet( sourceInfo ) );
        
    }

    /**
     * Gets the set of {@link SourceDescriptor} information
     *
     * @param sourceInfo the source information
     * @return the {@link Set} of source information
     */
    private static Set<SourceDescriptor> getSet( SourceDescriptor sourceInfo ) {
        Set<SourceDescriptor> sourceSet = new HashSet<SourceDescriptor>(1);
        sourceSet.add( sourceInfo );
        return sourceSet;
    }

    /* (non-Javadoc)
     * @see ddf.catalog.operation.SourceInfoResponse#getSourceInfo()
     */
    @Override
    public Set<SourceDescriptor> getSourceInfo() {
        return sourceInfos;
    }

}
