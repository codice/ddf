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
package org.codice.ddf.security.idp.server;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.text.StringContains.containsString;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URLDecoder;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.Response;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.cxf.helpers.DOMUtils;
import org.apache.cxf.ws.security.tokenstore.SecurityToken;
import org.apache.shiro.subject.PrincipalCollection;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.codice.ddf.security.common.jaxrs.RestSecurity;
import org.codice.ddf.security.handler.api.PKIAuthenticationTokenFactory;
import org.codice.ddf.security.policy.context.ContextPolicy;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

import ddf.security.Subject;
import ddf.security.assertion.SecurityAssertion;
import ddf.security.encryption.EncryptionService;
import ddf.security.samlp.SamlProtocol;
import ddf.security.service.SecurityManager;
import ddf.security.service.SecurityServiceException;

public class IdpEndpointTest {

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    String spMetadata =
            "<md:EntityDescriptor xmlns:md=\"urn:oasis:names:tc:SAML:2.0:metadata\" entityID=\"https://localhost:8993/services/saml\">\n"
                    + "<md:SPSSODescriptor protocolSupportEnumeration=\"urn:oasis:names:tc:SAML:2.0:protocol\">\n"
                    + "<md:KeyDescriptor use=\"signing\">\n"
                    + "<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n"
                    + "<ds:X509Data>\n" + "<ds:X509Certificate>\n"
                    + "MIIDEzCCAnygAwIBAgIJAIzc4FYrIp9mMA0GCSqGSIb3DQEBBQUAMHcxCzAJBgNVBAYTAlVTMQswCQYDVQQIDAJBWjEMMAoGA1UECgwDRERGMQwwCgYDVQQLDANEZXYxGTAXBgNVBAMMEERERiBEZW1vIFJvb3QgQ0ExJDAiBgkqhkiG9w0BCQEWFWRkZnJvb3RjYUBleGFtcGxlLm9yZzAeFw0xNDEyMTAyMTU4MThaFw0xNTEyMTAyMTU4MThaMIGDMQswCQYDVQQGEwJVUzELMAkGA1UECAwCQVoxETAPBgNVBAcMCEdvb2R5ZWFyMQwwCgYDVQQKDANEREYxDDAKBgNVBAsMA0RldjESMBAGA1UEAwwJbG9jYWxob3N0MSQwIgYJKoZIhvcNAQkBFhVsb2NhbGhvc3RAZXhhbXBsZS5vcmcwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAMeCyNZbCTZphHQfB5g8FrgBq1RYzV7ikVw/pVGkz8gx3l3A99s8WtA4mRAeb6n0vTR9yNBOekW4nYOiEOq//YTi/frI1kz0QbEH1s2cI5nFButabD3PYGxUSuapbc+AS7+Pklr0TDI4MRzPPkkTp4wlORQ/a6CfVsNr/mVgL2CfAgMBAAGjgZkwgZYwCQYDVR0TBAIwADAnBglghkgBhvhCAQ0EGhYYRk9SIFRFU1RJTkcgUFVSUE9TRSBPTkxZMB0GA1UdDgQWBBSA95QIMyBAHRsd0R4s7C3BreFrsDAfBgNVHSMEGDAWgBThVMeX3wrCv6lfeF47CyvkSBe9xjAgBgNVHREEGTAXgRVsb2NhbGhvc3RAZXhhbXBsZS5vcmcwDQYJKoZIhvcNAQEFBQADgYEAtRUp7fAxU/E6JD2Kj/+CTWqu8Elx13S0TxoIqv3gMoBW0ehyzEKjJi0bb1gUxO7n1SmOESp5sE3jGTnh0GtYV0D219z/09n90cd/imAEhknJlayyd0SjpnaL9JUd8uYxJexy8TJ2sMhsGAZ6EMTZCfT9m07XduxjsmDz0hlSGV0=\n"
                    + "</ds:X509Certificate>\n" + "</ds:X509Data>\n" + "</ds:KeyInfo>\n"
                    + "</md:KeyDescriptor>\n" + "<md:KeyDescriptor use=\"encryption\">\n"
                    + "<ds:KeyInfo xmlns:ds=\"http://www.w3.org/2000/09/xmldsig#\">\n"
                    + "<ds:X509Data>\n" + "<ds:X509Certificate>\n"
                    + "MIIDEzCCAnygAwIBAgIJAIzc4FYrIp9mMA0GCSqGSIb3DQEBBQUAMHcxCzAJBgNVBAYTAlVTMQswCQYDVQQIDAJBWjEMMAoGA1UECgwDRERGMQwwCgYDVQQLDANEZXYxGTAXBgNVBAMMEERERiBEZW1vIFJvb3QgQ0ExJDAiBgkqhkiG9w0BCQEWFWRkZnJvb3RjYUBleGFtcGxlLm9yZzAeFw0xNDEyMTAyMTU4MThaFw0xNTEyMTAyMTU4MThaMIGDMQswCQYDVQQGEwJVUzELMAkGA1UECAwCQVoxETAPBgNVBAcMCEdvb2R5ZWFyMQwwCgYDVQQKDANEREYxDDAKBgNVBAsMA0RldjESMBAGA1UEAwwJbG9jYWxob3N0MSQwIgYJKoZIhvcNAQkBFhVsb2NhbGhvc3RAZXhhbXBsZS5vcmcwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAMeCyNZbCTZphHQfB5g8FrgBq1RYzV7ikVw/pVGkz8gx3l3A99s8WtA4mRAeb6n0vTR9yNBOekW4nYOiEOq//YTi/frI1kz0QbEH1s2cI5nFButabD3PYGxUSuapbc+AS7+Pklr0TDI4MRzPPkkTp4wlORQ/a6CfVsNr/mVgL2CfAgMBAAGjgZkwgZYwCQYDVR0TBAIwADAnBglghkgBhvhCAQ0EGhYYRk9SIFRFU1RJTkcgUFVSUE9TRSBPTkxZMB0GA1UdDgQWBBSA95QIMyBAHRsd0R4s7C3BreFrsDAfBgNVHSMEGDAWgBThVMeX3wrCv6lfeF47CyvkSBe9xjAgBgNVHREEGTAXgRVsb2NhbGhvc3RAZXhhbXBsZS5vcmcwDQYJKoZIhvcNAQEFBQADgYEAtRUp7fAxU/E6JD2Kj/+CTWqu8Elx13S0TxoIqv3gMoBW0ehyzEKjJi0bb1gUxO7n1SmOESp5sE3jGTnh0GtYV0D219z/09n90cd/imAEhknJlayyd0SjpnaL9JUd8uYxJexy8TJ2sMhsGAZ6EMTZCfT9m07XduxjsmDz0hlSGV0=\n"
                    + "</ds:X509Certificate>\n" + "</ds:X509Data>\n" + "</ds:KeyInfo>\n"
                    + "</md:KeyDescriptor>\n"
                    + "<md:SingleLogoutService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\"https://localhost:8993/logout\"/>\n"
                    + "<md:SingleLogoutService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"https://localhost:8993/logout\"/>\n"
                    + "<md:AssertionConsumerService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Location=\"https://localhost:8993/services/saml/sso\"/>\n"
                    + "<md:AssertionConsumerService Binding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-POST\" Location=\"https://localhost:8993/services/saml/sso\"/>\n"
                    + "</md:SPSSODescriptor>\n" + "</md:EntityDescriptor>";

