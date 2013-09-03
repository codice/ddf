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
package ddf.util;

import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;


public class InetAddressUtil
{
    /**
     * Retrieves the first non-loopback IP address, (i.e., filters out 127.0.0.1 addresses) 
     * for machine your are running on.
     * 
     * @return
     * @throws SocketException
     */
    public static InetAddress getFirstNonLoopbackAddress() 
        throws SocketException
    {
        return getFirstNonLoopbackAddress( true, false );
    }
    
    
    /**
     * @param preferIpv4
     * @param preferIPv6
     * @return
     * @throws SocketException
     */
    public static InetAddress getFirstNonLoopbackAddress( boolean preferIpv4, boolean preferIPv6 ) 
        throws SocketException
    {
        Enumeration en = NetworkInterface.getNetworkInterfaces();
        while ( en.hasMoreElements() )
        {
            NetworkInterface i = (NetworkInterface) en.nextElement();
            for( Enumeration en2 = i.getInetAddresses(); en2.hasMoreElements(); )
            {
                InetAddress addr = (InetAddress) en2.nextElement();
                if ( !addr.isLoopbackAddress() )
                {
                    if ( addr instanceof Inet4Address )
                    {
                        if ( preferIPv6 )
                        {
                            continue;
                        }
                        return addr;
                    }
                    if ( addr instanceof Inet6Address )
                    {
                        if ( preferIpv4 )
                        {
                            continue;
                        }
                        return addr;
                    }
                }
            }
        }
        return null;
    }

}
