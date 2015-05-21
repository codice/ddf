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
package ddf.catalog.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.TimeZone;

import org.hamcrest.xml.HasXPath;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.Configuration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestSecurity extends AbstractIntegrationTest {

    private static boolean ranBefore = false;

    protected static boolean setupFailed = false;

    protected static final String SOAP_ENV = "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
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
            + "            </wsu:Timestamp>\n"
            + "        </wsse:Security>\n"
            + "    </soap:Header>\n"
            + "    <soap:Body>\n"
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
            + "                </t:Claims>\n"
            + "            </wst:SecondaryParameters>\n"
            + "                ON_BEHALF_OF"
            + "            <wst:RequestType>http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue</wst:RequestType>\n"
            + "            <wsp:AppliesTo xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n"
            + "                <wsa:EndpointReference xmlns:wsa=\"http://www.w3.org/2005/08/addressing\">\n"
            + "                    <wsa:Address>https://localhost:8993/services/QueryService</wsa:Address>\n"
            + "                </wsa:EndpointReference>\n"
            + "            </wsp:AppliesTo>\n"
            + "            <wst:Renewing/>\n"
            + "        </wst:RequestSecurityToken>\n"
            + "    </soap:Body>\n"
            + "</soap:Envelope>";

    @Before
    public void beforeTest() {
        if (setupFailed) {
            fail("Setup failed");
        }

        LOGGER.info("Before {}", testName.getMethodName());
        if (!ranBefore) {
            try {
                setLogLevels();
                waitForAllBundles();
                waitForHttpEndpoint(SERVICE_ROOT + "/catalog/query");
                ranBefore = true;
            } catch (Exception e) {
                LOGGER.error("Failed to setup test", e);
                setupFailed = true;
                fail("Failed to setup security tests: " + e.getMessage());
            }
        }
        LOGGER.info("Starting {}", testName.getMethodName());
    }

    @Test
    public void testAnonymousRestAccess() throws Exception {
        String url = SERVICE_ROOT + "/catalog/query?q=*";

        configureRestForAnonymous();

        //test that anonymous works and check that we get an sso token
        String cookie = when().get(url).then().log().all().assertThat().statusCode(
                equalTo(200)).assertThat().header("Set-Cookie", containsString("JSESSIONID")).extract().cookie("JSESSIONID");

        //try again with the sso token
        given().cookie("JSESSIONID", cookie).when().get(url).then().log().all().assertThat().statusCode(
                equalTo(200));

        //try to hit an admin restricted page and see that we are unauthorized
        given().cookie("JSESSIONID", cookie).when().get("https://localhost:9993/admin/index.html").then().log().all().assertThat().statusCode(
                equalTo(403));
    }

    @Test
    public void testBasicRestAccess() throws Exception {
        String url = SERVICE_ROOT + "/catalog/query?q=*";

        configureRestForBasic();

        //test that we get a 401 if no credentials are specified
        when().get(url).then().log().all().assertThat().statusCode(equalTo(401));

        //try a random user and get a 401
        given().auth().basic("bad", "user").when().get(url).then().log().all().assertThat().statusCode(
                equalTo(401));

        //try a real user and get an sso token back
        String cookie = given().auth().basic("admin", "admin").when().get(url).then().log().all()
                .assertThat().statusCode(equalTo(200)).assertThat().header("Set-Cookie", containsString("JSESSIONID")).extract().cookie("JSESSIONID");

        //try the sso token instead of basic auth
        given().cookie("JSESSIONID", cookie).when().get(url).then().log().all().assertThat().statusCode(equalTo(200));

        //try that admin level sso token on a restricted resource and get in... sso works!
        given().cookie("JSESSIONID", cookie).when().get("https://localhost:9993/admin/index.html").then().log().all().assertThat().statusCode(equalTo(200));
    }

    private void configureRestForAnonymous() throws IOException, InterruptedException {
        PolicyProperties policyProperties = new PolicyProperties();
        policyProperties.put("authenticationTypes",
                "/=SAML|ANON,/admin=SAML|basic,/jolokia=SAML|basic,/system=SAML|basic,/solr=SAML|PKI|basic");
        policyProperties.put("whiteListContexts",
                "/services/SecurityTokenService,/services/internal,/proxy," + SERVICE_ROOT
                        + "/sdk/SoapService");
        Configuration config = configAdmin.getConfiguration(PolicyProperties.FACTORY_PID, null);
        Dictionary<String, ?> configProps = new Hashtable<>(policyProperties);
        config.update(configProps);
        waitForAllBundles();
    }

    private void configureRestForBasic() throws IOException, InterruptedException {
        PolicyProperties policyProperties = new PolicyProperties();
        policyProperties.put("authenticationTypes", "/=SAML|basic,/admin=SAML|basic,/jolokia=SAML|basic,/system=SAML|basic,/solr=SAML|PKI|basic");
        policyProperties.put("whiteListContexts", "/services/SecurityTokenService,/services/internal,/proxy," + SERVICE_ROOT + "/sdk/SoapService");
        Configuration config = configAdmin.getConfiguration(PolicyProperties.FACTORY_PID, null);
        Dictionary<String, ?> configProps = new Hashtable<>(policyProperties);
        config.update(configProps);
        waitForAllBundles();
    }

    @Test
    public void testAnonymousSoapAccess() throws Exception {
        configureRestForAnonymous();

        String body = "<soapenv:Envelope xmlns:hel=\"http://ddf.sdk/soap/hello\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "   <soapenv:Header>\n"
                + "   </soapenv:Header>\n"
                + "   <soapenv:Body>\n"
                + "      <hel:helloWorld/>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        //we are only testing anonymous because that hits the most code, testing with an assertion would be mostly testing the same stuff that this is hitting
        given().log().all().body(body).header("Content-Type", "text/xml; charset=utf-8").header("SOAPAction", "helloWorld").expect().statusCode(
                equalTo(200)).when().post(SERVICE_ROOT + "/sdk/SoapService").then().log().all().assertThat()
                        .body(HasXPath
                                .hasXPath("//*[local-name()='helloWorldResponse']/result/text()",
                                        containsString("Anonymous")));
    }

    @Test
    public void testAnonymousSoapAccessHttp() throws Exception {
        configureRestForAnonymous();

        startFeature(true, "platform-http-proxy");

        String body = "<soapenv:Envelope xmlns:hel=\"http://ddf.sdk/soap/hello\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "   <soapenv:Header>\n"
                + "   </soapenv:Header>\n"
                + "   <soapenv:Body>\n"
                + "      <hel:helloWorld/>\n"
                + "   </soapenv:Body>\n"
                + "</soapenv:Envelope>";
        //we are only testing anonymous because that hits the most code, testing with an assertion would be mostly testing the same stuff that this is hitting
        given().log().all().body(body).header("Content-Type", "text/xml; charset=utf-8").header("SOAPAction", "helloWorld").expect().statusCode(
                equalTo(200)).when().post(INSECURE_SERVICE_ROOT + "/sdk/SoapService").then().log().all().assertThat()
                .body(HasXPath.hasXPath("//*[local-name()='helloWorldResponse']/result/text()", containsString("Anonymous")));

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

        given().log().all().body(body).header("Content-Type", "text/xml; charset=utf-8").header("SOAPAction",
                "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue").expect().statusCode(equalTo(200)).when().post(
                SERVICE_ROOT + "/SecurityTokenService").then().log().all().assertThat()
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

        given().log().all().body(body).header("Content-Type", "text/xml; charset=utf-8").header("SOAPAction",
                "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue").expect().statusCode(equalTo(500)).when().post(
                SERVICE_ROOT + "/SecurityTokenService").then().log().all();
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

        given().log().all().body(body).header("Content-Type", "text/xml; charset=utf-8").header("SOAPAction",
                "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue").expect().statusCode(equalTo(500)).when().post(
                SERVICE_ROOT + "/SecurityTokenService").then().log().all();

    }

    @Test
    public void testX509TokenSTS() throws Exception {
        String onBehalfOf = "<wst:OnBehalfOf>\n"
                + "                    <wsse:BinarySecurityToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\" >\n"
                + "                        MIIDEzCCAnygAwIBAgIJAIzc4FYrIp9mMA0GCSqGSIb3DQEBBQUAMHcxCzAJBgNV"
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
                + "GTnh0GtYV0D219z/09n90cd/imAEhknJlayyd0SjpnaL9JUd8uYxJexy8TJ2sMhs\n" + "GAZ6EMTZCfT9m07XduxjsmDz0hlSGV0="
                + "                   </wsse:BinarySecurityToken>\n"
                + "                </wst:OnBehalfOf>\n";
        String body = getSoapEnvelope(onBehalfOf);

        given().log().all().body(body).header("Content-Type", "text/xml; charset=utf-8").header("SOAPAction",
                "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue").expect().statusCode(equalTo(200))
                .when().post(SERVICE_ROOT + "/SecurityTokenService").then().log().all().assertThat().body(
                HasXPath.hasXPath("//*[local-name()='Assertion']"));

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

    public class PolicyProperties extends HashMap<String, Object> {

        public static final String SYMBOLIC_NAME = "security-policy-context";

        public static final String FACTORY_PID = "org.codice.ddf.security.policy.context.impl.PolicyManager";

        public PolicyProperties() {
            this.putAll(getMetatypeDefaults(SYMBOLIC_NAME, FACTORY_PID));
        }

    }
}
