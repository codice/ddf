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
package org.codice.ddf.security.validator.uname;

import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collection;
import javax.xml.bind.JAXBElement;
import javax.xml.namespace.QName;
import junit.framework.Assert;
import org.apache.cxf.sts.STSPropertiesMBean;
import org.apache.cxf.sts.request.ReceivedToken;
import org.apache.cxf.sts.token.validator.TokenValidatorParameters;
import org.apache.cxf.sts.token.validator.TokenValidatorResponse;
import org.apache.cxf.ws.security.sts.provider.model.secext.BinarySecurityTokenType;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.cxf.ws.security.tokenstore.TokenStore;
import org.apache.karaf.jaas.config.JaasRealm;
import org.apache.wss4j.common.crypto.Merlin;
import org.apache.wss4j.dom.handler.RequestData;
import org.apache.wss4j.dom.validate.Credential;
import org.apache.wss4j.dom.validate.JAASUsernameTokenValidator;
import org.codice.ddf.parser.xml.XmlParser;
import org.codice.ddf.security.common.FailedLoginDelayer;
import org.codice.ddf.security.handler.api.BSTAuthenticationToken;
import org.codice.ddf.security.handler.api.UPAuthenticationToken;
import org.junit.Before;
import org.junit.Test;
import org.osgi.framework.ServiceReference;

public class UPBSTValidatorTest {

  private final JAASUsernameTokenValidator niceValidator =
      new JAASUsernameTokenValidator() {
        public Credential validate(Credential credential, RequestData data) {

          return null;
        }
      };

  private final JAASUsernameTokenValidator meanValidator = new JAASUsernameTokenValidator();

  private JAXBElement<BinarySecurityTokenType> upbstToken;

  private STSPropertiesMBean stsPropertiesMBean;

  private FailedLoginDelayer failedLoginDelayer;

  @Before
  public void setup() {
    niceValidator.setContextName("realm");
    meanValidator.setContextName("realm");
    stsPropertiesMBean = mock(STSPropertiesMBean.class);
    when(stsPropertiesMBean.getSignatureCrypto()).thenReturn(new Merlin());
    when(stsPropertiesMBean.getCallbackHandler()).thenReturn(callbacks -> {});

    UPAuthenticationToken upAuthenticationToken =
        new UPAuthenticationToken("good", "password", "realm");
    BinarySecurityTokenType binarySecurityTokenType = new BinarySecurityTokenType();
    binarySecurityTokenType.setValueType(UPAuthenticationToken.UP_TOKEN_VALUE_TYPE);
    binarySecurityTokenType.setEncodingType(BSTAuthenticationToken.BASE64_ENCODING);
    binarySecurityTokenType.setId(UPAuthenticationToken.BST_USERNAME_LN);
    binarySecurityTokenType.setValue(upAuthenticationToken.getEncodedCredentials());
    upbstToken =
        new JAXBElement<>(
            new QName(
                "http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd",
                "BinarySecurityToken"),
            BinarySecurityTokenType.class,
            binarySecurityTokenType);

    failedLoginDelayer = mock(FailedLoginDelayer.class);
  }

  @Test
  public void testValidateGoodTokenNoCache() {
    UPBSTValidator upbstValidator = getUpbstValidator(new XmlParser(), niceValidator);

    upbstValidator.addRealm(null);
    TokenValidatorParameters tokenParameters = new TokenValidatorParameters();
    ReceivedToken validateTarget = new ReceivedToken(upbstToken);
    tokenParameters.setToken(validateTarget);
    tokenParameters.setStsProperties(stsPropertiesMBean);
    TokenValidatorResponse response = upbstValidator.validateToken(tokenParameters);

    Assert.assertEquals(ReceivedToken.STATE.VALID, response.getToken().getState());

    verify(failedLoginDelayer, never()).delay(anyString());
  }

  @Test
  public void testValidateGoodTokenCache() {
    UPBSTValidator upbstValidator = getUpbstValidator(new XmlParser(), meanValidator);

    upbstValidator.addRealm(null);
    TokenValidatorParameters tokenParameters = new TokenValidatorParameters();
    tokenParameters.setTokenStore(
        new TokenStore() {
          @Override
          public void add(SecurityToken token) {}

          @Override
          public void add(String identifier, SecurityToken token) {}

          @Override
          public void remove(String identifier) {}

          @Override
          public Collection<String> getTokenIdentifiers() {
            return null;
          }

          @Override
          public SecurityToken getToken(String identifier) {
            SecurityToken securityToken = new SecurityToken();
            securityToken.setTokenHash(584149325);
            return securityToken;
          }
        });
    ReceivedToken validateTarget = new ReceivedToken(upbstToken);
    tokenParameters.setToken(validateTarget);
    tokenParameters.setStsProperties(stsPropertiesMBean);
    TokenValidatorResponse response = upbstValidator.validateToken(tokenParameters);

    Assert.assertEquals(ReceivedToken.STATE.VALID, response.getToken().getState());

    verify(failedLoginDelayer, never()).delay(anyString());
  }

