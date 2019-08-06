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
import static com.jayway.restassured.RestAssured.when;
import static com.jayway.restassured.authentication.CertificateAuthSettings.certAuthSettings;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.TRANSFORMER_XML;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.delete;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.ingest;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.query;
import static org.codice.ddf.itests.common.catalog.CatalogTestCommons.queryWithBasicAuth;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureAdminConfigPolicy;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureAuthZRealm;
import static org.codice.ddf.itests.common.config.ConfigureTestCommons.configureMetacardAttributeSecurityFiltering;
import static org.codice.ddf.itests.common.csw.CswTestCommons.CSW_FEDERATED_SOURCE_FACTORY_PID;
import static org.codice.ddf.itests.common.csw.CswTestCommons.getCswSourceProperties;
import static org.codice.ddf.itests.common.opensearch.OpenSearchTestCommons.OPENSEARCH_FACTORY_PID;
import static org.codice.ddf.itests.common.opensearch.OpenSearchTestCommons.getOpenSearchSourceProperties;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyCollectionOf;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasXPath;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.jayway.restassured.path.json.JsonPath;
import ddf.catalog.data.Metacard;
import ddf.security.SecurityConstants;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.Dictionary;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLHandshakeException;
import javax.ws.rs.core.MediaType;
import org.apache.http.HttpStatus;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.codice.ddf.itests.common.AbstractIntegrationTest;
import org.codice.ddf.itests.common.catalog.CatalogTestCommons;
import org.codice.ddf.itests.common.opensearch.OpenSearchFeature;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.codice.ddf.test.common.LoggingUtils;
import org.codice.ddf.test.common.annotations.BeforeExam;
import org.hamcrest.xml.HasXPath;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.junit.PaxExam;
import org.ops4j.pax.exam.spi.reactors.ExamReactorStrategy;
import org.ops4j.pax.exam.spi.reactors.PerSuite;
import org.osgi.service.cm.Configuration;

@RunWith(PaxExam.class)
@ExamReactorStrategy(PerSuite.class)
public class TestSecurity extends AbstractIntegrationTest {

  public static final String SAMPLE_SOAP =
      "<?xml version=\"1.0\"?><soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><helloWorld xmlns=\"http://ddf.sdk/soap/hello\" /></soap:Body></soap:Envelope>";

  protected static final String TRUST_STORE_PATH = System.getProperty("javax.net.ssl.trustStore");

  protected static final String KEY_STORE_PATH = System.getProperty("javax.net.ssl.keyStore");

  protected static final String PASSWORD = System.getProperty("javax.net.ssl.trustStorePassword");

  protected static final List<String> SERVICES_TO_FILTER =
      Arrays.asList(
          "org.codice.ddf.catalog.security.CatalogPolicy",
          "org.codice.ddf.security.policy.context.impl.PolicyManager",
          "ddf.security.pdp.realm.AuthzRealm",
          "testCreateFactoryPid",
          "org.codice.ddf.admin.config.policy.AdminConfigPolicy");

  protected static final List<String> FEATURES_TO_FILTER =
      Arrays.asList("catalog-security-plugin", "security-sts-propertyclaimshandler");

  protected static final String ADD_SDK_APP_JOLOKIA_REQ =
      "{\"type\":\"EXEC\",\"mbean\":\"org.codice.ddf.admin.application.service.ApplicationService:service=application-service\",\"operation\":\"addApplications\",\"arguments\":[[{\"value\":\"mvn:ddf.distribution/sdk-app/"
          + System.getProperty("ddf.version")
          + "/xml/features\"}]]}";

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

  private static final String SDK_SOAP_CONTEXT = "/services/sdk";

  /** ************** USERS *************** */
  private static final String USER_PASSWORD = "password1";

  private static final String A_USER = "slang";

  private static final String B_USER = "tchalla";

  private static final String ACCESS_GROUP_REPLACE_TOKEN = "ACCESS_GROUP_REPLACE_TOKEN";

  private static final String BAD_X509_TOKEN =
      "                        MIIDQDCCAqmgAwIBAgICAQUwDQYJKoZIhvcNAQEFBQAwTjELMAkGA1UEBhMCSlAxETAPBg\n"
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
          + "    SYy64rfhRdr9giSs47F4D6woPsAd2ubg/YhMaXLTSyGxPdV3VqQsutuSgDUDoqWCA=";

  private static final String GOOD_X509_TOKEN =
      "MIIC8DCCAlmgAwIBAgIJAIzc4FYrIp9pMA0GCSqGSIb3DQEBCwUAMIGEMQswCQYD\n"
          + "VQQGEwJVUzELMAkGA1UECBMCQVoxDDAKBgNVBAoTA0RERjEMMAoGA1UECxMDRGV2\n"
          + "MRkwFwYDVQQDExBEREYgRGVtbyBSb290IENBMTEwLwYJKoZIhvcNAQkBFiJlbWFp\n"
          + "bEFkZHJlc3M9ZGRmcm9vdGNhQGV4YW1wbGUub3JnMCAXDTE1MTIxMTE1NDMyM1oY\n"
          + "DzIxMTUxMTE3MTU0MzIzWjBwMQswCQYDVQQGEwJVUzELMAkGA1UECBMCQVoxDDAK\n"
          + "BgNVBAoTA0RERjEMMAoGA1UECxMDRGV2MRIwEAYDVQQDEwlsb2NhbGhvc3QxJDAi\n"
          + "BgkqhkiG9w0BCQEWFWxvY2FsaG9zdEBleGFtcGxlLm9yZzCBnzANBgkqhkiG9w0B\n"
          + "AQEFAAOBjQAwgYkCgYEAx4LI1lsJNmmEdB8HmDwWuAGrVFjNXuKRXD+lUaTPyDHe\n"
          + "XcD32zxa0DiZEB5vqfS9NH3I0E56Rbidg6IQ6r/9hOL9+sjWTPRBsQfWzZwjmcUG\n"
          + "61psPc9gbFRK5qltz4BLv4+SWvRMMjgxHM8+SROnjCU5FD9roJ9Ww2v+ZWAvYJ8C\n"
          + "AwEAAaN7MHkwCQYDVR0TBAIwADAsBglghkgBhvhCAQ0EHxYdT3BlblNTTCBHZW5l\n"
          + "cmF0ZWQgQ2VydGlmaWNhdGUwHQYDVR0OBBYEFID3lAgzIEAdGx3RHizsLcGt4Wuw\n"
          + "MB8GA1UdIwQYMBaAFOFUx5ffCsK/qV94XjsLK+RIF73GMA0GCSqGSIb3DQEBCwUA\n"
          + "A4GBACWWsi4WusO5/u1O91obGn8ctFnxVlogBQ/tDZ+neQDxy8YB2J28tztELrRH\n"
          + "kaGiCPT4CCKdy0hx/bG/jSM1ypJnPKrPVrCkYL3Y68pzxvrFNq5NqAFCcBOCNsDN\n"
          + "fvCSZ/XHvFyGHIuso5wNVxJyvTdhQ+vWbnpiX8qr6vTx2Wgw";

  private static final String GOOD_X509_PATH_TOKEN =
      "MIIC9DCCAvAwggJZoAMCAQICCQCM3OBWKyKfaTANBgkqhkiG9w0BAQsFADCBhDELMAkGA1UEBhMCVVMxCzAJBgNVBAgTAkFaMQwwCgYDVQQKEwNEREYxDDAKBgNVBAsTA0RldjEZMBcGA1UEAxMQRERGIERlbW8gUm9vdCBDQTExMC8GCSqGSIb3DQEJARYiZW1haWxBZGRyZXNzPWRkZnJvb3RjYUBleGFtcGxlLm9yZzAgFw0xNTEyMTExNTQzMjNaGA8yMTE1MTExNzE1NDMyM1owcDELMAkGA1UEBhMCVVMxCzAJBgNVBAgTAkFaMQwwCgYDVQQKEwNEREYxDDAKBgNVBAsTA0RldjESMBAGA1UEAxMJbG9jYWxob3N0MSQwIgYJKoZIhvcNAQkBFhVsb2NhbGhvc3RAZXhhbXBsZS5vcmcwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAMeCyNZbCTZphHQfB5g8FrgBq1RYzV7ikVw/pVGkz8gx3l3A99s8WtA4mRAeb6n0vTR9yNBOekW4nYOiEOq//YTi/frI1kz0QbEH1s2cI5nFButabD3PYGxUSuapbc+AS7+Pklr0TDI4MRzPPkkTp4wlORQ/a6CfVsNr/mVgL2CfAgMBAAGjezB5MAkGA1UdEwQCMAAwLAYJYIZIAYb4QgENBB8WHU9wZW5TU0wgR2VuZXJhdGVkIENlcnRpZmljYXRlMB0GA1UdDgQWBBSA95QIMyBAHRsd0R4s7C3BreFrsDAfBgNVHSMEGDAWgBThVMeX3wrCv6lfeF47CyvkSBe9xjANBgkqhkiG9w0BAQsFAAOBgQAllrIuFrrDuf7tTvdaGxp/HLRZ8VZaIAUP7Q2fp3kA8cvGAdidvLc7RC60R5Ghogj0+AginctIcf2xv40jNcqSZzyqz1awpGC92OvKc8b6xTauTagBQnATgjbAzX7wkmf1x7xchhyLrKOcDVcScr03YUPr1m56Yl/Kq+r08dloMA==";

