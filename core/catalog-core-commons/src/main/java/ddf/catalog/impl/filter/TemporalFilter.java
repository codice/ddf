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
package ddf.catalog.impl.filter;

import java.util.Date;

import org.apache.commons.lang.builder.ToStringBuilder;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.LoggerFactory;
import org.slf4j.ext.XLogger;


public class TemporalFilter //implements Filter
{
    private Date startDate;
    
    private Date endDate;
    
    private static XLogger logger = new XLogger( LoggerFactory.getLogger( TemporalFilter.class ) );
    
    
    /**
     * Absolute time search
     * 
     * @param dateStart
     * @param endDate
     */
    public TemporalFilter( Date startDate, Date endDate )
    {
        setDates( startDate, endDate );
    }


    /**
     * Absolute time search, parses incoming strings to dates.
     * 
     * @param dtStartStr
     * @param dtEndStr
     */
    public TemporalFilter( String startDate, String endDate )
    {
        this( parseDate( startDate ), parseDate( endDate ) );
    }
    
    
    /**
     * Relative time search.
     * 
     * @param offset time range (in milliseconds) to search from current point
     *            in time. Positive offset goes backwards in time. Example:
     *            offset of 30000 will perform a search where the bounds are
     *            between now and 30 seconds prior to now.
     */
    public TemporalFilter( long offset )
    {
        this( new Date( System.currentTimeMillis() - offset ), new Date() );
    }


    /**
     * Sets dates and performs basic validation. dtStart and dtEnd may be null,
     * but not at the same time.
     * 
     * @param dtStart lower bound of the temporal range search. When null it is
     *            set to time 0.
     * @param endDate upper bound of the temporal range search. When null it is
     *            set to now.
     */
    private void setDates( Date startDate, Date endDate )
    {
        if ( startDate == null && endDate == null ) 
        {
            throw new RuntimeException( "Cannot have empty start and end date." );
        }
        
        if ( startDate == null )
        {
            startDate = new Date( 0 );
        }
        
        if ( endDate == null ) 
        {
            endDate = new Date();
        }

        this.startDate = startDate;
        this.endDate = endDate;
    }
    
    
    /**
     * Parses the date/time string and returns a date object.
     * 
     * @param date
     * @return
     */
    public static Date parseDate( String date )
    {
        Date returnDate = null;
        if ( date != null && !date.isEmpty() )
        {
            try
            {
                DateTimeFormatter dateFormatter = ISODateTimeFormat.dateTime();
                returnDate = dateFormatter.parseDateTime( date ).toDate();
            }
            catch ( IllegalArgumentException iae )
            {
                logger.warn( "Could not parse out updated date in response, date will not being passed back." );
            }
        }
        return returnDate;
    }


    public Date getStartDate()
    {
        return startDate;
    }


    public void setStartDate( Date startDate )
    {
        this.startDate = startDate;
    }


    public Date getEndDate()
    {
        return endDate;
    }


    public void setEndDate( Date endDate )
    {
        this.endDate = endDate;
    }        


    @Override
    public String toString()
    {
        return ToStringBuilder.reflectionToString( this );
    }
    
}