    String authNRequestPost =
            "PHNhbWwycDpBdXRoblJlcXVlc3QgQXNzZXJ0aW9uQ29uc3VtZXJTZXJ2aWNlVVJMPSJodHRwczovL2xvY2FsaG9zdDo4OTkzL3NlcnZpY2VzL3NhbWwvc3NvIiBEZXN0aW5hdGlvbj0iaHR0cHM6Ly9sb2NhbGhvc3Q6ODk5My9zZXJ2aWNlcy9pZHAvbG9naW4iIElEPSJfYjBlZWYzOTctMTExMy00OTBlLWJiZWYtM2I1ZGY2NThkYmU0IiBJc3N1ZUluc3RhbnQ9IjIwMTUtMTAtMjlUMTk6NTM6MTkuNzA0WiIgUHJvdG9jb2xCaW5kaW5nPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YmluZGluZ3M6SFRUUC1QT1NUIiBWZXJzaW9uPSIyLjAiIHhtbG5zOnNhbWwycD0idXJuOm9hc2lzOm5hbWVzOnRjOlNBTUw6Mi4wOnByb3RvY29sIj48c2FtbDI6SXNzdWVyIHhtbG5zOnNhbWwyPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6YXNzZXJ0aW9uIj5odHRwczovL2xvY2FsaG9zdDo4OTkzL3NlcnZpY2VzL3NhbWw8L3NhbWwyOklzc3Vlcj48ZHM6U2lnbmF0dXJlIHhtbG5zOmRzPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjIj48ZHM6U2lnbmVkSW5mbz48ZHM6Q2Fub25pY2FsaXphdGlvbk1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDEvMTAveG1sLWV4Yy1jMTRuIyIvPjxkczpTaWduYXR1cmVNZXRob2QgQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjcnNhLXNoYTEiLz48ZHM6UmVmZXJlbmNlIFVSST0iI19iMGVlZjM5Ny0xMTEzLTQ5MGUtYmJlZi0zYjVkZjY1OGRiZTQiPjxkczpUcmFuc2Zvcm1zPjxkczpUcmFuc2Zvcm0gQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAwLzA5L3htbGRzaWcjZW52ZWxvcGVkLXNpZ25hdHVyZSIvPjxkczpUcmFuc2Zvcm0gQWxnb3JpdGhtPSJodHRwOi8vd3d3LnczLm9yZy8yMDAxLzEwL3htbC1leGMtYzE0biMiLz48L2RzOlRyYW5zZm9ybXM+PGRzOkRpZ2VzdE1ldGhvZCBBbGdvcml0aG09Imh0dHA6Ly93d3cudzMub3JnLzIwMDAvMDkveG1sZHNpZyNzaGExIi8+PGRzOkRpZ2VzdFZhbHVlPjB0VFFadFlFNTIyR1djZjhmaUdscDV3Z2dNOD08L2RzOkRpZ2VzdFZhbHVlPjwvZHM6UmVmZXJlbmNlPjwvZHM6U2lnbmVkSW5mbz48ZHM6U2lnbmF0dXJlVmFsdWU+dVR0ZlZsZnZDNmY1VVlXeURLSGcrM3JUbWJHNUtTd3NVMHh2OFJuSWhUSWNmdkpxNmRoMmdrN3J2NE9PT1BLMlYvdmJOaFlNaUNBbDFFbmRyNTdqa3pDcW1vU2M0eHcyNnJMdlBWZ0VsQkM5cVpQWEZyMjZ2SVdZYjJaMWJRbTh2eG4xS2xmVVFkb3hDQjJpYkZyWHFXeG94NllIR2xzWnQ5RTZBSHFvZ3NrPTwvZHM6U2lnbmF0dXJlVmFsdWU+PGRzOktleUluZm8+PGRzOlg1MDlEYXRhPjxkczpYNTA5Q2VydGlmaWNhdGU+TUlJREV6Q0NBbnlnQXdJQkFnSUpBSXpjNEZZcklwOW1NQTBHQ1NxR1NJYjNEUUVCQlFVQU1IY3hDekFKQmdOVkJBWVRBbFZUTVFzdwpDUVlEVlFRSURBSkJXakVNTUFvR0ExVUVDZ3dEUkVSR01Rd3dDZ1lEVlFRTERBTkVaWFl4R1RBWEJnTlZCQU1NRUVSRVJpQkVaVzF2CklGSnZiM1FnUTBFeEpEQWlCZ2txaGtpRzl3MEJDUUVXRldSa1puSnZiM1JqWVVCbGVHRnRjR3hsTG05eVp6QWVGdzB4TkRFeU1UQXkKTVRVNE1UaGFGdzB4TlRFeU1UQXlNVFU0TVRoYU1JR0RNUXN3Q1FZRFZRUUdFd0pWVXpFTE1Ba0dBMVVFQ0F3Q1FWb3hFVEFQQmdOVgpCQWNNQ0VkdmIyUjVaV0Z5TVF3d0NnWURWUVFLREFORVJFWXhEREFLQmdOVkJBc01BMFJsZGpFU01CQUdBMVVFQXd3SmJHOWpZV3hvCmIzTjBNU1F3SWdZSktvWklodmNOQVFrQkZoVnNiMk5oYkdodmMzUkFaWGhoYlhCc1pTNXZjbWN3Z1o4d0RRWUpLb1pJaHZjTkFRRUIKQlFBRGdZMEFNSUdKQW9HQkFNZUN5TlpiQ1RacGhIUWZCNWc4RnJnQnExUll6Vjdpa1Z3L3BWR2t6OGd4M2wzQTk5czhXdEE0bVJBZQpiNm4wdlRSOXlOQk9la1c0bllPaUVPcS8vWVRpL2ZySTFrejBRYkVIMXMyY0k1bkZCdXRhYkQzUFlHeFVTdWFwYmMrQVM3K1BrbHIwClRESTRNUnpQUGtrVHA0d2xPUlEvYTZDZlZzTnIvbVZnTDJDZkFnTUJBQUdqZ1prd2daWXdDUVlEVlIwVEJBSXdBREFuQmdsZ2hrZ0IKaHZoQ0FRMEVHaFlZUms5U0lGUkZVMVJKVGtjZ1VGVlNVRTlUUlNCUFRreFpNQjBHQTFVZERnUVdCQlNBOTVRSU15QkFIUnNkMFI0cwo3QzNCcmVGcnNEQWZCZ05WSFNNRUdEQVdnQlRoVk1lWDN3ckN2NmxmZUY0N0N5dmtTQmU5eGpBZ0JnTlZIUkVFR1RBWGdSVnNiMk5oCmJHaHZjM1JBWlhoaGJYQnNaUzV2Y21jd0RRWUpLb1pJaHZjTkFRRUZCUUFEZ1lFQXRSVXA3ZkF4VS9FNkpEMktqLytDVFdxdThFbHgKMTNTMFR4b0lxdjNnTW9CVzBlaHl6RUtqSmkwYmIxZ1V4TzduMVNtT0VTcDVzRTNqR1RuaDBHdFlWMEQyMTl6LzA5bjkwY2QvaW1BRQpoa25KbGF5eWQwU2pwbmFMOUpVZDh1WXhKZXh5OFRKMnNNaHNHQVo2RU1UWkNmVDltMDdYZHV4anNtRHowaGxTR1YwPTwvZHM6WDUwOUNlcnRpZmljYXRlPjwvZHM6WDUwOURhdGE+PC9kczpLZXlJbmZvPjwvZHM6U2lnbmF0dXJlPjxzYW1sMnA6TmFtZUlEUG9saWN5IEFsbG93Q3JlYXRlPSJ0cnVlIiBGb3JtYXQ9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpuYW1laWQtZm9ybWF0OnBlcnNpc3RlbnQiIFNQTmFtZVF1YWxpZmllcj0iaHR0cHM6Ly9sb2NhbGhvc3Q6ODk5My9zZXJ2aWNlcy9zYW1sIi8+PC9zYW1sMnA6QXV0aG5SZXF1ZXN0Pg==";

