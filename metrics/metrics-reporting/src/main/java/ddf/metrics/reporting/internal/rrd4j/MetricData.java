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
package ddf.metrics.reporting.internal.rrd4j;

import java.util.List;


/**
 * Domain object to encapsulate a single metric's data which consists of its
 * list of timestamps (in seconds since Unix epoch) and their associated values.
 * If the metric is for an incremental counter then a total count is also maintained.
 * 
 * @author rodgersh
 *
 */
public class MetricData
{
    private List<Long> timestamps;
    private List<Double> values;
    private long totalCount;
    private boolean hasTotalCount;
        
    
    public List<Long> getTimestamps()
    {
        return timestamps;
    }


    public void setTimestamps( List<Long> timestamps )
    {
        this.timestamps = timestamps;
    }


    public List<Double> getValues()
    {
        return values;
    }


    public void setValues( List<Double> values )
    {
        this.values = values;
    }


    public boolean hasTotalCount()
    {
        return hasTotalCount;
    }


    public void setHasTotalCount( boolean hasTotalCount )
    {
        this.hasTotalCount = hasTotalCount;
    }


    public long getTotalCount()
    {
        return totalCount;
    }
    
    
    public void setTotalCount( long totalCount )
    {
        this.totalCount = totalCount;
    }
    
}