  private static final String OPENSEARCH_SAML_SOURCE_ID = "openSearchSamlSource";

  private static final DynamicUrl SECURE_ROOT_AND_PORT =
      new DynamicUrl(DynamicUrl.SECURE_ROOT, HTTPS_PORT);

  private static final DynamicUrl ADMIN_PATH =
      new DynamicUrl(SECURE_ROOT_AND_PORT, "/admin/index.html");

  // this uses a cert that won't be sent by the TLS connection
  private static final String BAD_HOK_EXAMPLE =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
          + "   <soap:Header>\n"
          + "        <Action xmlns=\"http://www.w3.org/2005/08/addressing\">http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue</Action>\n"
          + "        <MessageID xmlns=\"http://www.w3.org/2005/08/addressing\">urn:uuid:c0c43e1e-0264-4018-9a58-d1fda4332ab3</MessageID>\n"
          + "        <To xmlns=\"http://www.w3.org/2005/08/addressing\">https://localhost:8993/services/SecurityTokenService</To>\n"
          + "        <ReplyTo xmlns=\"http://www.w3.org/2005/08/addressing\">\n"
          + "            <Address>http://www.w3.org/2005/08/addressing/anonymous</Address>\n"
          + "        </ReplyTo>\n"
          + "      <wsse:Security soap:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n"
          + "         <wsu:Timestamp wsu:Id=\"TS-EF182B133DACAE158E14503737766347\">\n"
          + "            <wsu:Created>CREATED</wsu:Created>\n"
          + "            <wsu:Expires>EXPIRES</wsu:Expires>\n"
          + "         </wsu:Timestamp>\n"
          + "      </wsse:Security>\n"
          + "   </soap:Header>\n"
          + "   <soap:Body>\n"
          + "      <wst:RequestSecurityToken xmlns:wst=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">\n"
          + "         <wst:RequestType>http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue</wst:RequestType>\n"
          + "         <wsp:AppliesTo xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n"
          + "            <wsa:EndpointReference xmlns:wsa=\"http://www.w3.org/2005/08/addressing\">\n"
          + "               <wsa:Address>https://localhost:8993/services/SecurityTokenService</wsa:Address>\n"
          + "            </wsa:EndpointReference>\n"
          + "         </wsp:AppliesTo>\n"
          + "         <wst:Claims Dialect=\"http://schemas.xmlsoap.org/ws/2005/05/identity\" xmlns:ic=\"http://schemas.xmlsoap.org/ws/2005/05/identity\">\n"
          + "            <ic:ClaimType Optional=\"true\" Uri=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier\"/>\n"
          + "            <ic:ClaimType Optional=\"true\" Uri=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress\"/>\n"
          + "            <ic:ClaimType Optional=\"true\" Uri=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname\"/>\n"
          + "            <ic:ClaimType Optional=\"true\" Uri=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname\"/>\n"
          + "            <ic:ClaimType Optional=\"true\" Uri=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role\"/>\n"
          + "         </wst:Claims>\n"
          + "         <wst:OnBehalfOf>\n"
          + "            <wsse:UsernameToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n"
          + "               <wsse:Username>admin</wsse:Username>\n"
          + "               <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">admin</wsse:Password>\n"
          + "            </wsse:UsernameToken>\n"
          + "         </wst:OnBehalfOf>\n"
          + "         <wst:TokenType>http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0</wst:TokenType>\n"
          + "         <wst:KeyType>http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey</wst:KeyType>\n"
          + "         <wst:UseKey>\n"
          + "            <ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n"
          + "               <ds:X509Data>\n"
          + "                  <ds:X509Certificate>MIIDcDCCAtmgAwIBAgIJAIzc4FYrIp9qMA0GCSqGSIb3DQEBCwUAMIGEMQswCQYD\n"
          + "VQQGEwJVUzELMAkGA1UECBMCQVoxDDAKBgNVBAoTA0RERjEMMAoGA1UECxMDRGV2\n"
          + "MRkwFwYDVQQDExBEREYgRGVtbyBSb290IENBMTEwLwYJKoZIhvcNAQkBFiJlbWFp\n"
          + "bEFkZHJlc3M9ZGRmcm9vdGNhQGV4YW1wbGUub3JnMCAXDTE1MTIxNzE3MzUzMloY\n"
          + "DzIxMTUxMTIzMTczNTMyWjBsMQswCQYDVQQGEwJVUzELMAkGA1UECBMCQVoxDDAK\n"
          + "BgNVBAoTA0RERjEMMAoGA1UECxMDRGV2MRAwDgYDVQQDEwdleGFtcGxlMSIwIAYJ\n"
          + "KoZIhvcNAQkBFhNleGFtcGxlQGV4YW1wbGUub3JnMIIBIjANBgkqhkiG9w0BAQEF\n"
          + "AAOCAQ8AMIIBCgKCAQEAoMoUxCQOxA8INQ1NQQcd4k/pwraU+x58ymGJPWeT+SCA\n"
          + "OiD4xJs3qzqC4Ex9tztxUhGyAH56YYaZCtVrJxejYUPbXYRBLuU2ecw3adWJyk2f\n"
          + "fL+hyc4eDa640KQ8+W0dz2hI1OPSsI1KzRdaYbe8f1GcWL8TshOZ+o0fC036GOsi\n"
          + "szCnqXaQZbObddEMGHWMEPJzToIEUrt/+t3eAeNNF9A/jjhELJrzgaWqJNuEcC3q\n"
          + "gfgdeF/itjurRjIkmBDs4VkplUX+JWFPF78pyYcbLEle1dV1ZxZIZv7vFlZYjZn2\n"
          + "Qacf+iLQnk3m+tGCtA2Q8DKWCFl/fGtJPoIyHQsmswIDAQABo3sweTAJBgNVHRME\n"
          + "AjAAMCwGCWCGSAGG+EIBDQQfFh1PcGVuU1NMIEdlbmVyYXRlZCBDZXJ0aWZpY2F0\n"
          + "ZTAdBgNVHQ4EFgQUBicdpPA//+rQjR/DJwD/beoIwREwHwYDVR0jBBgwFoAU4VTH\n"
          + "l98Kwr+pX3heOwsr5EgXvcYwDQYJKoZIhvcNAQELBQADgYEALUz4LJAtaGfRpEuC\n"
          + "VtjdpQT1E2gL0PXyBgR5jchBVzvHckectvaUh+rHbwATh1jahbk/0/0J53NMEi49\n"
          + "TOuYQtmHtiMvl1oBqAke1mJgDPgoGE9T3wWM4FcnA8z7LpBJeo661mchRge+vyW/\n"
          + "kVCd/oPtz1DRhKttYBa6LB7gswk=</ds:X509Certificate>\n"
          + "               </ds:X509Data>\n"
          + "            </ds:KeyInfo>\n"
          + "         </wst:UseKey>\n"
          + "         <wst:Renewing/>\n"
          + "      </wst:RequestSecurityToken>\n"
          + "   </soap:Body>\n"
          + "</soap:Envelope>";