    String authNRequestPkiPost =
            "jZJRT8IwFIX/StP3sa1jCA0jQYiRBHUy9MEXU7Y7aNK1s7dD/fdugAZf0Nf29Dun594xikqxmk8bt9MreGsAHVlgKhDlHhLqbAOUTBHBOmn0zGhsKrAZ2L3M4Wm1TOjOuRq57yuTC7Uz6PhwNIp8PErQ7xx8REPJvIVLLTrQ389kUbd3W6kpWcwT+irieLCJ8tLbDCD3+kHEvE0c9j2AYTksclawMGqliA0sNDqhXUJZEMZeGHhstA6vOIs4G/aCIHihJLXGmdyoa6kLqbcJbazmRqBErkUFyF3Os+ndkrNewDdHEfLb9Tr10odsTckzWDz8oxVQ8lEpjfzY5WVWfTKmk/FBzg+J7TnhMkB8z4JO/tP82D+3OZnW/L7lLuapUTL/JFOlzPvMgnA/E78xthLucpLuRBZeeZDyuisEHWhHSZZ2/MdGKFlKsP/bEepPTmF/r+PkCw==";

    String authNRequestGet =
            "jZLNbsIwEIRfJfLdJHEIDRZBoqCqSLSiQHvopTLOApYcO/U6/Xn7JoFK9EK52uNvZsc7QlFqVvFJ7Q9mBe81oA8miOC8smZqDdYluDW4DyXhebXIycH7CnkYaiuFPlj0PBsOkxCPEgxbYIhoSTBrWMqIFvT/M1VUzd1eGRLMZzl5E2k62CZyR7cDkLQfJYxu07hPAbJdVkhWsDhppIg1zA16YXxOWBSnNI4oG27iG84SzrJeFEWvJFg66620+laZQpl9TmpnuBWokBtRAnIv+XrysOCsF/HtUYT8frNZ0hUUyoH0JHgBh90sjYgEX6U2yI/1XeZVJ3MyHnVy3qV254TLAPH7H2R8Tfuj8NzmZFrxx4Y7ny2tVvI7mGhtP6cOhIeceFcDCe6sK4W/nKQ9UQXddVJetYWgB9OUs162/KdaaLVT4K7bExKOT2H/buD4Bw==";

    String authNRequestGetForce =
            "<saml2p:AuthnRequest AssertionConsumerServiceURL=\"https://localhost:8993/services/saml/sso\" Destination=\"https://localhost:8993/services/idp/login\" ID=\"_a556b3cf-b6ec-4032-b514-ee8f8dc2d213\" IssueInstant=\"2015-10-29T17:23:28.000Z\" ProtocolBinding=\"urn:oasis:names:tc:SAML:2.0:bindings:HTTP-Redirect\" Version=\"2.0\" xmlns:saml2p=\"urn:oasis:names:tc:SAML:2.0:protocol\" ForceAuthn=\"true\"><saml2:Issuer xmlns:saml2=\"urn:oasis:names:tc:SAML:2.0:assertion\">https://localhost:8993/services/saml</saml2:Issuer><saml2p:NameIDPolicy AllowCreate=\"true\" Format=\"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent\" SPNameQualifier=\"https://localhost:8993/services/saml\"/></saml2p:AuthnRequest>";

