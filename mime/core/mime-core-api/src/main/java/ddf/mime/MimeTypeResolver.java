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
package ddf.mime;

/**
 * Interface defining an OSGi service that can map a list of file extensions
 * to their corresponding mime types, and vice versa.
 * 
 * @author Hugh Rodgers, Lockheed Martin
 * @author ddf.isgs@lmco.com
 *
 */
public interface MimeTypeResolver
{
    /**
     * Gets the name of the MimeTypeResolver.
     * 
     * @return the name of the MimeTypeResolver
     */
    public String getName();
    
    
    /**
     * Gets the priority of the MimeTypeResolver. The higher the number the higher
     * the priority, meaning the MimeTypeResolver is invoked earlier.
     * 
     * @return the priority of the MimeTypeResolver
     */
    public int getPriority();
    
    
    /**
     * Gets the file extension for the specific mime type. For example, if the
     * mime type is image/nitf a file extension of .nitf would be returned. The mapping
     * of file extensions to mime types is part of the configuration of a MimeTypeResolver.
     * 
     * @param contentType the mime type
     * @return the file extension, including the period in the extension
     */
    public String getFileExtensionForMimeType( String contentType );
    
    
    /**
     * Gets the mime type for the specified file extension.
     * 
     * @param fileExtension the file extension without the period in it
     * @return the mime type
     */
    public String getMimeTypeForFileExtension( String fileExtension ); 
}