  // this uses the default localhost cert which will be in the TLS connection
  private static final String GOOD_HOK_EXAMPLE =
      "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
          + "   <soap:Header>\n"
          + "        <Action xmlns=\"http://www.w3.org/2005/08/addressing\">http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue</Action>\n"
          + "        <MessageID xmlns=\"http://www.w3.org/2005/08/addressing\">urn:uuid:c0c43e1e-0264-4018-9a58-d1fda4332ab3</MessageID>\n"
          + "        <To xmlns=\"http://www.w3.org/2005/08/addressing\">https://localhost:8993/services/SecurityTokenService</To>\n"
          + "        <ReplyTo xmlns=\"http://www.w3.org/2005/08/addressing\">\n"
          + "            <Address>http://www.w3.org/2005/08/addressing/anonymous</Address>\n"
          + "        </ReplyTo>\n"
          + "      <wsse:Security soap:mustUnderstand=\"1\" xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" xmlns:wsu=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-utility-1.0.xsd\">\n"
          + "         <wsu:Timestamp wsu:Id=\"TS-EF182B133DACAE158E14503728805225\">\n"
          + "            <wsu:Created>CREATED</wsu:Created>\n"
          + "            <wsu:Expires>EXPIRES</wsu:Expires>\n"
          + "         </wsu:Timestamp>\n"
          + "      </wsse:Security>\n"
          + "   </soap:Header>\n"
          + "   <soap:Body>\n"
          + "      <wst:RequestSecurityToken xmlns:wst=\"http://docs.oasis-open.org/ws-sx/ws-trust/200512\">\n"
          + "         <wst:RequestType>http://docs.oasis-open.org/ws-sx/ws-trust/200512/Issue</wst:RequestType>\n"
          + "         <wsp:AppliesTo xmlns:wsp=\"http://schemas.xmlsoap.org/ws/2004/09/policy\">\n"
          + "            <wsa:EndpointReference xmlns:wsa=\"http://www.w3.org/2005/08/addressing\">\n"
          + "               <wsa:Address>https://localhost:8993/services/SecurityTokenService</wsa:Address>\n"
          + "            </wsa:EndpointReference>\n"
          + "         </wsp:AppliesTo>\n"
          + "         <wst:Claims Dialect=\"http://schemas.xmlsoap.org/ws/2005/05/identity\" xmlns:ic=\"http://schemas.xmlsoap.org/ws/2005/05/identity\">\n"
          + "            <ic:ClaimType Optional=\"true\" Uri=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/nameidentifier\"/>\n"
          + "            <ic:ClaimType Optional=\"true\" Uri=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/emailaddress\"/>\n"
          + "            <ic:ClaimType Optional=\"true\" Uri=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/surname\"/>\n"
          + "            <ic:ClaimType Optional=\"true\" Uri=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/givenname\"/>\n"
          + "            <ic:ClaimType Optional=\"true\" Uri=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role\"/>\n"
          + "         </wst:Claims>\n"
          + "         <wst:OnBehalfOf>\n"
          + "            <wsse:UsernameToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n"
          + "               <wsse:Username>admin</wsse:Username>\n"
          + "               <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">admin</wsse:Password>\n"
          + "            </wsse:UsernameToken>\n"
          + "         </wst:OnBehalfOf>\n"
          + "         <wst:TokenType>http://docs.oasis-open.org/wss/oasis-wss-saml-token-profile-1.1#SAMLV2.0</wst:TokenType>\n"
          + "         <wst:KeyType>http://docs.oasis-open.org/ws-sx/ws-trust/200512/PublicKey</wst:KeyType>\n"
          + "         <wst:UseKey>\n"
          + "            <ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n"
          + "               <ds:X509Data>\n"
          + "                  <ds:X509Certificate>"
          + "MIICsjCCAhugAwIBAgIGAWBQwru6MA0GCSqGSIb3DQEBCwUAMIGEMQswCQYDVQQG\n"
          + "EwJVUzELMAkGA1UECBMCQVoxDDAKBgNVBAoTA0RERjEMMAoGA1UECxMDRGV2MRkw\n"
          + "FwYDVQQDExBEREYgRGVtbyBSb290IENBMTEwLwYJKoZIhvcNAQkBFiJlbWFpbEFk\n"
          + "ZHJlc3M9ZGRmcm9vdGNhQGV4YW1wbGUub3JnMCAXDTE3MTIxMjE2NDM0N1oYDzIx\n"
          + "MTcxMjEyMTY0MzQ3WjAUMRIwEAYDVQQDDAlsb2NhbGhvc3QwggEiMA0GCSqGSIb3\n"
          + "DQEBAQUAA4IBDwAwggEKAoIBAQDEwOqpKuMr+GmiT3kKcMKp4361ByVBaxvj5M6r\n"
          + "ptn+Al8oOZfxfaDOhOikUVuaPTuIYvTWW04KaQNHI3nm86RwldbwMlPZka5jGeYk\n"
          + "OJ3qah1C6OMmMDEC7lM0/bhxPLc8C0cUwAG8FdlBLDKCYFjHj6UBq5xhD/1uJyvS\n"
          + "oWcFqJQbXP+igVV7tDdRxRDhp8f5eActecjQXK5+bpzNs8KC+3gvnhBj/ux/DYyr\n"
          + "E6iA4nKMnelIaZjhW11bsuQwM6XK8Tq7PQ2LuAuhFoKUq/p8p8Sva75/3nGeCHOl\n"
          + "cjj7rnoSCcUgX+u1nnCN/wsshyRAX30wS7C75nQilQkxCfAlAgMBAAGjGDAWMBQG\n"
          + "A1UdEQQNMAuCCWxvY2FsaG9zdDANBgkqhkiG9w0BAQsFAAOBgQAQBBked2MsqlmI\n"
          + "wxSUXLee6f9R14GPsNsOkLohFhBTKuxC++n+q0+LvMRLmFxPj4dois/dHKQiD7wY\n"
          + "WYY4WPvb3nD2aPev4NZm8erB9+XPVueRzxaLXQ+K1oW9Kjui8+2q1lTBBGOjma1D\n"
          + "T/b6FVhyJo5SjQ3JMKe9TACVp/YA7A=="
          + "                  </ds:X509Certificate>\n"
          + "               </ds:X509Data>\n"
          + "            </ds:KeyInfo>\n"
          + "         </wst:UseKey>\n"
          + "         <wst:Renewing/>\n"
          + "      </wst:RequestSecurityToken>\n"
          + "   </soap:Body>\n"
          + "</soap:Envelope>";

  @BeforeExam
  public void beforeTest() throws Exception {
    try {
      waitForSystemReady();

      List<String> featurePolicies = new ArrayList<>();
      featurePolicies.addAll(Arrays.asList(getDefaultRequiredApps()));
      featurePolicies.addAll(FEATURES_TO_FILTER);
      featurePolicies.replaceAll(
          featureName ->
              featureName
                  + "=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=admin\"");

      List<String> servicePolicies = new ArrayList<>(SERVICES_TO_FILTER);
      servicePolicies.replaceAll(
          serviceName ->
              serviceName
                  + "=\"http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=admin\"");

      String adminConfigPolicyPid = "org.codice.ddf.admin.config.policy.AdminConfigPolicy";
      Configuration config = getAdminConfig().getConfiguration(adminConfigPolicyPid);
      config.setBundleLocation(
          "mvn:ddf.admin.core/admin-core-configpolicy/" + System.getProperty("ddf.version"));

      configureAdminConfigPolicy(featurePolicies, servicePolicies, getAdminConfig());

    } catch (Exception e) {
      LoggingUtils.failWithThrowableStacktrace(e, "Failed in @BeforeExam: ");
    }
  }

  @After
  public void teardown() throws Exception {
    clearCatalog();
  }

  @Test
  public void testGuestRestAccess() throws Exception {
    String url = SERVICE_ROOT.getUrl() + "/catalog/query?q=*&src=local";

    configureRestForGuest(SDK_SOAP_CONTEXT);

    waitForSecurityHandlers(url);

    // test that guest works and check that we get an sso token
    String cookie =
        when()
            .get(url)
            .then()
            .log()
            .ifValidationFails()
            .assertThat()
            .statusCode(equalTo(200))
            .assertThat()
            .header("Set-Cookie", containsString("JSESSIONID"))
            .extract()
            .cookie("JSESSIONID");

    // try again with the sso token
    given()
        .cookie("JSESSIONID", cookie)
        .when()
        .get(url)
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .statusCode(equalTo(200));

    // try to hit an admin restricted page and see that we are unauthorized
    given()
        .cookie("JSESSIONID", cookie)
        .when()
        .get(ADMIN_PATH.getUrl())
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .statusCode(equalTo(403));
  }

