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
package ddf.mime.mapper;

import java.util.HashMap;

import ddf.mime.MimeTypeResolver;

//import org.apache.tika.mime.MimeTypeException;


public class MockMimeTypeResolver implements MimeTypeResolver
{
    private HashMap<String,String> customFileExtensionsToMimeTypesMap;
    private HashMap<String,String> customMimeTypesToFileExtensionsMap;
    private String name;
    private int priority;
    
    
    public MockMimeTypeResolver( String name )
    {
        this( name, 10 );
    }
    
    
    public MockMimeTypeResolver( String name, int priority )
    {
        this.name = name;
        this.priority = priority;
        
        this.customFileExtensionsToMimeTypesMap = new HashMap<String,String>();
        customFileExtensionsToMimeTypesMap.put( "nitf", "image/nitf" );
        customFileExtensionsToMimeTypesMap.put( "ntf", "image/nitf" );
        
        this.customMimeTypesToFileExtensionsMap = new HashMap<String,String>();
        customMimeTypesToFileExtensionsMap.put( "image/nitf", "nitf" );
    }
    

    @Override
    public String getName()
    {
        return this.name;
    }
    
    
    @Override
    public int getPriority()
    {
        return priority;
    }
    
    
    public void setPriority( int priority )
    {
        this.priority = priority;
    }
    
    
    @Override
    public String getFileExtensionForMimeType( String contentType ) //throws MimeTypeException
    {
        String fileExtension = customMimeTypesToFileExtensionsMap.get( contentType );

        if ( fileExtension != null && !fileExtension.isEmpty() )
        {
            return "." + fileExtension;
        }
        
        return null;
    }

    
    @Override
    public String getMimeTypeForFileExtension( String fileExtension ) //throws MimeTypeException
    {
        String mimeType = customFileExtensionsToMimeTypesMap.get( fileExtension );
        
        return mimeType;
    }

}
