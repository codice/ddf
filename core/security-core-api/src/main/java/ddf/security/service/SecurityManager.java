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
package ddf.security.service;


import ddf.security.Subject;


/**
 * Service that is used to perform security operations by endpoints. Currently
 * allows them to exchange a token for a subject.
 * 
 */
public interface SecurityManager
{

    /**
     * Exchanges an authentication token for a subject. The {@link Subject}
     * contains a security assertion which can be used to obtain information
     * about the current credentials.
     * 
     * @param token  An object containing information about the user that can be used to populate the subject.
     * @return  the Subject corresponding to the provided security token
     * @throws SecurityServiceException if an error occurs during the
     *             authentication process
     */
    Subject getSubject( Object token ) throws SecurityServiceException;

}