  @Test
  public void testBasicRestAccess() throws Exception {
    String url = SERVICE_ROOT.getUrl() + "/catalog/query?q=*&src=local";

    configureRestForBasic(SDK_SOAP_CONTEXT);

    waitForSecurityHandlers(url);

    // test that we get a 401 if no credentials are specified
    getSecurityPolicy().waitForBasicAuthReady(url);
    when().get(url).then().log().all().assertThat().statusCode(equalTo(401));

    // try a random user and get a 401
    given()
        .auth()
        .basic("bad", "user")
        .when()
        .get(url)
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .statusCode(equalTo(401));

    // try a real user and get an sso token back
    String cookie =
        given()
            .auth()
            .basic("admin", "admin")
            .when()
            .get(url)
            .then()
            .log()
            .ifValidationFails()
            .assertThat()
            .statusCode(equalTo(200))
            .assertThat()
            .header("Set-Cookie", containsString("JSESSIONID"))
            .extract()
            .cookie("JSESSIONID");

    // try the sso token instead of basic auth
    given()
        .cookie("JSESSIONID", cookie)
        .when()
        .get(url)
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .statusCode(equalTo(200));

    // try that admin level sso token on a restricted resource and get in... sso works!
    given()
        .cookie("JSESSIONID", cookie)
        .when()
        .get(ADMIN_PATH.getUrl())
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .statusCode(equalTo(200));

    configureRestForGuest(SDK_SOAP_CONTEXT);
    getSecurityPolicy().waitForGuestAuthReady(url);
  }

  @Test
  public void testTLSv11IsAllowed() throws Exception {
    String url = SERVICE_ROOT.getUrl() + "/catalog/query?q=*&src=local";
    HttpClient client = createHttpClient("TLSv1.1", createBasicAuth("admin", "admin"));

    assertBasicAuth(client, url, 200);
  }

  @Test
  public void testTLSv12IsAllowed() throws Exception {
    String url = SERVICE_ROOT.getUrl() + "/catalog/query?q=*&src=local";
    HttpClient client = createHttpClient("TLSv1.2", createBasicAuth("admin", "admin"));

    assertBasicAuth(client, url, 200);
  }

  @Test(expected = SSLHandshakeException.class)
  public void testTLSv1IsDisabled() throws Exception {
    String url = SERVICE_ROOT.getUrl() + "/catalog/query?q=*&src=local";
    HttpClient client = createHttpClient("TLSv1");

    HttpGet get = new HttpGet(url);
    client.execute(get);
  }

  @Test
  public void testAllowedCipherSuites() throws Exception {
    String[] supportedCipherSuites = {
      "TLS_DHE_RSA_WITH_AES_128_GCM_SHA256",
      "TLS_DHE_RSA_WITH_AES_128_CBC_SHA256",
      "TLS_DHE_RSA_WITH_AES_128_CBC_SHA",
      "TLS_ECDHE_ECDSA_WITH_AES_128_GCM_SHA256",
      "TLS_ECDHE_RSA_WITH_AES_128_GCM_SHA256"
    };

    List<String> systemCipherSuites =
        Arrays.asList(System.getProperty("https.cipherSuites").split(","));
    assertThat(
        "Missing a supported cipher suite",
        systemCipherSuites,
        equalTo(Arrays.asList(supportedCipherSuites)));

    // Used to filter out cipher's that don't use our current key algorithm
    KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
    keystore.load(new FileInputStream(KEY_STORE_PATH), "changeit".toCharArray());
    String keyAlgorithm = keystore.getKey("localhost", "changeit".toCharArray()).getAlgorithm();

    String url = SERVICE_ROOT.getUrl() + "/catalog/query?q=*&src=local";
    CredentialsProvider credentialsProvider = createBasicAuth("admin", "admin");
    for (String cipher : supportedCipherSuites) {
      if (cipher.contains("_" + keyAlgorithm + "_")) {
        HttpClient client = createHttpClient("TLSv1.2", new String[] {cipher}, credentialsProvider);
        assertBasicAuth(client, url, 200);
      }
    }
  }

  @Test(expected = SSLHandshakeException.class)
  public void testDisallowedCipherSuites() throws Exception {
    String[] disallowedCipherSuites =
        new String[] {
          // We can't test any cipher suite with > 128 encryption. 256 requires the unlimited
          // strength policy to be installed
          //                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA384",
          // "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA384", "TLS_RSA_WITH_AES_256_CBC_SHA256",
          //                "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA384",
          // "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA384",
          //                "TLS_DHE_RSA_WITH_AES_256_CBC_SHA256",
          // "TLS_DHE_DSS_WITH_AES_256_CBC_SHA256",
          //                "TLS_ECDHE_ECDSA_WITH_AES_256_CBC_SHA",
          // "TLS_ECDHE_RSA_WITH_AES_256_CBC_SHA",
          //                "TLS_RSA_WITH_AES_256_CBC_SHA", "TLS_ECDH_ECDSA_WITH_AES_256_CBC_SHA",
          //                "TLS_ECDH_RSA_WITH_AES_256_CBC_SHA", "TLS_DHE_RSA_WITH_AES_256_CBC_SHA",
          //                "TLS_DHE_DSS_WITH_AES_256_CBC_SHA",
          // "TLS_ECDHE_ECDSA_WITH_AES_256_GCM_SHA384",
          //                "TLS_ECDHE_RSA_WITH_AES_256_GCM_SHA384",
          // "TLS_RSA_WITH_AES_256_GCM_SHA384",
          //                "TLS_ECDH_ECDSA_WITH_AES_256_GCM_SHA384",
          // "TLS_ECDH_RSA_WITH_AES_256_GCM_SHA384",
          //                "TLS_DHE_RSA_WITH_AES_256_GCM_SHA384",
          // "TLS_DHE_DSS_WITH_AES_256_GCM_SHA384",
          "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA256", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA256",
          "TLS_RSA_WITH_AES_128_CBC_SHA256", "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA256",
          "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA256", "TLS_DHE_DSS_WITH_AES_128_CBC_SHA256",
          "TLS_ECDHE_ECDSA_WITH_AES_128_CBC_SHA", "TLS_ECDHE_RSA_WITH_AES_128_CBC_SHA",
          "TLS_RSA_WITH_AES_128_CBC_SHA", "TLS_ECDH_ECDSA_WITH_AES_128_CBC_SHA",
          "TLS_ECDH_RSA_WITH_AES_128_CBC_SHA", "TLS_DHE_DSS_WITH_AES_128_CBC_SHA",
          "TLS_ECDHE_ECDSA_WITH_RC4_128_SHA", "TLS_ECDHE_RSA_WITH_RC4_128_SHA",
          "SSL_RSA_WITH_RC4_128_SHA", "TLS_ECDH_ECDSA_WITH_RC4_128_SHA",
          "TLS_ECDH_RSA_WITH_RC4_128_SHA", "TLS_RSA_WITH_AES_128_GCM_SHA256",
          "TLS_ECDH_ECDSA_WITH_AES_128_GCM_SHA256", "TLS_ECDH_RSA_WITH_AES_128_GCM_SHA256",
          "TLS_DHE_DSS_WITH_AES_128_GCM_SHA256", "TLS_ECDHE_ECDSA_WITH_3DES_EDE_CBC_SHA",
          "TLS_ECDHE_RSA_WITH_3DES_EDE_CBC_SHA", "SSL_RSA_WITH_3DES_EDE_CBC_SHA",
          "TLS_ECDH_ECDSA_WITH_3DES_EDE_CBC_SHA", "TLS_ECDH_RSA_WITH_3DES_EDE_CBC_SHA",
          "SSL_DHE_RSA_WITH_3DES_EDE_CBC_SHA", "SSL_DHE_DSS_WITH_3DES_EDE_CBC_SHA",
          "SSL_RSA_WITH_RC4_128_MD5", "TLS_EMPTY_RENEGOTIATION_INFO_SCSV"
        };

    List<String> systemCipherSuites =
        Arrays.asList(System.getProperty("https.cipherSuites").split(","));
    List<String> intersection = new ArrayList<>(Arrays.asList(disallowedCipherSuites));
    intersection.retainAll(systemCipherSuites);
    assertThat(
        "Supported cipher suite in disallowed ciphers",
        intersection,
        emptyCollectionOf(String.class));

    String url = SERVICE_ROOT.getUrl() + "/catalog/query?q=*&src=local";
    CredentialsProvider credentialsProvider = createBasicAuth("admin", "admin");
    HttpClient client = createHttpClient("TLSv1.2", disallowedCipherSuites, credentialsProvider);

    HttpGet get = new HttpGet(url);
    client.execute(get);
  }

