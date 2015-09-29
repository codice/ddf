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

import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.hamcrest.xml.HasXPath;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;

import com.jayway.restassured.path.json.JsonPath;
import com.jayway.restassured.response.Response;

import ddf.common.test.BeforeExam;
import ddf.security.SecurityConstants;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestSecurity extends AbstractIntegrationTest {

    protected static final String SOAP_ENV =
            "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                    + "    <soap:Header>\n"
                    + "        <Action xmlns=\"http://www.w3.org/2005/08/addressing\">http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue</Action>\n"
                    + "        <MessageID xmlns=\"http://www.w3.org/2005/08/addressing\">urn:uuid:c0c43e1e-0264-4018-9a58-d1fda4332ab3</MessageID>\n"
                    + "        <To xmlns=\"http://www.w3.org/2005/08/addressing\">https://localhost:8993/services/SecurityTokenService</To>\n"
                    + "        <ReplyTo xmlns=\"http://www.w3.org/2005/08/addressing\">\n"
                    + "            <Address>http://www.w3.org/2005/08/addressing/anonymous</Address>\n"
                    + "        </ReplyTo>\n"
                    + "        <wsse:Security xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\" soap:mustUnderstand=\"1\">\n"
                    + "            <wsu:Timestamp wsu:Id=\"TS-3\">\n"
                    + "                <wsu:Created>CREATED</wsu:Created>\n"
                    + "                <wsu:Expires>EXPIRES</wsu:Expires>\n"
                    + "            </wsu:Timestamp>\n" + "        </wsse:Security>\n"
                    + "    </soap:Header>\n" + "    <soap:Body>\n"
                    + "        <wst:RequestSecurityToken xmlns:wst=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">\n"
                    + "            <wst:SecondaryParameters>\n"
                    + "                <t:TokenType xmlns:t=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0</t:TokenType>\n"
                    + "                <t:KeyType xmlns:t=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">http://docs.oasis-open.org/ws-sx/ws-trust/200512/Bearer</t:KeyType>\n"
                    + "                <t:Claims xmlns:ic=\"http://schemas.xmlsoap.org/ws/2005/05/identity\" xmlns:t=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\" Dialect=\"http://schemas.xmlsoap.org/ws/2005/05/identity\">\n"
                    + "                    <!--Add any additional claims you want to grab for the service-->\n"
                    + "                    <ic:ClaimType Optional=\"true\" Uri=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role\"/>\n"
                    + "                    <ic:ClaimType Optional=\"true\" Uri=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier\"/>\n"
                    + "                    <ic:ClaimType Optional=\"true\" Uri=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress\"/>\n"
                    + "                    <ic:ClaimType Optional=\"true\" Uri=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname\"/>\n"
                    + "                    <ic:ClaimType Optional=\"true\" Uri=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname\"/>\n"
                    + "                </t:Claims>\n" + "            </wst:SecondaryParameters>\n"
                    + "                ON_BEHALF_OF"
                    + "            <wst:RequestType>http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue</wst:RequestType>\n"
                    + "            <wsp:AppliesTo xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n"
                    + "                <wsa:EndpointReference xmlns:wsa=\"http://www.w3.org/2005/08/addressing\">\n"
                    + "                    <wsa:Address>https://localhost:8993/services/QueryService</wsa:Address>\n"
                    + "                </wsa:EndpointReference>\n"
                    + "            </wsp:AppliesTo>\n" + "            <wst:Renewing/>\n"
                    + "        </wst:RequestSecurityToken>\n" + "    </soap:Body>\n"
                    + "</soap:Envelope>";

    protected static final String SDK_SOAP_CONTEXT = "/services/sdk/SoapService";

    @BeforeExam
    public void beforeTest() throws Exception {
        setLogLevels();
        waitForAllBundles();
        configurePDP();
        waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query");
    }

    public void configurePDP() throws Exception {
    }

    @Before
    public void before() throws Exception {
        configureRestForAnonymous();
    }

    private void configureRestForAnonymous() throws Exception {
        getSecurityPolicy().configureRestForAnonymous(SDK_SOAP_CONTEXT);
    }

    private void configureRestForBasic() throws Exception {
        getSecurityPolicy().configureRestForBasic(SDK_SOAP_CONTEXT);
    }

    @Test
    public void testAnonymousRestAccess() throws Exception {
        String url = SERVICE_ROOT + "/catalog/query?q=*";

        //test that anonymous works and check that we get an sso token
        String cookie = when().get(url).then().log().all().assertThat().statusCode(equalTo(200))
                .assertThat().header("Set-Cookie", containsString("JSESSIONID")).extract()
                .cookie("JSESSIONID");

        //try again with the sso token
        given().cookie("JSESSIONID", cookie).when().get(url).then().log().all().assertThat()
                .statusCode(equalTo(200));

        //try to hit an admin restricted page and see that we are unauthorized
        given().cookie("JSESSIONID", cookie).when().get("https://localhost:9993/admin/index.html")
                .then().log().all().assertThat().statusCode(equalTo(403));
    }

    @Test
    public void testBasicRestAccess() throws Exception {
        String url = SERVICE_ROOT + "/catalog/query?q=*";

        configureRestForBasic();

        //test that we get a 401 if no credentials are specified
        when().get(url).then().log().all().assertThat().statusCode(equalTo(401));

        //try a random user and get a 401
        given().auth().basic("bad", "user").when().get(url).then().log().all().assertThat()
                .statusCode(equalTo(401));

        //try a real user and get an sso token back
        String cookie = given().auth().basic("admin", "admin").when().get(url).then().log().all()
                .assertThat().statusCode(equalTo(200)).assertThat()
                .header("Set-Cookie", containsString("JSESSIONID")).extract().cookie("JSESSIONID");

        //try the sso token instead of basic auth
        given().cookie("JSESSIONID", cookie).when().get(url).then().log().all().assertThat()
                .statusCode(equalTo(200));

        //try that admin level sso token on a restricted resource and get in... sso works!
        given().cookie("JSESSIONID", cookie).when().get("https://localhost:9993/admin/index.html")
                .then().log().all().assertThat().statusCode(equalTo(200));
    }

    @Test
    public void testBasicFederatedAuth() throws Exception {
        String recordId = TestCatalog.ingest(Library.getSimpleGeoJson(), "application/json");
        configureRestForBasic();

        //Positive tests
        OpenSearchSourceProperties openSearchProperties = new OpenSearchSourceProperties(
                OPENSEARCH_SOURCE_ID);
        openSearchProperties.put("username", "admin");
        openSearchProperties.put("password", "admin");
        createManagedService(OpenSearchSourceProperties.FACTORY_PID, openSearchProperties);

        CswSourceProperties cswProperties = new CswSourceProperties(CSW_SOURCE_ID);
        cswProperties.put("username", "admin");
        cswProperties.put("password", "admin");
        createManagedService(CswSourceProperties.FACTORY_PID, cswProperties);

        waitForFederatedSource(OPENSEARCH_SOURCE_ID);
        waitForFederatedSource(CSW_SOURCE_ID);

        String openSearchQuery = SERVICE_ROOT + "/catalog/query?q=*&src=" + OPENSEARCH_SOURCE_ID;
        given().auth().basic("admin", "admin").when().get(openSearchQuery).then().log().all()
                .assertThat().statusCode(equalTo(200)).assertThat().body(containsString("myTitle"));

        String cswQuery = SERVICE_ROOT + "/catalog/query?q=*&src=" + CSW_SOURCE_ID;
        given().auth().basic("admin", "admin").when().get(cswQuery).then().log().all().assertThat()
                .statusCode(equalTo(200)).assertThat().body(containsString("myTitle"));

        //Negative tests
        String unavailableCswSourceId = "Unavailable Csw";
        cswProperties = new CswSourceProperties(unavailableCswSourceId);
        cswProperties.put("username", "bad");
        cswProperties.put("password", "auth");
        createManagedService(CswSourceProperties.FACTORY_PID, cswProperties);

        String cswQueryUnavail = SERVICE_ROOT + "/catalog/query?q=*&src=" + unavailableCswSourceId;
        given().auth().basic("admin", "admin").when().get(cswQueryUnavail).then().log().all()
                .assertThat().statusCode(equalTo(500));

        /*
        //Negative tests TODO: DDF-1444 Negative OpenSearchSource BasicAuth tests currently do not work
        String unavailableOpenSourceId = "Unavailable OpenSearchSource";

        OpenSearchSourceProperties openSearchUnavailProp = new OpenSearchSourceProperties(
                unavailableOpenSourceId);
        openSearchUnavailProp.put("username", "bad");
        openSearchUnavailProp.put("password", "auth");
        createManagedService(OpenSearchSourceProperties.FACTORY_PID, openSearchUnavailProp);
        waitForFederatedSource(unavailableOpenSourceId);

        String unavailableOpenSearchQuery =
                SERVICE_ROOT + "/catalog/query?q=*&src=" + unavailableOpenSourceId;

        given().auth().basic("admin", "admin").when().get(unavailableOpenSearchQuery).then().log()
                .all().assertThat().statusCode(equalTo(200)).assertThat()
                .body(not(containsString("myTitle")));

        String unavailableCswQuery = SERVICE_ROOT + "/catalog/query?q=*&src=" + unavailableCswSourceId;
        */

        configureRestForAnonymous();
        TestCatalog.deleteMetacard(recordId);
    }

    @Test
    public void testAnonymousSoapAccess() throws Exception {
        String body =
                "<soapenv:Envelope xmlns:hel=\"http://ddf.sdk/soap/hello\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                        + "   <soapenv:Header>\n" + "   </soapenv:Header>\n" + "   <soapenv:Body>\n"
                        + "      <hel:helloWorld/>\n" + "   </soapenv:Body>\n"
                        + "</soapenv:Envelope>";
        //we are only testing anonymous because that hits the most code, testing with an assertion would be mostly testing the same stuff that this is hitting
        given().log().all().body(body).header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "helloWorld").expect().statusCode(equalTo(200)).when()
                .post(SERVICE_ROOT + "/sdk/SoapService").then().log().all().assertThat()
                .body(HasXPath.hasXPath("//*[local-name()='helloWorldResponse']/result/text()",
                        containsString("Anonymous")));
    }

    @Test
    public void testAnonymousSoapAccessHttp() throws Exception {
        startFeature(true, "platform-http-proxy");

        String body =
                "<soapenv:Envelope xmlns:hel=\"http://ddf.sdk/soap/hello\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                        + "   <soapenv:Header>\n" + "   </soapenv:Header>\n" + "   <soapenv:Body>\n"
                        + "      <hel:helloWorld/>\n" + "   </soapenv:Body>\n"
                        + "</soapenv:Envelope>";
        //we are only testing anonymous because that hits the most code, testing with an assertion would be mostly testing the same stuff that this is hitting
        given().log().all().body(body).header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "helloWorld").expect().statusCode(equalTo(200)).when()
                .post(INSECURE_SERVICE_ROOT + "/sdk/SoapService").then().log().all().assertThat()
                .body(HasXPath.hasXPath("//*[local-name()='helloWorldResponse']/result/text()",
                        containsString("Anonymous")));

        stopFeature(false, "platform-http-proxy");
    }

    /* These STS tests are here to prove out functionality that doesn't get hit when accessing internal services. The standard UsernameToken and BinarySecurityToken elements are supported
     * by DDF, but not used internally. These elements need to be checked for functionality independently since going through our REST security framework won't touch these validators. */

    @Test
    public void testUsernameTokenSTS() throws Exception {
        String onBehalfOf = "<wst:OnBehalfOf>"
                + "                    <wsse:UsernameToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n"
                + "                        <wsse:Username>admin</wsse:Username>\n"
                + "                        <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">admin</wsse:Password>\n"
                + "                   </wsse:UsernameToken>\n"
                + "                </wst:OnBehalfOf>\n";
        String body = getSoapEnvelope(onBehalfOf);

        given().log().all().body(body).header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue")
                .expect().statusCode(equalTo(200)).when()
                .post(SERVICE_ROOT + "/SecurityTokenService").then().log().all().assertThat()
                .body(HasXPath.hasXPath("//*[local-name()='Assertion']"));
    }

    @Test
    public void testBadUsernameTokenSTS() throws Exception {
        String onBehalfOf = "<wst:OnBehalfOf>"
                + "                    <wsse:UsernameToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n"
                + "                        <wsse:Username>admin</wsse:Username>\n"
                + "                        <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">blah</wsse:Password>\n"
                + "                   </wsse:UsernameToken>\n"
                + "                </wst:OnBehalfOf>\n";
        String body = getSoapEnvelope(onBehalfOf);

        given().log().all().body(body).header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue")
                .expect().statusCode(equalTo(500)).when()
                .post(SERVICE_ROOT + "/SecurityTokenService").then().log().all();
    }

    @Test
    public void testBadX509TokenSTS() throws Exception {
        String onBehalfOf = "<wst:OnBehalfOf>\n"
                + "                    <wsse:BinarySecurityToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\" >\n"
                + "                        MIIDQDCCAqmgAwIBAgICAQUwDQYJKoZIhvcNAQEFBQAwTjELMAkGA1UEBhMCSlAxETAPBg\n"
                + "    NVBAgTCEthbmFnYXdhMQwwCgYDVQQKEwNJQk0xDDAKBgNVBAsTA1RSTDEQMA4GA1UEAxMH\n"
                + "    SW50IENBMjAeFw0wMTEwMDExMDAwMzlaFw0xMTEwMDExMDAwMzlaMFMxCzAJBgNVBAYTAk\n"
                + "    pQMREwDwYDVQQIEwhLYW5hZ2F3YTEMMAoGA1UEChMDSUJNMQwwCgYDVQQLEwNUUkwxFTAT\n"
                + "    BgNVBAMTDFNPQVBQcm92aWRlcjCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEAraakNJ\n"
                + "    1JzkPUuvPdXRvPOOCl12nBwmqvt65dk/x+QzxxarDNwH+eWRbLyyKcrAyd0XGV+Zbvj6V3\n"
                + "    O9DSVCZUCJttw6bbqqeYhwAP3V8s24sID77tk3gOhUTEGYxsljX2orL26SLqFJMrvnvk2F\n"
                + "    RS2mrdkZEBUG97mD4QWcln4d0CAwEAAaOCASYwggEiMAkGA1UdEwQCMAAwCwYDVR0PBAQD\n"
                + "    AgXgMCwGCWCGSAGG+EIBDQQfFh1PcGVuU1NMIEdlbmVyYXRlZCBDZXJ0aWZpY2F0ZTAdBg\n"
                + "    NVHQ4EFgQUlXSsrVRfZOLGdJdjEIwTbuSTe4UwgboGA1UdIwSBsjCBr4AUvfkg1Tj5ZHLT\n"
                + "    29p/3M6w/tC872+hgZKkgY8wgYwxCzAJBgNVBAYTAkpQMREwDwYDVQQIEwhLYW5hZ2F3YT\n"
                + "    EPMA0GA1UEBxMGWWFtYXRvMQwwCgYDVQQKEwNJQk0xDDAKBgNVBAsTA1RSTDEZMBcGA1UE\n"
                + "    AxMQU09BUCAyLjEgVGVzdCBDQTEiMCAGCSqGSIb3DQEJARYTbWFydXlhbWFAanAuaWJtLm\n"
                + "    NvbYICAQEwDQYJKoZIhvcNAQEFBQADgYEAXE7mE1RPb3lYAYJFzBb3VAHvkCWa/HQtCOZd\n"
                + "    yniCHp3MJ9EbNTq+QpOHV60YE8u0+5SejCzFSOHOpyBgLPjWoz8JXQnjV7VcAbTglw+ZoO\n"
                + "    SYy64rfhRdr9giSs47F4D6woPsAd2ubg/YhMaXLTSyGxPdV3VqQsutuSgDUDoqWCA="
                + "                   </wsse:BinarySecurityToken>\n"
                + "                </wst:OnBehalfOf>\n";
        String body = getSoapEnvelope(onBehalfOf);

        given().log().all().body(body).header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue")
                .expect().statusCode(equalTo(500)).when()
                .post(SERVICE_ROOT + "/SecurityTokenService").then().log().all();

    }

    @Test
    public void testX509TokenSTS() throws Exception {
        String onBehalfOf = "<wst:OnBehalfOf>\n"
                + "                    <wsse:BinarySecurityToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\" >"
                + "MIIDEzCCAnygAwIBAgIJAIzc4FYrIp9mMA0GCSqGSIb3DQEBBQUAMHcxCzAJBgNV\n"
                + "BAYTAlVTMQswCQYDVQQIDAJBWjEMMAoGA1UECgwDRERGMQwwCgYDVQQLDANEZXYx\n"
                + "GTAXBgNVBAMMEERERiBEZW1vIFJvb3QgQ0ExJDAiBgkqhkiG9w0BCQEWFWRkZnJv\n"
                + "b3RjYUBleGFtcGxlLm9yZzAeFw0xNDEyMTAyMTU4MThaFw0xNTEyMTAyMTU4MTha\n"
                + "MIGDMQswCQYDVQQGEwJVUzELMAkGA1UECAwCQVoxETAPBgNVBAcMCEdvb2R5ZWFy\n"
                + "MQwwCgYDVQQKDANEREYxDDAKBgNVBAsMA0RldjESMBAGA1UEAwwJbG9jYWxob3N0\n"
                + "MSQwIgYJKoZIhvcNAQkBFhVsb2NhbGhvc3RAZXhhbXBsZS5vcmcwgZ8wDQYJKoZI\n"
                + "hvcNAQEBBQADgY0AMIGJAoGBAMeCyNZbCTZphHQfB5g8FrgBq1RYzV7ikVw/pVGk\n"
                + "z8gx3l3A99s8WtA4mRAeb6n0vTR9yNBOekW4nYOiEOq//YTi/frI1kz0QbEH1s2c\n"
                + "I5nFButabD3PYGxUSuapbc+AS7+Pklr0TDI4MRzPPkkTp4wlORQ/a6CfVsNr/mVg\n"
                + "L2CfAgMBAAGjgZkwgZYwCQYDVR0TBAIwADAnBglghkgBhvhCAQ0EGhYYRk9SIFRF\n"
                + "U1RJTkcgUFVSUE9TRSBPTkxZMB0GA1UdDgQWBBSA95QIMyBAHRsd0R4s7C3BreFr\n"
                + "sDAfBgNVHSMEGDAWgBThVMeX3wrCv6lfeF47CyvkSBe9xjAgBgNVHREEGTAXgRVs\n"
                + "b2NhbGhvc3RAZXhhbXBsZS5vcmcwDQYJKoZIhvcNAQEFBQADgYEAtRUp7fAxU/E6\n"
                + "JD2Kj/+CTWqu8Elx13S0TxoIqv3gMoBW0ehyzEKjJi0bb1gUxO7n1SmOESp5sE3j\n"
                + "GTnh0GtYV0D219z/09n90cd/imAEhknJlayyd0SjpnaL9JUd8uYxJexy8TJ2sMhs\n"
                + "GAZ6EMTZCfT9m07XduxjsmDz0hlSGV0=" + "</wsse:BinarySecurityToken>\n"
                + "                </wst:OnBehalfOf>\n";
        String body = getSoapEnvelope(onBehalfOf);

        given().log().all().body(body).header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue")
                .expect().statusCode(equalTo(200)).when()
                .post(SERVICE_ROOT + "/SecurityTokenService").then().log().all().assertThat()
                .body(HasXPath.hasXPath("//*[local-name()='Assertion']"));

    }

    @Test
    public void testX509PathSTS() throws Exception {
        String onBehalfOf = "<wst:OnBehalfOf>\n"
                + "                    <wsse:BinarySecurityToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509PKIPathv1\" >"
                + "MIIF1zCCArwwggIloAMCAQICCQCM3OBWKyKfZTANBgkqhkiG9w0BAQUFADB3MQsw"
                + "CQYDVQQGEwJVUzELMAkGA1UECAwCQVoxDDAKBgNVBAoMA0RERjEMMAoGA1UECwwD"
                + "RGV2MRkwFwYDVQQDDBBEREYgRGVtbyBSb290IENBMSQwIgYJKoZIhvcNAQkBFhVk"
                + "ZGZyb290Y2FAZXhhbXBsZS5vcmcwHhcNMTQxMjEwMjE1NjMwWhcNMTcxMjA5MjE1"
                + "NjMwWjB3MQswCQYDVQQGEwJVUzELMAkGA1UECAwCQVoxDDAKBgNVBAoMA0RERjEM"
                + "MAoGA1UECwwDRGV2MRkwFwYDVQQDDBBEREYgRGVtbyBSb290IENBMSQwIgYJKoZI"
                + "hvcNAQkBFhVkZGZyb290Y2FAZXhhbXBsZS5vcmcwgZ8wDQYJKoZIhvcNAQEBBQAD"
                + "gY0AMIGJAoGBALVtFJIVYgb+07/jBZ1KXZVCxuf0hUoOMOw2vYJ8VqhS755Sf74q"
                + "RcVaPm8BcrWVG80OdutXtzP+ylnO/tjmr+myxsKnpodXZcLqCzQE58rh57bFJRAJ"
                + "SjqJjny+JBSy0MdI3NtJS3yVmrUgZRVHdIquYBPMjxIxgRsT230F1MnfAgMBAAGj"
                + "UDBOMB0GA1UdDgQWBBThVMeX3wrCv6lfeF47CyvkSBe9xjAfBgNVHSMEGDAWgBTh"
                + "VMeX3wrCv6lfeF47CyvkSBe9xjAMBgNVHRMEBTADAQH/MA0GCSqGSIb3DQEBBQUA"
                + "A4GBAJ8QlzOYMjNiqA6YAIJ+LGbTSXbc1nnAgP6unnvatSmew8o/nGIzcrmRCeGg"
                + "d5Bsx/xHncFtSeLRidgp6AvXA96/0ik7W5PWxFzbwy7vWIrkHx/tnWka6b95/FlB"
                + "I+GxycJiGZZCwNWGFBWXWrn/aVCVicZQ7q+nPAuCkV+TKIalMIIDEzCCAnygAwIB"
                + "AgIJAIzc4FYrIp9mMA0GCSqGSIb3DQEBBQUAMHcxCzAJBgNVBAYTAlVTMQswCQYD"
                + "VQQIDAJBWjEMMAoGA1UECgwDRERGMQwwCgYDVQQLDANEZXYxGTAXBgNVBAMMEERE"
                + "RiBEZW1vIFJvb3QgQ0ExJDAiBgkqhkiG9w0BCQEWFWRkZnJvb3RjYUBleGFtcGxl"
                + "Lm9yZzAeFw0xNDEyMTAyMTU4MThaFw0xNTEyMTAyMTU4MThaMIGDMQswCQYDVQQG"
                + "EwJVUzELMAkGA1UECAwCQVoxETAPBgNVBAcMCEdvb2R5ZWFyMQwwCgYDVQQKDANE"
                + "REYxDDAKBgNVBAsMA0RldjESMBAGA1UEAwwJbG9jYWxob3N0MSQwIgYJKoZIhvcN"
                + "AQkBFhVsb2NhbGhvc3RAZXhhbXBsZS5vcmcwgZ8wDQYJKoZIhvcNAQEBBQADgY0A"
                + "MIGJAoGBAMeCyNZbCTZphHQfB5g8FrgBq1RYzV7ikVw/pVGkz8gx3l3A99s8WtA4"
                + "mRAeb6n0vTR9yNBOekW4nYOiEOq//YTi/frI1kz0QbEH1s2cI5nFButabD3PYGxU"
                + "Suapbc+AS7+Pklr0TDI4MRzPPkkTp4wlORQ/a6CfVsNr/mVgL2CfAgMBAAGjgZkw"
                + "gZYwCQYDVR0TBAIwADAnBglghkgBhvhCAQ0EGhYYRk9SIFRFU1RJTkcgUFVSUE9T"
                + "RSBPTkxZMB0GA1UdDgQWBBSA95QIMyBAHRsd0R4s7C3BreFrsDAfBgNVHSMEGDAW"
                + "gBThVMeX3wrCv6lfeF47CyvkSBe9xjAgBgNVHREEGTAXgRVsb2NhbGhvc3RAZXhh"
                + "bXBsZS5vcmcwDQYJKoZIhvcNAQEFBQADgYEAtRUp7fAxU/E6JD2Kj/+CTWqu8Elx"
                + "13S0TxoIqv3gMoBW0ehyzEKjJi0bb1gUxO7n1SmOESp5sE3jGTnh0GtYV0D219z/"
                + "09n90cd/imAEhknJlayyd0SjpnaL9JUd8uYxJexy8TJ2sMhsGAZ6EMTZCfT9m07X"
                + "duxjsmDz0hlSGV0=\n"
                + "</wsse:BinarySecurityToken>\n" + "                </wst:OnBehalfOf>\n";
        String body = getSoapEnvelope(onBehalfOf);

        given().log().all().body(body).header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue")
                .expect().statusCode(equalTo(200)).when()
                .post(SERVICE_ROOT + "/SecurityTokenService").then().log().all().assertThat()
                .body(HasXPath.hasXPath("//*[local-name()='Assertion']"));

    }

    @Test
    public void testSamlAssertionInHeaders() throws Exception {
        String onBehalfOf = "<wst:OnBehalfOf>"
                + "                    <wsse:UsernameToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n"
                + "                        <wsse:Username>admin</wsse:Username>\n"
                + "                        <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">admin</wsse:Password>\n"
                + "                   </wsse:UsernameToken>\n"
                + "                </wst:OnBehalfOf>\n";
        String body = getSoapEnvelope(onBehalfOf);

        String assertionHeader = given().log().all().body(body)
                .header("Content-Type", "text/xml; charset=utf-8")
                .header("SOAPAction", "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue")
                .expect().statusCode(equalTo(200)).when()
                .post(SERVICE_ROOT + "/SecurityTokenService").then().extract().response()
                .asString();
        assertionHeader = assertionHeader.substring(assertionHeader.indexOf("<saml2:Assertion"),
                assertionHeader.indexOf("</saml2:Assertion>") + "</saml2:Assertion>".length());

        LOGGER.trace(assertionHeader);

        //try that admin level assertion token on a restricted resource
        given().header(SecurityConstants.SAML_HEADER_NAME,
                "SAML " + RestSecurity.deflateAndBase64Encode(assertionHeader)).when()
                .get("https://localhost:9993/admin/index.html").then().log().all().assertThat()
                .statusCode(equalTo(200));
    }

    private String getSoapEnvelope(String onBehalfOf) {
        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.500Z'");
        format.setCalendar(calendar);
        String created = format.format(new Date(calendar.getTimeInMillis()));
        long now = calendar.getTimeInMillis();
        now += 60000;
        String expires = format.format(new Date(now));
        String body = SOAP_ENV;
        body = body.replace("ON_BEHALF_OF", onBehalfOf);
        body = body.replace("CREATED", created);
        body = body.replace("EXPIRES", expires);
        return body;
    }

    String getKeystoreFilename() {
        return System.getProperty("javax.net.ssl.keyStore");
    }

    String getBackupFilename() {
        return getKeystoreFilename() + ".backup";
    }

    void getBackupKeystoreFile() throws IOException {
        Files.copy(Paths.get(getKeystoreFilename()), Paths.get(getBackupFilename()),
                REPLACE_EXISTING);
    }

    void restoreKeystoreFile() throws IOException {
        Files.copy(Paths.get(getBackupFilename()), Paths.get(getKeystoreFilename()),
                REPLACE_EXISTING);
    }

    //Purpose is to make sure operations of the security certificate generator are accessible
    //at runtime. The actual functionality of these operations is proved in unit tests.
    @Test
    public void testCertificateGeneratorService() throws Exception {
        String commonName = "pangalactic";
        String expectedValue = "CN=" + commonName;
        String featureName = "security-certificate-generator";
        String certGenPath = SECURE_ROOT + HTTPS_PORT
                + "/jolokia/exec/org.codice.ddf.security.certificate.generator.CertificateGenerator:service=certgenerator";
        getBackupKeystoreFile();
        try {
            getServiceManager().startFeature(true, featureName);

            //Test first operation
            Response response = given().auth().basic("admin", "admin").when()
                    .get(certGenPath + "/configureDemoCert/" + commonName);
            String actualValue = JsonPath.from(response.getBody().asString()).getString("value");
            assertThat(actualValue, equalTo(expectedValue));

            //Test second operation
            response = given().auth().basic("admin", "admin").when()
                    .get(certGenPath + "/configureDemoCertWithDefaultHostname");

            String jsonString = response.getBody().asString();
            JsonPath jsonPath = JsonPath.from(jsonString);
            //If the key value exists, the return value is well-formatted (i.e. not a stacktrace)
            assertThat(jsonPath.getString("value"), notNullValue());

            //Make sure an invalid key would return null
            assertThat(jsonPath.getString("someinvalidkey"), nullValue());

            getServiceManager().stopFeature(false, featureName);
        } finally {
            restoreKeystoreFile();
        }
    }
}