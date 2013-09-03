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


public class SpatialDistanceFilter extends SpatialFilter
{
    private double distance;
    

    public SpatialDistanceFilter( String lon, String lat, String distance )
    {
        this( createWKT( Double.parseDouble( lat ), Double.parseDouble( lon ) ), Double.parseDouble( distance ) );
    }
    

    public SpatialDistanceFilter( double lon, double lat, double distance )
    {
        this( createWKT( lat, lon ), distance );
    }
    

    public SpatialDistanceFilter( String geometryWkt, String distance )
    {
        this( geometryWkt, Double.parseDouble( distance ) );
    }
    

    public SpatialDistanceFilter( String geometryWkt, double distance )
    {
        this.distance = distance;
        this.geometryWkt = geometryWkt;
    }


    static private String createWKT( double lat, double lon )
    {
        StringBuilder wkt = new StringBuilder( "POINT(" );
        wkt.append( lon + " " + lat );
        wkt.append( ")" );
        
        return wkt.toString();
    }


    public double getDistanceInMeters()
    {
        return distance;
    }
}
