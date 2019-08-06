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

import static com.jayway.restassured.RestAssured.given;
import static com.xebialabs.restito.builder.stub.StubHttp.whenHttp;
import static com.xebialabs.restito.semantics.Action.bytesContent;
import static com.xebialabs.restito.semantics.Action.contentType;
import static com.xebialabs.restito.semantics.Action.ok;
import static com.xebialabs.restito.semantics.Condition.get;
import static com.xebialabs.restito.semantics.Condition.parameter;
import static com.xebialabs.restito.semantics.Condition.post;
import static com.xebialabs.restito.semantics.Condition.withHeader;
import static javax.ws.rs.HttpMethod.GET;
import static javax.ws.rs.core.HttpHeaders.AUTHORIZATION;
import static javax.ws.rs.core.HttpHeaders.HOST;
import static javax.ws.rs.core.HttpHeaders.LOCATION;
import static javax.ws.rs.core.HttpHeaders.USER_AGENT;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import static org.codice.ddf.itests.common.AbstractIntegrationTest.DynamicUrl.SECURE_ROOT;
import static org.codice.ddf.itests.common.WaitCondition.expect;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertTrue;
import static org.pac4j.oidc.profile.OidcProfileDefinition.ACCESS_TOKEN;
import static org.pac4j.oidc.profile.OidcProfileDefinition.AUTH_TIME;
import static org.pac4j.oidc.profile.OidcProfileDefinition.AZP;
import static org.pac4j.oidc.profile.OidcProfileDefinition.EMAIL_VERIFIED;
import static org.pac4j.oidc.profile.OidcProfileDefinition.NONCE;
import static org.pac4j.oidc.profile.OidcProfileDefinition.PREFERRED_USERNAME;
import static org.pac4j.oidc.profile.OidcProfileDefinition.REFRESH_TOKEN;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTCreator;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.impl.PublicClaims;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.jayway.restassured.response.Response;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.util.Base64URL;
import com.xebialabs.restito.semantics.Call;
import com.xebialabs.restito.server.StubServer;
import java.net.URI;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.test.common.LoggingUtils;
import org.codice.ddf.test.common.annotations.AfterExam;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.codice.gsonsupport.GsonTypeAdapters;
import org.json.simple.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.service.cm.Configuration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class TestOidc extends AbstractIntegrationTest {

  private static final String OIDC_AUTH_TYPES = "/=OIDC,/solr=SAML|PKI|basic";

  private static final DynamicUrl SEARCH_URL = new DynamicUrl(SEARCH_ROOT, "/catalog/");

  private static final DynamicUrl LOGOUT_REQUEST_URL =
      new DynamicUrl(SEARCH_ROOT, "/catalog/internal/logout/actions");

  private static final DynamicUrl LOGOUT_URL = new DynamicUrl(SERVICE_ROOT, "/oidc/logout");

  private static final DynamicUrl WHO_AM_I_URL =
      new DynamicUrl(DynamicUrl.SECURE_ROOT, HTTPS_PORT, "/whoami");

  public static final String BROWSER_USER_AGENT =
      "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/51.0.2704.103 Safari/537.36";

  private static final DynamicPort IDP_PORT =
      new DynamicPort("org.codice.ddf.system_metadata_stub_server_port", 9);

  private static final String METADATA_PATH =
      "/auth/realms/master/.well-known/openid-configuration";

  private static final String TOKEN_ENDPOINT_PATH =
      "/auth/realms/master/protocol/openid-connect/token";

  private static final String USER_INFO_ENDPOINT_PATH =
      "/auth/realms/master/protocol/openid-connect/userinfo";

  private static final String CERTS_ENDPOINT_PATH =
      "/auth/realms/master/protocol/openid-connect/certs";

  private static final String LOGOUT_URL_PATH = "/auth/admin/master/protocol/openid-connect/logout";

  private static final DynamicUrl URL_START = new DynamicUrl(DynamicUrl.INSECURE_ROOT, IDP_PORT);

  private static final String TEMPORARY_CODE =
      "6c8cc55f-5942-463d-b41e-48095dc99aa7.170c2093-3dce-4553-9ad4-fc2dadb0997d.970cf267-fe8f-4d27-a1ae-72f16d366a38";

  private static final String DDF_CLIENT_ID = "ddf.client";
  private static final String DDF_CLIENT_SECRET = "c0eb883e-8714-43aa-962f-e6b4f9486c28";
  private static final String DDF_SCOPE = "openid profile email resource.read";
  // Unique value. OIDC Spec: the OpenID Provider MUST calculate a unique sub (subject) value for
  // each Sector Identifier. The Subject Identifier value MUST NOT be reversible by any party other
  // than the OpenID Provider.
  private static final String SUBJECT = "fa0e76c5-5a58-483a-bb8c-8a3cf72cdde5";
  private static final String ROLES = "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role";

  private static final String CLIENT_ID = "client_id";
  private static final String SECRET = "secret";
  private static final String SCOPE = "scope";
  private static final String RESPONSE_TYPE = "response_type";
  private static final String RESPONSE_MODE = "response_mode";
  private static final String REDIRECT_URI = "redirect_uri";
  private static final String STATE = "state";

  private static final String ID_TOKEN = "id_token";
  private static final String ID_TOKEN_TOKEN = "id_token token";
  private static final String CODE = "code";
  private static final String FORM_POST = "form_post";

  private static final String ADMIN = "admin";
  private static final String JSESSIONID = "JSESSIONID";
  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .registerTypeAdapterFactory(GsonTypeAdapters.LongDoubleTypeAdapter.FACTORY)
          .create();

  private static Dictionary<String, Object> handlerConfig;
  private static StubServer server;
  private static RSAPublicKey publicKey;
  private static Algorithm validAlgorithm;
  private static Algorithm invalidAlgorithm;
  private static String jwk;

  @BeforeExam
  public void beforeTest() {
    try {
      waitForSystemReady();
      getServiceManager().waitForAllBundles();
      getServiceManager().waitForHttpEndpoint(WHO_AM_I_URL.getUrl());
      getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query");
      getSecurityPolicy().configureWebContextPolicy(OIDC_AUTH_TYPES, null, null);

      // start stub server
      server = new StubServer(Integer.parseInt(IDP_PORT.getPort())).run();
      server.start();

      // Generate the RSA key pair
      KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
      gen.initialize(2048);
      KeyPair keyPair = gen.generateKeyPair();
      RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
      publicKey = (RSAPublicKey) keyPair.getPublic();

      // Convert to JSON Web Key (JWK) format
      JWK sigJwk =
          new RSAKey.Builder(publicKey)
              .privateKey(privateKey)
              .keyUse(KeyUse.SIGNATURE)
              .keyID(UUID.randomUUID().toString())
              .build();

      jwk = "{\"keys\": [" + sigJwk.toPublicJWK().toJSONString() + "] }";

      validAlgorithm = Algorithm.RSA256(publicKey, privateKey);
      invalidAlgorithm = Algorithm.HMAC256("WRONG");

      setUp();

      // Configure OIDC Handler
      handlerConfig = new Hashtable<>();
      handlerConfig.put("idpType", "Keycloak");
      handlerConfig.put("clientId", DDF_CLIENT_ID);
      handlerConfig.put("realm", "master");
      handlerConfig.put(SECRET, DDF_CLIENT_SECRET);
      handlerConfig.put("logoutUri", URL_START.toString() + LOGOUT_URL_PATH);
      handlerConfig.put("baseUri", URL_START.toString() + "/auth");
      handlerConfig.put("discoveryUri", URL_START.toString() + METADATA_PATH);
      handlerConfig.put(SCOPE, DDF_SCOPE);
      handlerConfig.put("useNonce", true);
      handlerConfig.put("responseMode", FORM_POST);
      setConfig();
    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed in @BeforeExam: ");
    }
  }

  @Before
  public void setUp() {
    // Reset server
    server.clear();

    // Host metadata
    whenHttp(server)
        .match(get(METADATA_PATH))
        .then(
            ok(),
            contentType(APPLICATION_JSON),
            bytesContent(
                getFileContent("oidcMetadata.json")
                    .replaceAll("\\{\\{IDP_PORT}}", IDP_PORT.getPort())
                    .getBytes()));

    // Host JWK
    whenHttp(server)
        .match(get(CERTS_ENDPOINT_PATH))
        .then(ok(), contentType(APPLICATION_JSON), bytesContent((jwk).getBytes()));
  }

  @AfterExam
  public void afterExam() throws Exception {
    getSecurityPolicy().configureRestForGuest();

    if (server != null) {
      server.stop();
    }
  }

  // --------------------------Implicit Flow Tests--------------------------//

  @Test
  public void testImplicitFlowLogin() throws Exception {
    Map<String, String> initialResponseParams = sendInitialRequest(ID_TOKEN);

    assertThat(initialResponseParams.get(SCOPE), is(DDF_SCOPE));
    assertThat(initialResponseParams.get(RESPONSE_TYPE), is(ID_TOKEN));
    assertThat(initialResponseParams.get(CLIENT_ID), is(DDF_CLIENT_ID));
    assertThat(initialResponseParams.get(RESPONSE_MODE), is(FORM_POST)); // optional but sent in DDF

    assertTrue(initialResponseParams.containsKey(REDIRECT_URI));
    assertTrue(initialResponseParams.containsKey(NONCE));
    assertTrue(initialResponseParams.containsKey(STATE)); // recommended by spec

    // Respond to request after user logged in
    String idToken =
        getBaseIdTokenBuilder()
            .withClaim(NONCE, initialResponseParams.get(NONCE))
            .sign(validAlgorithm);

    Response searchResponse =
        given()
            .cookie(JSESSIONID, initialResponseParams.get(JSESSIONID))
            .header(USER_AGENT, BROWSER_USER_AGENT)
            .header(HOST, "localhost:" + HTTPS_PORT.getPort())
            .header("Origin", URL_START.toString())
            .param(STATE, initialResponseParams.get(STATE))
            .param(ID_TOKEN, idToken)
            .redirects()
            .follow(false)
            .expect()
            .statusCode(302)
            .when()
            .post(initialResponseParams.get(REDIRECT_URI));

    // Verify that we're redirected to Intrigue
    assertThat(searchResponse.header(LOCATION), is(SEARCH_URL.getUrl()));

    // Verify that we're logged in as admin
    Map<String, Object> userInfoList = getUserInfo(initialResponseParams.get(JSESSIONID));
    assertThat(userInfoList.get("name"), is(ADMIN));

    logout(initialResponseParams.get(JSESSIONID));
  }

  @Test
  public void testIdTokenTokenResponseTypeImplicitFlow() throws Exception {
    Map<String, String> initialResponseParams = sendInitialRequest(ID_TOKEN_TOKEN);

    assertThat(initialResponseParams.get(SCOPE), is(DDF_SCOPE));
    assertThat(initialResponseParams.get(RESPONSE_TYPE), is(ID_TOKEN_TOKEN));
    assertThat(initialResponseParams.get(CLIENT_ID), is(DDF_CLIENT_ID));
    assertThat(initialResponseParams.get(RESPONSE_MODE), is(FORM_POST)); // optional but sent in DDF

    assertTrue(initialResponseParams.containsKey(REDIRECT_URI));
    assertTrue(initialResponseParams.containsKey(NONCE));
    assertTrue(initialResponseParams.containsKey(STATE)); // recommended by spec

    // Respond to request after user logged in with an access token, an id token, and an at_hash
    // claim in the id token
    String accessToken = createAccessToken(true);

    MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
    messageDigest.update(accessToken.getBytes(Charset.forName("US-ASCII")));
    byte[] hash = messageDigest.digest();
    byte[] firstHalf = Arrays.copyOf(hash, hash.length / 2);

    String idToken =
        getBaseIdTokenBuilder()
            .withClaim(NONCE, initialResponseParams.get(NONCE))
            .withClaim("at_hash", Base64URL.encode(firstHalf).toString())
            .sign(validAlgorithm);

    Response searchResponse =
        given()
            .cookie(JSESSIONID, initialResponseParams.get(JSESSIONID))
            .header(USER_AGENT, BROWSER_USER_AGENT)
            .header(HOST, "localhost:" + HTTPS_PORT.getPort())
            .header("Origin", URL_START.toString())
            .param(STATE, initialResponseParams.get(STATE))
            .param(ID_TOKEN, idToken)
            .param(ACCESS_TOKEN, accessToken)
            .param("token_type", "bearer")
            .redirects()
            .follow(false)
            .expect()
            .statusCode(302)
            .when()
            .post(initialResponseParams.get(REDIRECT_URI));

    // Verify that we're redirected to Intrigue
    assertThat(searchResponse.header(LOCATION), is(SEARCH_URL.getUrl()));

    // Verify that we're logged in as admin
    Map<String, Object> userInfoList = getUserInfo(initialResponseParams.get(JSESSIONID));
    assertThat(userInfoList.get("name"), is(ADMIN));

    logout(initialResponseParams.get(JSESSIONID));
  }

  /**
   * OAuth 2.0 state value. REQUIRED if the state parameter is present in the Authorization Request.
   * Clients MUST verify that the state value is equal to the value of state parameter in the
   * Authorization Request.
   */
  @Test
  public void testResponseWithNoStateImplicitFlow() throws Exception {
    Map<String, String> initialResponseParams = sendInitialRequest(ID_TOKEN);

    String validIdToken =
        getBaseIdTokenBuilder()
            .withClaim(NONCE, initialResponseParams.get(NONCE))
            .sign(validAlgorithm);

    Response response =
        given()
            .cookie(JSESSIONID, initialResponseParams.get(JSESSIONID))
            .header(USER_AGENT, BROWSER_USER_AGENT)
            .header(HOST, "localhost:" + HTTPS_PORT.getPort())
            .header("Origin", URL_START.toString())
            .param(ID_TOKEN, validIdToken)
            .redirects()
            .follow(false)
            .expect()
            .statusCode(302)
            .when()
            .post(initialResponseParams.get(REDIRECT_URI));

    // Should be redirected back to the IdP to login
    URI locationUri = new URI(response.header(LOCATION));
    assertThat(locationUri.getPath(), is("/auth/realms/master/protocol/openid-connect/auth"));

    // Verify that we're NOT logged in
    Map<String, Object> userInfoList = getUserInfo(initialResponseParams.get(JSESSIONID));
    assertThat(userInfoList.get("isGuest"), is(true));
  }

  /**
   * OAuth 2.0 state value. REQUIRED if the state parameter is present in the Authorization Request.
   * Clients MUST verify that the state value is equal to the value of state parameter in the
   * Authorization Request.
   */
  @Test
  public void testResponseWithIncorrectStateImplicitFlow() throws Exception {
    Map<String, String> initialResponseParams = sendInitialRequest(ID_TOKEN);

    String validIdToken =
        getBaseIdTokenBuilder()
            .withClaim(NONCE, initialResponseParams.get(NONCE))
            .sign(validAlgorithm);

    Response response =
        given()
            .cookie(JSESSIONID, initialResponseParams.get(JSESSIONID))
            .header(USER_AGENT, BROWSER_USER_AGENT)
            .header(HOST, "localhost:" + HTTPS_PORT.getPort())
            .header("Origin", URL_START.toString())
            .param(STATE, "WRONG")
            .param(ID_TOKEN, validIdToken)
            .redirects()
            .follow(false)
            .expect()
            .statusCode(302)
            .when()
            .post(initialResponseParams.get(REDIRECT_URI));

    // Should be redirected back to the IdP to login
    URI locationUri = new URI(response.header(LOCATION));
    assertThat(locationUri.getPath(), is("/auth/realms/master/protocol/openid-connect/auth"));

    // Verify that we're NOT logged in
    Map<String, Object> userInfoList = getUserInfo(initialResponseParams.get(JSESSIONID));
    assertThat(userInfoList.get("isGuest"), is(true));
  }

  /**
   * at_hash - Access Token hash value. Its value is the base64url encoding of the left-most half of
   * the hash of the octets of the ASCII representation of the access_token value, where the hash
   * algorithm used is the hash algorithm used in the alg Header Parameter of the ID Token's JOSE
   * Header. For instance, if the alg is RS256, hash the access_token value with SHA-256, then take
   * the left-most 128 bits and base64url encode them. The at_hash value is a case sensitive string.
   * If the ID Token is issued from the Authorization Endpoint with an access_token value, which is
   * the case for the response_type value  id_token token, this is REQUIRED
   */
  @Test
  public void testInvalidAtHashImplicitFlow() throws Exception {
    Map<String, String> initialResponseParams = sendInitialRequest(ID_TOKEN);

    String validAccessToken = createAccessToken(true);

    String invalidAtHashIdToken =
        getBaseIdTokenBuilder()
            .withClaim(NONCE, initialResponseParams.get(NONCE))
            .withClaim("at_hash", "WRONG")
            .sign(validAlgorithm);

    given()
        .cookie(JSESSIONID, initialResponseParams.get(JSESSIONID))
        .header(USER_AGENT, BROWSER_USER_AGENT)
        .header(HOST, "localhost:" + HTTPS_PORT.getPort())
        .header("Origin", URL_START.toString())
        .param(STATE, initialResponseParams.get(STATE))
        .param(ID_TOKEN, invalidAtHashIdToken)
        .param(ACCESS_TOKEN, validAccessToken)
        .param("token_type", "bearer")
        .redirects()
        .follow(false)
        .expect()
        .statusCode(400)
        .when()
        .post(initialResponseParams.get(REDIRECT_URI));

    // Verify that we're NOT logged in
    Map<String, Object> userInfoList = getUserInfo(initialResponseParams.get(JSESSIONID));
    assertThat(userInfoList.get("isGuest"), is(true));
  }

  @Test
  public void testInvalidIdTokenSignatureImplicitFlow() throws Exception {
    Map<String, String> initialResponseParams = sendInitialRequest(ID_TOKEN);

    String invalidSigIdToken =
        getBaseIdTokenBuilder()
            .withClaim(NONCE, initialResponseParams.get(NONCE))
            .sign(invalidAlgorithm);

    sendAndAssertInvalidImplicitFlowResponse(invalidSigIdToken, initialResponseParams);
  }

  /**
   * REQUIRED. String value used to associate a Client session with an ID Token, and to mitigate
   * replay attacks.
   *
   * <p>If a nonce value was sent in the Authentication Request, a nonce Claim MUST be present and
   * its value checked to verify that it is the same value as the one that was sent in the
   * Authentication Request. The Client SHOULD check the nonce value for replay attacks.
   */
  @Test
  public void testInvalidNonceResponseImplicitFlow() throws Exception {
    Map<String, String> initialResponseParams = sendInitialRequest(ID_TOKEN);

    String invalidNonceIdToken =
        getBaseIdTokenBuilder().withClaim(NONCE, "WRONG").sign(validAlgorithm);
    sendAndAssertInvalidImplicitFlowResponse(invalidNonceIdToken, initialResponseParams);
  }

  /**
   * The Issuer Identifier for the OpenID Provider (which is typically obtained during Discovery)
   * MUST exactly match the value of the iss (issuer) Claim.
   */
  @Test
  public void testInvalidIssuerImplicitFlow() throws Exception {
    Map<String, String> initialResponseParams = sendInitialRequest(ID_TOKEN);

    String invalidIssuerIdToken =
        getBaseIdTokenBuilder()
            .withClaim(NONCE, initialResponseParams.get(NONCE))
            .withIssuer("WRONG")
            .sign(validAlgorithm);

    sendAndAssertInvalidImplicitFlowResponse(invalidIssuerIdToken, initialResponseParams);
  }

  /**
   * The Client MUST validate that the aud (audience) Claim contains its client_id value[…]. The ID
   * Token MUST be rejected if the ID Token does not list the Client as a valid audience, or if it
   * contains additional audiences not trusted by the Client.
   */
  @Test
  public void testInvalidAudienceImplicitFlow() throws Exception {
    Map<String, String> initialResponseParams = sendInitialRequest(ID_TOKEN);

    String invalidAudienceIdToken =
        getBaseIdTokenBuilder()
            .withClaim(NONCE, initialResponseParams.get(NONCE))
            .withAudience("WRONG")
            .sign(validAlgorithm);

    sendAndAssertInvalidImplicitFlowResponse(invalidAudienceIdToken, initialResponseParams);
  }

  /** The current time MUST be before the time represented by the exp Claim. */
  @Test
  public void testExpiredIdTokenImplicitFlow() throws Exception {
    Map<String, String> initialResponseParams = sendInitialRequest(ID_TOKEN);

    long exp = Instant.now().minus(Duration.ofDays(3)).toEpochMilli();
    String expiredIdToken =
        getBaseIdTokenBuilder()
            .withClaim(NONCE, initialResponseParams.get(NONCE))
            .withExpiresAt(new Date(exp))
            .sign(validAlgorithm);

    sendAndAssertInvalidImplicitFlowResponse(expiredIdToken, initialResponseParams);
  }

  /**
   * Sends a response to DDF with the given invalid ID Token and asserts that the user is not logged
   * in.
   *
   * @param invalidIdToken - the invalid token we wish to response with
   * @param requestParams - a map of the parameters received in the initial response and the {@code
   *     JSESSIONID}
   */
  private void sendAndAssertInvalidImplicitFlowResponse(
      String invalidIdToken, Map<String, String> requestParams) {

    given()
        .cookie(JSESSIONID, requestParams.get(JSESSIONID))
        .header(USER_AGENT, BROWSER_USER_AGENT)
        .header(HOST, "localhost:" + HTTPS_PORT.getPort())
        .header("Origin", URL_START.toString())
        .param(STATE, requestParams.get(STATE))
        .param(ID_TOKEN, invalidIdToken)
        .redirects()
        .follow(false)
        .expect()
        .statusCode(400)
        .when()
        .post(requestParams.get(REDIRECT_URI));

    // Verify that we're NOT logged in
    Map<String, Object> userInfoList = getUserInfo(requestParams.get(JSESSIONID));
    assertThat(userInfoList.get("isGuest"), is(true));
  }

  // --------------------------Code Flow Tests--------------------------//

  @Test
  public void testCodeFlowLogin() throws Exception {
    Map<String, String> initialResponseParams = sendInitialRequest(CODE);

    assertThat(initialResponseParams.get(SCOPE), is(DDF_SCOPE));
    assertThat(initialResponseParams.get(RESPONSE_TYPE), is(CODE));
    assertThat(initialResponseParams.get(CLIENT_ID), is(DDF_CLIENT_ID));

    assertTrue(initialResponseParams.containsKey(REDIRECT_URI));
    assertTrue(initialResponseParams.containsKey(STATE)); // recommended by spec
    assertTrue(initialResponseParams.containsKey(RESPONSE_MODE)); // optional but sent in DDF
    assertTrue(initialResponseParams.containsKey(NONCE)); // optional but sent in DDF

    // Add token endpoint information to stub server
    String basicAuthHeader =
        "Basic "
            + java.util.Base64.getEncoder()
                .encodeToString(
                    (DDF_CLIENT_ID + ":" + DDF_CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

    String validIdToken =
        getBaseIdTokenBuilder()
            .withClaim(NONCE, initialResponseParams.get(NONCE))
            .sign(validAlgorithm);

    String validAccessToken = createAccessToken(true);
    String tokenEndpointResponse = createTokenEndpointResponse(validIdToken, validAccessToken);
    whenHttp(server)
        .match(
            post(TOKEN_ENDPOINT_PATH),
            parameter(CODE, TEMPORARY_CODE),
            parameter("grant_type", "authorization_code"),
            withHeader(AUTHORIZATION, basicAuthHeader))
        .then(ok(), contentType(APPLICATION_JSON), bytesContent(tokenEndpointResponse.getBytes()));

    // Respond to request after user logged in with the temporary code
    Response searchResponse =
        given()
            .cookie(JSESSIONID, initialResponseParams.get(JSESSIONID))
            .header(USER_AGENT, BROWSER_USER_AGENT)
            .header(HOST, "localhost:" + HTTPS_PORT.getPort())
            .header("Origin", URL_START.toString())
            .param(CODE, TEMPORARY_CODE)
            .param(STATE, initialResponseParams.get(STATE))
            .redirects()
            .follow(false)
            .expect()
            .statusCode(302)
            .when()
            .post(initialResponseParams.get(REDIRECT_URI));

    assertThat(searchResponse.header(LOCATION), is(SEARCH_URL.getUrl()));

    // Verify that the stub server was hit
    List<Call> tokenEndpointCalls =
        server
            .getCalls()
            .stream()
            .filter(call -> call.getUrl().equals(URL_START + TOKEN_ENDPOINT_PATH))
            .collect(Collectors.toList());
    assertThat(tokenEndpointCalls.size(), is(1));

    // Verify that we're logged in as admin
    Map<String, Object> userInfoList = getUserInfo(initialResponseParams.get(JSESSIONID));
    assertThat(userInfoList.get("name"), is(ADMIN));

    logout(initialResponseParams.get(JSESSIONID));
  }

  /**
   * Client MUST validate the signature of the ID Token according to JWS using the algorithm
   * specified in the all Header Parameter of the JOSE Header.
   */
  @Test
  public void testInvalidIdTokenSignatureCodeFlow() throws Exception {
    Map<String, String> initialResponseParams = sendInitialRequest(CODE);

    String idTokenWithInvalidSig =
        getBaseIdTokenBuilder()
            .withClaim(NONCE, initialResponseParams.get(NONCE))
            .sign(invalidAlgorithm);

    String validAccessToken = createAccessToken(true);
    String tokenEndpointResponse =
        createTokenEndpointResponse(idTokenWithInvalidSig, validAccessToken);

    sendAndAssertInvalidCodeFlowResponse(tokenEndpointResponse, initialResponseParams);
  }

  /**
   * REQUIRED. String value used to associate a Client session with an ID Token, and to mitigate
   * replay attacks. If a nonce value was sent in the Authentication Request, a nonce Claim MUST be
   * present and its value checked to verify that it is the same value as the one that was sent in
   * the Authentication Request. The Client SHOULD check the nonce value for replay attacks.
   */
  @Test
  public void testIdTokenWithInvalidNonceCodeFlow() throws Exception {
    Map<String, String> initialResponseParams = sendInitialRequest(CODE);

    String idTokenWthInvalidNonce =
        getBaseIdTokenBuilder().withClaim(NONCE, "WRONG").sign(validAlgorithm);

    String validAccessToken = createAccessToken(true);
    String tokenEndpointResponse =
        createTokenEndpointResponse(idTokenWthInvalidNonce, validAccessToken);

    sendAndAssertInvalidCodeFlowResponse(tokenEndpointResponse, initialResponseParams);
  }

  /**
   * The Issuer Identifier for the OpenID Provider (which is typically obtained during Discovery)
   * MUST exactly match the value of the iss (issuer) Claim.
   */
  @Test
  public void testIdTokenWithInvalidIssuerCodeFlow() throws Exception {
    Map<String, String> initialResponseParams = sendInitialRequest(CODE);

    String idTokenWithIncorrectIssuer =
        getBaseIdTokenBuilder()
            .withClaim(NONCE, initialResponseParams.get(NONCE))
            .withIssuer("WRONG")
            .sign(validAlgorithm);

    String validAccessToken = createAccessToken(true);
    String tokenEndpointResponse =
        createTokenEndpointResponse(idTokenWithIncorrectIssuer, validAccessToken);

    sendAndAssertInvalidCodeFlowResponse(tokenEndpointResponse, initialResponseParams);
  }

  /**
   * The Client MUST validate that the aud (audience) Claim contains its client_id value[…]. The ID
   * Token MUST be rejected if the ID Token does not list the Client as a valid audience, or if it
   * contains additional audiences not trusted by the Client.
   */
  @Test
  public void testIdTokenWithInvalidAudienceCodeFlow() throws Exception {
    Map<String, String> initialResponseParams = sendInitialRequest(CODE);

    String idTokenWithIncorrectAudience =
        getBaseIdTokenBuilder()
            .withClaim(NONCE, initialResponseParams.get(NONCE))
            .withAudience("WRONG")
            .sign(validAlgorithm);

    String validAccessToken = createAccessToken(true);
    String tokenEndpointResponse =
        createTokenEndpointResponse(idTokenWithIncorrectAudience, validAccessToken);

    sendAndAssertInvalidCodeFlowResponse(tokenEndpointResponse, initialResponseParams);
  }

  /** The current time MUST be before the time represented by the exp Claim. */
  @Test
  public void testExpiredIdTokenCodeFlow() throws Exception {
    Map<String, String> initialResponseParams = sendInitialRequest(CODE);

    long exp = Instant.now().minus(Duration.ofDays(3)).toEpochMilli();
    String expiredIdToken =
        getBaseIdTokenBuilder()
            .withClaim(NONCE, initialResponseParams.get(NONCE))
            .withExpiresAt(new Date(exp))
            .sign(validAlgorithm);

    String validAccessToken = createAccessToken(true);
    String tokenEndpointResponse = createTokenEndpointResponse(expiredIdToken, validAccessToken);

    sendAndAssertInvalidCodeFlowResponse(tokenEndpointResponse, initialResponseParams);
  }

  @Test
  public void testInvalidAccessTokenSignatureCodeFlow() throws Exception {
    Map<String, String> initialResponseParams = sendInitialRequest(CODE);

    String validIdToken =
        getBaseIdTokenBuilder()
            .withClaim(NONCE, initialResponseParams.get(NONCE))
            .sign(validAlgorithm);

    String invalidAccessToken = createAccessToken(false);
    String tokenEndpointResponse = createTokenEndpointResponse(validIdToken, invalidAccessToken);

    sendAndAssertInvalidCodeFlowResponse(tokenEndpointResponse, initialResponseParams);
  }

  /**
   * Hosts the given {@param invalidTokenEndpointResponse} under the token endpoint and sends a
   * response to DDF with a temporary code.
   *
   * <p>Asserts that the user is not logged in.
   *
   * @param invalidTokenEndpointResponse - response we wish DDf gets from the Token Endpoint
   * @param requestParams - a map of the parameters received in the initial response and the {@code
   *     JSESSIONID}
   */
  private void sendAndAssertInvalidCodeFlowResponse(
      String invalidTokenEndpointResponse, Map<String, String> requestParams) {

    // Add token endpoint information to stub server with an invalid id token in the response
    String basicAuthHeader =
        "Basic "
            + java.util.Base64.getEncoder()
                .encodeToString(
                    (DDF_CLIENT_ID + ":" + DDF_CLIENT_SECRET).getBytes(StandardCharsets.UTF_8));

    whenHttp(server)
        .match(
            post(TOKEN_ENDPOINT_PATH),
            parameter(CODE, TEMPORARY_CODE),
            parameter("grant_type", "authorization_code"),
            withHeader(AUTHORIZATION, basicAuthHeader))
        .then(
            ok(),
            contentType(APPLICATION_JSON),
            bytesContent(invalidTokenEndpointResponse.getBytes()));

    // Respond to request after user logged in with the temporary code
    // @formatter:off
    given()
        .cookie(JSESSIONID, requestParams.get(JSESSIONID))
        .header(USER_AGENT, BROWSER_USER_AGENT)
        .header(HOST, "localhost:" + HTTPS_PORT.getPort())
        .header("Origin", URL_START.toString())
        .param(CODE, TEMPORARY_CODE)
        .param(STATE, requestParams.get(STATE))
        .redirects()
        .follow(false)
        .expect()
        .statusCode(400)
        .when()
        .post(requestParams.get(REDIRECT_URI));
    // @formatter:on

    // Verify that we're NOT logged in
    Map<String, Object> userInfoList = getUserInfo(requestParams.get(JSESSIONID));
    assertThat(userInfoList.get("isGuest"), is(true));
  }

  // --------------------------Credential Flow Tests--------------------------//

  @Test
  public void testCredentialFlow() throws Exception {
    String accessToken = createAccessToken(true);
    String userInfo = createUserInfoJson(true, false);

    Response response =
        processCredentialFlow(accessToken, userInfo, false, HttpStatus.SC_MOVED_TEMPORARILY, true);

    assertThat(response.header(LOCATION), is(SEARCH_URL.getUrl()));

    // Verify that we're logged in as admin
    Map<String, Object> userInfoList = getUserInfo(response.cookies().get(JSESSIONID));
    assertThat(userInfoList.get("name"), is(ADMIN));

    logout(response.cookies().get(JSESSIONID));
  }

  @Test
  public void testSignedUserInfoResponseCredentialFlow() throws Exception {
    String accessToken = createAccessToken(true);
    String signedUserInfo = createUserInfoJson(true, true);

    Response response =
        processCredentialFlow(
            accessToken, signedUserInfo, true, HttpStatus.SC_MOVED_TEMPORARILY, true);

    assertThat(response.header(LOCATION), is(SEARCH_URL.getUrl()));

    // Verify that we're logged in as admin
    Map<String, Object> userInfoList = getUserInfo(response.cookies().get(JSESSIONID));
    assertThat(userInfoList.get("name"), is(ADMIN));

    logout(response.cookies().get(JSESSIONID));
  }

  @Test
  public void testInvalidUserInfoSignatureCredentialFlow() throws Exception {
    String accessToken = createAccessToken(true);
    String invalidSigUserInfo = createUserInfoJson(false, true);

    Response response =
        processCredentialFlow(
            accessToken, invalidSigUserInfo, true, HttpStatus.SC_BAD_REQUEST, true);

    // Verify that we're logged in as admin
    Map<String, Object> userInfoList = getUserInfo(response.cookies().get(JSESSIONID));
    assertThat(userInfoList.get("isGuest"), is(true));
  }

  @Test
  public void testInvalidAccessTokenCredentialFlow() throws Exception {
    String invalidAccessToken = createAccessToken(false);
    String userInfo = createUserInfoJson(true, false);

    Response response =
        processCredentialFlow(
            invalidAccessToken, userInfo, false, HttpStatus.SC_BAD_REQUEST, false);

    // Verify that we're logged in as admin
    Map<String, Object> userInfoList = getUserInfo(response.cookies().get(JSESSIONID));
    assertThat(userInfoList.get("isGuest"), is(true));
  }

  /**
   * Processes a credential flow request/response
   *
   * <ul>
   *   <li>Sets up a userinfo endpoint that responds with the given {@param userInfoResponse} when
   *       given {@param accessToken}
   *   <li>Sends a request to Intrigue with the {@param accessToken} as a parameter
   *   <li>Asserts that the response is teh expected response
   *   <li>Verifies if the userinfo endpoint is hit or not
   * </ul>
   *
   * @return the response for additional verification
   */
  private Response processCredentialFlow(
      String accessToken,
      String userInfoResponse,
      boolean isSigned,
      int expectedStatusCode,
      boolean userInfoShouldBeHit) {

    // Host the user info endpoint with the access token in the auth header
    String basicAuthHeader = "Bearer " + accessToken;

    String contentType = isSigned ? "application/jwt" : APPLICATION_JSON;
    whenHttp(server)
        .match(get(USER_INFO_ENDPOINT_PATH), withHeader(AUTHORIZATION, basicAuthHeader))
        .then(ok(), contentType(contentType), bytesContent(userInfoResponse.getBytes()));

    // Send a request to DDF with the access token
    Response response =
        given()
            .redirects()
            .follow(false)
            .expect()
            .statusCode(expectedStatusCode)
            .when()
            .get(SEARCH_URL.getUrl() + "?access_token=" + accessToken);

    List<Call> endpointCalls =
        server
            .getCalls()
            .stream()
            .filter(call -> call.getMethod().getMethodString().equals(GET))
            .filter(call -> call.getUrl().equals(URL_START + USER_INFO_ENDPOINT_PATH))
            .collect(Collectors.toList());

    if (userInfoShouldBeHit) {
      assertThat(endpointCalls.size(), is(greaterThanOrEqualTo(1)));
    } else {
      assertThat(endpointCalls.size(), is(0));
    }

    return response;
  }

  // --------------------------Logout Tests--------------------------//

  @Test
  public void testLogout() throws Exception {
    String jsessionidValue = login();

    // Send initial request to get OIDC logout url
    Response initialLogoutResponse =
        given()
            .cookie(JSESSIONID, jsessionidValue)
            .header(USER_AGENT, BROWSER_USER_AGENT)
            .header("Referer", SEARCH_URL + "?client_name=" + DDF_CLIENT_ID)
            .header(HOST, "localhost:" + HTTPS_PORT.getPort())
            .header("X-Requested-With", "XMLHttpRequest")
            .redirects()
            .follow(false)
            .expect()
            .statusCode(200)
            .when()
            .get(LOGOUT_REQUEST_URL.getUrl());

    // Get and verify the logout url from body
    String body = initialLogoutResponse.getBody().prettyPrint();
    List<Map<String, String>> responseJson = GSON.fromJson(body, List.class);
    Map<String, String> oidcLogoutProperties =
        responseJson
            .stream()
            .filter(entry -> ((String) ((Map) entry).get("title")).contains("OIDC"))
            .findFirst()
            .get();

    assertThat(oidcLogoutProperties.get("auth"), is(ADMIN));

    URI logoutUri = new URI(oidcLogoutProperties.get("url"));
    assertThat(logoutUri.getPath(), is("/auth/admin/master/protocol/openid-connect/logout"));

    Map<String, String> requestParams =
        URLEncodedUtils.parse(logoutUri, StandardCharsets.UTF_8)
            .stream()
            .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));

    assertTrue(requestParams.containsKey("id_token_hint"));
    assertTrue(requestParams.containsKey("post_logout_redirect_uri"));
    assertThat(requestParams.get("post_logout_redirect_uri"), is(LOGOUT_URL.getUrl()));

    // send response keycloak would have sent after logging out user
    Response logoutResponse =
        given()
            .cookie(JSESSIONID, jsessionidValue)
            .header(USER_AGENT, BROWSER_USER_AGENT)
            .header(HOST, "localhost:" + HTTPS_PORT.getPort())
            .redirects()
            .follow(false)
            .expect()
            .statusCode(307)
            .when()
            .get(requestParams.get("post_logout_redirect_uri"));

    String location = logoutResponse.header(LOCATION);
    assertThat(location, is(SECURE_ROOT + HTTPS_PORT.getPort() + "/logout"));

    // Verify that we're not logged in
    Map<String, Object> userInfoList = getUserInfo(jsessionidValue);
    assertThat(userInfoList.get("isGuest"), is(true));
  }

  // --------------------------Helper Methods--------------------------//

  /**
   * Configures the OIDC Handler and sends a request to /search/catalog.
   *
   * @param responseType - The response type to configure the flow with.
   * @return a map of the parameters received in the response and the {@code JSESSIONID}
   */
  private Map<String, String> sendInitialRequest(String responseType) throws Exception {
    // Configure DDF to use given flow
    handlerConfig.put("responseType", responseType);
    setConfig();

    Response initialResponse =
        given()
            .header(USER_AGENT, BROWSER_USER_AGENT)
            .redirects()
            .follow(false)
            .expect()
            .statusCode(302)
            .when()
            .get(SEARCH_URL.getUrl());

    URI locationUri = new URI(initialResponse.header(LOCATION));
    assertThat(locationUri.getPath(), is("/auth/realms/master/protocol/openid-connect/auth"));
    Map<String, String> requestParams =
        URLEncodedUtils.parse(locationUri, StandardCharsets.UTF_8)
            .stream()
            .collect(Collectors.toMap(NameValuePair::getName, NameValuePair::getValue));

    requestParams.putAll(initialResponse.cookies());
    return requestParams;
  }

  /**
   * Logs in with the admin user using the implicit flow
   *
   * @return the {@code JSESSIONID} cookie value returned by the login response
   */
  private String login() throws Exception {
    Map<String, String> requestParams = sendInitialRequest(ID_TOKEN);

    String validIdToken =
        getBaseIdTokenBuilder().withClaim(NONCE, requestParams.get(NONCE)).sign(validAlgorithm);

    given()
        .cookie(JSESSIONID, requestParams.get(JSESSIONID))
        .header(USER_AGENT, BROWSER_USER_AGENT)
        .header(HOST, "localhost:" + HTTPS_PORT.getPort())
        .header("Origin", URL_START.toString())
        .param(STATE, requestParams.get(STATE))
        .param(ID_TOKEN, validIdToken)
        .redirects()
        .follow(false)
        .expect()
        .statusCode(302)
        .when()
        .post(requestParams.get(REDIRECT_URI));

    // Verify that we're logged in as admin
    Map<String, Object> userInfoList = getUserInfo(requestParams.get(JSESSIONID));
    assertThat(userInfoList.get("name"), is(ADMIN));

    return requestParams.get(JSESSIONID);
  }

  /**
   * Logs out the user
   *
   * @param jsessionidValue - {@code JSESSIONID} value returned during login
   */
  public void logout(String jsessionidValue) {
    given()
        .cookie(JSESSIONID, jsessionidValue)
        .header(USER_AGENT, BROWSER_USER_AGENT)
        .header(HOST, "localhost:" + HTTPS_PORT.getPort())
        .redirects()
        .follow(false)
        .expect()
        .statusCode(307)
        .when()
        .get(LOGOUT_URL.getUrl());

    // Verify that we're not logged in
    Map<String, Object> userInfoList = getUserInfo(jsessionidValue);
    assertThat(userInfoList.get("isGuest"), is(true));
  }

  /**
   * Creates a JSON response that the token endpoint would respond with.
   *
   * @param idToken - id token the response will contain
   * @param accessToken - access token the response will contain
   * @return
   */
  private String createTokenEndpointResponse(String idToken, String accessToken) {
    JSONObject jsonObject = new JSONObject();
    jsonObject.put(ACCESS_TOKEN, accessToken);
    jsonObject.put("expires_in", 60);
    jsonObject.put("refresh_expires_in", 1800);
    jsonObject.put(REFRESH_TOKEN, createRefreshToken());
    jsonObject.put("token_type", "bearer");
    jsonObject.put(ID_TOKEN, idToken);
    jsonObject.put("not-before-policy", 0);
    jsonObject.put(SCOPE, "openid profile email");
    return jsonObject.toJSONString();
  }

  /** @return a JWT Builder with default ID token values */
  private JWTCreator.Builder getBaseIdTokenBuilder() {
    String[] roles = {"create-realm", "offline_access", "admin", "uma_authorization"};

    return JWT.create()
        .withJWTId(UUID.randomUUID().toString())
        .withExpiresAt(new Date(Instant.now().plus(Duration.ofDays(3)).toEpochMilli()))
        .withNotBefore(new Date(0))
        .withIssuedAt(new Date())
        .withIssuer(URL_START.toString() + "/auth/realms/master")
        .withAudience(DDF_CLIENT_ID)
        .withSubject(SUBJECT)
        .withClaim(PublicClaims.TYPE, "ID")
        .withClaim(AUTH_TIME, new Date())
        .withArrayClaim(ROLES, roles)
        .withClaim(EMAIL_VERIFIED, false)
        .withClaim(PREFERRED_USERNAME, ADMIN);
  }

  /**
   * Creates and returns a user info JSON
   *
   * @param valid - whether it should be valid or not
   * @param signed - whether it should be signed or not
   */
  private String createUserInfoJson(boolean valid, boolean signed) throws Exception {
    String[] roles = {"create-realm", "offline_access", "admin", "uma_authorization"};

    if (!signed) {
      String jsonString =
          GSON.toJson(
              ImmutableMap.of(
                  ROLES,
                  roles,
                  PublicClaims.SUBJECT,
                  SUBJECT,
                  EMAIL_VERIFIED,
                  false,
                  PREFERRED_USERNAME,
                  ADMIN));

      return valid ? jsonString : jsonString.substring(20);
    }

    JWTCreator.Builder builder =
        JWT.create()
            .withArrayClaim(ROLES, roles)
            .withClaim(PublicClaims.SUBJECT, SUBJECT)
            .withClaim(EMAIL_VERIFIED, false)
            .withClaim(PREFERRED_USERNAME, ADMIN);

    return valid ? builder.sign(validAlgorithm) : builder.sign(invalidAlgorithm);
  }

  /**
   * Creates an access token
   *
   * @param valid - whether the access token should have a valid signature or not
   * @return an access token to respond to DDF
   */
  private String createAccessToken(boolean valid) {
    long exp = Instant.now().plus(Duration.ofDays(3)).toEpochMilli();
    String[] audience = {"master-realm", "account"};
    String[] allowed = {SECURE_ROOT + HTTPS_PORT.getPort()};
    String[] roles = {"create-realm", "offline_access", "admin", "uma_authorization"};

    JSONObject realmAccess = new JSONObject();
    realmAccess.put(
        "roles", ImmutableList.of("create-realm", "offline_access", "admin", "uma_authorization"));

    JSONObject resourceAccess = createMasterRealmJsonObject();

    JWTCreator.Builder builder =
        JWT.create()
            .withJWTId(UUID.randomUUID().toString())
            .withExpiresAt(new Date(exp))
            .withNotBefore(new Date(0))
            .withIssuedAt(new Date())
            .withIssuer(URL_START.toString() + "/auth/realms/master")
            .withArrayClaim("aud", audience)
            .withSubject(SUBJECT)
            .withClaim("typ", "Bearer")
            .withClaim(AZP, DDF_CLIENT_ID)
            .withClaim("auth_time", new Date())
            .withArrayClaim("allowed-origins", allowed)
            .withClaim("realm_access", realmAccess.toString())
            .withClaim("resource_access", resourceAccess.toString())
            .withClaim(SCOPE, "openid profile email")
            .withArrayClaim(ROLES, roles)
            .withClaim(EMAIL_VERIFIED, false)
            .withClaim(PREFERRED_USERNAME, ADMIN);

    return valid ? builder.sign(validAlgorithm) : builder.sign(invalidAlgorithm);
  }

  /** @return a refresh token to respond to DDF */
  private String createRefreshToken() {
    long exp = Instant.now().plus(Duration.ofDays(3)).toEpochMilli();
    String[] audience = {"master-realm", "account"};
    JSONObject realmAccess = new JSONObject();
    realmAccess.put(
        "roles", ImmutableList.of("create-realm", "offline_access", "admin", "uma_authorization"));

    JSONObject resourceAccess = createMasterRealmJsonObject();

    return JWT.create()
        .withJWTId(UUID.randomUUID().toString())
        .withExpiresAt(new Date(exp))
        .withNotBefore(new Date(0))
        .withIssuedAt(new Date())
        .withIssuer(URL_START.toString() + "/auth/realms/master")
        .withAudience(URL_START.toString() + "/auth/realms/master")
        .withArrayClaim("aud", audience)
        .withSubject(SUBJECT)
        .withClaim("typ", "Refresh")
        .withClaim(AZP, DDF_CLIENT_ID)
        .withClaim("auth_time", 0)
        .withClaim("realm_access", realmAccess.toString())
        .withClaim("resource_access", resourceAccess.toString())
        .withClaim(SCOPE, "openid profile email")
        .sign(validAlgorithm);
  }

  /** Used in the creation of access tokens and refresh tokens */
  private JSONObject createMasterRealmJsonObject() {
    JSONObject masterRealm = new JSONObject();
    masterRealm.put(
        "roles",
        ImmutableList.of(
            "view-realm",
            "view-identity-providers",
            "manage-identity-providers",
            "impersonation",
            "create-client",
            "manage-users",
            "query-realms",
            "view-authorization",
            "query-clients",
            "query-users",
            "manage-events",
            "manage-realm",
            "view-events",
            "view-users",
            "view-clients",
            "manage-authorization",
            "manage-clients",
            "query-groups"));

    JSONObject account = new JSONObject();
    account.put(
        "roles", ImmutableList.of("manage-account", "manage-account-links", "view-profile"));

    JSONObject resourceAccess = new JSONObject();
    resourceAccess.put("master-realm", masterRealm);
    resourceAccess.put("account", account);
    return resourceAccess;
  }

  /**
   * Hits the whoami endpoint and returns the user's information
   *
   * @param jsessionId - the {@code JSESSIONID} value to use when hitting the endpoint
   * @return - the user info found
   */
  private Map<String, Object> getUserInfo(String jsessionId) {
    List<Map<String, Object>> subjectList =
        given()
            .cookie(JSESSIONID, jsessionId)
            .when()
            .get(WHO_AM_I_URL.getUrl())
            .then()
            .extract()
            .response()
            .jsonPath()
            .get("default.whoAmISubjects");

    if (subjectList.size() == 1) {
      return subjectList.get(0);
    }

    return subjectList
        .stream()
        .filter(subject -> !((boolean) subject.get("isGuest")))
        .findFirst()
        .get();
  }

  /** Configure the OIDC Handler with the given parameters */
  private void setConfig() throws Exception {
    Configuration config =
        getAdminConfig()
            .getConfiguration("org.codice.ddf.security.handler.api.OidcHandlerConfiguration", null);

    config.update(handlerConfig);

    for (Enumeration<String> keys = handlerConfig.keys(); keys.hasMoreElements(); ) {
      String key = keys.nextElement();
      expect("Wait for the OIDC Handler Configuration to be updated.")
          .within(2, TimeUnit.MINUTES)
          .until(() -> config.getProperties().get(key).equals(handlerConfig.get(key)));
    }

    // Wait for the OIDC Handler to test the connection, create the OIDC client etc..
    Thread.sleep(1000);
  }
}