  @Test
  public void testValidateBadTokenNoCache() {
    UPBSTValidator upbstValidator = getUpbstValidator(new XmlParser(), meanValidator);

    upbstValidator.addRealm(null);
    TokenValidatorParameters tokenParameters = new TokenValidatorParameters();
    ReceivedToken validateTarget = new ReceivedToken(upbstToken);
    tokenParameters.setToken(validateTarget);
    tokenParameters.setStsProperties(stsPropertiesMBean);
    TokenValidatorResponse response = upbstValidator.validateToken(tokenParameters);

    Assert.assertEquals(ReceivedToken.STATE.INVALID, response.getToken().getState());

    verify(failedLoginDelayer, times(1)).delay(anyString());
  }

  @Test
  public void testValidateBadTokenCache() {
    UPBSTValidator upbstValidator = getUpbstValidator(new XmlParser(), meanValidator);

    upbstValidator.addRealm(null);
    TokenValidatorParameters tokenParameters = new TokenValidatorParameters();
    tokenParameters.setTokenStore(
        new TokenStore() {
          @Override
          public void add(SecurityToken token) {}

          @Override
          public void add(String identifier, SecurityToken token) {}

          @Override
          public void remove(String identifier) {}

          @Override
          public Collection<String> getTokenIdentifiers() {
            return null;
          }

          @Override
          public SecurityToken getToken(String identifier) {
            SecurityToken securityToken = new SecurityToken();
            securityToken.setTokenHash(-1432225336);
            return securityToken;
          }
        });
    ReceivedToken validateTarget = new ReceivedToken(upbstToken);
    tokenParameters.setToken(validateTarget);
    tokenParameters.setStsProperties(stsPropertiesMBean);
    TokenValidatorResponse response = upbstValidator.validateToken(tokenParameters);

    Assert.assertEquals(ReceivedToken.STATE.INVALID, response.getToken().getState());

    verify(failedLoginDelayer, times(1)).delay(anyString());
  }

  @Test(expected = IllegalStateException.class)
  public void testNoParser() {
    UPBSTValidator upbstValidator = getUpbstValidator(null, meanValidator);

    upbstValidator.addRealm(null);
    TokenValidatorParameters tokenParameters = new TokenValidatorParameters();
    tokenParameters.setTokenStore(
        new TokenStore() {
          @Override
          public void add(SecurityToken token) {}

          @Override
          public void add(String identifier, SecurityToken token) {}

          @Override
          public void remove(String identifier) {}

          @Override
          public Collection<String> getTokenIdentifiers() {
            return null;
          }

          @Override
          public SecurityToken getToken(String identifier) {
            SecurityToken securityToken = new SecurityToken();
            securityToken.setTokenHash(584149325);
            return securityToken;
          }
        });
    ReceivedToken validateTarget = new ReceivedToken(upbstToken);
    tokenParameters.setToken(validateTarget);
    tokenParameters.setStsProperties(stsPropertiesMBean);
    upbstValidator.validateToken(tokenParameters);
  }

  @Test(expected = IllegalStateException.class)
  public void testNoFailedDelayer() {
    UPBSTValidator upbstValidator =
        new UPBSTValidator(new XmlParser(), null) {
          public void addRealm(ServiceReference<JaasRealm> serviceReference) {
            validators.put("realm", meanValidator);
          }
        };

    upbstValidator.addRealm(null);
    TokenValidatorParameters tokenParameters = new TokenValidatorParameters();
    tokenParameters.setTokenStore(
        new TokenStore() {
          @Override
          public void add(SecurityToken token) {}

          @Override
          public void add(String identifier, SecurityToken token) {}

          @Override
          public void remove(String identifier) {}

          @Override
          public Collection<String> getTokenIdentifiers() {
            return null;
          }

          @Override
          public SecurityToken getToken(String identifier) {
            SecurityToken securityToken = new SecurityToken();
            securityToken.setTokenHash(584149325);
            return securityToken;
          }
        });
    ReceivedToken validateTarget = new ReceivedToken(upbstToken);
    tokenParameters.setToken(validateTarget);
    tokenParameters.setStsProperties(stsPropertiesMBean);
    upbstValidator.validateToken(tokenParameters);
  }

  private UPBSTValidator getUpbstValidator(
      final XmlParser parser, final JAASUsernameTokenValidator validator) {
    return new UPBSTValidator(parser, failedLoginDelayer) {
      public void addRealm(ServiceReference<JaasRealm> serviceReference) {
        validators.put("realm", validator);
      }
    };
  }
}
