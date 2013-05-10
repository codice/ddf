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
package ddf.catalog.pubsub;


import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.security.auth.Subject;
import javax.xml.datatype.XMLGregorianCalendar;

import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.FilterFactoryImpl;
import org.geotools.styling.UomOgcMapping;
import org.geotools.temporal.object.DefaultInstant;
import org.geotools.temporal.object.DefaultPeriod;
import org.geotools.temporal.object.DefaultPosition;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterVisitor;
import org.opengis.filter.PropertyIsLike;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.temporal.Instant;
import org.opengis.temporal.Period;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;

import ddf.catalog.data.ContentType;
import ddf.catalog.data.Metacard;
import ddf.catalog.impl.filter.SpatialFilter;
import ddf.catalog.operation.Query;
import ddf.catalog.operation.QueryRequest;
import ddf.catalog.operation.ResourceResponse;
import ddf.catalog.operation.SourceResponse;
import ddf.catalog.resource.ResourceNotFoundException;
import ddf.catalog.resource.ResourceNotSupportedException;
import ddf.catalog.source.FederatedSource;
import ddf.catalog.source.SourceMonitor;
import ddf.catalog.source.UnsupportedQueryException;
import ddf.measure.Distance;
import ddf.measure.Distance.LinearUnit;


public class MockQuery implements FederatedSource, Query
{
    // PLACEHOLDER for security
    private Subject user;

    private Integer startIndex;

    private Integer count;

    private long maxTimeout;

    private boolean isEnterprise;

    private Set<String> siteIds;

    private SortBy sortBy;

    public static final FilterFactory filterFactory = new FilterFactoryImpl();
    private Filter filter;
    private List<Filter> filters;

    private static XLogger logger = new XLogger( LoggerFactory.getLogger( MockQuery.class ) );


    public MockQuery()
    {
        this( null, 0, 10, "RELEVANCE", SortOrder.DESCENDING, 30000 );
    }
    
    
    public MockQuery( Subject user, int startIndex, int count, String sortField, SortOrder sortOrder, long maxTimeout )
    {
        this.user = user;
        this.startIndex = startIndex;
        this.count = count;  
        if ( sortField != null && sortOrder != null )
        {
            this.sortBy = filterFactory.sort( sortField.toUpperCase(), sortOrder );  // RELEVANCE or TEMPORAL
        }
        this.maxTimeout = maxTimeout;
        this.filters = new ArrayList<Filter>();
    }

    
    public void addContextualFilter( String searchPhrase, String textPathSections )
    {
        Filter filter = null;
           
        if ( searchPhrase != null )
        {
            if ( textPathSections != null )
            {
                List<Filter> xpathFilters = new ArrayList<Filter>();
                String[] selectors = textPathSections.split( "," );
                for( int i = 0; i < selectors.length; i++ )
                {
                    Expression xpathRef = new AttributeExpressionImpl( selectors[i] );
                    filter = filterFactory.like( xpathRef, searchPhrase );
                    xpathFilters.add( filter );
                }
                filter = filterFactory.or( xpathFilters );
            }
            else
            {
                filter = filterFactory.like( filterFactory.property( Metacard.ANY_TEXT ), searchPhrase );
            }
            
            if ( filter != null )
            {
                filters.add( filter );
                //this.filter = getFilter();
            }
        }
    }

    
    public void addTemporalFilter( XMLGregorianCalendar start, XMLGregorianCalendar end, String timeType )
    {
        Filter filter = null;
        
        String timeProperty = Metacard.MODIFIED;
        
        if ( timeType != null && timeType.toLowerCase().equals( Metacard.EFFECTIVE ) ) 
        {
            timeProperty = Metacard.EFFECTIVE;
        }
        
        if ( start != null && end != null )
        {
            int compareTo = start.toGregorianCalendar().compareTo(end.toGregorianCalendar());
            if(compareTo > 0) {
                
                throw new IllegalArgumentException("start date [" + start +"] should not be later than" +
                        " end date [" + end +"]" ) ;
                
            } else if( compareTo == 0 ) {
                
                filter = filterFactory.equals(filterFactory.property(timeProperty), filterFactory.literal(start.toGregorianCalendar().getTime())) ;
                
            } else {
            
                // t1.start < timeType instance < t1.end
                DefaultPosition defaultPosition = new DefaultPosition(start.toGregorianCalendar().getTime());
                Instant startInstant = new DefaultInstant(defaultPosition);
                Instant endInstant = new DefaultInstant(new DefaultPosition(end.toGregorianCalendar().getTime()));
                Period period = new DefaultPeriod(startInstant, endInstant);
                
                filter = filterFactory.during(filterFactory.property(timeProperty), filterFactory.literal(period)) ;
            }
            
            filters.add( filter );
            //this.filter = getFilter();
        }
    }   

