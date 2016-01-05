/**
 * Copyright (c) Codice Foundation
 * <p>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.test.itests.platform;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.CombinableMatcher.both;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.authentication.CertificateAuthSettings.certAuthSettings;
import static ddf.common.test.WaitCondition.expect;

import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.commons.io.IOUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.Configuration;
import org.xml.sax.SAXException;

import com.jayway.restassured.response.Response;

import ddf.common.test.BeforeExam;
import ddf.security.samlp.SamlProtocol;
import ddf.test.itests.AbstractIntegrationTest;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestSingleSignOn extends AbstractIntegrationTest {

    private static final String IDP_AUTH_TYPES =
            "/=SAML|GUEST,/search=IDP,/cometd=IDP,/solr=SAML|PKI|basic,/services/whoami=IDP|GUEST";

    private static final String KEY_STORE_PATH = System.getProperty("javax.net.ssl.keyStore");

    private static final String PASSWORD = System.getProperty("javax.net.ssl.keyStorePassword");

    private static final DynamicUrl SEARCH_URL = new DynamicUrl(DynamicUrl.SECURE_ROOT,
            HTTPS_PORT,
            "/search");

    private static final DynamicUrl IDP_URL = new DynamicUrl(SERVICE_ROOT, "/idp/login");

    private static final DynamicUrl WHO_AM_I_URL = new DynamicUrl(SERVICE_ROOT, "/whoami");

    private static final DynamicUrl AUTHENTICATION_REQUEST_ISSUER = new DynamicUrl(SERVICE_ROOT,
            "/saml/sso");

    private static final DynamicUrl LOGOUT_REQUEST_URL = new DynamicUrl(SERVICE_ROOT,
            "/logout/actions");

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
        METADATA, PROTOCOL
    }

    @BeforeExam
    public void beforeTest() throws Exception {
        basePort = getBasePort();
        getAdminConfig().setLogLevels();
        getSecurityPolicy().configureWebContextPolicy(null, IDP_AUTH_TYPES, null, null);
        getServiceManager().waitForAllBundles();
        getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query");
        getServiceManager().waitForHttpEndpoint(WHO_AM_I_URL.getUrl());

        // Start the services needed for testing.
        // We need to start the Search UI to test that it redirects properly
        getServiceManager().startFeature(true, "security-idp", "search-ui-app");

        // Get all of the metadata
        String metadata = get(SERVICE_ROOT + "/idp/login/metadata").asString();
        String ddfSpMetadata = get(SERVICE_ROOT + "/saml/sso/metadata").asString();

        // Make sure all the metadata is valid before we set it
        validateSaml(metadata, SamlSchema.METADATA);
        validateSaml(ddfSpMetadata, SamlSchema.METADATA);

        // The IdP server can point to multiple Service Providers and as such expects an array.
        // Thus, even though we are only setting a single item, we must wrap it in an array.
        setConfig("org.codice.ddf.security.idp.client.IdpMetadata", "metadata", metadata);
        setConfig("org.codice.ddf.security.idp.server.IdpEndpoint",
                "spMetadata",
                new String[] {ddfSpMetadata});
    }

    private void validateSaml(String xml, SamlSchema schema) throws IOException {

        // Prepare the schema and xml
        String schemaFileName = "saml-schema-" + schema.toString()
                .toLowerCase() + "-2.0.xsd";
        URL schemaURL = getClass().getClassLoader()
                .getResource(schemaFileName);
        StreamSource streamSource = new StreamSource(new StringReader(xml));

        // If we fail to create a validator we don't want to stop the show, so we just log a warning
        Validator validator = null;
        try {
            validator = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                    .newSchema(schemaURL)
                    .newValidator();
        } catch (SAXException e) {
            LOGGER.warn("Exception creating validator. ", e);
        }

        // If the xml is invalid, then we want to fail completely
        if (validator != null) {
            try {
                validator.validate(streamSource);
            } catch (SAXException e) {
                fail("Failed to validate SAML " + e.getMessage());
            }
        }
    }

    private void setConfig(String pid, String param, Object value) throws Exception {

        // Update the config
        Configuration config = getAdminConfig().getConfiguration(pid, null);
        // @formatter:off
        config.update(new Hashtable<String, Object>() { { put(param, value); } });
        // @formatter:on

        // We have to make sure the config has been updated before we can use it
        // @formatter:off
        expect("Configs to update").
                within(2, TimeUnit.MINUTES).
                until(() -> config.getProperties() != null
                        && (config.getProperties().get(param) != null));
        // @formatter:on
    }

    private ResponseHelper getSearchResponse(boolean isPassiveRequest) throws Exception {

        // We should be redirected to the IdP when we first try to hit the search page
        // @formatter:off
        Response searchResponse =
                given().
                        redirects().follow(false).
                expect().
                        statusCode(302).
                when().
                        get(SEARCH_URL.getUrl());
        // @formatter:on
        // Because we get back a 302, we know the redirect location is in the header
        ResponseHelper searchHelper = new ResponseHelper(searchResponse);
        searchHelper.parseHeader();

        // We get bounced to a login page which has the parameters we need to pass to the IdP
        // embedded on the page in a JSON Object
        // @formatter:off
        Response redirectResponse =
                given().
                        params(searchHelper.params).
                expect().
                        statusCode(200).
                when().
                        get(searchHelper.redirectUrl);
        // @formatter:on

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
        // @formatter:off
        return get(WHO_AM_I_URL.getUrl()).body().asString();
        // @formatter:on
    }

    private String getUserName(Map<String, String> cookies) {
        // @formatter:off
        return given().cookies(cookies).when().get(WHO_AM_I_URL.getUrl()).body().asString();
        // @formatter:on
    }

    private ResponseHelper performHttpRequestUsingBinding(Binding binding, String relayState)
            throws Exception {

        // Signing is tested in the unit tests, so we don't require signing here to make things simpler
        setConfig("org.codice.ddf.security.idp.server.IdpEndpoint", "strictSignature", false);

        // Set the metadata
        String confluenceSpMetadata = String.format(IOUtils.toString(getClass().
                        getResourceAsStream("/confluence-sp-metadata.xml")),
                AUTHENTICATION_REQUEST_ISSUER,
                binding.toString());
        validateSaml(confluenceSpMetadata, SamlSchema.METADATA);
        setConfig("org.codice.ddf.security.idp.server.IdpEndpoint",
                "spMetadata",
                new String[] {confluenceSpMetadata});

        // Get the authn request
        String mockAuthnRequest = String.format(IOUtils.toString(getClass().
                        getResourceAsStream("/confluence-sp-authentication-request.xml")),
                binding.toString(),
                AUTHENTICATION_REQUEST_ISSUER);
        validateSaml(mockAuthnRequest, SamlSchema.PROTOCOL);

        String encodedRequest = RestSecurity.deflateAndBase64Encode(mockAuthnRequest);
        // @formatter:off
        Response idpResponse =
                given().
                        auth().preemptive().basic("admin", "admin").
                        param("AuthMethod", "up").
                        param("SAMLRequest", encodedRequest).
                        param("RelayState", relayState).
                        param("OriginalBinding", Binding.POST == binding ?
                                SamlProtocol.Binding.HTTP_POST.getUri() :
                                SamlProtocol.Binding.HTTP_REDIRECT.getUri()).
                expect().
                        statusCode(200).
                when().
                        get(IDP_URL.getUrl() + "/sso");
        // @formatter:on
        return new ResponseHelper(idpResponse);
    }

    @Test
    public void testRedirectBinding() throws Exception {
        String relayState = "test";
        ResponseHelper helper = performHttpRequestUsingBinding(Binding.REDIRECT, relayState);

        assertThat(helper.parseBody(), is(Binding.REDIRECT));
        assertThat(helper.get("RelayState"), is(relayState));

        String inflatedSamlResponse = RestSecurity.inflateBase64(helper.get("SAMLResponse"));
        validateSaml(inflatedSamlResponse, SamlSchema.PROTOCOL);
    }

    @Test
    public void testPostBinding() throws Exception {

        // We should get back a POST form that has everything we put in, thus the only thing
        // of interest to really check is that we do get back a post form
        ResponseHelper helper = performHttpRequestUsingBinding(Binding.POST, "test");
        assertThat(helper.parseBody(), is(Binding.POST));
    }

    @Test
    public void testBadUsernamePassword() throws Exception {
        ResponseHelper searchHelper = getSearchResponse(false);

        // We're using an AJAX call, so anything other than 200 means not authenticated
        // @formatter:off
        given().
                auth().preemptive().basic("definitely", "notright").
                param("AuthMethod", "up").
                params(searchHelper.params).
        expect().
                statusCode(not(200)).
        when().
                get(searchHelper.redirectUrl);
        // @formatter:on
    }

    @Test
    public void testPkiAuth() throws Exception {

        // Note that PKI is passive (as opposed to username/password which is not)
        ResponseHelper searchHelper = getSearchResponse(true);

        // @formatter:off
        given().
                auth().
                certificate(KEY_STORE_PATH, PASSWORD, certAuthSettings()
                        .sslSocketFactory(SSLSocketFactory.getSystemSocketFactory())).
                param("AuthMethod", "pki").
                params(searchHelper.params).
        expect().
                statusCode(200).
        when().
                get(searchHelper.redirectUrl);
        // @formatter:on
    }

    @Test
    public void testGuestAuth() throws Exception {
        ResponseHelper searchHelper = getSearchResponse(false);

        // @formatter:off
        given().
                param("AuthMethod", "guest").
                params(searchHelper.params).
        expect().
                statusCode(200).
        when().
                get(searchHelper.redirectUrl);
        // @formatter:on
    }

    @Test
    public void testRedirectFlow() throws Exception {

        // Negative test to make sure we aren't admin yet
        assertThat(getUserName(), not("admin"));

        // First time hitting search, expect to get redirected to the Identity Provider.
        ResponseHelper searchHelper = getSearchResponse(false);

        // Pass our credentials to the IDP, it should redirect us to the Assertion Consumer Service.
        // The redirect is currently done via javascript and not an HTTP redirect.
        // @formatter:off
        Response idpResponse =
                given().
                        auth().preemptive().basic("admin", "admin").
                        param("AuthMethod", "up").
                        params(searchHelper.params).
                expect().
                        statusCode(200).
                when().
                        get(searchHelper.redirectUrl);
        // @formatter:on

        ResponseHelper idpHelper = new ResponseHelper(idpResponse);

        // Perform a bunch of checks to make sure we're valid against both the spec and schema
        assertThat(idpHelper.parseBody(), is(Binding.REDIRECT));
        String inflatedSamlResponse = RestSecurity.inflateBase64(idpHelper.get("SAMLResponse"));
        validateSaml(inflatedSamlResponse, SamlSchema.PROTOCOL);
        assertThat(inflatedSamlResponse,
                allOf(containsString("urn:oasis:names:tc:SAML:2.0:status:Success"),
                        containsString("ds:SignatureValue"),
                        containsString("saml2:Assertion")));
        assertThat(idpHelper.get("SigAlg"), not(isEmptyOrNullString()));
        assertThat(idpHelper.get("Signature"), not(isEmptyOrNullString()));
        assertThat(idpHelper.get("RelayState")
                .length(), is(both(greaterThanOrEqualTo(0)).and(lessThanOrEqualTo(80))));

        // After passing the SAML Assertion to the ACS, we should be redirected back to Search.
        // @formatter:off
        Response acsResponse =
                given().
                        params(idpHelper.params).
                        redirects().follow(false).
                expect().
                        statusCode(anyOf(is(302), is(303))).
                        when().
                get(idpHelper.redirectUrl);
        // @formatter:on

        ResponseHelper acsHelper = new ResponseHelper(acsResponse);
        acsHelper.parseHeader();

        // Access search again, but now as an authenticated user.
        // @formatter:off
        given().
                cookies(acsResponse.getCookies()).
        expect().
                statusCode(200).
        when().
                get(acsHelper.redirectUrl);
        // @formatter:on

        // Make sure we are logged in as admin.
        assertThat(getUserName(acsResponse.getCookies()), is("admin"));
    }

    @Test
    public void testLogout() throws Exception {

        // Negative test to make sure we aren't admin yet
        assertThat(getUserName(), not("admin"));

        // First time hitting search, expect to get redirected to the Identity Provider.
        ResponseHelper searchHelper = getSearchResponse(false);

        // Pass our credentials to the IDP, it should redirect us to the Assertion Consumer Service.
        // The redirect is currently done via javascript and not an HTTP redirect.
        // @formatter:off
        Response idpResponse =
                given().
                        auth().preemptive().basic("admin", "admin").
                        param("AuthMethod", "up").
                        params(searchHelper.params).
                expect().
                        statusCode(200).
                when().
                        get(searchHelper.redirectUrl);
        // @formatter:on

        ResponseHelper idpHelper = new ResponseHelper(idpResponse);
        idpHelper.parseBody();

        // After passing the SAML Assertion to the ACS, we should be redirected back to Search.
        // @formatter:off
        Response acsResponse =
                given().
                        params(idpHelper.params).
                        redirects().follow(false).
                expect().
                        statusCode(anyOf(is(302), is(303))).
                when().
                        get(idpHelper.redirectUrl);
        // @formatter:on

        ResponseHelper acsHelper = new ResponseHelper(acsResponse);
        acsHelper.parseHeader();

        // Access search again, but now as an authenticated user.
        // @formatter:off
        given().
                cookies(acsResponse.getCookies()).
        expect().
                statusCode(200).
        when().
                get(acsHelper.redirectUrl);
        // @formatter:on

        // Make sure we are logged in as admin.
        assertThat(getUserName(acsResponse.getCookies()), is("admin"));

        // Create logout request
        // @formatter:off
        Response createLogoutRequest =
                given().
                        cookies(acsResponse.getCookies()).
                expect().
                        statusCode(200).
                when().
                        get(LOGOUT_REQUEST_URL.getUrl());
        // @formatter:on

        ResponseHelper createLogoutHelper = new ResponseHelper(createLogoutRequest);
        createLogoutHelper.parseBody();

        // Logout via url returned in logout request
        // @formatter:off
        given().
        expect().
                statusCode(200).
                body(containsString("Successfully logged out")).
        when().
                get(createLogoutHelper.get("url"));
        // @formatter:on

        // Verify admin user is no longer logged in
        assertThat(getUserName(), not("admin"));
    }

    private class ResponseHelper {

        private final Response response;

        private String redirectUrl;

        private final Map<String, String> params = new HashMap<>();

        private ResponseHelper(Response response) throws IOException {
            this.response = response;
        }

        private String get(String key) {
            return params.get(key);
        }

        private void parseParamsFromUrl(String url) throws URISyntaxException {
            redirectUrl = url.split("[?]")[0];

            List<NameValuePair> paramList = URLEncodedUtils.parse(new URI(url), "UTF-8");
            for (NameValuePair param : paramList) {
                params.put(param.getName(), param.getValue());
            }
        }

        private void parseHeader() throws URISyntaxException {
            if (response.headers()
                    .hasHeaderWithName("Location")) {
                parseParamsFromUrl(response.header("Location"));
            } else {
                fail("Response does not have a header \"Location\"");
            }
        }

        private Binding parseBody() throws URISyntaxException {

            // Because a POST form only has the stuff we put into it, we don't care
            // about any parsing beyond recognizing it as a form
            String body = response.body()
                    .asString();
            Binding binding = null;
            if (body.contains("<form")) {
                binding = Binding.POST;
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
            String[] keyValuePairs = json.split("[,]");
            for (String pair : keyValuePairs) {
                String key = pair.split("[:]", 2)[0].replaceAll("(^\")|(\"$)", "");
                String value = pair.split("[:]", 2)[1].replaceAll("(^\")|(\"$)", "");
                params.put(key, value);
            }
        }

        private void parseBodyLogin() throws URISyntaxException {
            // We're trying to parse a javascript variable that is embedded in an HTML form
            Pattern pattern = Pattern.compile("window.idpState *= *\\{(.*)\\}",
                    Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(response.body()
                    .asString());
            if (matcher.find()) {
                parseJson(matcher.group(1));
            } else {
                String failMessage =
                        "Failed to find the javascript var." + "\nPattern: " + pattern.toString()
                                + "\nResponse Body: " + response.body()
                                .asString();
                fail(failMessage);
            }
        }

        private void parseBodyRedirect() throws URISyntaxException {

            // We're trying to parse a javascript variable that is embedded in an HTML form
            Pattern pattern = Pattern.compile("encoded *= *\"(.*)\"", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(response.body()
                    .asString());
            if (matcher.find()) {
                parseParamsFromUrl(matcher.group(1));
            } else {
                String failMessage =
                        "Failed to find the javascript var." + "\nPattern: " + pattern.toString()
                                + "\nResponse Body: " + response.body()
                                .asString();
                fail(failMessage);
            }
        }

        private void parseBodyLogout() {
            parseJson(response.body()
                    .asString());
        }
    }
}