  @Test
  public void testSamlFederatedAuth() throws Exception {
    configureRestForGuest(SDK_SOAP_CONTEXT);
    getSecurityPolicy().waitForGuestAuthReady(SERVICE_ROOT.getUrl());
    String recordId =
        ingest(
            getFileContent(JSON_RECORD_RESOURCE_PATH + "/SimpleGeoJsonRecord"), "application/json");

    // Creating a new OpenSearch source with no username/password.
    // When an OpenSearch source attempts to authenticate without a username/password it will
    // use the subject in the request to create a SAML authentication token
    Map<String, Object> openSearchProperties =
        getOpenSearchSourceProperties(
            OPENSEARCH_SAML_SOURCE_ID, OPENSEARCH_PATH.getUrl(), getServiceManager());
    getServiceManager().createManagedService(OPENSEARCH_FACTORY_PID, openSearchProperties);

    getCatalogBundle().waitForFederatedSource(OPENSEARCH_SAML_SOURCE_ID);

    String openSearchQuery =
        SERVICE_ROOT.getUrl() + "/catalog/query?q=*&src=" + OPENSEARCH_SAML_SOURCE_ID;
    waitForSecurityHandlers(openSearchQuery);
    given()
        .when()
        .get(openSearchQuery)
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .statusCode(equalTo(200))
        .assertThat()
        .body(
            hasXPath("//metacard/string[@name='" + Metacard.TITLE + "']/value[text()='myTitle']"));

    delete(recordId);
  }

  @Test
  public void testBasicFederatedAuth() throws Exception {
    configureRestForGuest(SDK_SOAP_CONTEXT);
    getSecurityPolicy().waitForGuestAuthReady(SERVICE_ROOT.getUrl());

    String recordId =
        ingest(
            getFileContent(JSON_RECORD_RESOURCE_PATH + "/SimpleGeoJsonRecord"), "application/json");
    configureRestForBasic(SDK_SOAP_CONTEXT);

    // Positive tests
    Map<String, Object> openSearchProperties =
        getOpenSearchSourceProperties(
            OPENSEARCH_SOURCE_ID, OPENSEARCH_PATH.getUrl(), getServiceManager());
    openSearchProperties.put("username", "admin");
    openSearchProperties.put("password", "admin");
    getServiceManager().createManagedService(OPENSEARCH_FACTORY_PID, openSearchProperties);

    Map<String, Object> cswProperties =
        getCswSourceProperties(CSW_SOURCE_ID, CSW_PATH.getUrl(), getServiceManager());
    cswProperties.put("username", "admin");
    cswProperties.put("password", "admin");
    getServiceManager().createManagedService(CSW_FEDERATED_SOURCE_FACTORY_PID, cswProperties);

    getCatalogBundle().waitForFederatedSource(OPENSEARCH_SOURCE_ID);
    getCatalogBundle().waitForFederatedSource(CSW_SOURCE_ID);

    String openSearchQuery =
        SERVICE_ROOT.getUrl() + "/catalog/query?q=*&src=" + OPENSEARCH_SOURCE_ID;
    waitForSecurityHandlers(openSearchQuery);
    getSecurityPolicy().waitForBasicAuthReady(openSearchQuery);
    given()
        .auth()
        .basic("admin", "admin")
        .when()
        .get(openSearchQuery)
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .statusCode(equalTo(200))
        .assertThat()
        .body(
            hasXPath("//metacard/string[@name='" + Metacard.TITLE + "']/value[text()='myTitle']"));

    String cswQuery = SERVICE_ROOT.getUrl() + "/catalog/query?q=*&src=" + CSW_SOURCE_ID;
    given()
        .auth()
        .basic("admin", "admin")
        .when()
        .get(cswQuery)
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .statusCode(equalTo(200))
        .assertThat()
        .body(
            hasXPath("//metacard/string[@name='" + Metacard.TITLE + "']/value[text()='myTitle']"));

    // Negative tests
    String unavailableCswSourceId = "Unavailable Csw";
    cswProperties =
        getCswSourceProperties(unavailableCswSourceId, CSW_PATH.getUrl(), getServiceManager());
    cswProperties.put("username", "bad");
    cswProperties.put("password", "auth");
    getServiceManager().createManagedService(CSW_FEDERATED_SOURCE_FACTORY_PID, cswProperties);

    String cswQueryUnavail =
        SERVICE_ROOT.getUrl() + "/catalog/query?q=*&src=" + unavailableCswSourceId;
    given()
        .auth()
        .basic("admin", "admin")
        .when()
        .get(cswQueryUnavail)
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .statusCode(equalTo(500));

    String unavailableOpenSourceId = "unavailableOpenSearchSource";

    OpenSearchFeature.createManagedService(
        getServiceManager(), unavailableOpenSourceId, "bad", "auth");
    getCatalogBundle().waitForFederatedSource(unavailableOpenSourceId);

    String unavailableOpenSearchQuery =
        SERVICE_ROOT.getUrl() + "/catalog/query?q=*&src=" + unavailableOpenSourceId;

    given()
        .auth()
        .basic("admin", "admin")
        .when()
        .get(unavailableOpenSearchQuery)
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .statusCode(equalTo(200))
        .assertThat()
        .body(
            not(
                hasXPath(
                    "//metacard/string[@name='" + Metacard.TITLE + "']/value[text()='myTitle']")));

    configureRestForGuest(SDK_SOAP_CONTEXT);
    getSecurityPolicy().waitForGuestAuthReady(openSearchQuery);
    delete(recordId);
  }

  @Test
  public void testGuestSoapAccess() throws Exception {
    String body =
        "<soapenv:Envelope xmlns:hel=\"http://ddf.sdk/soap/hello\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
            + "   <soapenv:Header>\n"
            + "   </soapenv:Header>\n"
            + "   <soapenv:Body>\n"
            + "      <hel:helloWorld/>\n"
            + "   </soapenv:Body>\n"
            + "</soapenv:Envelope>";
    // we are only testing guest because that hits the most code, testing with an assertion would be
    // mostly testing the same stuff that this is hitting
    given()
        .log()
        .ifValidationFails()
        .body(body)
        .header("Content-Type", "text/xml; charset=utf-8")
        .header("SOAPAction", "helloWorld")
        .expect()
        .statusCode(equalTo(200))
        .when()
        .post(SERVICE_ROOT.getUrl() + "/sdk/SoapService")
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .body(
            HasXPath.hasXPath(
                "//*[local-name()='helloWorldResponse']/result/text()", containsString("Guest")));
  }

  @Test
  public void testGuestSoapAccessHttp() throws Exception {
    getServiceManager().startFeature(true, "platform-http-proxy");

    String body =
        "<soapenv:Envelope xmlns:hel=\"http://ddf.sdk/soap/hello\" xmlns:soapenv=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
            + "   <soapenv:Header>\n"
            + "   </soapenv:Header>\n"
            + "   <soapenv:Body>\n"
            + "      <hel:helloWorld/>\n"
            + "   </soapenv:Body>\n"
            + "</soapenv:Envelope>";
    // we are only testing guest because that hits the most code, testing with an assertion would be
    // mostly testing the same stuff that this is hitting
    given()
        .log()
        .ifValidationFails()
        .body(body)
        .header("Content-Type", "text/xml; charset=utf-8")
        .header("SOAPAction", "helloWorld")
        .expect()
        .statusCode(equalTo(200))
        .when()
        .post(INSECURE_SERVICE_ROOT.getUrl() + "/sdk/SoapService")
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .body(
            HasXPath.hasXPath(
                "//*[local-name()='helloWorldResponse']/result/text()", containsString("Guest")));

    getServiceManager().stopFeature(false, "platform-http-proxy");
  }

  /* These STS tests are here to prove out functionality that doesn't get hit when accessing internal services. The standard UsernameToken and BinarySecurityToken elements are supported
   * by DDF, but not used internally. These elements need to be checked for functionality independently since going through our REST security framework won't touch these validators. */

