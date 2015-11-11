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
package ddf.catalog.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.AllOf.allOf;
import static org.hamcrest.core.CombinableMatcher.both;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.get;
import static com.jayway.restassured.RestAssured.given;

import java.io.StringReader;
import java.net.URI;
import java.net.URL;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import javax.xml.validation.Validator;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.Configuration;

import com.jayway.restassured.response.Response;

import ddf.common.test.BeforeExam;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestSingleSignOn extends AbstractIntegrationTest {

    public static final String IDP_AUTH_TYPES = "/=SAML|ANON,/search=SAML|IDP,/solr=SAML|PKI|basic";

    private static final DynamicUrl SEARCH_URL = new DynamicUrl(DynamicUrl.SECURE_ROOT, HTTPS_PORT,
            "/search");

    private static final DynamicUrl WHO_AM_I_URL = new DynamicUrl(SERVICE_ROOT, "/whoami");

    @BeforeExam
    public void beforeTest() throws Exception {
        basePort = getBasePort();
        getAdminConfig().setLogLevels();
        getSecurityPolicy().configureWebContextPolicy(null, IDP_AUTH_TYPES, null, null);
        getServiceManager().waitForAllBundles();
        getServiceManager().waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query");
        getServiceManager().waitForHttpEndpoint(WHO_AM_I_URL.getUrl());

        // Start the services needed for testing. We need to start the Search UI to test that it redirects properly
        getServiceManager().startFeature(true, "security-idp", "search-ui-app");

        // Create a validator from the metadata schema so that we can validate the metadata
        // TODO: Maybe don't go out to the internet for this? The schema itself is going to go out to the internet anyway though.
        URL schemaFile = new URL(
                "http://docs.oasis-open.org/security/saml/v2.0/saml-schema-metadata-2.0.xsd");
        SchemaFactory schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        Schema schema = schemaFactory.newSchema(schemaFile);
        Validator validator = schema.newValidator();

        // Get the metadata
        String clientMetadata = get(SERVICE_ROOT + "/idp/login/metadata").asString();
        String serverMetadata = get(SERVICE_ROOT + "/saml/sso/metadata").asString();

        // TODO: The server metadata requires an "index" attribute to validate properly
        validator.validate(new StreamSource(new StringReader(clientMetadata)));
        validator.validate(new StreamSource(new StringReader(serverMetadata)));

        // To find the right inputs for the settings, go into the metatype.xml file.
        // The key is the "id" and the value type is determined by the cardinality as such:
        // Positive = array, negative = vector, 0 (none) = single variable
        Dictionary<String, Object> clientSettings = new Hashtable<>();
        clientSettings.put("metadata", clientMetadata);
        Dictionary<String, Object> serverSettings = new Hashtable<>();
        serverSettings.put("spMetadata", new String[] {serverMetadata});

        // Update the client and server with the metadata
        Configuration clientConfig = getAdminConfig()
                .getConfiguration("org.codice.ddf.security.idp.client.IdpMetadata", null);
        Configuration serverConfig = getAdminConfig()
                .getConfiguration("org.codice.ddf.security.idp.server.IdpEndpoint", null);
        clientConfig.update(clientSettings);
        serverConfig.update(serverSettings);
        assertThat(clientConfig.getProperties(), notNullValue());
        assertThat(serverConfig.getProperties(), notNullValue());
    }

    private String getRedirectUrl(Response response) {
        String fullUrl = null;

        // We can either get a legit redirect from the header, or we can have javascript redirect us.
        // Whenever javascript redirects us, we have to do ugly, fragile parsing of the HTML.
        if (response.headers().hasHeaderWithName("Location")) {
            fullUrl = response.header("Location");
        } else if (response.getBody().asString()
                .contains("<title>Redirect</title>")) { // Javascript redirect
            Pattern pattern = Pattern.compile("encoded *= *\"(.*)\"", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(response.getBody().asString());
            matcher.find();
            fullUrl = matcher.group(1);
        } else {
            fail("Unable to extract the redirect URL from the HTTP response. "
                    + "No redirect found in header and body does not match "
                    + "regular expression [encoded *= *\"(.*)\"]");
        }
        return fullUrl;
    }

    private String parseUrl(Response response) {
        return getRedirectUrl(response).split("[?]")[0];
    }

    private Map<String, String> parseParams(Response response) throws Exception {
        String url = getRedirectUrl(response);

        // RestAssured expects url query parameters to be in the form of a Dictionary.
        List<NameValuePair> paramList = URLEncodedUtils.parse(new URI(url), "UTF-8");
        HashMap<String, String> jsonParams = new HashMap<>();
        for (NameValuePair param : paramList) {
            jsonParams.put(param.getName(), param.getValue());
        }
        return jsonParams;
    }

    @Test
    public void testBadUsernamePassword() throws Exception {

        // Request search service
        Response searchResponse = given().redirects().follow(false).expect().statusCode(302).when()
                .get(SEARCH_URL.getUrl());

        // Pass bad credentials to IDP
        // TODO: This should be a 401, not 500
        given().auth().preemptive().basic("definitely", "notright").param("AuthMethod", "up")
                .params(parseParams(searchResponse)).expect().statusCode(500)
                .body(containsString("could not be authenticated")).when()
                .get(parseUrl(searchResponse) + "/sso");
    }

    private void validateSamlResponse(Map<String, String> samlParams) throws Exception {
        String samlResponse = RestSecurity.inflateBase64(samlParams.get("SAMLResponse"));

        assertThat(samlParams.get("SigAlg"), not(isEmptyOrNullString()));
        assertThat(samlParams.get("Signature"), not(isEmptyOrNullString()));

        assertThat(samlResponse, allOf(containsString("urn:oasis:names:tc:SAML:2.0:status:Success"),
                containsString("ds:SignatureValue"), containsString("saml2:Assertion")));
        assertThat(samlParams.get("RelayState").length(),
                is(both(greaterThan(0)).and(lessThanOrEqualTo(80))));
    }

    @Test
    public void testIdpAuth() throws Exception {

        // Negative test to make sure we aren't admin yet
        given().expect().statusCode(200).body(not(containsString("admin"))).when()
                .get(WHO_AM_I_URL.getUrl());

        // First time hitting search, expect to get redirected to the Identity Provider.
        Response searchResponse = given().redirects().follow(false).expect().statusCode(302).when()
                .get(SEARCH_URL.getUrl());

        // Pass our credentials to the IDP, it should redirect us to the Assertion Consumer Service.
        // The redirect is currently done via javascript and not an HTTP redirect.
        Response idpResponse = given().auth().preemptive().basic("admin", "admin")
                .param("AuthMethod", "up").params(parseParams(searchResponse)).expect()
                .statusCode(200).body(containsString("<title>Redirect</title>")).when()
                .get(parseUrl(searchResponse) + "/sso");

        // Make sure we pass a valid SAML assertion to the ACS
        validateSamlResponse(parseParams(idpResponse));

        // After passing the SAML Assertion to the ACS, we should be redirected back to Search.
        Response acsResponse = given().params(parseParams(idpResponse)).redirects().follow(false)
                .expect().statusCode(anyOf(is(303), is(302))).when().get(parseUrl(idpResponse));

        // Access search again, but now as an authenticated user.
        given().cookies(acsResponse.getCookies()).expect().statusCode(200).when()
                .get(parseUrl(acsResponse));

        // Make sure we are logged in as admin.
        given().cookies(acsResponse.getCookies()).expect().statusCode(200)
                .body(containsString("admin")).when().get(WHO_AM_I_URL.getUrl());
    }
}