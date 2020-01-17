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
package ddf.security.samlp.impl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import com.google.common.collect.ImmutableSet;
import ddf.security.samlp.SamlProtocol.Binding;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.apache.commons.io.IOUtils;
import org.junit.BeforeClass;
import org.junit.Test;

public class SPMetadataParserTest {

  private static Set<Binding> bindingSet;

  private static final Map EMPTY_MAP = Collections.emptyMap();

  private static EntityInformation testInfo;

  private static EntityInformation testInfo2;

  private static final String METADATA_FILE = "SPMetadata";
  private static final String METADATA_FILE_2 = "SPMetadata2";

  private static final String CERTIFICATE_NAME = "Certificate.cert";

  private static final String ENTITY_ID =
      "http://localhost:80/spring-security-saml2-sample/saml/metadata";
  private static final String ENTITY_ID_2 = "https://localhost:8993/services/saml/test";

  private static final String REDIRECT_LOGOUT_URL =
      "https://localhost:8993/services/saml/logout/redirect";
  private static final String POST_LOGOUT_URL = "https://localhost:8993/services/saml/logout/post";
  private static final String REDIRECT_ACS_URL =
      "https://localhost:8993/services/saml/sso/redirect";
  private static final String POST_ACS_URL = "https://localhost:8993/services/saml/sso/post";
  private static final String SOAP_ACS_URL = "https://localhost:8993/services/saml/sso/soap";
  private static String certificate;

  @BeforeClass
  public static void setupClass() throws Exception {

    // read Certificate file into certificate
    certificate =
        IOUtils.toString(
            SPMetadataParserTest.class.getClassLoader().getResourceAsStream(CERTIFICATE_NAME));

    // read SPMetadata file into spMetadata
    List<String> spMetadata = new ArrayList<>();
    spMetadata.add(
        IOUtils.toString(
            SPMetadataParserTest.class.getClassLoader().getResourceAsStream(METADATA_FILE)));
    spMetadata.add(
        IOUtils.toString(
            SPMetadataParserTest.class.getClassLoader().getResourceAsStream(METADATA_FILE_2)));

    // set up binding set
    bindingSet = ImmutableSet.of(Binding.HTTP_POST, Binding.HTTP_REDIRECT, Binding.SOAP);

    // set up metadata
    Map<String, EntityInformation> testMap = SPMetadataParser.parse(spMetadata, bindingSet);

    // get the METADATA
    testInfo = testMap.get(ENTITY_ID);
    testInfo2 = testMap.get(ENTITY_ID_2);
  }

  @Test
  public void testParseNullMetadata() throws Exception {
    Map<String, EntityInformation> testMap = SPMetadataParser.parse(null, bindingSet);

    assertThat(testMap, is(equalTo(EMPTY_MAP)));
  }

  @Test
  public void testInfoIsPresent() throws Exception {
    assertThat(
        String.format("Entity ID %s was not found in the map", ENTITY_ID),
        testInfo,
        is(notNullValue()));
    assertThat(
        String.format("Entity ID %s was not found in the map", ENTITY_ID_2),
        testInfo2,
        is(notNullValue()));
  }

  @Test
  public void testSigningCertificates() {
    assertThat(testInfo.getSigningCertificate(), is(equalTo(certificate)));
    assertThat(testInfo2.getSigningCertificate(), is(equalTo(certificate)));
  }

  @Test
  public void testEncryptionCertficates() {
    assertThat(testInfo.getEncryptionCertificate(), is(equalTo(certificate)));
    assertThat(testInfo2.getEncryptionCertificate(), is(equalTo(certificate)));
  }

  @Test
  public void testRedirectLogoutUrls() {
    assertThat(
        testInfo.getLogoutService(Binding.HTTP_REDIRECT).getUrl(),
        is(equalTo(REDIRECT_LOGOUT_URL)));
    assertThat(
        testInfo2.getLogoutService(Binding.HTTP_REDIRECT).getUrl(),
        is(equalTo(REDIRECT_LOGOUT_URL)));
  }

  @Test
  public void testPostLogoutUrls() {
    assertThat(
        testInfo2.getLogoutService(Binding.HTTP_POST).getUrl(), is(equalTo(POST_LOGOUT_URL)));
    assertThat(testInfo.getLogoutService(Binding.HTTP_POST).getUrl(), is(equalTo(POST_LOGOUT_URL)));
  }

  @Test
  public void testRedirectAssertionUrls() {
    assertThat(
        testInfo.getAssertionConsumerService(null, Binding.HTTP_REDIRECT, null).getUrl(),
        is(equalTo(REDIRECT_ACS_URL)));
    assertThat(
        testInfo2.getAssertionConsumerService(null, Binding.HTTP_REDIRECT, null).getUrl(),
        is(equalTo(REDIRECT_ACS_URL)));
  }

  @Test
  public void testPostAssertionUrls() {
    assertThat(
        testInfo.getAssertionConsumerService(null, Binding.HTTP_POST, null).getUrl(),
        is(equalTo(POST_ACS_URL)));
    assertThat(
        testInfo2.getAssertionConsumerService(null, Binding.HTTP_POST, null).getUrl(),
        is(equalTo(POST_ACS_URL)));
  }

  @Test
  public void testSoapAssertionUrl() {
    assertThat(
        testInfo2.getAssertionConsumerService(null, Binding.SOAP, null).getUrl(),
        is(equalTo(SOAP_ACS_URL)));
    // no soap
    assertThat(
        String.format(
            "Expected to not find a SOAP binding (url: %s), and for the method to return the default Post binding (url: %s) instead",
            SOAP_ACS_URL, POST_ACS_URL),
        testInfo.getAssertionConsumerService(null, Binding.SOAP, null).getUrl(),
        is(equalTo(POST_ACS_URL)));
  }

  @Test
  public void testArtifactAssertionUrl() {
    assertThat(
        testInfo.getAssertionConsumerService(null, Binding.HTTP_REDIRECT, null).getUrl(),
        is(equalTo(REDIRECT_ACS_URL)));
    // no artifact
    assertThat(
        String.format(
            "Expected to not find an artifact binding (url: Non-existent), and for the method to return the default Redirect binding (url: %s) instead",
            REDIRECT_ACS_URL),
        testInfo2.getAssertionConsumerService(null, Binding.HTTP_ARTIFACT, null).getUrl(),
        is(equalTo(REDIRECT_ACS_URL)));
  }
}
