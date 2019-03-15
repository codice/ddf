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
package ddf.test.itests.platform;

import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.authentication.CertificateAuthSettings.certAuthSettings;
import static org.codice.ddf.itests.common.WaitCondition.expect;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingest;
import static org.codice.ddf.itests.common.opensearch.OpenSearchTestCommons.OPENSEARCH_FACTORY_PID;
import static org.codice.ddf.itests.common.opensearch.OpenSearchTestCommons.getOpenSearchSourceProperties;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasXPath;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.CombinableMatcher.both;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import com.jayway.restassured.response.Response;
import ddf.catalog.data.Metacard;
import ddf.security.Subject;
import ddf.security.samlp.MetadataConfigurationParser;
import ddf.security.samlp.SamlProtocol;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Collections;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.XmlSearch;
import org.codice.ddf.itests.common.annotations.ConditionalIgnoreRule;
import org.codice.ddf.itests.common.annotations.SkipUnstableTest;
import org.codice.ddf.security.common.Security;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.codice.ddf.security.handler.api.SessionHandler;
import org.codice.ddf.test.common.LoggingUtils;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.service.cm.Configuration;
import org.xml.sax.SAXException;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class TestSingleSignOn extends AbstractIntegrationTest {

  private static final String IDP_AUTH_TYPES = "/=SAML,/solr=PKI|BASIC";

  private static final String KEY_STORE_PATH = System.getProperty("javax.net.ssl.keyStore");

  private static final String PASSWORD = System.getProperty("javax.net.ssl.keyStorePassword");

  private static final DynamicUrl SEARCH_URL =
      new DynamicUrl(DynamicUrl.SECURE_ROOT, HTTPS_PORT, "/search");

  private static final DynamicUrl IDP_URL = new DynamicUrl(SERVICE_ROOT, "/idp/login");

  private static final DynamicUrl WHO_AM_I_URL =
      new DynamicUrl(DynamicUrl.SECURE_ROOT, HTTPS_PORT, "/whoami");

  private static final DynamicUrl AUTHENTICATION_REQUEST_ISSUER =
      new DynamicUrl(SERVICE_ROOT, "/saml/sso");

  private static final DynamicUrl LOGOUT_REQUEST_URL =
      new DynamicUrl(SERVICE_ROOT, "/logout/actions");

  private static final String RECORD_TITLE_1 = "myTitle";

  public static final String BROWSER_USER_AGENT =
      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36";

  private String ddfSpMetadataEntityId;

  private Subject systemSubject;

  private enum Binding {
    REDIRECT {
      @Override
      public String toString() {
        return "Redirect";
      }
    },
    POST {
      @Override
      public String toString() {
        return "POST";
      }
    }
  }

  private enum SamlSchema {
    METADATA,
    PROTOCOL
  }

  @Rule public ConditionalIgnoreRule rule = new ConditionalIgnoreRule();

  @Before
  public void setup() throws Exception {
    // Get all of the metadata
    String metadata = get(SERVICE_ROOT + "/idp/login/metadata").asString();
    String ddfSpMetadata = get(SERVICE_ROOT + "/saml/sso/metadata").asString();

    // Make sure all the metadata is valid before we set it
    validateSaml(metadata, SamlSchema.METADATA);
    validateSaml(ddfSpMetadata, SamlSchema.METADATA);

    // Save the spMetadata entityId
    new MetadataConfigurationParser(
        Collections.singletonList(ddfSpMetadata), ed -> ddfSpMetadataEntityId = ed.getEntityID());

    // The IdP server can point to multiple Service Providers and as such expects an array.
    // Thus, even though we are only setting a single item, we must wrap it in an array.
    setConfig("org.codice.ddf.security.idp.client.IdpMetadata", "metadata", metadata);
    setConfig(
        "org.codice.ddf.security.idp.server.IdpEndpoint",
        "spMetadata",
        new String[] {ddfSpMetadata});

    Security security = Security.getInstance();
    systemSubject = security.runAsAdmin(security::getSystemSubject);
  }

  @After
  public void teardown() throws Exception {
    // Clear all active login sessions to ensure a clean slate
    SessionHandler sessionHandler = getServiceManager().getService(SessionHandler.class);
    Map<String, Set<String>> activeSessions =
        systemSubject.execute(sessionHandler::getActiveSessions);
    for (String sessionName : activeSessions.keySet()) {
      systemSubject.execute(() -> sessionHandler.invalidateSession(sessionName));
    }
  }

  @BeforeExam
  public void beforeTest() throws Exception {
    try {
      waitForSystemReady();
      // Start the services needed for testing.
      // We need to start the Search UI to test that it redirects properly
      getServiceManager().startFeature(true, "security-idp");
      getServiceManager().waitForAllBundles();

      ingest(
          getFileContent(JSON_RECORD_RESOURCE_PATH + "/SimpleGeoJsonRecord"), "application/json");

      getSecurityPolicy().configureWebContextPolicy(IDP_AUTH_TYPES, null, null);
      getServiceManager().waitForAllBundles();

      getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query");
      getServiceManager().waitForHttpEndpoint(WHO_AM_I_URL.getUrl());
      getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/idp/login/metadata");
      getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/saml/sso/metadata");

      Map<String, Object> openSearchProperties =
          getOpenSearchSourceProperties(
              OPENSEARCH_SOURCE_ID, OPENSEARCH_PATH.getUrl(), getServiceManager());
      openSearchProperties.put("username", "admin");
      openSearchProperties.put("password", "admin");
      getServiceManager().createManagedService(OPENSEARCH_FACTORY_PID, openSearchProperties);
      getCatalogBundle().waitForFederatedSource(OPENSEARCH_SOURCE_ID);
    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed in @BeforeExam: ");
    }
  }

  private void validateSaml(String xml, SamlSchema schema) throws IOException {

    // Prepare the schema and xml
    String schemaFileName = "saml-schema-" + schema.toString().toLowerCase() + "-2.0.xsd";
    URL schemaURL = AbstractIntegrationTest.class.getClassLoader().getResource(schemaFileName);
    StreamSource streamSource = new StreamSource(new StringReader(xml));

    // If we fail to create a validator we don't want to stop the show, so we just log a warning
    Validator validator = null;
    try {
      validator =
          SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
              .newSchema(schemaURL)
              .newValidator();
    } catch (SAXException e) {
      LOGGER.debug("Exception creating validator. ", e);
    }

    // If the xml is invalid, then we want to fail completely
    if (validator != null) {
      try {
        validator.validate(streamSource);
      } catch (SAXException e) {
        LoggingUtils.failWithThrowableStacktrace(e, "Failed to validate SAML: ");
      }
    }
  }

  private void setConfig(String pid, String param, Object value) throws Exception {

    // Update the config
    Configuration config = getAdminConfig().getConfiguration(pid, null);

    config.update(
        new Hashtable<String, Object>() {
          {
            put(param, value);
          }
        });

    // We have to make sure the config has been updated before we can use it

    expect("Configs to update")
        .within(2, TimeUnit.MINUTES)
        .until(() -> config.getProperties() != null && (config.getProperties().get(param) != null));
  }

  private ResponseHelper getSearchResponse(boolean isPassiveRequest, String url) throws Exception {

    // We should be redirected to the IdP when we first try to hit the search page

    Response searchResponse =
        given()
            .header("User-Agent", BROWSER_USER_AGENT)
            .redirects()
            .follow(false)
            .expect()
            .statusCode(302)
            .when()
            .get(url == null ? SEARCH_URL.getUrl() : url);

    // Because we get back a 302, we know the redirect location is in the header
    ResponseHelper searchHelper = new ResponseHelper(searchResponse);
    searchHelper.parseHeader();

    // We get bounced to a login page which has the parameters we need to pass to the IdP
    // embedded on the page in a JSON Object

    Response redirectResponse =
        given()
            .header("User-Agent", BROWSER_USER_AGENT)
            .params(searchHelper.params)
            .expect()
            .statusCode(200)
            .when()
            .get(searchHelper.redirectUrl);

    // The body has the parameters we passed it plus some extra, so we need to parse again
    ResponseHelper redirectHelper = new ResponseHelper(redirectResponse);
    redirectHelper.parseBody();

    // Make sure the authn request is valid before proceeding
    String inflated = RestSecurity.inflateBase64(redirectHelper.get("SAMLRequest"));
    validateSaml(inflated, SamlSchema.PROTOCOL);

    // The /sso endpoint is hidden to the outside world. Normally we'd be hitting /idp/login
    // with a browser which would show us a login page and redirect us via javascript
    // to /idp/login/sso, but because we are playing the role of the browser we have to do this
    // manually.
    if (isPassiveRequest) {
      redirectHelper.redirectUrl = searchHelper.redirectUrl;
    } else {
      redirectHelper.redirectUrl = searchHelper.redirectUrl + "/sso";
    }

    return redirectHelper;
  }

  private String getUserName() {

    Response response = get(WHO_AM_I_URL.getUrl()).then().extract().response();
    return response.jsonPath().get("karaf.name");
  }

  private String getUserName(Map<String, String> cookies) {

    Response response =
        given().cookies(cookies).when().get(WHO_AM_I_URL.getUrl()).then().extract().response();
    return response.jsonPath().get("idp.name");
  }

  private String setupHttpRequestUsingBinding(Binding requestBinding, Binding metadataBinding)
      throws Exception {
    // Signing is tested in the unit tests, so we don't require signing here to make things simpler
    setConfig("org.codice.ddf.security.idp.server.IdpEndpoint", "strictSignature", false);

    // Set the metadata
    String confluenceSpMetadata =
        String.format(
            getFileContent("confluence-sp-metadata.xml"),
            AUTHENTICATION_REQUEST_ISSUER,
            metadataBinding.toString());
    validateSaml(confluenceSpMetadata, SamlSchema.METADATA);
    setConfig(
        "org.codice.ddf.security.idp.server.IdpEndpoint",
        "spMetadata",
        new String[] {confluenceSpMetadata});

    // Get the authn request
    String mockAuthnRequest =
        String.format(
            getFileContent("confluence-sp-authentication-request.xml"),
            requestBinding.toString(),
            AUTHENTICATION_REQUEST_ISSUER);
    validateSaml(mockAuthnRequest, SamlSchema.PROTOCOL);
    return mockAuthnRequest;
  }

  private ResponseHelper performHttpRequestUsingBinding(
      Binding binding, String relayState, String encodedRequest) throws Exception {

    Response idpResponse =
        given()
            .auth()
            .preemptive()
            .basic("admin", "admin")
            .param("AuthMethod", "up")
            .param("SAMLRequest", encodedRequest)
            .param("RelayState", relayState)
            .param(
                "OriginalBinding",
                Binding.POST == binding
                    ? SamlProtocol.Binding.HTTP_POST.getUri()
                    : SamlProtocol.Binding.HTTP_REDIRECT.getUri())
            .expect()
            .statusCode(200)
            .when()
            .get(IDP_URL.getUrl() + "/sso");

    return new ResponseHelper(idpResponse);
  }

  @Test
  public void testRedirectBinding() throws Exception {
    String relayState = "test";
    String mockAuthnRequest = setupHttpRequestUsingBinding(Binding.REDIRECT, Binding.POST);
    String encodedRequest = RestSecurity.deflateAndBase64Encode(mockAuthnRequest);
    ResponseHelper helper =
        performHttpRequestUsingBinding(Binding.REDIRECT, relayState, encodedRequest);

    assertThat(helper.parseBody(), is(Binding.POST));
    assertThat(helper.postRelayState, is(relayState));

    String inflatedSamlResponse = RestSecurity.base64Decode(helper.postSamlResponse);
    validateSaml(inflatedSamlResponse, SamlSchema.PROTOCOL);
  }

  @Test
  public void testPostBinding() throws Exception {

    // We should get back a POST form that has everything we put in, thus the only thing
    // of interest to really check is that we do get back a post form
    String mockAuthnRequest = setupHttpRequestUsingBinding(Binding.POST, Binding.POST);
    String encodedRequest =
        Base64.getEncoder()
            .encodeToString(mockAuthnRequest.getBytes(StandardCharsets.UTF_8.name()));
    ResponseHelper helper = performHttpRequestUsingBinding(Binding.POST, "test", encodedRequest);
    assertThat(helper.parseBody(), is(Binding.POST));
  }

  @Test
  public void testIncorrectResponseRedirectBinding() throws Exception {
    String relayState = "test";
    String mockAuthnRequest = setupHttpRequestUsingBinding(Binding.REDIRECT, Binding.REDIRECT);
    String encodedRequest = RestSecurity.deflateAndBase64Encode(mockAuthnRequest);

    given()
        .auth()
        .preemptive()
        .basic("admin", "admin")
        .param("AuthMethod", "up")
        .param("SAMLRequest", encodedRequest)
        .param("RelayState", relayState)
        .param("OriginalBinding", SamlProtocol.Binding.HTTP_REDIRECT.getUri())
        .expect()
        .statusCode(400)
        .when()
        .get(IDP_URL.getUrl() + "/sso");
  }

  @Test
  public void testBadUsernamePassword() throws Exception {
    ResponseHelper searchHelper = getSearchResponse(false, null);

    // We're using an AJAX call, so anything other than 200 means not authenticated

    given()
        .auth()
        .preemptive()
        .basic("definitely", "notright")
        .param("AuthMethod", "up")
        .params(searchHelper.params)
        .expect()
        .statusCode(not(200))
        .when()
        .get(searchHelper.redirectUrl);
  }

  @Test
  public void testPkiAuth() throws Exception {

    // Note that PKI is passive (as opposed to username/password which is not)
    ResponseHelper searchHelper = getSearchResponse(true, null);

    given()
        .auth()
        .certificate(
            KEY_STORE_PATH,
            PASSWORD,
            certAuthSettings().sslSocketFactory(SSLSocketFactory.getSystemSocketFactory()))
        .header("User-Agent", BROWSER_USER_AGENT)
        .param("AuthMethod", "pki")
        .params(searchHelper.params)
        .expect()
        .statusCode(200)
        .when()
        .get(searchHelper.redirectUrl);
  }

  @Ignore("TODO: DDF-5072")
  @Test
  public void testGuestAuth() throws Exception {
    ResponseHelper searchHelper = getSearchResponse(false, null);

    given()
        .param("AuthMethod", "guest")
        .params(searchHelper.params)
        .header("User-Agent", BROWSER_USER_AGENT)
        .expect()
        .statusCode(200)
        .when()
        .get(searchHelper.redirectUrl);
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipUnstableTest.class) // DDF-3848
  public void testRedirectFlow() throws Exception {

    // Negative test to make sure we aren't admin yet
    assertThat(getUserName(), not("admin"));

    // First time hitting search, expect to get redirected to the Identity Provider.
    ResponseHelper searchHelper = getSearchResponse(false, null);

    // Pass our credentials to the IDP, it should redirect us to the Assertion Consumer Service.
    // The redirect is currently done via javascript and not an HTTP redirect.

    Response idpResponse =
        given()
            .auth()
            .preemptive()
            .basic("admin", "admin")
            .param("AuthMethod", "up")
            .params(searchHelper.params)
            .expect()
            .statusCode(200)
            .when()
            .get(searchHelper.redirectUrl);

    ResponseHelper idpHelper = new ResponseHelper(idpResponse);

    // Perform a bunch of checks to make sure we're valid against both the spec and schema
    assertThat(idpHelper.parseBody(), is(Binding.POST));
    String decodedSamlResponse = RestSecurity.base64Decode(idpHelper.postSamlResponse);
    validateSaml(decodedSamlResponse, SamlSchema.PROTOCOL);
    assertThat(
        decodedSamlResponse,
        allOf(
            containsString("urn:oasis:names:tc:SAML:2.0:status:Success"),
            containsString("ds:SignatureValue"),
            containsString("saml2:Assertion"),
            containsString("ds:Signature")));
    assertThat(
        idpHelper.postRelayState.length(),
        is(both(greaterThanOrEqualTo(0)).and(lessThanOrEqualTo(80))));

    // After passing the SAML Assertion to the ACS, we should be redirected back to Search.

    String body =
        String.format(
            "SAMLResponse=%s&RelayState=%s",
            URLEncoder.encode(idpHelper.postSamlResponse, StandardCharsets.UTF_8.name()),
            URLEncoder.encode(idpHelper.postRelayState, StandardCharsets.UTF_8.name()));

    Response acsResponse =
        given()
            .body(body)
            .contentType("application/x-www-form-urlencoded")
            .redirects()
            .follow(false)
            .expect()
            .statusCode(anyOf(is(307), is(303)))
            .when()
            .post(searchHelper.params.get("ACSURL"));

    ResponseHelper acsHelper = new ResponseHelper(acsResponse);
    acsHelper.parseHeader();

    // Access search again, but now as an authenticated user.
    given()
        .cookies(acsResponse.getCookies())
        .expect()
        .statusCode(200)
        .when()
        .get(acsHelper.redirectUrl)
        .then()
        .log()
        .ifValidationFails();

    // Make sure we are logged in as admin.
    assertThat(getUserName(acsResponse.getCookies()), is("admin"));
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipUnstableTest.class) // DDF-3848
  public void testLogout() throws Exception {
    Response acsResponse = loginUser("admin", "admin");

    // Create logout request

    Response createLogoutRequest =
        given()
            .cookies(acsResponse.getCookies())
            .expect()
            .statusCode(200)
            .when()
            .get(LOGOUT_REQUEST_URL.getUrl());

    ResponseHelper createLogoutHelper = new ResponseHelper(createLogoutRequest);
    createLogoutHelper.parseBody();

    // Logout via url returned in logout request

    given()
        .expect()
        .statusCode(200)
        .body(containsString("You are now signed out."))
        .when()
        .get(createLogoutHelper.get("url"));

    // Verify admin user is no longer logged in
    assertThat(getUserName(), not("admin"));
  }

  private Response loginUser(String username, String password) throws Exception {
    // Negative test to make sure we aren't admin yet
    assertThat(getUserName(), not(username));

    // First time hitting search, expect to get redirected to the Identity Provider.
    ResponseHelper searchHelper = getSearchResponse(false, null);

    // Pass our credentials to the IDP, it should redirect us to the Assertion Consumer Service.
    // The redirect is currently done via javascript and not an HTTP redirect.

    Response idpResponse =
        given()
            .auth()
            .preemptive()
            .basic(username, password)
            .param("AuthMethod", "up")
            .params(searchHelper.params)
            .expect()
            .statusCode(200)
            .when()
            .get(searchHelper.redirectUrl);

    ResponseHelper idpHelper = new ResponseHelper(idpResponse);
    assertThat(idpHelper.parseBody(), is(Binding.POST));

    // After passing the SAML Assertion to the ACS, we should be redirected back to Search.

    String body =
        String.format(
            "SAMLResponse=%s&RelayState=%s",
            URLEncoder.encode(idpHelper.postSamlResponse, StandardCharsets.UTF_8.name()),
            URLEncoder.encode(idpHelper.postRelayState, StandardCharsets.UTF_8.name()));
    Response acsResponse =
        given()
            .body(body)
            .contentType("application/x-www-form-urlencoded")
            .redirects()
            .follow(false)
            .expect()
            .statusCode(anyOf(is(307), is(303)))
            .when()
            .post(searchHelper.params.get("ACSURL"));

    ResponseHelper acsHelper = new ResponseHelper(acsResponse);
    acsHelper.parseHeader();

    // Access search again, but now as an authenticated user.
    given()
        .cookies(acsResponse.getCookies())
        .expect()
        .statusCode(200)
        .when()
        .get(acsHelper.redirectUrl)
        .then()
        .log()
        .ifValidationFails();

    // Make sure we are logged in as admin.
    assertThat(getUserName(acsResponse.getCookies()), is(username));
    return acsResponse;
  }

  @Test
  public void testEcpByFederatedQueryWithUsernamePassword() throws Exception {
    String queryUrl = OPENSEARCH_PATH.getUrl() + "?q=*&format=xml&src=" + OPENSEARCH_SOURCE_ID;
    // First time hitting search, expect to get redirected to the Identity Provider.
    ResponseHelper searchHelper = getSearchResponse(false, queryUrl);

    // Pass our credentials to the IDP, it should redirect us to the Assertion Consumer Service.
    // The redirect is currently done via javascript and not an HTTP redirect.

    Response idpResponse =
        given()
            .auth()
            .preemptive()
            .basic("admin", "admin")
            .param("AuthMethod", "up")
            .params(searchHelper.params)
            .expect()
            .statusCode(200)
            .when()
            .get(searchHelper.redirectUrl);

    ResponseHelper idpHelper = new ResponseHelper(idpResponse);

    // Perform a bunch of checks to make sure we're valid against both the spec and schema
    assertThat(idpHelper.parseBody(), is(Binding.POST));
    String decodedResponse = RestSecurity.base64Decode(idpHelper.postSamlResponse);
    validateSaml(decodedResponse, SamlSchema.PROTOCOL);
    assertThat(
        decodedResponse,
        allOf(
            containsString("urn:oasis:names:tc:SAML:2.0:status:Success"),
            containsString("ds:SignatureValue"),
            containsString("ds:Signature"),
            containsString("saml2:Assertion")));
    assertThat(
        idpHelper.postRelayState.length(),
        is(both(greaterThanOrEqualTo(0)).and(lessThanOrEqualTo(80))));

    // After passing the SAML Assertion to the ACS, we should be redirected back to Search.

    String body =
        String.format(
            "SAMLResponse=%s&RelayState=%s",
            URLEncoder.encode(idpHelper.postSamlResponse, StandardCharsets.UTF_8.name()),
            URLEncoder.encode(idpHelper.postRelayState, StandardCharsets.UTF_8.name()));

    Response acsResponse =
        given()
            .body(body)
            .contentType("application/x-www-form-urlencoded")
            .redirects()
            .follow(false)
            .expect()
            .statusCode(anyOf(is(307), is(303)))
            .when()
            .post(searchHelper.params.get("ACSURL"));

    ResponseHelper acsHelper = new ResponseHelper(acsResponse);
    acsHelper.parseHeader();

    Response response =
        given().cookies(acsResponse.getCookies()).expect().statusCode(200).when().get(queryUrl);

    // The federated query using username/password against the IDP auth type on all of /services
    // would fail without ECP

    response
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .body(
            hasXPath(
                "/metacards/metacard/string[@name='"
                    + Metacard.TITLE
                    + "']/value[text()='"
                    + RECORD_TITLE_1
                    + "']"),
            hasXPath("/metacards/metacard/geometry/value"));
  }

  @Test
  @ConditionalIgnoreRule.ConditionalIgnore(condition = SkipUnstableTest.class) // DDF-3848
  public void testSessionManagement() throws Exception {
    // logon a couple of users
    loginUser("tchalla", "password1");
    loginUser("slang", "password1");
    loginUser("srogers", "password1");

    SessionHandler sessionHandler = getServiceManager().getService(SessionHandler.class);
    assertNotNull(sessionHandler);

    // Attempt query for sessions without admin subject
    Map<String, Set<String>> activeSessions = sessionHandler.getActiveSessions();
    assertThat(activeSessions.size(), is(0));

    // Query with admin subject
    activeSessions = systemSubject.execute(sessionHandler::getActiveSessions);
    assertThat(activeSessions.size(), is(3));

    // Attempt to invalidate real session without admin subject
    sessionHandler.invalidateSession("slang");
    activeSessions = systemSubject.execute(sessionHandler::getActiveSessions);
    assertThat(activeSessions.size(), is(3));

    // Attempt to invalidate non-existent subject with admin subject
    systemSubject.execute(() -> sessionHandler.invalidateSession("NOT_A_SESSION"));
    activeSessions = systemSubject.execute(sessionHandler::getActiveSessions);
    assertThat(activeSessions.size(), is(3));

    // Attempt to invalidate real session with admin subject
    systemSubject.execute(() -> sessionHandler.invalidateSession("slang"));
    activeSessions = systemSubject.execute(sessionHandler::getActiveSessions);
    assertThat(activeSessions.size(), is(2));

    Set<String> activeSPs = activeSessions.get("tchalla");
    assertThat(activeSPs.size(), is(1));
    assertThat(activeSPs.iterator().next(), is(ddfSpMetadataEntityId));
  }

  private class ResponseHelper {

    private final Response response;

    private String postSamlResponse;

    private String postRelayState;

    private String redirectUrl;

    private final Map<String, String> params = new HashMap<>();

    private ResponseHelper(Response response) {
      this.response = response;
    }

    private String get(String key) {
      return params.get(key);
    }

    private void parseParamsFromUrl(String url) throws URISyntaxException {
      redirectUrl = url.split("[?]")[0];

      List<NameValuePair> paramList =
          URLEncodedUtils.parse(new URI(url), StandardCharsets.UTF_8.name());
      for (NameValuePair param : paramList) {
        params.put(param.getName(), param.getValue());
      }
    }

    private void parseHeader() throws URISyntaxException {
      if (response.headers().hasHeaderWithName("Location")) {
        parseParamsFromUrl(response.header("Location"));
      } else {
        fail("Response does not have a header \"Location\"");
      }
    }

    private Binding parseBody() throws Exception {
      String body = response.body().asString();
      Binding binding = null;
      if (body.contains("<form")) {
        binding = Binding.POST;
        parseBodyPost();
      } else if (body.contains("<title>Redirect</title>")) {
        parseBodyRedirect();
        binding = Binding.REDIRECT;
      } else if (body.contains("<title>Login</title>")) {
        parseBodyLogin();
      } else if (body.contains("Identity Provider Logout")) {
        parseBodyLogout();
      } else {
        fail("Failed to parse body as redirect or post\n" + body);
      }
      return binding;
    }

    private void parseJson(String json) {
      final String whiteSpaceRegex = "(^\\s*\")|(\"\\s*$)";
      String[] keyValuePairs = json.split("[,]");
      for (String pair : keyValuePairs) {
        String[] keyVal = pair.split("[:]", 2);
        String key = keyVal[0].replaceAll(whiteSpaceRegex, "");
        String value = keyVal[1].replaceAll(whiteSpaceRegex, "");
        params.put(key, value);
      }
    }

    private void parseBodyLogin() {
      // We're trying to parse a javascript variable that is embedded in an HTML form
      Pattern pattern =
          Pattern.compile(
              "window.idpState\\s*=\\s*\\{\\s*(.*?)\\s?\\}",
              Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
      Matcher matcher = pattern.matcher(response.body().asString());
      if (matcher.find()) {
        parseJson(matcher.group(1));
      } else {
        String failMessage =
            "Failed to find the javascript var."
                + "\nPattern: "
                + pattern.toString()
                + "\nResponse Body: "
                + response.body().asString();
        fail(failMessage);
      }
    }

    private void parseBodyRedirect() throws URISyntaxException {

      // We're trying to parse a javascript variable that is embedded in an HTML form
      Pattern pattern = Pattern.compile("encoded *= *\"(.*)\"", Pattern.CASE_INSENSITIVE);
      Matcher matcher = pattern.matcher(response.body().asString());
      if (matcher.find()) {
        parseParamsFromUrl(matcher.group(1));
      } else {
        String failMessage =
            "Failed to find the javascript var."
                + "\nPattern: "
                + pattern.toString()
                + "\nResponse Body: "
                + response.body().asString();
        fail(failMessage);
      }
    }

    private void parseBodyPost() throws Exception {
      postSamlResponse =
          XmlSearch.evaluate(
              "/html/body/form/input[@name='SAMLResponse']/@value", response.body().asString());
      postRelayState =
          XmlSearch.evaluate(
              "/html/body/form/input[@name='RelayState']/@value", response.body().asString());

      if (postSamlResponse == null) {
        String failMessage =
            "Failed to parse POST response." + "\nResponse Body: " + response.body().asString();
        fail(failMessage);
      }
    }

    private void parseBodyLogout() {
      parseJson(response.body().asString());
    }
  }
}