  @Test
  public void testUsernameTokenSTS() throws Exception {
    String onBehalfOf =
        "<wst:OnBehalfOf>"
            + "                    <wsse:UsernameToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n"
            + "                        <wsse:Username>admin</wsse:Username>\n"
            + "                        <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">admin</wsse:Password>\n"
            + "                   </wsse:UsernameToken>\n"
            + "                </wst:OnBehalfOf>\n";
    String body = getSoapEnvelope(onBehalfOf);

    given()
        .auth()
        .certificate(
            KEY_STORE_PATH,
            PASSWORD,
            certAuthSettings().sslSocketFactory(SSLSocketFactory.getSystemSocketFactory()))
        .log()
        .ifValidationFails()
        .body(body)
        .header("Content-Type", "text/xml; charset=utf-8")
        .header("SOAPAction", "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue")
        .expect()
        .statusCode(equalTo(200))
        .when()
        .post(SERVICE_ROOT.getUrl() + "/SecurityTokenService")
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .body(HasXPath.hasXPath("//*[local-name()='Assertion']"));
  }

  @Test
  public void testBadUsernameTokenSTS() throws Exception {
    String onBehalfOf =
        "<wst:OnBehalfOf>"
            + "                    <wsse:UsernameToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n"
            + "                        <wsse:Username>admin</wsse:Username>\n"
            + "                        <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">blah</wsse:Password>\n"
            + "                   </wsse:UsernameToken>\n"
            + "                </wst:OnBehalfOf>\n";
    String body = getSoapEnvelope(onBehalfOf);

    given()
        .auth()
        .certificate(
            KEY_STORE_PATH,
            PASSWORD,
            certAuthSettings().sslSocketFactory(SSLSocketFactory.getSystemSocketFactory()))
        .log()
        .ifValidationFails()
        .body(body)
        .header("Content-Type", "text/xml; charset=utf-8")
        .header("SOAPAction", "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue")
        .expect()
        .statusCode(equalTo(500))
        .when()
        .post(SERVICE_ROOT.getUrl() + "/SecurityTokenService")
        .then()
        .log()
        .ifValidationFails();
  }

  @Test
  public void testBadX509TokenSTS() throws Exception {
    String onBehalfOf =
        "<wst:OnBehalfOf>\n"
            + "                    <wsse:BinarySecurityToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\" >\n"
            + BAD_X509_TOKEN
            + "                   </wsse:BinarySecurityToken>\n"
            + "                </wst:OnBehalfOf>\n";
    String body = getSoapEnvelope(onBehalfOf);

    given()
        .auth()
        .certificate(
            KEY_STORE_PATH,
            PASSWORD,
            certAuthSettings().sslSocketFactory(SSLSocketFactory.getSystemSocketFactory()))
        .log()
        .ifValidationFails()
        .body(body)
        .header("Content-Type", "text/xml; charset=utf-8")
        .header("SOAPAction", "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue")
        .expect()
        .statusCode(equalTo(500))
        .when()
        .post(SERVICE_ROOT.getUrl() + "/SecurityTokenService")
        .then()
        .log()
        .ifValidationFails();
  }

  @Test
  public void testX509TokenSTS() throws Exception {
    String onBehalfOf =
        "<wst:OnBehalfOf>\n"
            + "<wsse:BinarySecurityToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" "
            + "EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" "
            + "ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\" >"
            + GOOD_X509_TOKEN
            + "</wsse:BinarySecurityToken>\n"
            + "</wst:OnBehalfOf>\n";
    String body = getSoapEnvelope(onBehalfOf);

    given()
        .auth()
        .certificate(
            KEY_STORE_PATH,
            PASSWORD,
            certAuthSettings().sslSocketFactory(SSLSocketFactory.getSystemSocketFactory()))
        .log()
        .ifValidationFails()
        .body(body)
        .header("Content-Type", "text/xml; charset=utf-8")
        .header("SOAPAction", "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue")
        .expect()
        .statusCode(equalTo(200))
        .when()
        .post(SERVICE_ROOT.getUrl() + "/SecurityTokenService")
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .body(HasXPath.hasXPath("//*[local-name()='Assertion']"));
  }

  @Test
  public void testX509PathSTS() throws Exception {
    String onBehalfOf =
        "<wst:OnBehalfOf>\n"
            + "<wsse:BinarySecurityToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" "
            + "EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" "
            + "ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509PKIPathv1\" >"
            + GOOD_X509_PATH_TOKEN
            + "</wsse:BinarySecurityToken>\n"
            + "</wst:OnBehalfOf>\n";
    String body = getSoapEnvelope(onBehalfOf);

    given()
        .auth()
        .certificate(
            KEY_STORE_PATH,
            PASSWORD,
            certAuthSettings().sslSocketFactory(SSLSocketFactory.getSystemSocketFactory()))
        .log()
        .ifValidationFails()
        .body(body)
        .header("Content-Type", "text/xml; charset=utf-8")
        .header("SOAPAction", "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue")
        .expect()
        .statusCode(equalTo(200))
        .when()
        .post(SERVICE_ROOT.getUrl() + "/SecurityTokenService")
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .body(HasXPath.hasXPath("//*[local-name()='Assertion']"));
  }

  @Test
  public void testX509TokenWithPathSTS() throws Exception {
    String onBehalfOf =
        "<wst:OnBehalfOf>\n"
            + "<wsse:BinarySecurityToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" "
            + "EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" "
            + "ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509v3\" >\n"
            + GOOD_X509_PATH_TOKEN
            + "</wsse:BinarySecurityToken>\n"
            + "</wst:OnBehalfOf>\n";
    String body = getSoapEnvelope(onBehalfOf);

    given()
        .log()
        .ifValidationFails()
        .body(body)
        .header("Content-Type", "text/xml; charset=utf-8")
        .header("SOAPAction", "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue")
        .expect()
        .statusCode(equalTo(500))
        .when()
        .post(SERVICE_ROOT.getUrl() + "/SecurityTokenService")
        .then()
        .log()
        .ifValidationFails();
  }

  @Test
  public void testX509PathWithTokenSTS() throws Exception {
    String onBehalfOf =
        "<wst:OnBehalfOf>\n"
            + "<wsse:BinarySecurityToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\" "
            + "EncodingType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-soap-message-security-1.0#Base64Binary\" "
            + "ValueType=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-x509-token-profile-1.0#X509PKIPathv1\" >"
            + GOOD_X509_TOKEN
            + "</wsse:BinarySecurityToken>\n"
            + "</wst:OnBehalfOf>\n";
    String body = getSoapEnvelope(onBehalfOf);

    given()
        .log()
        .ifValidationFails()
        .body(body)
        .header("Content-Type", "text/xml; charset=utf-8")
        .header("SOAPAction", "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue")
        .expect()
        .statusCode(equalTo(500))
        .when()
        .post(SERVICE_ROOT.getUrl() + "/SecurityTokenService")
        .then()
        .log()
        .ifValidationFails();
  }

  @Test
  public void testSamlAssertionInHeaders() throws Exception {
    String onBehalfOf =
        "<wst:OnBehalfOf>"
            + "                    <wsse:UsernameToken xmlns:wsse=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-wssecurity-secext-1.0.xsd\">\n"
            + "                        <wsse:Username>admin</wsse:Username>\n"
            + "                        <wsse:Password Type=\"http://docs.oasis-open.org/wss/2004/01/oasis-200401-wss-username-token-profile-1.0#PasswordText\">admin</wsse:Password>\n"
            + "                   </wsse:UsernameToken>\n"
            + "                </wst:OnBehalfOf>\n";
    String body = getSoapEnvelope(onBehalfOf);

    String assertionHeader =
        given()
            .auth()
            .certificate(
                KEY_STORE_PATH,
                PASSWORD,
                certAuthSettings().sslSocketFactory(SSLSocketFactory.getSystemSocketFactory()))
            .log()
            .ifValidationFails()
            .body(body)
            .header("Content-Type", "text/xml; charset=utf-8")
            .header("SOAPAction", "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue")
            .expect()
            .statusCode(equalTo(200))
            .when()
            .post(SERVICE_ROOT.getUrl() + "/SecurityTokenService")
            .then()
            .extract()
            .response()
            .asString();
    assertionHeader =
        assertionHeader.substring(
            assertionHeader.indexOf("<saml2:Assertion"),
            assertionHeader.indexOf("</saml2:Assertion>") + "</saml2:Assertion>".length());

    LOGGER.trace(assertionHeader);

    configureRestForSaml(SDK_SOAP_CONTEXT);
    getSecurityPolicy().waitForSamlAuthReady(ADMIN_PATH.getUrl());

    // try that admin level assertion token on a restricted resource
    given()
        .header(
            SecurityConstants.SAML_HEADER_NAME,
            "SAML " + RestSecurity.deflateAndBase64Encode(assertionHeader))
        .when()
        .get(ADMIN_PATH.getUrl())
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .statusCode(equalTo(200));
  }

