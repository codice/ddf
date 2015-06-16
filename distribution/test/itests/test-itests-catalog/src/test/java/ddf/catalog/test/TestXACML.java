/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 **/
package ddf.catalog.test;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.editConfigurationFileExtend;
import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.when;

import java.io.IOException;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.Hashtable;

import org.apache.commons.lang.StringUtils;
import org.hamcrest.xml.HasXPath;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerClass;
import org.osgi.service.cm.Configuration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerClass.class)
public class TestXACML extends AbstractIntegrationTest {

    private static boolean ranBefore = false;

    protected static boolean setupFailed = false;

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

    @Override
    protected Option[] configureStartScript() {
        return options(
                editConfigurationFileExtend("etc/org.apache.karaf.features.cfg", "featuresBoot",
                        "security-services-app,catalog-app,solr-app,spatial-app,sdk-app,sdk-xacml"),
                editConfigurationFileExtend("etc/org.apache.karaf.features.cfg",
                        "featuresRepositories",
                        "mvn:ddf.sdk/sdk-app/2.3.0.ALPHA1-SNAPSHOT/xml/features"));
    }

    @Override
    protected void setLogLevels() throws IOException {

        logLevel = System.getProperty(TEST_LOGLEVEL_PROPERTY);

        Configuration logConfig = configAdmin.getConfiguration(LOG_CONFIG_PID, null);
        Dictionary<String, Object> properties = logConfig.getProperties();
        if (StringUtils.isEmpty(logLevel)) {
            properties.put(LOGGER_PREFIX + "ddf", DEFAULT_LOG_LEVEL);
            properties.put(LOGGER_PREFIX + "org.codice", DEFAULT_LOG_LEVEL);
            properties.put(LOGGER_PREFIX + "org.wso2.balana", DEFAULT_LOG_LEVEL);
        } else {
            properties.put(LOGGER_PREFIX + "*", logLevel);
        }

        logConfig.update(properties);
    }

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
                stopFeature(true, "security-pdp-java");
                startFeature(true, "security-pdp-xacml");
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
        policyProperties.put("authenticationTypes",
                "/=SAML|basic,/admin=SAML|basic,/jolokia=SAML|basic,/system=SAML|basic,/solr=SAML|PKI|basic");
        policyProperties.put("whiteListContexts",
                "/services/SecurityTokenService,/services/internal,/proxy," + SERVICE_ROOT
                        + "/sdk/SoapService");
        Configuration config = configAdmin.getConfiguration(PolicyProperties.FACTORY_PID, null);
        Dictionary<String, ?> configProps = new Hashtable<>(policyProperties);
        config.update(configProps);
        waitForAllBundles();
    }

    @Test
    public void testAnonymousSoapAccess() throws Exception {
        configureRestForAnonymous();

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
        configureRestForAnonymous();

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

    public class PolicyProperties extends HashMap<String, Object> {

        public static final String SYMBOLIC_NAME = "security-policy-context";

        public static final String FACTORY_PID = "org.codice.ddf.security.policy.context.impl.PolicyManager";

        public PolicyProperties() {
            this.putAll(getMetatypeDefaults(SYMBOLIC_NAME, FACTORY_PID));
        }

    }
}
