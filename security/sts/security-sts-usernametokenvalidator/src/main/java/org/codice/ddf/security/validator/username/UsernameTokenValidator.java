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
/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.codice.ddf.security.validator.username;

import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.sts.QNameConstants;
import org.apache.cxf.sts.STSConstants;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.realm.UsernameTokenRealmCodec;
import org.apache.cxf.sts.token.validator.TokenValidator;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.model.ObjectFactory;
import org.apache.cxf.ws.security.sts.provider.model.secext.UsernameTokenType;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.common.principal.CustomTokenPrincipal;
import org.apache.wss4j.common.principal.WSUsernameTokenPrincipalImpl;
import org.apache.wss4j.dom.WSConstants;
import org.apache.wss4j.dom.WSSConfig;
import org.apache.wss4j.dom.bsp.BSPEnforcer;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.message.token.UsernameToken;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.JAASUsernameTokenValidator;
import org.apache.wss4j.dom.validate.Validator;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.security.auth.callback.CallbackHandler;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import java.security.Principal;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * This class validates a wsse UsernameToken.
 */
public class UsernameTokenValidator implements TokenValidator {

    private static final Logger LOGGER = LoggerFactory.getLogger(UsernameTokenValidator.class);

    private UsernameTokenRealmCodec usernameTokenRealmCodec;

    protected Map<String, Validator> validators = new ConcurrentHashMap<>();

    public void addRealm(ServiceReference<JaasRealm> serviceReference) {
        JaasRealm realm = FrameworkUtil.getBundle(UsernameTokenValidator.class).getBundleContext()
                .getService(serviceReference);
        LOGGER.trace("Adding validator for JaasRealm {}", realm.getName());
        JAASUsernameTokenValidator validator = new JAASUsernameTokenValidator();
        validator.setContextName(realm.getName());
        validators.put(realm.getName(), validator);
    }

    public void removeRealm(ServiceReference<JaasRealm> serviceReference) {
        JaasRealm realm = FrameworkUtil.getBundle(UsernameTokenValidator.class).getBundleContext()
                .getService(serviceReference);
        LOGGER.trace("Removing validator for JaasRealm {}", realm.getName());
        validators.remove(realm.getName());
    }

    /**
     * Set the UsernameTokenRealmCodec instance to use to return a realm from a validated token
     * @param usernameTokenRealmCodec the UsernameTokenRealmCodec instance to use to return a
     *                                realm from a validated token
     */
    public void setUsernameTokenRealmCodec(UsernameTokenRealmCodec usernameTokenRealmCodec) {
        this.usernameTokenRealmCodec = usernameTokenRealmCodec;
    }

    /**
     * Return true if this TokenValidator implementation is capable of validating the
     * ReceivedToken argument.
     */
    public boolean canHandleToken(ReceivedToken validateTarget) {
        return canHandleToken(validateTarget, null);
    }

    /**
     * Return true if this TokenValidator implementation is capable of validating the
     * ReceivedToken argument. The realm is ignored in this token Validator.
     */
    public boolean canHandleToken(ReceivedToken validateTarget, String realm) {
        if (validateTarget.getToken() instanceof UsernameTokenType) {
            return true;
        }
        return false;
    }