  @Test
  public void testGoodHokSamlAssertionInHeaders() throws Exception {
    String body = getSoapEnvelope(GOOD_HOK_EXAMPLE, null);

    String assertionHeader =
        given()
            .auth()
            .certificate(
                KEY_STORE_PATH,
                PASSWORD,
                certAuthSettings().sslSocketFactory(SSLSocketFactory.getSystemSocketFactory()))
            .log()
            .ifValidationFails()
            .body(body)
            .header("Content-Type", "text/xml; charset=utf-8")
            .header("SOAPAction", "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue")
            .expect()
            .statusCode(equalTo(200))
            .when()
            .post(SERVICE_ROOT.getUrl() + "/SecurityTokenService")
            .then()
            .extract()
            .response()
            .asString();
    assertionHeader =
        assertionHeader.substring(
            assertionHeader.indexOf("<saml2:Assertion"),
            assertionHeader.indexOf("</saml2:Assertion>") + "</saml2:Assertion>".length());

    LOGGER.trace(assertionHeader);

    configureRestForSaml(SDK_SOAP_CONTEXT);
    getSecurityPolicy().waitForSamlAuthReady(ADMIN_PATH.getUrl());

    // try that admin level assertion token on a restricted resource
    given()
        .auth()
        .certificate(
            KEY_STORE_PATH,
            PASSWORD,
            certAuthSettings().sslSocketFactory(SSLSocketFactory.getSystemSocketFactory()))
        .header(
            SecurityConstants.SAML_HEADER_NAME,
            "SAML " + RestSecurity.deflateAndBase64Encode(assertionHeader))
        .when()
        .get(ADMIN_PATH.getUrl())
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .statusCode(equalTo(200));
  }

  @Test
  public void testBadHokSamlAssertionInHeaders() throws Exception {
    String body = getSoapEnvelope(BAD_HOK_EXAMPLE, null);

    String assertionHeader =
        given()
            .auth()
            .certificate(
                KEY_STORE_PATH,
                PASSWORD,
                certAuthSettings().sslSocketFactory(SSLSocketFactory.getSystemSocketFactory()))
            .log()
            .ifValidationFails()
            .body(body)
            .header("Content-Type", "text/xml; charset=utf-8")
            .header("SOAPAction", "http://docs.oasis-open.org/ws-sx/ws-trust/200512/RST/Issue")
            .expect()
            .statusCode(equalTo(200))
            .when()
            .post(SERVICE_ROOT.getUrl() + "/SecurityTokenService")
            .then()
            .extract()
            .response()
            .asString();
    assertionHeader =
        assertionHeader.substring(
            assertionHeader.indexOf("<saml2:Assertion"),
            assertionHeader.indexOf("</saml2:Assertion>") + "</saml2:Assertion>".length());

    LOGGER.trace(assertionHeader);

    configureRestForSaml(SDK_SOAP_CONTEXT);
    getSecurityPolicy().waitForSamlAuthReady(ADMIN_PATH.getUrl());

    // try that admin level assertion token on a restricted resource
    given()
        .auth()
        .certificate(
            KEY_STORE_PATH,
            PASSWORD,
            certAuthSettings().sslSocketFactory(SSLSocketFactory.getSystemSocketFactory()))
        .header(
            SecurityConstants.SAML_HEADER_NAME,
            "SAML " + RestSecurity.deflateAndBase64Encode(assertionHeader))
        .when()
        .get(ADMIN_PATH.getUrl())
        .then()
        .log()
        .ifValidationFails()
        .assertThat()
        .statusCode(equalTo(400));
  }

  private String getSoapEnvelope(String onBehalfOf) {
    return getSoapEnvelope(SOAP_ENV, onBehalfOf);
  }

  private String getSoapEnvelope(String body, String onBehalfOf) {
    Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
    SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'.500Z'");
    format.setCalendar(calendar);
    String created = format.format(new Date(calendar.getTimeInMillis()));
    long now = calendar.getTimeInMillis();
    now += 60000;
    String expires = format.format(new Date(now));
    if (onBehalfOf != null) {
      body = body.replace("ON_BEHALF_OF", onBehalfOf);
    }
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
    Files.copy(Paths.get(getKeystoreFilename()), Paths.get(getBackupFilename()), REPLACE_EXISTING);
  }

  void restoreKeystoreFile() throws IOException {
    Files.copy(Paths.get(getBackupFilename()), Paths.get(getKeystoreFilename()), REPLACE_EXISTING);
  }

  // ConfigurationAdmin tests
  @Test
  public void testAdminConfigPolicyGetServices() {

    String getAllServicesResponsePermitted =
        sendPermittedRequest(
            "/admin/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/listServices");

    for (String service : SERVICES_TO_FILTER) {
      assertTrue(getAllServicesResponsePermitted.contains(service));
    }

    String getAllServicesResponseNotPermitted =
        sendNotPermittedRequest(
            "/admin/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/listServices");

    // Verify there are configurations the user can see other than the restricted ones
    assertTrue(
        getAllServicesResponseNotPermitted.contains(
            "org.codice.ddf.ui.admin.api.ConfigurationAdmin"));

    for (String filteredService : SERVICES_TO_FILTER) {
      assertFalse(getAllServicesResponseNotPermitted.contains(filteredService));
    }
  }

  @Test
  public void testAdminConfigPolicyCreateAndModifyConfiguration() {

    // create config CreateFactoryConfiguration

    String createFactoryConfigPermittedResponse =
        sendPermittedRequest(
            "/admin/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/createFactoryConfiguration/testCreateFactoryPid");
    assertThat(
        JsonPath.given(createFactoryConfigPermittedResponse).getString("value"),
        containsString("testCreateFactoryPid"));

    String createFactoryConfigNotPermittedResponse =
        sendNotPermittedRequest(
            "/admin/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/createFactoryConfiguration/testCreateFactoryPid");
    assertNull(JsonPath.given(createFactoryConfigNotPermittedResponse).get("value"));

    // get config GetConfigurations

    String getConfigurationsPermitted =
        sendPermittedRequest(
            "/admin/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/getConfigurations/(service.pid=ddf.security.pdp.realm.AuthzRealm)");
    assertThat(
        JsonPath.given(getConfigurationsPermitted).getString("value"),
        containsString("ddf.security.pdp.realm.AuthzRealm"));

    String getConfigurationsNotPermitted =
        sendNotPermittedRequest(
            "/admin/jolokia/exec/org.codice.ddf.ui.admin.api.ConfigurationAdmin:service=ui,version=2.3.0/getConfigurations/(service.pid=ddf.security.pdp.realm.AuthzRealm)");
    assertEquals(JsonPath.given(getConfigurationsNotPermitted).getString("value"), "[]");
  }

  // ApplicationService tests
  @Test
  public void testAdminConfigPolicyGetAllFeaturesAndApps() throws Exception {

    configureRestForBasic();
    getSecurityPolicy().waitForBasicAuthReady(ADMIN_PATH.getUrl());

    String getAllFeaturesResponseNotPermitted =
        sendNotPermittedRequest(
            "/admin/jolokia/read/org.codice.ddf.admin.application.service.ApplicationService:service=application-service");

    String filteredApplications =
        JsonPath.given(getAllFeaturesResponseNotPermitted).getString("value.Applications.name");

    String filteredFeatures =
        JsonPath.given(getAllFeaturesResponseNotPermitted).getString("value.AllFeatures.name");

    for (String app : getDefaultRequiredApps()) {
      assertThat(filteredApplications, not(containsString(app)));
    }

    for (String feature : FEATURES_TO_FILTER) {
      assertThat(filteredFeatures, not(containsString(feature)));
    }

    String getAllFeaturesResponsePermitted =
        sendPermittedRequest(
            "/admin/jolokia/read/org.codice.ddf.admin.application.service.ApplicationService:service=application-service");

    filteredApplications =
        JsonPath.given(getAllFeaturesResponsePermitted).getString("value.Applications.name");
    filteredFeatures =
        JsonPath.given(getAllFeaturesResponsePermitted).getString("value.AllFeatures.name");

    for (String app : getDefaultRequiredApps()) {
      if (!"sdk-app".equals(app)) {
        assertThat(filteredApplications, containsString(app));
      }
    }

    for (String feature : FEATURES_TO_FILTER) {
      assertThat(filteredFeatures, containsString(feature));
    }
  }