    String authNRequestGetSignature =
            "CQyv7hn001ClryULPk+DCqMiZYFnpmnVneP2byUoQ8rQr2fkM9F7y5f7CsIyyO8+uLO0ffXLHy7OrmnnbAeKJ2hRUJLUl8dt5NWqDPjGLfNCA8KshSeoxhbwc5PS3zjIGzdox/rdbsDJ4BOL2Jyh2OzlVKry+QAh3wmJDQrFcxs=";

    String authNRequestPassivePkiGet =
            "jZJRT8IwFIX/ytL3sa0DhIYtQYhxCZoJ6IMvpnQXaNK1s7dD/fduAw2+IK/t6XdOz70T5KWiFZvWbq+X8F4DOi/DnCPKAyTE2RqIN0UE66TRM6OxLsGuwB6kgOflIiF75ypkQaCM4Gpv0LHReBwHeJRg0DoEiIZ48wYuNW9B/z+TRdXc7aQmXjZPyBsfDIabWGz9zRCE3w9j6m8GUd8HGG1HhaAFjeJGilhDptFx7RJCw2jgR6FPx+vohtGY0VEvDMNX4uXWOCOMupW6kHqXkNpqZjhKZJqXgMwJtpo+LBjthWxzFCG7X69zfwmFtCAc8V7AYveXRkS8z1JpZMc+L/OqkzlJJ52cdantOeEygP/Mg6TXtD8Jzm1OphV7bLjZPDdKii9vqpT5mFng7nfqd8aW3F1O0p7Iwt92Ula1haAD3ZSzylv+U82V3Eqw1+0JCdJT2L8rmX4D";

    String authNRequestPassivePkiPost =
            "PHNhbWwycDpBdXRoblJlcXVlc3QgSXNQYXNzaXZlPSJ0cnVlIiBBc3NlcnRpb25Db25zdW1lclNlcnZpY2VVUkw9Imh0dHBzOi8vbG9jYWxob3N0Ojg5OTMvc2VydmljZXMvc2FtbC9zc28iIERlc3RpbmF0aW9uPSJodHRwczovL2xvY2FsaG9zdDo4OTkzL3NlcnZpY2VzL2lkcC9sb2dpbiIgSUQ9Il9hNTU2YjNjZi1iNmVjLTQwMzItYjUxNC1lZThmOGRjMmQyMTMiIElzc3VlSW5zdGFudD0iMjAxNS0xMC0yOVQxNzoyMzoyOC4wMDBaIiBQcm90b2NvbEJpbmRpbmc9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpiaW5kaW5nczpIVFRQLVBPU1QiIFZlcnNpb249IjIuMCIgeG1sbnM6c2FtbDJwPSJ1cm46b2FzaXM6bmFtZXM6dGM6U0FNTDoyLjA6cHJvdG9jb2wiPjxzYW1sMjpJc3N1ZXIgeG1sbnM6c2FtbDI9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDphc3NlcnRpb24iPmh0dHBzOi8vbG9jYWxob3N0Ojg5OTMvc2VydmljZXMvc2FtbDwvc2FtbDI6SXNzdWVyPjxzYW1sMnA6TmFtZUlEUG9saWN5IEFsbG93Q3JlYXRlPSJ0cnVlIiBGb3JtYXQ9InVybjpvYXNpczpuYW1lczp0YzpTQU1MOjIuMDpuYW1laWQtZm9ybWF0OnBlcnNpc3RlbnQiIFNQTmFtZVF1YWxpZmllcj0iaHR0cHM6Ly9sb2NhbGhvc3Q6ODk5My9zZXJ2aWNlcy9zYW1sIi8+PC9zYW1sMnA6QXV0aG5SZXF1ZXN0Pg==";

    IdpEndpoint idpEndpoint;

    String relayState;

    String requestCertificateAttributeName;

    StringBuffer requestURL;

    String samlConditionDateFormat;

    String signature;

    String signatureAlgorithm;

    String ssoSAMLResponse;

    public static Document readXml(InputStream is)
            throws SAXException, IOException, ParserConfigurationException {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();

        dbf.setValidating(false);
        dbf.setIgnoringComments(false);
        dbf.setIgnoringElementContentWhitespace(true);
        dbf.setNamespaceAware(true);

        DocumentBuilder db = dbf.newDocumentBuilder();
        db.setEntityResolver(new DOMUtils.NullResolver());

        return db.parse(is);
    }

    @Before
    public void setup() throws IOException, SecurityServiceException, ParserConfigurationException,
            SAXException {
        System.setProperty("org.codice.ddf.system.hostname", "localhost");
        System.setProperty("javax.net.ssl.keyStorePassword", "changeit");
        File jksFile = temporaryFolder.newFile("serverKeystore.jks");
        FileOutputStream jksOutStream = new FileOutputStream(jksFile);
        InputStream jksStream = IdpEndpointTest.class.getResourceAsStream("/serverKeystore.jks");
        IOUtils.copy(jksStream, jksOutStream);
        IOUtils.closeQuietly(jksStream);
        IOUtils.closeQuietly(jksOutStream);

        File signatureFile = temporaryFolder.newFile("signature.properties");
        FileOutputStream signatureOutStream = new FileOutputStream(signatureFile);
        InputStream signatureStream = IdpEndpointTest.class.getResourceAsStream(
                "/signature.properties");
        IOUtils.copy(signatureStream, signatureOutStream);
        IOUtils.closeQuietly(signatureStream);
        IOUtils.closeQuietly(signatureOutStream);

        File encryptionFile = temporaryFolder.newFile("encryption.properties");
        FileOutputStream encryptionOutStream = new FileOutputStream(encryptionFile);
        InputStream encryptionStream = IdpEndpointTest.class.getResourceAsStream(
                "/encryption.properties");
        IOUtils.copy(encryptionStream, encryptionOutStream);
        IOUtils.closeQuietly(encryptionStream);
        IOUtils.closeQuietly(encryptionOutStream);

        EncryptionService encryptionService = mock(EncryptionService.class);

        when(encryptionService.decrypt(anyString())).thenReturn("changeit");
        when(encryptionService.encrypt(anyString())).thenReturn("changeit");

        SecurityManager securityManager = mock(SecurityManager.class);
        Subject subject = mock(Subject.class);
        PrincipalCollection principalCollection = mock(PrincipalCollection.class);
        SecurityAssertion securityAssertion = mock(SecurityAssertion.class);
        SecurityToken securityToken = mock(SecurityToken.class);
        when(subject.getPrincipals()).thenReturn(principalCollection);
        when(principalCollection.asList()).thenReturn(Collections.singletonList(securityAssertion));
        when(securityAssertion.getSecurityToken()).thenReturn(securityToken);
        when(securityToken.getToken()).thenReturn(readDocument("/saml.xml").getDocumentElement());
        when(securityManager.getSubject(anyObject())).thenReturn(subject);

        System.setProperty("javax.net.ssl.keyStore", jksFile.getAbsolutePath());
        idpEndpoint = new IdpEndpoint(signatureFile.getAbsolutePath(),
                encryptionFile.getAbsolutePath(),
                encryptionService);
        idpEndpoint.setStrictSignature(true);
        idpEndpoint.init();
        idpEndpoint.setSpMetadata(Collections.singletonList(spMetadata));
        idpEndpoint.setSecurityManager(securityManager);
        PKIAuthenticationTokenFactory pkiAuthenticationTokenFactory =
                new PKIAuthenticationTokenFactory();
        pkiAuthenticationTokenFactory.setSignaturePropertiesPath(signatureFile.getAbsolutePath());
        pkiAuthenticationTokenFactory.init();
        idpEndpoint.setTokenFactory(pkiAuthenticationTokenFactory);
        idpEndpoint.cookieCache.cacheSamlAssertion("1",
                readDocument("/saml.xml").getDocumentElement());
        idpEndpoint.setExpirationTime(30);

        relayState = "ef95c04b-6c05-4d12-b65f-dd32fed8811e";
        requestCertificateAttributeName = "javax.servlet.request.X509Certificate";
        requestURL = new StringBuffer("https://www.example.com");
        samlConditionDateFormat = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
        signature = authNRequestGetSignature;
        signatureAlgorithm = "http://www.w3.org/2000/09/xmldsig#rsa-sha1";
        ssoSAMLResponse = "https://localhost:8993/services/saml/sso?SAMLResponse=";
    }

