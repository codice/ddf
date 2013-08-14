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
package ddf.security.sts.client.configuration;

import java.util.List;

public interface STSClientConfiguration
{
    
    public String getAddress();
    
    public void setAddress(String address);
    
    public String getEndpointName();
    
    public void setEndpointName(String endpointName);
    
    public String getServiceName();
    
    public void setServiceName(String serviceName);
    
    public String getUsername();
    
    public void setUsername(String username);
    
    public String getPassword();
    
    public void setPassword(String password);
    
    public String getSignatureUsername();
    
    public void setSignatureUsername(String signatureUsername);
    
    public String getSignatureProperties();
    
    public void setSignatureProperties(String signatureProperties);
    
    public String getEncryptionUsername();
    
    public void setEncryptionUsername(String encryptionUsername);
    
    public String getEncryptionProperties();
    
    public void setEncryptionProperties(String encryptionProperties);
    
    public String getTokenUsername();
    
    public void setTokenUsername(String tokenUsername);
    
    public String getTokenProperties();
    
    public void setTokenProperties(String tokenProperties);
    
    public List<String> getClaims();
    
    public void setClaims(List<String> claims);
    
    public void setClaims(String claims);

}
