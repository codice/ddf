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
package org.codice.ddf.security.validator.username;

import org.apache.cxf.common.jaxb.JAXBContextCache;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.model.ObjectFactory;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.wss4j.common.crypto.Crypto;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.JAASUsernameTokenValidator;
import org.junit.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class TestUsernameTokenValidator {

    @Test
    public void testValidateBadToken() {
        UsernameTokenValidator usernameTokenValidator = new UsernameTokenValidator();
        List<JaasRealm> jaasRealms = new ArrayList<>();
        JaasRealm jaasRealm = mock(JaasRealm.class);
        when(jaasRealm.getName()).thenReturn("myrealm");
        jaasRealms.add(jaasRealm);
        usernameTokenValidator.setJaasRealmList(jaasRealms);

        TokenValidatorParameters tokenValidatorParameters = mock(TokenValidatorParameters.class);
        STSPropertiesMBean stsPropertiesMBean = mock(STSPropertiesMBean.class);
        when(stsPropertiesMBean.getSignatureCrypto()).thenReturn(mock(Crypto.class));
        when(tokenValidatorParameters.getStsProperties()).thenReturn(stsPropertiesMBean);
        ReceivedToken receivedToken = mock(ReceivedToken.class);
        doCallRealMethod().when(receivedToken).setState(any(ReceivedToken.STATE.class));
        doCallRealMethod().when(receivedToken).getState();
        when(receivedToken.isUsernameToken()).thenReturn(true);
        when(tokenValidatorParameters.getToken()).thenReturn(receivedToken);

        Set<Class<?>> classes = new HashSet<>();
        classes.add(ObjectFactory.class);
        classes.add(org.apache.cxf.ws.security.sts.provider.model.wstrust14.ObjectFactory.class);
        JAXBContextCache.CachedContextAndSchemas cache = null;
        try {
            cache = JAXBContextCache.getCachedContextAndSchemas(classes, null, null, null, false);
        } catch (JAXBException e) {
            fail(e.getMessage());
        }
        JAXBContext jaxbContext = cache.getContext();
        Unmarshaller unmarshaller = null;
        try {
            if (jaxbContext != null) {
                unmarshaller = jaxbContext.createUnmarshaller();
            }
        } catch (JAXBException e) {
            fail(e.getMessage());
        }
        JAXBElement<?> token = null;
        if (unmarshaller != null) {
            try {
                token = (JAXBElement<?>) unmarshaller.unmarshal(this.getClass().getResourceAsStream("/user.xml"));
            } catch (JAXBException e) {
                fail(e.getMessage());
            }
        }
        when(receivedToken.getToken()).thenReturn(token.getValue());

        TokenValidatorResponse tokenValidatorResponse = usernameTokenValidator.validateToken(tokenValidatorParameters);
        assertEquals(ReceivedToken.STATE.INVALID, tokenValidatorResponse.getToken().getState());
    }

    @Test
    public void testValidateGoodToken() {
        UsernameTokenValidator usernameTokenValidator = new UsernameTokenValidator();
        List<JaasRealm> jaasRealms = new ArrayList<>();
        JaasRealm jaasRealm = mock(JaasRealm.class);
        when(jaasRealm.getName()).thenReturn("myrealm");
        jaasRealms.add(jaasRealm);
        usernameTokenValidator.setJaasRealmList(jaasRealms);
        usernameTokenValidator.validators = new HashMap<>();
        JAASUsernameTokenValidator myvalidator = mock(JAASUsernameTokenValidator.class);
        try {
            Credential credential = mock(Credential.class);
            when(myvalidator.validate(any(Credential.class), any(RequestData.class))).thenReturn(credential);
        } catch (WSSecurityException ignore) {
            //do nothing
        }
        usernameTokenValidator.validators.put("myrealm", myvalidator);

        TokenValidatorParameters tokenValidatorParameters = mock(TokenValidatorParameters.class);
        STSPropertiesMBean stsPropertiesMBean = mock(STSPropertiesMBean.class);
        when(stsPropertiesMBean.getSignatureCrypto()).thenReturn(mock(Crypto.class));
        when(tokenValidatorParameters.getStsProperties()).thenReturn(stsPropertiesMBean);
        ReceivedToken receivedToken = mock(ReceivedToken.class);
        doCallRealMethod().when(receivedToken).setState(any(ReceivedToken.STATE.class));
        doCallRealMethod().when(receivedToken).getState();
        when(receivedToken.isUsernameToken()).thenReturn(true);
        when(tokenValidatorParameters.getToken()).thenReturn(receivedToken);

        Set<Class<?>> classes = new HashSet<>();
        classes.add(ObjectFactory.class);
        classes.add(org.apache.cxf.ws.security.sts.provider.model.wstrust14.ObjectFactory.class);
        JAXBContextCache.CachedContextAndSchemas cache = null;
        try {
            cache = JAXBContextCache.getCachedContextAndSchemas(classes, null, null, null, false);
        } catch (JAXBException e) {
            fail(e.getMessage());
        }
        JAXBContext jaxbContext = cache.getContext();
        Unmarshaller unmarshaller = null;
        try {
            if (jaxbContext != null) {
                unmarshaller = jaxbContext.createUnmarshaller();
            }
        } catch (JAXBException e) {
            fail(e.getMessage());
        }
        JAXBElement<?> token = null;
        if (unmarshaller != null) {
            try {
                token = (JAXBElement<?>) unmarshaller.unmarshal(this.getClass().getResourceAsStream("/user.xml"));
            } catch (JAXBException e) {
                fail(e.getMessage());
            }
        }
        when(receivedToken.getToken()).thenReturn(token.getValue());

        TokenValidatorResponse tokenValidatorResponse = usernameTokenValidator.validateToken(tokenValidatorParameters);
        assertEquals(ReceivedToken.STATE.VALID, tokenValidatorResponse.getToken().getState());
    }
}
