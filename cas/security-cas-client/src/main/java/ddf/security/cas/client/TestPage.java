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
package ddf.security.cas.client;

import java.io.IOException;
import java.io.PrintWriter;
import java.security.Principal;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jasig.cas.client.authentication.AttributePrincipal;

/**
 * 
 * Proxy granting ticket callback used by the CAS server.  Nothing really to do here, we just need something 
 * running at the callback url so the CAS server doesn't get a HTTP 404 error. This is also used as a test page.
 * 
 *  @see javax.servlet.http.HttpServlet
 *  
 */
public class TestPage extends HttpServlet
{
    private static final long serialVersionUID = 1L;
    
    private static final String HTML_ENDLINE = "<br/>";
    
    private static final Logger LOGGER = Logger.getLogger( TestPage.class );
    
    /**
     * @see javax.servlet.http.HttpServlet#doGet(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doGet( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException
    {
        response.setContentType( "text/html" );
        doPost( request, response );
    }
    
    /**
     * @see javax.servlet.http.HttpServlet#doPost(javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)
     */
    @Override
    protected void doPost( HttpServletRequest request, HttpServletResponse response ) throws ServletException, IOException
    {
        PrintWriter out = null;
        
        try
        {
            response.setContentType( "text/html" );
            out = response.getWriter();
            createPage( request, out );
        }
        catch ( Exception e )
        {
            LOGGER.error( "Error creating SSO Test Page.  See log.", e );
            out.print( "<html><head><title>Error Creating Test Page</title></head><body>Error Creating Test Page</body></html>" );
        }
    }


    @SuppressWarnings( "rawtypes" )
    protected void createPage( HttpServletRequest request, PrintWriter out ) throws Exception
    {
        String endl = System.getProperty( "line.separator" );
        StringBuilder builder = new StringBuilder();

        builder.append( "<html>" );
        builder.append( "<head>" );
        builder.append( "<title>Test Page</title>" );
        builder.append( "</head>" );
        builder.append( "<body>" );
        builder.append( "<h2 align=\"center\">Test Page" ).append( endl );
        
        builder.append( "<h2 align=\"center\">Protected Page on '" ).append( request.getServerName() ).append( "'</h2>" ).append( endl );

        builder.append( new java.util.Date() ).append( HTML_ENDLINE + HTML_ENDLINE ).append( endl );

        builder.append( "request.getRemoteUser() = " );
        builder.append( request.getRemoteUser() ).append( HTML_ENDLINE ).append( endl );
       

        builder.append( "request.getUserPrincipal() = " );

        Principal principal = request.getUserPrincipal();
        builder.append( principal ).append( HTML_ENDLINE + HTML_ENDLINE).append( endl );
    

        if ( request.getContextPath() != null && !"".equals(request.getContextPath()) )
        {
            builder.append( "</p><p>" ).append( endl );
            builder.append( "The context root name of this application is " ).append( request.getContextPath() ).append( endl );
            builder.append( "</p>" ).append( endl );
        }
        
        builder.append( "<h3>Released Attributes:</h3>" ).append( endl );

        Map attributes = null;

        if ( principal != null )
        {
            AttributePrincipal attributePrincipal = (AttributePrincipal) principal;

            attributes = attributePrincipal.getAttributes();

            if ( attributes != null && attributes.size() > 0 )
            {
                @SuppressWarnings( "unchecked" )
                Iterator<Entry<String,Object>> iterator = attributes.entrySet().iterator();
                
                while ( iterator.hasNext() )
                {
                    Entry<String,Object> curEntry = iterator.next();
                    String key = curEntry.getKey();
                    Object value = curEntry.getValue();
                    if ( value instanceof String )
                    {
                        builder.append( key ).append( ": " ).append( value ).append( HTML_ENDLINE ).append( endl );
                    }
                    else if ( value instanceof List )
                    {
                        builder.append( key ).append( " is a List:<br/>" ).append( endl );
                        for( Object o : ( (List) value ) )
                        {
                            builder.append( "&nbsp;&nbsp;&nbsp;" ).append( o.toString() ).append( HTML_ENDLINE ).append( endl );
                        }
                    }
                }
            }
            else
            {
                builder.append( "None" ).append( HTML_ENDLINE + HTML_ENDLINE ).append( endl );
            }

        }

        builder.append( "<h3>Cookies:</h3>" ).append( endl );
        Cookie[] cookies = request.getCookies();
        if ( cookies != null && cookies.length > 0 )
        {
            builder.append( "getCookies() = <br/>" ).append( endl );
            for( Cookie o : cookies )
            {
                builder.append( "&nbsp;&nbsp;&nbsp;" ).append( o.getName() ).append( ": " ).append( o.getValue() ).append( HTML_ENDLINE ).append( endl );
            }
        }
        else
        {
            builder.append( "getCookies() = null<br/>" ).append( endl );
        }

        builder.append( "<h3>Headers:</h3>" ).append( endl );
        Enumeration headers = request.getHeaderNames();

        if ( headers != null )
        {
            builder.append( "getHeaders() = <br/>" ).append( endl );
            while ( headers.hasMoreElements() )
            {
                String name = (String) headers.nextElement();
                builder.append( "&nbsp;&nbsp;&nbsp;" ).append( name ).append( ": " ).append( request.getHeader( name ) ).append( HTML_ENDLINE ).append( endl );
            }
        }
        else
        {
            builder.append( "getHeaderNames() = null<br/>" ).append( endl );
        }
   
        builder.append( "</body></html>" ).append( endl );
        out.println( builder.toString() );
    }
}

