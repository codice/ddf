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
package ddf.security.common.util;


import java.io.Serializable;
import java.util.HashMap;

import org.apache.cxf.message.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ddf.catalog.Constants;
import ddf.security.SecurityConstants;
import ddf.security.Subject;


public class SecurityProperties extends HashMap<String, Serializable>
{

    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = LoggerFactory.getLogger(SecurityProperties.class);

    /**
     * Looks for the Subject object from the incoming CXF message. If available
     * (should have been added by the interceptors) it adds the subject to this
     * map so that it can be added to the query request.
     * 
     * @param message Incoming CXF message that may contain a subject
     */
    public SecurityProperties( Message message )
    {
        if (message != null)
        {
            // grab the SAML assertion associated with this Message from the
            // token store
            Object subjectObj = message.get(Constants.SAML_ASSERTION);
            if ((subjectObj != null) && (subjectObj instanceof Subject))
            {
                LOGGER.debug("Found subject on message, adding to query request.");
                put(SecurityConstants.SECURITY_SUBJECT, (Subject) subjectObj);
            }
            else
            {
                LOGGER.info("Did not receive a proper subject from the message. Instead got this: " + subjectObj);
            }
        }
        else
        {
            LOGGER.warn("Unable to retrieve the current message associated with the web service call.");
        }
    }

}