    public void addEntryFilter( String id ) 
    {      
        Filter filter = null;
        
        if ( id != null )
        {
            logger.debug( "Creating entry by ID filter" );

            filter = filterFactory.equals( filterFactory.property( Metacard.ID ), filterFactory.literal( id ) );
            filters.add( filter );
        }
        else
        {
            logger.debug( "id was NULL - EntryFilter not created" );
        }
    }    
    
    
    public void addSpatialFilter( String geometryWkt, Double inputRadius, String linearUnit, String spatialType ) 
    {       
        Filter filter = null;
        
        try
        {
            if (geometryWkt == null || geometryWkt.isEmpty())
            {
                return;
            }
    
            SpatialFilter spatialFilter = new SpatialFilter( geometryWkt );
            
            if ( spatialType.equals( "CONTAINS" ) )
            {
                filter = filterFactory.within( Metacard.ANY_GEO, spatialFilter.getGeometry() );
            }
            else if ( spatialType.equals( "OVERLAPS" ) )
            {
                filter = filterFactory.intersects( Metacard.ANY_GEO, spatialFilter.getGeometry() );
            }
            else if ( spatialType.equals( "NEAREST_NEIGHBOR" ) )
            {
                filter = filterFactory.beyond( Metacard.ANY_GEO, spatialFilter.getGeometry(), 0.0, UomOgcMapping.METRE.name() );
            }
            else if ( spatialType.equals( "POINT_RADIUS" ) )
            {
                Double normalizedRadius = convertRadius( linearUnit, inputRadius );
                filter = filterFactory.dwithin( Metacard.ANY_GEO, spatialFilter.getGeometry(), normalizedRadius, UomOgcMapping.METRE.name() );
            }
            else
            {
                return;
            }
        }
        catch(IllegalArgumentException e)
        {
            logger.debug("Invalid spatial query type specified.  Will not apply spatial filter.");
            return;
        }
        
        if ( filter != null ) filters.add( filter );
    }    
    
    
    private Double convertRadius( String linearUnit, Double inputRadius ) 
    {
        if ( linearUnit.equals( "FOOT_U_S" ) )
        {
        	return new Distance(inputRadius, LinearUnit.FOOT_U_S).getAs(LinearUnit.METER);
        }
        else if ( linearUnit.equals( "KILOMETER" ) )
        {
            return  new Distance( inputRadius, LinearUnit.KILOMETER ).getAs(LinearUnit.METER);
        }
        else if ( linearUnit.equals( "MILE" ) )
        {
            return new Distance( inputRadius, LinearUnit.MILE ).getAs(LinearUnit.METER);
        }
        else if ( linearUnit.equals( "NAUTICAL_MILE" ) )
        {
            return new Distance( inputRadius, LinearUnit.NAUTICAL_MILE ).getAs(LinearUnit.METER);
        }
        else if ( linearUnit.equals( "YARD" ) )
        {
            return new Distance( inputRadius, LinearUnit.YARD ).getAs(LinearUnit.METER);
        }
        else if ( linearUnit.equals( "METER" ) )
        {
        }
        
        return inputRadius ;
    }
        
    
    public void addTypeFilter( List<MockTypeVersionsExtension> extensionList )
    {       
        List<Filter> runningFilterList = new ArrayList<Filter>() ;
        
        for ( MockTypeVersionsExtension e : extensionList ) 
        {
            String type = e.getExtensionTypeName() ;
            List<String> versions = e.getVersions() ;
            Filter oneTypeFilter = null ;
            Expression expressionType = filterFactory.property( Metacard.CONTENT_TYPE );
            Expression expressionVersion = filterFactory.property( Metacard.CONTENT_TYPE_VERSION );
            
            // Create a list of type-version pairs
            // Logically 'AND' the type and versions together
            if(versions != null && !versions.isEmpty()) {
                
                List<Filter> andedTypeVersionPairs = new ArrayList<Filter>() ;
                
                for(String v : versions) {
                    
                    if(v != null ) {
                        PropertyIsLike typeFilter = filterFactory.like( expressionType, type, "*", "?", "\\", false );

                        PropertyIsLike versionFilter = filterFactory.like( expressionVersion, v, "*", "?", "\\", false );

                        andedTypeVersionPairs.add(filterFactory.and(typeFilter, versionFilter) ) ;
                    }
                }
                
                // Check if we had any pairs and logically 'OR' them together.
                if(!andedTypeVersionPairs.isEmpty()) {
                    oneTypeFilter = filterFactory.or(andedTypeVersionPairs) ;
                }
                else {
                    // if we don't have any pairs, means we don't have versions, handle single type
                    oneTypeFilter = filterFactory.like( expressionType, type, "*", "?", "\\", false );
                }
                
            } else {
                // we do not have versions, handle single type case
                oneTypeFilter = filterFactory.like( expressionType, type, "*", "?", "\\", false );
            }
            
            runningFilterList.add(oneTypeFilter) ;
        }
        
        if ( !runningFilterList.isEmpty() )
        {
            Filter filter = filterFactory.or( runningFilterList );
            filters.add( filter );
            //this.filter = getFilter();
        }
    }
    
    
    @Override
    public Object accept( FilterVisitor visitor, Object obj )
    {
        logger.debug( "accept" );
        return filter.accept( visitor, obj );
    }


