/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package org.codice.ddf.security.validator.username;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashSet;
import java.util.Set;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
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
import org.codice.ddf.parser.Parser;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.security.common.FailedLoginDelayer;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

public class UsernameTokenValidatorTest {

  private final JAASUsernameTokenValidator niceValidator = mock(JAASUsernameTokenValidator.class);

  private final JAASUsernameTokenValidator meanValidator = new JAASUsernameTokenValidator();

  private FailedLoginDelayer failedLoginDelayer;

  @Before
  public void setup() {
    try {
      Credential credential = mock(Credential.class);
      when(niceValidator.validate(any(Credential.class), any(RequestData.class)))
          .thenReturn(credential);
    } catch (WSSecurityException ignore) {
      // do nothing
    }

    failedLoginDelayer = mock(FailedLoginDelayer.class);
  }

  @Test
  public void testValidateBadTokenNoTokenStore() {
    UsernameTokenValidator usernameTokenValidator =
        getUsernameTokenValidator(new XmlParser(), meanValidator);
    usernameTokenValidator.addRealm(null);

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
        token =
            (JAXBElement<?>)
                unmarshaller.unmarshal(
                    this.getClass().getResourceAsStream("/user-no-password.xml"));
      } catch (JAXBException e) {
        fail(e.getMessage());
      }
    }
    when(receivedToken.getToken()).thenReturn(token.getValue());

    TokenValidatorResponse tokenValidatorResponse =
        usernameTokenValidator.validateToken(tokenValidatorParameters);
    assertEquals(ReceivedToken.STATE.INVALID, tokenValidatorResponse.getToken().getState());

    verify(failedLoginDelayer, times(1)).delay(anyString());
  }

  @Test
  public void testValidateBadToken() {
    UsernameTokenValidator usernameTokenValidator =
        getUsernameTokenValidator(new XmlParser(), meanValidator);
    usernameTokenValidator.addRealm(null);

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
        token =
            (JAXBElement<?>)
                unmarshaller.unmarshal(this.getClass().getResourceAsStream("/user.xml"));
      } catch (JAXBException e) {
        fail(e.getMessage());
      }
    }
    when(receivedToken.getToken()).thenReturn(token.getValue());

    TokenValidatorResponse tokenValidatorResponse =
        usernameTokenValidator.validateToken(tokenValidatorParameters);
    assertEquals(ReceivedToken.STATE.INVALID, tokenValidatorResponse.getToken().getState());

    verify(failedLoginDelayer, times(1)).delay(anyString());
  }

  @Test
  public void testValidateGoodToken() {
    UsernameTokenValidator usernameTokenValidator =
        getUsernameTokenValidator(new XmlParser(), niceValidator);
    usernameTokenValidator.addRealm(null);

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
        token =
            (JAXBElement<?>)
                unmarshaller.unmarshal(this.getClass().getResourceAsStream("/user.xml"));
      } catch (JAXBException e) {
        fail(e.getMessage());
      }
    }
    when(receivedToken.getToken()).thenReturn(token.getValue());

    TokenValidatorResponse tokenValidatorResponse =
        usernameTokenValidator.validateToken(tokenValidatorParameters);
    assertEquals(ReceivedToken.STATE.VALID, tokenValidatorResponse.getToken().getState());

    verify(failedLoginDelayer, never()).delay(anyString());
  }

  @Test(expected = IllegalStateException.class)
  public void testNoParser() {
    UsernameTokenValidator usernameTokenValidator = getUsernameTokenValidator(null, meanValidator);
    usernameTokenValidator.addRealm(null);

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
        token =
            (JAXBElement<?>)
                unmarshaller.unmarshal(
                    this.getClass().getResourceAsStream("/user-no-password.xml"));
      } catch (JAXBException e) {
        fail(e.getMessage());
      }
    }
    when(receivedToken.getToken()).thenReturn(token.getValue());

    usernameTokenValidator.validateToken(tokenValidatorParameters);
  }

  @Test(expected = IllegalStateException.class)
  public void testNoFailedDelayer() {
    UsernameTokenValidator usernameTokenValidator =
        new UsernameTokenValidator(new XmlParser(), null) {
          public void addRealm(ServiceReference<JaasRealm> serviceReference) {
            validators.put("myrealm", meanValidator);
          }
        };
    usernameTokenValidator.addRealm(null);

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
        token =
            (JAXBElement<?>)
                unmarshaller.unmarshal(
                    this.getClass().getResourceAsStream("/user-no-password.xml"));
      } catch (JAXBException e) {
        fail(e.getMessage());
      }
    }
    when(receivedToken.getToken()).thenReturn(token.getValue());

    usernameTokenValidator.validateToken(tokenValidatorParameters);
  }

  private UsernameTokenValidator getUsernameTokenValidator(
      final Parser parser, final JAASUsernameTokenValidator validator) {
    return new UsernameTokenValidator(parser, failedLoginDelayer) {
      public void addRealm(ServiceReference<JaasRealm> serviceReference) {
        validators.put("myrealm", validator);
      }
    };
  }
}
