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
package ddf.security.common.callback;

import java.io.IOException;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;

import org.apache.ws.security.WSPasswordCallback;

public class CommonCallbackHandler implements CallbackHandler
{
    private final UserPass[] userPassArr = { new UserPass("tokenissuer", "changeit"), new UserPass("client", "changeit"), 
            new UserPass("server", "changeit")
    };

    public CommonCallbackHandler()
    {
    }

    public void handle(Callback[] callbacks) throws IOException, UnsupportedCallbackException
    {
        boolean set = false;
        for (Callback callback : callbacks)
        {
            if (callback instanceof WSPasswordCallback)
            {
                WSPasswordCallback passwordCallback = (WSPasswordCallback) callback;
                for(UserPass userPass : userPassArr)
                {
                    if (passwordCallback.getIdentifier() != null && passwordCallback.getIdentifier().equalsIgnoreCase(userPass.getUsername()))
                    {
                        passwordCallback.setPassword(userPass.getPassword());
                        set = true;
                        break;
                    }
                }
                if(set)
                {
                    break;
                }
            }
        }
    }
    
    private class UserPass
    {
        private String username;
        private String password;
        
        public UserPass(String username, String password)
        {
            this.username = username;
            this.password = password;
        }
        
        public String getUsername()
        {
            return username;
        }
        
        public String getPassword()
        {
            return password;
        }
        
    }
}