    @Override
    public boolean evaluate( Object object )
    {
        return filter.evaluate( object );
    }

    @Override
    public boolean requestsTotalResultsCount()
    {
        // always send back total count for NCES
        return true;
    }


    @Override
    public long getTimeoutMillis()
    {
        return maxTimeout;
    }


    public void setSiteIds( Set<String> siteIds )
    {
        this.siteIds = siteIds;
    }

    public void setIsEnterprise( boolean isEnterprise )
    {
        this.isEnterprise = isEnterprise;
    }


    @Override
    public SortBy getSortBy()
    {
        return sortBy;
    }


    public Filter getFilter()
    {
        // If multiple filters, then AND them all together
        if ( filters.size() > 1 )
        {
            return filterFactory.and( filters );
        }
        
        // If only one filter, then just return it
        // (AND'ing it would create an erroneous </ogc:and> closing tag)
        else if ( filters.size() == 1 )
        {
            return (Filter) filters.get( 0 );
        }
        
        // Otherwise, no filters
        else
        {
            return null;
        }
    }


    @Override
    public boolean isAvailable()
    {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public boolean isAvailable(SourceMonitor callback) 
    {
        return isAvailable();
    }

    
    @Override
    public SourceResponse query( QueryRequest request ) throws UnsupportedQueryException
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public Set<ContentType> getContentTypes()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public String getId()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public String getVersion()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public String getTitle()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public String getDescription()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public String getOrganization()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public ResourceResponse retrieveResource( URI uri, Map<String, Serializable> arguments ) throws IOException,
        ResourceNotFoundException, ResourceNotSupportedException
    {
        // TODO Auto-generated method stub
        return null;
    }


    //@Override
    public Set<String> getSupportedSchemes()
    {
        // TODO Auto-generated method stub
        return null;
    }


    @Override
    public int getStartIndex()
    {
        return startIndex;
    }


    @Override
    public int getPageSize()
    {
        return count;
    }


	@Override
	public Set<String> getOptions(Metacard metacard) {
		// TODO Auto-generated method stub
		return null;
	}

}