    @Test
    public void testShowPostLogin() throws WSSecurityException {
        idpEndpoint.setStrictSignature(false);
        String samlRequest = authNRequestPost;
        relayState = "94697cdc-e64f-4edf-b26a-52c14c2314dd";
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.isSecure()).thenReturn(true);

        Response response = idpEndpoint.showPostLogin(samlRequest, relayState, request);

        assertThat(response.getEntity()
                .toString(), containsString("SAMLRequest"));
        assertThat(response.getEntity()
                .toString(), containsString("RelayState"));
        assertThat(response.getEntity()
                .toString(), containsString("ACSURL"));
    }

    @Test
    public void testShowGetLogin() throws WSSecurityException {
        String samlRequest = authNRequestGet;
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.isSecure()).thenReturn(true);

        Response response = idpEndpoint.showGetLogin(samlRequest,
                relayState,
                signatureAlgorithm,
                signature,
                request);

        assertThat(response.getEntity()
                .toString(), containsString("SAMLRequest"));
        assertThat(response.getEntity()
                .toString(), containsString("RelayState"));
        assertThat(response.getEntity()
                .toString(), containsString("ACSURL"));
    }

    @Test
    public void testShowGetLoginNoRelayState() throws WSSecurityException {
        String samlRequest = authNRequestGet;
        idpEndpoint.setStrictSignature(false);
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.isSecure()).thenReturn(true);

        Response response = idpEndpoint.showGetLogin(samlRequest,
                null,
                signatureAlgorithm,
                signature,
                request);
        assertThat(response.getEntity()
                .toString(), containsString("SAMLRequest"));
        assertThat(response.getEntity()
                .toString(), containsString("ACSURL"));
    }

    @Test
    public void testShowGetLoginNotSecure() throws WSSecurityException {
        String samlRequest = authNRequestGet;
        HttpServletRequest request = mock(HttpServletRequest.class);

        Response response = idpEndpoint.showGetLogin(samlRequest,
                relayState,
                signatureAlgorithm,
                signature,
                request);

        assertThat(response.getEntity()
                .toString(), containsString("SAMLResponse"));
        assertThat(response.getEntity()
                .toString(), containsString("RelayState"));
    }

    @Test
    public void testRetrieveMetadata() throws WSSecurityException, CertificateEncodingException {
        Response response = idpEndpoint.retrieveMetadata();

        assertThat(response.getEntity()
                .toString(), containsString("IDPSSODescriptor"));
        assertThat(response.getEntity()
                .toString(), containsString("SingleLogoutService"));
        assertThat(response.getEntity()
                .toString(), containsString("SingleSignOnService"));
    }

    @Test
    public void testProcessLoginBasic() {
        String samlRequest = authNRequestGet;
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.isSecure()).thenReturn(true);
        when(request.getRequestURL()).thenReturn(requestURL);
        when(request.getAttribute(ContextPolicy.ACTIVE_REALM)).thenReturn("*");
        //admin:admin
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic YWRtaW46YWRtaW4=");

        Response response = idpEndpoint.processLogin(samlRequest,
                relayState,
                Idp.USER_PASS,
                signatureAlgorithm,
                signature,
                SamlProtocol.REDIRECT_BINDING,
                request);

        assertThat(response.getEntity()
                .toString(), containsString(ssoSAMLResponse));
        assertThat(response.getEntity()
                .toString(), containsString("RelayState="));
    }

    @Test
    public void testProcessLoginBasicNotSecure() {
        String samlRequest = authNRequestGet;
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.getRequestURL()).thenReturn(new StringBuffer("https://www.example.com"));
        when(request.getAttribute(ContextPolicy.ACTIVE_REALM)).thenReturn("*");
        //admin:admin
        when(request.getHeader(HttpHeaders.AUTHORIZATION)).thenReturn("Basic YWRtaW46YWRtaW4=");

        Response response = idpEndpoint.processLogin(samlRequest,
                relayState,
                Idp.USER_PASS,
                signatureAlgorithm,
                signature,
                SamlProtocol.REDIRECT_BINDING,
                request);

        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void testProcessLoginPki() throws CertificateEncodingException, WSSecurityException {
        String samlRequest = authNRequestGet;
        HttpServletRequest request = mock(HttpServletRequest.class);
        X509Certificate x509Certificate = mock(X509Certificate.class);

        when(request.isSecure()).thenReturn(true);
        when(request.getRequestURL()).thenReturn(requestURL);
        when(request.getAttribute(ContextPolicy.ACTIVE_REALM)).thenReturn("*");
        //dummy cert
        when((X509Certificate[]) request.getAttribute(requestCertificateAttributeName)).thenReturn(
                new X509Certificate[] {x509Certificate});
        when(x509Certificate.getEncoded()).thenReturn(new byte[48]);

        Response response = idpEndpoint.processLogin(samlRequest,
                relayState,
                Idp.PKI,
                signatureAlgorithm,
                signature,
                SamlProtocol.REDIRECT_BINDING,
                request);

        assertThat(response.getEntity()
                .toString(), containsString(ssoSAMLResponse));
        assertThat(response.getEntity()
                .toString(), containsString("RelayState="));
    }

    @Test
    public void testProcessLoginPkiPost() throws CertificateEncodingException {
        idpEndpoint.setStrictSignature(false);
        String samlRequest = authNRequestPkiPost;
        HttpServletRequest request = mock(HttpServletRequest.class);
        X509Certificate x509Certificate = mock(X509Certificate.class);

        when(request.isSecure()).thenReturn(true);
        when(request.getRequestURL()).thenReturn(requestURL);
        when(request.getAttribute(ContextPolicy.ACTIVE_REALM)).thenReturn("*");
        //dummy cert
        when((X509Certificate[]) request.getAttribute(requestCertificateAttributeName)).thenReturn(
                new X509Certificate[] {x509Certificate});
        when(x509Certificate.getEncoded()).thenReturn(new byte[48]);

        Response response = idpEndpoint.processLogin(samlRequest,
                relayState,
                Idp.PKI,
                null,
                null,
                SamlProtocol.POST_BINDING,
                request);

        assertThat(response.getEntity()
                .toString(), containsString("Form Submit"));
        assertThat(response.getEntity()
                .toString(), containsString("SAMLResponse"));
        assertThat(response.getEntity()
                .toString(), containsString("RelayState"));
    }

    @Test
    public void testProcessLoginGuest() throws CertificateEncodingException {
        String samlRequest = authNRequestGet;
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.isSecure()).thenReturn(true);
        when(request.getRequestURL()).thenReturn(requestURL);
        when(request.getAttribute(ContextPolicy.ACTIVE_REALM)).thenReturn("*");

        Response response = idpEndpoint.processLogin(samlRequest,
                relayState,
                Idp.GUEST,
                signatureAlgorithm,
                signature,
                SamlProtocol.REDIRECT_BINDING,
                request);

        assertThat(response.getEntity()
                .toString(), containsString(ssoSAMLResponse));
        assertThat(response.getEntity()
                .toString(), containsString("RelayState="));
    }

    @Test
    public void testShowGetLoginWithValidCookie()
            throws CertificateEncodingException, WSSecurityException {
        String samlRequest = authNRequestGet;
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie cookie = mock(Cookie.class);

        when(request.isSecure()).thenReturn(true);
        when(request.getRequestURL()).thenReturn(requestURL);
        when(request.getAttribute(ContextPolicy.ACTIVE_REALM)).thenReturn("*");
        when(request.getCookies()).thenReturn(new Cookie[] {cookie});
        when(cookie.getName()).thenReturn(IdpEndpoint.COOKIE);
        when(cookie.getValue()).thenReturn("1");

        Response response = idpEndpoint.showGetLogin(samlRequest,
                relayState,
                signatureAlgorithm,
                signature,
                request);

        assertThat(response.getEntity()
                .toString(), containsString(ssoSAMLResponse));
        assertThat(response.getEntity()
                .toString(), containsString("RelayState="));
    }

    @Test
    public void testShowGetLoginWithCookieAssertionAfterTimeBounds()
            throws CertificateEncodingException, WSSecurityException, SAXException, IOException,
            ParserConfigurationException {
        String samlRequest = authNRequestGet;
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie cookie = mock(Cookie.class);

        Calendar calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        calendar.add(Calendar.SECOND, -1);
        Date beforeNow = calendar.getTime();
        DateFormat dateFormat = new SimpleDateFormat(samlConditionDateFormat);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        Element assertionElement = readDocument("/saml.xml").getDocumentElement();

        //Change the NotOnOrAfter Date on the SAML Assertion to be before "now"
        assertionElement.getElementsByTagName("saml2:Conditions")
                .item(0)
                .getAttributes()
                .getNamedItem("NotOnOrAfter")
                .setNodeValue(dateFormat.format(beforeNow));

        when(request.isSecure()).thenReturn(true);
        when(request.getRequestURL()).thenReturn(requestURL);
        when(request.getAttribute(ContextPolicy.ACTIVE_REALM)).thenReturn("*");
        when(request.getCookies()).thenReturn(new Cookie[] {cookie});
        when(cookie.getName()).thenReturn(IdpEndpoint.COOKIE);
        when(cookie.getValue()).thenReturn("2");

        idpEndpoint.cookieCache.cacheSamlAssertion("2", assertionElement);
        assertNotNull(idpEndpoint.cookieCache.getSamlAssertion("2"));

        Response response = idpEndpoint.showGetLogin(samlRequest,
                relayState,
                signatureAlgorithm,
                signature,
                request);

        assertThat(response.getEntity()
                .toString(), containsString("<title>Login</title>"));
        assertNull(idpEndpoint.cookieCache.getSamlAssertion("2"));
    }

    @Test
    public void testFailedLogin() throws SecurityServiceException {
        String samlRequest = authNRequestGet;
        HttpServletRequest request = mock(HttpServletRequest.class);

        SecurityManager securityManager = mock(SecurityManager.class);
        when(securityManager.getSubject(anyObject())).thenThrow(new SecurityServiceException("test"));
        idpEndpoint.setSecurityManager(securityManager);

        when(request.isSecure()).thenReturn(true);
        when(request.getRequestURL()).thenReturn(requestURL);
        when(request.getAttribute(ContextPolicy.ACTIVE_REALM)).thenReturn("*");

        Response response = idpEndpoint.processLogin(samlRequest,
                relayState,
                Idp.GUEST,
                signatureAlgorithm,
                signature,
                SamlProtocol.REDIRECT_BINDING,
                request);

        assertThat(response.getStatus(), is(401));
    }

    @Test
    public void testExpiredLoginCookie() throws SecurityServiceException, WSSecurityException {
        String samlRequest = authNRequestGet;
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie cookie = mock(Cookie.class);

        SecurityManager securityManager = mock(SecurityManager.class);
        when(securityManager.getSubject(anyObject())).thenThrow(new SecurityServiceException("test"));
        idpEndpoint.setSecurityManager(securityManager);

        when(request.isSecure()).thenReturn(true);
        when(request.getRequestURL()).thenReturn(requestURL);
        when(request.getAttribute(ContextPolicy.ACTIVE_REALM)).thenReturn("*");
        when(request.getCookies()).thenReturn(new Cookie[] {cookie});
        when(cookie.getName()).thenReturn(IdpEndpoint.COOKIE);
        when(cookie.getValue()).thenReturn("2");

        Response response = idpEndpoint.showGetLogin(samlRequest,
                relayState,
                signatureAlgorithm,
                signature,
                request);

        //the only cookie that should exist is the "1" cookie so "2" should send us to the login webapp
        assertThat(response.getEntity()
                .toString(), containsString("<title>Login</title>"));
    }

    @Test
    public void testLoginForceAuthnCookie()
            throws SecurityServiceException, WSSecurityException, IOException {
        String samlRequest = RestSecurity.deflateAndBase64Encode(authNRequestGetForce);
        HttpServletRequest request = mock(HttpServletRequest.class);
        Cookie cookie = mock(Cookie.class);

        SecurityManager securityManager = mock(SecurityManager.class);
        when(securityManager.getSubject(anyObject())).thenThrow(new SecurityServiceException("test"));
        idpEndpoint.setSecurityManager(securityManager);
        idpEndpoint.setStrictSignature(false);

        when(request.isSecure()).thenReturn(true);
        when(request.getRequestURL()).thenReturn(requestURL);
        when(request.getAttribute(ContextPolicy.ACTIVE_REALM)).thenReturn("*");
        when(request.getCookies()).thenReturn(new Cookie[] {cookie});
        when(cookie.getName()).thenReturn(IdpEndpoint.COOKIE);
        when(cookie.getValue()).thenReturn("1");

        Response response = idpEndpoint.showGetLogin(samlRequest,
                relayState,
                signatureAlgorithm,
                signature,
                request);

        assertThat(response.getEntity()
                .toString(), containsString("<title>Login</title>"));
    }

    @Test
    public void testPassiveLoginPki()
            throws SecurityServiceException, WSSecurityException, CertificateEncodingException {
        String samlRequest = authNRequestPassivePkiGet;
        HttpServletRequest request = mock(HttpServletRequest.class);
        X509Certificate x509Certificate = mock(X509Certificate.class);

        idpEndpoint.setStrictSignature(false);

        when(request.isSecure()).thenReturn(true);
        when(request.getRequestURL()).thenReturn(requestURL);
        when(request.getAttribute(ContextPolicy.ACTIVE_REALM)).thenReturn("*");
        //dummy cert
        when((X509Certificate[]) request.getAttribute(requestCertificateAttributeName)).thenReturn(
                new X509Certificate[] {x509Certificate});
        when(x509Certificate.getEncoded()).thenReturn(new byte[48]);

        Response response = idpEndpoint.showGetLogin(samlRequest,
                relayState,
                signatureAlgorithm,
                signature,
                request);

        assertThat(response.getEntity()
                .toString(), containsString(ssoSAMLResponse));
        assertThat(response.getEntity()
                .toString(), containsString("RelayState="));
    }

    @Test
    public void testPassiveLoginPkiPost()
            throws SecurityServiceException, WSSecurityException, CertificateEncodingException {
        String samlRequest = authNRequestPassivePkiPost;
        HttpServletRequest request = mock(HttpServletRequest.class);
        X509Certificate x509Certificate = mock(X509Certificate.class);

        idpEndpoint.setStrictSignature(false);

        when(request.isSecure()).thenReturn(true);
        when(request.getRequestURL()).thenReturn(requestURL);
        when(request.getAttribute(ContextPolicy.ACTIVE_REALM)).thenReturn("*");
        //dummy cert
        when((X509Certificate[]) request.getAttribute(requestCertificateAttributeName)).thenReturn(
                new X509Certificate[] {x509Certificate});
        when(x509Certificate.getEncoded()).thenReturn(new byte[48]);

        Response response = idpEndpoint.showPostLogin(samlRequest, relayState, request);

        assertThat(response.getEntity()
                .toString(), containsString("Form Submit"));
        assertThat(response.getEntity()
                .toString(), containsString("SAMLResponse"));
        assertThat(response.getEntity()
                .toString(), containsString("RelayState"));
    }

    @Test
    public void testPassiveLoginPkiFail()
            throws SecurityServiceException, WSSecurityException, CertificateEncodingException,
            IOException {
        String samlRequest = authNRequestPassivePkiGet;
        HttpServletRequest request = mock(HttpServletRequest.class);
        X509Certificate x509Certificate = mock(X509Certificate.class);

        SecurityManager securityManager = mock(SecurityManager.class);
        when(securityManager.getSubject(anyObject())).thenThrow(new SecurityServiceException("test"));
        idpEndpoint.setSecurityManager(securityManager);
        idpEndpoint.setStrictSignature(false);

        when(request.isSecure()).thenReturn(true);
        when(request.getRequestURL()).thenReturn(requestURL);
        when(request.getAttribute(ContextPolicy.ACTIVE_REALM)).thenReturn("*");
        //dummy cert
        when((X509Certificate[]) request.getAttribute(requestCertificateAttributeName)).thenReturn(
                new X509Certificate[] {x509Certificate});
        when(x509Certificate.getEncoded()).thenReturn(new byte[48]);

        Response response = idpEndpoint.showGetLogin(samlRequest,
                relayState,
                signatureAlgorithm,
                signature,
                request);
        String responseStr = StringUtils.substringBetween(response.getEntity()
                .toString(), "SAMLResponse=", "RelayState");
        responseStr = URLDecoder.decode(responseStr, "UTF-8");
        responseStr = RestSecurity.inflateBase64(responseStr);

        //the only cookie that should exist is the "1" cookie so "2" should send us to the login webapp
        assertThat(responseStr, containsString("status:AuthnFailed"));
    }

    @Test
    public void testPassiveLoginPkiSignatureErrorPost()
            throws SecurityServiceException, WSSecurityException, CertificateEncodingException,
            IOException {
        String samlRequest = authNRequestPassivePkiPost;
        HttpServletRequest request = mock(HttpServletRequest.class);
        X509Certificate x509Certificate = mock(X509Certificate.class);

        SecurityManager securityManager = mock(SecurityManager.class);
        when(securityManager.getSubject(anyObject())).thenThrow(new SecurityServiceException("test"));
        idpEndpoint.setSecurityManager(securityManager);

        when(request.isSecure()).thenReturn(true);
        when(request.getRequestURL()).thenReturn(requestURL);
        when(request.getAttribute(ContextPolicy.ACTIVE_REALM)).thenReturn("*");
        //dummy cert
        when((X509Certificate[]) request.getAttribute(requestCertificateAttributeName)).thenReturn(
                new X509Certificate[] {x509Certificate});
        when(x509Certificate.getEncoded()).thenReturn(new byte[48]);

        Response response = idpEndpoint.showPostLogin(samlRequest, relayState, request);

        assertThat(response.getStatus(), is(500));
    }

    @Test
    public void testPassiveLoginPkiUnsupportedPost()
            throws SecurityServiceException, WSSecurityException, CertificateEncodingException,
            IOException {
        String samlRequest = authNRequestPassivePkiPost;
        HttpServletRequest request = mock(HttpServletRequest.class);
        X509Certificate x509Certificate = mock(X509Certificate.class);

        Subject subject = mock(Subject.class);
        PrincipalCollection principalCollection = mock(PrincipalCollection.class);
        SecurityAssertion securityAssertion = mock(SecurityAssertion.class);
        SecurityToken securityToken = mock(SecurityToken.class);
        SecurityManager securityManager = mock(SecurityManager.class);

        when(subject.getPrincipals()).thenReturn(principalCollection);
        when(principalCollection.asList()).thenReturn(Collections.singletonList(securityAssertion));
        when(securityAssertion.getSecurityToken()).thenReturn(securityToken);
        //this mock element is what will cause the signature error
        when(securityToken.getToken()).thenReturn(mock(Element.class));
        when(securityManager.getSubject(anyObject())).thenReturn(subject);
        idpEndpoint.setSecurityManager(securityManager);
        idpEndpoint.setStrictSignature(false);

        when(request.isSecure()).thenReturn(true);
        when(request.getRequestURL()).thenReturn(requestURL);
        when(request.getAttribute(ContextPolicy.ACTIVE_REALM)).thenReturn("*");
        //dummy cert
        when((X509Certificate[]) request.getAttribute(requestCertificateAttributeName)).thenReturn(
                new X509Certificate[] {x509Certificate});
        when(x509Certificate.getEncoded()).thenReturn(new byte[48]);

        Response response = idpEndpoint.showPostLogin(samlRequest, relayState, request);
        String responseStr = StringUtils.substringBetween(response.getEntity()
                .toString(), "SAMLResponse\" value=\"", "\" />");
        responseStr = URLDecoder.decode(responseStr, "UTF-8");
        responseStr = new String(Base64.decodeBase64(responseStr));

        //the only cookie that should exist is the "1" cookie so "2" should send us to the login webapp
        assertThat(responseStr, containsString("status:RequestUnsupported"));
    }

    @Test
    public void testPassiveLoginPkiUnsupported()
            throws SecurityServiceException, WSSecurityException, CertificateEncodingException,
            IOException {
        String samlRequest = authNRequestPassivePkiGet;
        HttpServletRequest request = mock(HttpServletRequest.class);
        X509Certificate x509Certificate = mock(X509Certificate.class);

        Subject subject = mock(Subject.class);
        PrincipalCollection principalCollection = mock(PrincipalCollection.class);
        SecurityAssertion securityAssertion = mock(SecurityAssertion.class);
        SecurityToken securityToken = mock(SecurityToken.class);
        SecurityManager securityManager = mock(SecurityManager.class);
        when(subject.getPrincipals()).thenReturn(principalCollection);
        when(principalCollection.asList()).thenReturn(Collections.singletonList(securityAssertion));
        when(securityAssertion.getSecurityToken()).thenReturn(securityToken);
        //this mock element is what will cause the signature error
        when(securityToken.getToken()).thenReturn(mock(Element.class));
        when(securityManager.getSubject(anyObject())).thenReturn(subject);
        idpEndpoint.setSecurityManager(securityManager);
        idpEndpoint.setStrictSignature(false);

        when(request.isSecure()).thenReturn(true);
        when(request.getRequestURL()).thenReturn(requestURL);
        when(request.getAttribute(ContextPolicy.ACTIVE_REALM)).thenReturn("*");
        //dummy cert
        when((X509Certificate[]) request.getAttribute(requestCertificateAttributeName)).thenReturn(
                new X509Certificate[] {x509Certificate});
        when(x509Certificate.getEncoded()).thenReturn(new byte[48]);

        Response response = idpEndpoint.showGetLogin(samlRequest,
                relayState,
                signatureAlgorithm,
                signature,
                request);
        String responseStr = StringUtils.substringBetween(response.getEntity()
                .toString(), "SAMLResponse=", "RelayState");
        responseStr = URLDecoder.decode(responseStr, "UTF-8");
        responseStr = RestSecurity.inflateBase64(responseStr);

        //the only cookie that should exist is the "1" cookie so "2" should send us to the login webapp
        assertThat(responseStr, containsString("status:RequestUnsupported"));
    }

    @Test
    public void testUnsupportedAuthMethod() {
        String samlRequest = authNRequestGet;
        HttpServletRequest request = mock(HttpServletRequest.class);

        when(request.isSecure()).thenReturn(true);
        when(request.getRequestURL()).thenReturn(requestURL);
        when(request.getAttribute(ContextPolicy.ACTIVE_REALM)).thenReturn("*");

        Response response = idpEndpoint.processLogin(samlRequest,
                relayState,
                "notsupported",
                signatureAlgorithm,
                signature,
                SamlProtocol.Binding.SOAP.getUri(),
                request);

        assertThat(response.getStatus(), is(400));
    }

    private Document readDocument(String name)
            throws SAXException, IOException, ParserConfigurationException {
        try (InputStream inStream = getClass().getResourceAsStream(name)) {
            return readXml(inStream);
        }
    }
}