    /**
     * Validate a Token using the given TokenValidatorParameters.
     */
    public TokenValidatorResponse validateToken(TokenValidatorParameters tokenParameters) {
        LOGGER.info("Validating UsernameToken");
        STSPropertiesMBean stsProperties = tokenParameters.getStsProperties();
        Crypto sigCrypto = stsProperties.getSignatureCrypto();
        CallbackHandler callbackHandler = stsProperties.getCallbackHandler();

        RequestData requestData = new RequestData();
        requestData.setSigVerCrypto(sigCrypto);
        WSSConfig wssConfig = WSSConfig.getNewInstance();
        requestData.setWssConfig(wssConfig);
        requestData.setCallbackHandler(callbackHandler);

        TokenValidatorResponse response = new TokenValidatorResponse();
        ReceivedToken validateTarget = tokenParameters.getToken();
        validateTarget.setState(ReceivedToken.STATE.INVALID);
        response.setToken(validateTarget);

        if (!validateTarget.isUsernameToken()) {
            return response;
        }

        //
        // Turn the JAXB UsernameTokenType into a DOM Element for validation
        //
        UsernameTokenType usernameTokenType = (UsernameTokenType)validateTarget.getToken();

        // Marshall the received JAXB object into a DOM Element
        Element usernameTokenElement = null;
        try {
            Set<Class<?>> classes = new HashSet<>();
            classes.add(ObjectFactory.class);
            classes.add(org.apache.cxf.ws.security.sts.provider.model.wstrust14.ObjectFactory.class);

            JAXBContextCache.CachedContextAndSchemas cache =
                    JAXBContextCache.getCachedContextAndSchemas(classes, null, null, null, false);
            JAXBContext jaxbContext = cache.getContext();

            Marshaller marshaller = jaxbContext.createMarshaller();
            Document doc = DOMUtils.createDocument();
            Element rootElement = doc.createElement("root-element");
            JAXBElement<UsernameTokenType> tokenType =
                    new JAXBElement<>(
                            QNameConstants.USERNAME_TOKEN, UsernameTokenType.class, usernameTokenType
                    );
            marshaller.marshal(tokenType, rootElement);
            usernameTokenElement = (Element)rootElement.getFirstChild();
        } catch (JAXBException ex) {
            LOGGER.warn("", ex);
            return response;
        }

        //
        // Validate the token
        //
        try {
            boolean allowNamespaceQualifiedPasswordTypes =
                    wssConfig.getAllowNamespaceQualifiedPasswordTypes();
            UsernameToken ut =
                    new UsernameToken(usernameTokenElement, allowNamespaceQualifiedPasswordTypes, new BSPEnforcer());
            // The parsed principal is set independent whether validation is successful or not
            response.setPrincipal(new CustomTokenPrincipal(ut.getName()));
            if (ut.getPassword() == null) {
                return response;
            }

            // See if the UsernameToken is stored in the cache
            int hash = ut.hashCode();
            SecurityToken secToken = null;
            if (tokenParameters.getTokenStore() != null) {
                secToken = tokenParameters.getTokenStore().getToken(Integer.toString(hash));
                if (secToken != null && secToken.getTokenHash() != hash) {
                    secToken = null;
                }
            }

            if (secToken == null) {
                Credential credential = new Credential();
                credential.setUsernametoken(ut);
                //Only this section is new, the rest is copied from the apache class
                Set<Map.Entry<String, Validator>> entries = validators.entrySet();
                for (Map.Entry<String, Validator> entry : entries) {
                    try {
                        entry.getValue().validate(credential, requestData);
                        validateTarget.setState(ReceivedToken.STATE.VALID);
                        break;
                    } catch (WSSecurityException ex) {
                        LOGGER.debug("Unable to validate user against {}" + entry.getKey(), ex);
                    }
                }
                if (ReceivedToken.STATE.INVALID.equals(validateTarget.getState())) {
                    return response;
                }
                //end new section
            }

            Principal principal =
                    createPrincipal(
                            ut.getName(), ut.getPassword(), ut.getPasswordType(), ut.getNonce(), ut.getCreated()
                    );

            // Get the realm of the UsernameToken
            String tokenRealm = null;
            if (usernameTokenRealmCodec != null) {
                tokenRealm = usernameTokenRealmCodec.getRealmFromToken(ut);
                // verify the realm against the cached token
                if (secToken != null) {
                    Properties props = secToken.getProperties();
                    if (props != null) {
                        String cachedRealm = props.getProperty(STSConstants.TOKEN_REALM);
                        if (!tokenRealm.equals(cachedRealm)) {
                            validateTarget.setState(ReceivedToken.STATE.INVALID);
                            return response;
                        }
                    }
                }
            }

            // Store the successfully validated token in the cache
            if (tokenParameters.getTokenStore() != null && secToken == null && ReceivedToken.STATE.VALID.equals(validateTarget.getState())) {
                secToken = new SecurityToken(ut.getID());
                secToken.setToken(ut.getElement());
                int hashCode = ut.hashCode();
                String identifier = Integer.toString(hashCode);
                secToken.setTokenHash(hashCode);
                tokenParameters.getTokenStore().add(identifier, secToken);
            }

            response.setPrincipal(principal);
            response.setTokenRealm(tokenRealm);
            validateTarget.setState(ReceivedToken.STATE.VALID);
        } catch (WSSecurityException ex) {
            LOGGER.warn("", ex);
        }

        return response;
    }

    /**
     * Create a principal based on the authenticated UsernameToken.
     */
    private Principal createPrincipal(
            String username,
            String passwordValue,
            String passwordType,
            String nonce,
            String createdTime
    ) {
        boolean hashed = false;
        if (WSConstants.PASSWORD_DIGEST.equals(passwordType)) {
            hashed = true;
        }
        WSUsernameTokenPrincipalImpl principal = new WSUsernameTokenPrincipalImpl(username, hashed);
        if (nonce != null) {
            principal.setNonce(nonce.getBytes());
        }
        principal.setPassword(passwordValue);
        principal.setCreatedTime(createdTime);
        principal.setPasswordType(passwordType);
        return principal;
    }

}
