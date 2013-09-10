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
package ddf.metrics.collector;

import java.util.Calendar;

public class MetricsUtil {
	
    /**
     * Used for formatting long timestamps into more readable calendar dates/times.
     */
    private static final String months[] = 
    { 
        "Jan", "Feb", "Mar", "Apr", 
        "May", "Jun", "Jul", "Aug", 
        "Sep", "Oct", "Nov", "Dec"
    }; 

    
    /**
     * Formats timestamp (in seconds since Unix epoch) into human-readable
     * format of MMM DD YYYY hh:mm:ss.
     * 
     * Example:
     *     Apr 10 2013 09:14:43
     *     
     * @param timestamp time in seconds since Unix epoch of Jan 1, 1970 12:00:00
     * 
     * @return formatted date/time string of the form MMM DD YYYY hh:mm:ss
     */
    public static String getCalendarTime(long timestamp)
    {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp * 1000);
        
        String calTime = months[calendar.get(Calendar.MONTH)] +
            " " + calendar.get(Calendar.DATE) + " " + 
            calendar.get(Calendar.YEAR) + " ";
       
        calTime += addLeadingZero(calendar.get(Calendar.HOUR_OF_DAY)) + ":";
        calTime += addLeadingZero(calendar.get(Calendar.MINUTE)) + ":";
        calTime += addLeadingZero(calendar.get(Calendar.SECOND)); 
        
        return calTime;
    }
    
    
    static String addLeadingZero(int value)
    {
        if (value < 10)
        {
            return "0" + String.valueOf(value);
        }       
        
        return String.valueOf(value);
    }

}
