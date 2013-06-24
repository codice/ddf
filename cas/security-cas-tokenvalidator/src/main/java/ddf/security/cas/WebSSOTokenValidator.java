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
package ddf.security.cas;

import ddf.catalog.util.DdfConfigurationManager;
import ddf.catalog.util.DdfConfigurationWatcher;
import ddf.security.common.audit.SecurityLogger;
import ddf.security.common.util.CommonSSLFactory;
import ddf.security.encryption.EncryptionService;
import ddf.security.sts.client.configuration.STSClientConfigurationManager;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.request.ReceivedToken.STATE;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.ws.security.WSSConfig;
import org.apache.ws.security.WSSecurityException;
import org.apache.ws.security.components.crypto.Crypto;
import org.apache.ws.security.handler.RequestData;
import org.apache.ws.security.util.Base64;
import org.jasig.cas.client.authentication.AttributePrincipal;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.security.auth.callback.CallbackHandler;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

/**
 * Validates Web Single Sign-On Tokens.
 *
 * @author kcwire
 */
public class WebSSOTokenValidator implements TokenValidator, DdfConfigurationWatcher
{

    // The Supported SSO Token Types
    public static final String CAS_TYPE = "#CAS";

    private String casServerUrl;

    private String stsAddress;

    private String trustStorePath;

    private String trustStorePassword;

    private String keyStorePath;

    private String keyStorePassword;

    private static final Logger LOGGER = LoggerFactory.getLogger(WebSSOTokenValidator.class);

	private EncryptionService encryptionService;

    public String getCasServerUrl()
    {
        return casServerUrl;
    }

    public void setCasServerUrl(String casServerUrl)
    {
        this.casServerUrl = casServerUrl;
    }

	public void setEncryptionService(EncryptionService encryptionService) 
	{
		this.encryptionService = encryptionService;
	}

    public void setKeyStorePassword(String pw)
    {
        this.keyStorePassword = pw;
    }

    public void setTrustStorePassword(String pw)
    {
        this.trustStorePassword = pw;
    }

    public void setStsAddress(String stsAddress)
    {
        this.stsAddress = stsAddress;
    }

    public void setTrustStorePath(String trustStorePath)
    {
        this.trustStorePath = trustStorePath;
    }

    public void setKeyStorePath(String keyStorePath)
    {
        this.keyStorePath = keyStorePath;
    }

    /*
     * Return true if this TokenValidator implementation is capable of
     * validating the ReceivedToken argument.
     */
    @Override
    public boolean canHandleToken(ReceivedToken validateTarget)
    {
        return canHandleToken(validateTarget, null);
    }

    /*
     * Return true if this TokenValidator implementation is capable of
     * validating the ReceivedToken argument. The realm is ignored in this token
     * Validator.
     */
    @Override
    public boolean canHandleToken(ReceivedToken validateTarget, String realm)
    {
        final Object token = validateTarget.getToken();
        // Check the ValueType to see if this is a supported SSO Token.
        if ((token instanceof BinarySecurityTokenType))
        {
            if (CAS_TYPE.equalsIgnoreCase(((BinarySecurityTokenType) token).getValueType()))
            {
                LOGGER.debug("Can handle token type of: " + ((BinarySecurityTokenType) token).getValueType());
                return true;
            }
            LOGGER.debug("Cannot handle token type of: " + ((BinarySecurityTokenType) token).getValueType());
        }

        return false;
    }

    /**
     * Validate a Token using the given TokenValidatorParameters.
     */
    @Override
    public TokenValidatorResponse validateToken(
            TokenValidatorParameters tokenParameters)
    {
        LOGGER.debug("Validating SSO Token");
        STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
        Crypto sigCrypto = stsProperties.getSignatureCrypto();
        CallbackHandler callbackHandler = stsProperties.getCallbackHandler();

        RequestData requestData = new RequestData();
        requestData.setSigCrypto(sigCrypto);
        WSSConfig wssConfig = WSSConfig.getNewInstance();
        requestData.setWssConfig(wssConfig);
        requestData.setCallbackHandler(callbackHandler);

        LOGGER.debug("Setting validate state to invalid before check.");
        TokenValidatorResponse response = new TokenValidatorResponse();
        ReceivedToken validateTarget = tokenParameters.getToken();
        validateTarget.setState(STATE.INVALID);
        response.setToken(validateTarget);

        if (!validateTarget.isBinarySecurityToken())
        {
            LOGGER.debug("Validate target is not a binary security token, returning invalid response.");
            return response;
        }
        LOGGER.debug("Getting binary security token from validate target");
        BinarySecurityTokenType binarySecurityToken = (BinarySecurityTokenType) validateTarget.getToken();

        //
        // Decode the token
        //
        LOGGER.debug("Decoding binary security token.");
        String base64Token = binarySecurityToken.getValue();
        String decodedToken = null;
        try
        {
            decodedToken = new String(Base64.decode(base64Token), Charset.forName("UTF-8"));
            LOGGER.debug("Binary security token successfully decoded: " + decodedToken);
        }
        catch (WSSecurityException e)
        {
            LOGGER.error("Unable to decode BST.", e);
        }

        //
        // Do some validation of the token here
        //
        // Token is in the format ticket|service
        //
        try
        {
            //Assertion assertion = casValidator.validate(ticket, service);
            LOGGER.debug("Validating ticket [{}] for service [{}].", decodedToken, stsAddress);
            SecurityLogger.logInfo("Validating ticket [" + decodedToken + "] for service [" + stsAddress + "].");
            
            //validate either returns an assertion or throws an exception
            Assertion assertion = validate(decodedToken, stsAddress);

            AttributePrincipal principal = assertion.getPrincipal();
            LOGGER.debug("principal name: " + principal.getName());
            
            LOGGER.debug("Principal {} has {} attribute(s).", principal.getName(), principal.getAttributes().size());
            SecurityLogger.logInfo("Principal " + principal.getName() + " has " + principal.getAttributes().size() +" attribute(s).");
            
            for(Object key : principal.getAttributes().keySet())
            {
                LOGGER.debug("Key [{}], value [{}].", key, principal.getAttributes().get( key ));
            }
            
            response.setPrincipal(principal);
            LOGGER.debug("CAS ticket successfully validated, setting state to valid.");
            SecurityLogger.logInfo("CAS ticket successfully validated, setting state to valid.");
            validateTarget.setState(STATE.VALID);

        }
        catch (TicketValidationException e)
        {
            LOGGER.error("Unable to validate CAS token.", e);
            SecurityLogger.logError("Unable to validate CAS token.");
        }

        return response;
    }