  private void waitForSecurityHandlers(String url) throws InterruptedException {
    long stop = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(5);
    while (get(url).statusCode() == 503) {
      Thread.sleep(1000);
      if (System.currentTimeMillis() > stop) {
        fail("Failed waiting for security handlers to become available.");
      }
    }
  }

  public String sendPermittedRequest(String jolokiaEndpoint) {
    return given()
        .auth()
        .basic("admin", "admin")
        .header("X-Requested-With", "XMLHttpRequest")
        .header("Origin", SECURE_ROOT_AND_PORT.getUrl())
        .get(SECURE_ROOT_AND_PORT + jolokiaEndpoint)
        .body()
        .print();
  }

  public String sendNotPermittedRequest(String jolokiaEndpoint) {
    return given()
        .auth()
        .basic(SYSTEM_ADMIN_USER, SYSTEM_ADMIN_USER_PASSWORD)
        .header("X-Requested-With", "XMLHttpRequest")
        .header("Origin", SECURE_ROOT_AND_PORT.getUrl())
        .get(SECURE_ROOT_AND_PORT + jolokiaEndpoint)
        .body()
        .print();
  }

  @Test
  public void testAccessGroupsGuest() throws Exception {
    Dictionary authZProperties = null;
    Dictionary metacardAttributeSecurityFilterProperties = null;

    String url = SERVICE_ROOT.getUrl() + "/catalog/query?q=*&src=local";

    try {
      configureRestForGuest(SDK_SOAP_CONTEXT);
      getSecurityPolicy().waitForGuestAuthReady(url);

      authZProperties =
          configureAuthZRealm(
              Collections.emptyList(),
              Collections.singletonList(
                  "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=accessGroups"),
              Collections.emptyList(),
              getAdminConfig());
      metacardAttributeSecurityFilterProperties =
          configureMetacardAttributeSecurityFiltering(
              Collections.singletonList("security.access-groups=accessGroups"),
              Collections.emptyList(),
              getAdminConfig());

      String testData = getFileContent(XML_RECORD_RESOURCE_PATH + "/accessGroupTokenMetacard.xml");
      testData = testData.replace(ACCESS_GROUP_REPLACE_TOKEN, "guest");
      String id = CatalogTestCommons.ingest(testData, MediaType.TEXT_XML);

      // anon guest
      String response = query(id, TRANSFORMER_XML);
      assertThat(response, containsString("Lady Liberty"));

      // configure for basic
      configureRestForBasic(SDK_SOAP_CONTEXT);
      waitForSecurityHandlers(url);
      getSecurityPolicy().waitForBasicAuthReady(url);

      // user with permissions gets results
      response = queryWithBasicAuth(id, TRANSFORMER_XML, A_USER, USER_PASSWORD);
      assertThat(response, containsString("Lady Liberty"));

      // user without permissions gets results
      response = queryWithBasicAuth(id, TRANSFORMER_XML, B_USER, USER_PASSWORD);
      assertThat(response, containsString("Lady Liberty"));

    } finally {
      if (authZProperties != null) {
        configureAuthZRealm(authZProperties, getAdminConfig());
      }
      if (metacardAttributeSecurityFilterProperties != null) {
        configureMetacardAttributeSecurityFiltering(
            metacardAttributeSecurityFilterProperties, getAdminConfig());
      }
      configureRestForGuest(SDK_SOAP_CONTEXT);
      getSecurityPolicy().waitForGuestAuthReady(url);
    }
  }

  @Test
  public void testAccessGroups() throws Exception {
    Dictionary authZProperties = null;
    Dictionary metacardAttributeSecurityFilterProperties = null;

    String url = SERVICE_ROOT.getUrl() + "/catalog/query?q=*&src=local";

    configureRestForGuest(SDK_SOAP_CONTEXT);
    getSecurityPolicy().waitForGuestAuthReady(url);

    String testData = getFileContent(XML_RECORD_RESOURCE_PATH + "/accessGroupTokenMetacard.xml");
    testData = testData.replace(ACCESS_GROUP_REPLACE_TOKEN, "B");
    String id = CatalogTestCommons.ingest(testData, MediaType.TEXT_XML);

    try {
      authZProperties =
          configureAuthZRealm(
              Collections.emptyList(),
              Collections.singletonList(
                  "http://schemas.xmlsoap.org/ws/2005/05/identity/claims/role=accessGroups"),
              Collections.emptyList(),
              getAdminConfig());
      metacardAttributeSecurityFilterProperties =
          configureMetacardAttributeSecurityFiltering(
              Collections.singletonList("security.access-groups=accessGroups"),
              Collections.emptyList(),
              getAdminConfig());

      // configure for basic auth
      configureRestForBasic(SDK_SOAP_CONTEXT);
      waitForSecurityHandlers(url);
      getSecurityPolicy().waitForBasicAuthReady(url);

      // user without permissions cannot get results
      queryWithBasicAuth(id, TRANSFORMER_XML, A_USER, USER_PASSWORD, HttpStatus.SC_NOT_FOUND);

      // user with permissions gets results
      String response = queryWithBasicAuth(id, TRANSFORMER_XML, B_USER, USER_PASSWORD);
      assertThat(response, containsString("Lady Liberty"));

      // configure for guest
      configureRestForGuest(SDK_SOAP_CONTEXT);
      getSecurityPolicy().waitForGuestAuthReady(url);

      // guest cannot get results
      query(id, TRANSFORMER_XML, HttpStatus.SC_NOT_FOUND);

    } finally {
      if (authZProperties != null) {
        configureAuthZRealm(authZProperties, getAdminConfig());
      }
      if (metacardAttributeSecurityFilterProperties != null) {
        configureMetacardAttributeSecurityFiltering(
            metacardAttributeSecurityFilterProperties, getAdminConfig());
      }
      configureRestForGuest(SDK_SOAP_CONTEXT);
      waitForSecurityHandlers(url);
      getSecurityPolicy().waitForGuestAuthReady(url);
    }
  }

  private CredentialsProvider createBasicAuth(String username, String password) {
    CredentialsProvider credentialsProvider = new BasicCredentialsProvider();
    UsernamePasswordCredentials usernamePasswordCredentials =
        new UsernamePasswordCredentials(username, password);
    credentialsProvider.setCredentials(AuthScope.ANY, usernamePasswordCredentials);
    return credentialsProvider;
  }

  private HttpClient createHttpClient(String protocol)
      throws KeyManagementException, NoSuchAlgorithmException {
    return createHttpClient(protocol, new BasicCredentialsProvider());
  }

  private HttpClient createHttpClient(String protocol, CredentialsProvider credentialsProvider)
      throws KeyManagementException, NoSuchAlgorithmException {
    return createHttpClient(protocol, null, credentialsProvider);
  }

  private HttpClient createHttpClient(
      String protocol, String[] cipherSuites, CredentialsProvider credentialsProvider)
      throws KeyManagementException, NoSuchAlgorithmException {
    SSLContext context = SSLContexts.custom().useProtocol(protocol).build();

    SSLConnectionSocketFactory socketFactory =
        new SSLConnectionSocketFactory(
            context, null, cipherSuites, SSLConnectionSocketFactory.getDefaultHostnameVerifier());

    return HttpClients.custom()
        .setDefaultCredentialsProvider(credentialsProvider)
        .setSSLSocketFactory(socketFactory)
        .build();
  }

  private void assertBasicAuth(HttpClient client, String url, int statusCode) throws Exception {
    configureRestForBasic(SDK_SOAP_CONTEXT);
    waitForSecurityHandlers(url);

    try {
      HttpGet get = new HttpGet(url);
      int result = client.execute(get).getStatusLine().getStatusCode();

      assertThat(result, equalTo(statusCode));
    } finally {
      configureRestForGuest(SDK_SOAP_CONTEXT);
      getSecurityPolicy().waitForGuestAuthReady(url);
    }
  }
}
