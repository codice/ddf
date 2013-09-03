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
 * Interface defining a mapper that accesses {@link MimeTypeResolver}s to retieve
 * file extension for a given mime type, and vice versa.
 * 
 * @author Hugh Rodgers, Lockheed Martin
 * @author ddf.isgs@lmco.com
 *
 */
public interface MimeTypeMapper
{
    /**
     * Retrieves the file extension for the specified mime type, e.g., returns ".nitf"
     * for a mime type of "image/nitf".
     * 
     * @param contentType the mime type
     * @return the file extension mapped to the specified mime type
     * @throws MimeTypeException if any problems encountered during mime type mapping
     */
    public String getFileExtensionForMimeType( String contentType ) throws MimeTypeResolutionException;
    
    
    /**
     * Retrieves the mime type for the specified file extension, e.g., returns "image/nitf"
     * for a file extension of "nitf".
     * 
     * @param fileExtension the file extension to look up the mime type for
     * @return the mime type mapped to the specified file extension
     * @throws MimeTypeException if any problems encountered during mime type mapping
     */
    public String getMimeTypeForFileExtension( String fileExtension ) throws MimeTypeResolutionException;
}
