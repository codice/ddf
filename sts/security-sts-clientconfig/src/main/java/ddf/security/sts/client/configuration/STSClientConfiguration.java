/**
 * Copyright (c) Codice Foundation
 * 
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 * 
 **/
package ddf.security.sts.client.configuration;

import java.util.List;

/**
 * Interface for service containing STS client configurations. This can be used by clients to an STS
 * when they want to communicate with it.
 * 
 */
public interface STSClientConfiguration {
    /**
     * Retrieves the address of the STS server.
     * 
     * @return String-based URL of the STS endpoint with no "WSDL" on the end.
     */
    public String getAddress();

    /**
     * Sets the address of the STS server.
     * 
     * @param address
     *            String-based URL of the STS endpoint with no "WSDL" on the end.
     */
    public void setAddress(String address);

    /**
     * Retrieves the endpoint name of the STS service.
     * <p/>
     * Default is <b>{http://docs.oasis-open.org/ws-sx/ws-trust/200512/}STS_Port</b>
     * 
     * @return String-based endpoint name
     */
    public String getEndpointName();

    /**
     * Sets the endpoint name of the STS service.
     * 
     * @param endpointName
     *            String-based endpoint name.
     */
    public void setEndpointName(String endpointName);

    /**
     * Retrieves the service name of the STS service.
     * <p/>
     * Default is <b>{http://docs.oasis-open.org/ws-sx/ws-trust/200512/} SecurityTokenService</b>
     * 
     * @return String-based service name
     */
    public String getServiceName();

    /**
     * Sets the service name of the STS service.
     * 
     * @param serviceName
     *            String-based service name
     */
    public void setServiceName(String serviceName);

    /**
     * Retrieves the user's name for performing operations on the STS.
     * 
     * @return username
     */
    public String getUsername();

    /**
     * Sets the user's name to use for performing STS operations.
     * 
     * @param username
     */
    public void setUsername(String username);

    /**
     * Retrieves the password for the associated username set in {@link #setUsername()}
     * 
     * @return password
     */
    public String getPassword();

    /**
     * Sets the password for the current user.
     * 
     * @param password
     */
    public void setPassword(String password);

    /**
     * Per <a href="http://cxf.apache.org/docs/ws-securitypolicy.html">WS-Security Policy</a>:
     * <p/>
     * The user's name for signature. It is used as the alias name in the keystore to get the user's
     * cert and private key for signature.
     * 
     * @return username
     */
    public String getSignatureUsername();

    /**
     * Sets the user's signature name.
     * 
     * @param signatureUsername
     */
    public void setSignatureUsername(String signatureUsername);

    /**
     * Per <a href="http://cxf.apache.org/docs/ws-securitypolicy.html">WS-Security Policy</a>:
     * <p/>
     * Location of the crypto property configuration to use for signature.
     * 
     * @return Location of the property file.
     */
    public String getSignatureProperties();

    /**
     * Sets the location of the signature properties file.
     * 
     * @param signatureProperties
     */
    public void setSignatureProperties(String signatureProperties);

    /**
     * Per <a href="http://cxf.apache.org/docs/ws-securitypolicy.html">WS-Security Policy</a>:
     * <p/>
     * The user's name for encryption. It is used as the alias name in the keystore to get the
     * user's public key for encryption.
     * 
     * @return user's name for encryption.
     */
    public String getEncryptionUsername();

    /**
     * Sets the user's name for encryption.
     * 
     * @param encryptionUsername
     */
    public void setEncryptionUsername(String encryptionUsername);

    /**
     * Per <a href="http://cxf.apache.org/docs/ws-securitypolicy.html">WS-Security Policy</a>:
     * <p/>
     * Location of the crypto property configuration to use for encryption.
     * 
     * @return Location of the property file.
     */
    public String getEncryptionProperties();

    /**
     * Sets the location of the encryption properties file.
     * 
     * @param encryptionProperties
     */
    public void setEncryptionProperties(String encryptionProperties);

    /**
     * Per <a href="http://cxf.apache.org/docs/ws-securitypolicy.html">WS-Security Policy</a>:
     * <p/>
     * The alias name in the keystore to get the user's public key to send to the STS for the
     * PublicKey KeyType case.
     * 
     * @return user's name for the publickey.
     */
    public String getTokenUsername();

    /**
     * Sets the alias name for the user's public key.
     * 
     * @param tokenUsername
     */
    public void setTokenUsername(String tokenUsername);

    /**
     * Per <a href="http://cxf.apache.org/docs/ws-securitypolicy.html">WS-Security Policy</a>:
     * <p/>
     * Location of the crypto property configuration used by the STSClient to send/process any
     * RSA/DSAKeyValue tokens used if the KeyType is "PublicKey".
     * 
     * @return Location of the property file.
     */
    public String getTokenProperties();

    /**
     * Sets the location of the token properties file.
     * 
     * @param tokenProperties
     */
    public void setTokenProperties(String tokenProperties);

    /**
     * Retrieves the list of claims that should be requested from the STS.
     * 
     * @return List of string-based claim URIs.
     */
    public List<String> getClaims();

    /**
     * Sets the claim list with the incoming list.
     * 
     * @param claims
     */
    public void setClaims(List<String> claims);

    /**
     * Sets the claim list with the incoming comma-delimieted string of URI values.
     * 
     * @param claims
     */
    public void setClaims(String claims);

    /**
     * Retrieves the assertion type that should be requested from the STS.
     *
     * @return assertion type
     */
    public String getAssertionType();

    /**
     * Sets the assertion type that should be requested from the STS.
     *
     * @param assertionType
     */
    public void setAssertionType(String assertionType);

    /**
     * Retrieves the key type that should be used when communicating with the STS.
     *
     * @return key type
     */
    public String getKeyType();

    /**
     * Sets the key type that should be used when communicating with the STS.
     *
     * @param keyType
     */
    public void setKeyType(String keyType);

    /**
     * Retrieves the size of the key that should be used.
     *
     * @return key size
     */
    public String getKeySize();

    /**
     * Sets the size of the key that should be used.
     *
     * @param keySize
     */
    public void setKeySize(String keySize);

    /**
     * Flags whether or not to supply a key in the request.
     *
     * @return true to supply a key
     */
    public Boolean getUseKey();

    /**
     * Sets whether or not to supply a key in the request.
     *
     * @param useKey
     */
    public void setUseKey(Boolean useKey);

}