    /**
     * Updates SSL settings from the DDF ConfigurationManager
     * @param properties
     */
    @Override
    public void ddfConfigurationUpdated(@SuppressWarnings("rawtypes") Map properties)
    {
        String setTrustStorePath = (String) properties.get(DdfConfigurationManager.TRUST_STORE);
        if (setTrustStorePath != null)
        {
            LOGGER.debug("Setting trust store path: " + setTrustStorePath);
            this.trustStorePath = setTrustStorePath;
        }

        String setTrustStorePassword = (String) properties.get(DdfConfigurationManager.TRUST_STORE_PASSWORD);
        if (setTrustStorePassword != null)
        {
            LOGGER.debug("Changing trust store password from {} to {}", this.trustStorePassword, setTrustStorePassword); /*** DEBUG ONLY ***/
            this.trustStorePassword = setTrustStorePassword;
    		if(encryptionService == null)
    		{
    		    LOGGER.error("The WebSSOTokenValidator has a null Encryption Service. Unable to decrypt " +
    		    		"the encrypted trustStore password. Setting decrypted password to null.");
    		    this.trustStorePassword = null;
    		}
    		else
    		{
        	    setTrustStorePassword = encryptionService.decryptValue(setTrustStorePassword);
                LOGGER.debug("Setting trust store password.");
                this.trustStorePassword = setTrustStorePassword;
    		}
        }

        String setKeyStorePath = (String) properties.get(DdfConfigurationManager.KEY_STORE);
        if (setKeyStorePath != null)
        {
            LOGGER.debug("Setting key store path: " + setKeyStorePath);
            this.keyStorePath = setKeyStorePath;
        }

        String setKeyStorePassword = (String) properties.get(DdfConfigurationManager.KEY_STORE_PASSWORD);
        if (setKeyStorePassword != null)
        {
            LOGGER.debug("Changing key store password from {} to {}", this.keyStorePassword, setKeyStorePassword); /*** DEBUG ONLY ***/
            this.keyStorePassword = setKeyStorePassword;
    		if(encryptionService == null)
    		{
    		    LOGGER.error("The WebSSOTokenValidator has a null Encryption Service. Unable to decrypt " +
    		    		"the encrypted keyStore password. Setting decrypted password to null.");
    		    this.keyStorePassword = null;
    		}
    		else
    		{
        	    setKeyStorePassword = encryptionService.decryptValue(setKeyStorePassword);
                LOGGER.debug("Setting key store password.");
                this.keyStorePassword = setKeyStorePassword;
    		}
        }

        String setStsAddress = (String) properties.get(STSClientConfigurationManager.STS_ADDRESS);
        if (setStsAddress != null)
        {
            LOGGER.debug("Setting STS address: " + setStsAddress);
            this.stsAddress = setStsAddress;
        }
    }

    /**
     * Validate the CAS ticket and service
     * @param ticket
     * @param service
     * @return
     * @throws TicketValidationException
     */
    public Assertion validate(String ticket, String service)
            throws TicketValidationException
    {
        LOGGER.trace("CAS Server URL = " + casServerUrl);
        try{
            HttpsURLConnection.setDefaultSSLSocketFactory(CommonSSLFactory.createSocket(trustStorePath, trustStorePassword,
            keyStorePath, keyStorePassword));
        }
        catch (IOException ioe)
        {
            throw new TicketValidationException("Could not set up connection to CAS Server.", ioe);
        }
   
        Cas20ProxyTicketValidator casValidator = new Cas20ProxyTicketValidator(casServerUrl);
        casValidator.setAcceptAnyProxy(true);
        
        return casValidator.validate(ticket, service); 
    }

}
