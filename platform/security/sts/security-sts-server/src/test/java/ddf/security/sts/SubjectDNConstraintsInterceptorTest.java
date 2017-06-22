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
package ddf.security.sts;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.regex.Pattern;

import javax.security.auth.x500.X500Principal;

import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Message;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.eclipse.jetty.server.Request;
import org.junit.Before;
import org.junit.Test;

public class SubjectDNConstraintsInterceptorTest {

    SubjectDNConstraintsInterceptor subjectDNConstraintsInterceptor;

    @Before
    public void setup() {
        subjectDNConstraintsInterceptor = new SubjectDNConstraintsInterceptor();
    }

    @Test
    public void testHandleMessageForAllowedDn() {
        Message message = getMessage("CN=cert", "CN=cert");

        subjectDNConstraintsInterceptor.handleMessage(message);
    }

    @Test
    public void testHandleMessageCommaPatternForAllowedDn() {
        Message message = getMessage("CN=cert,OU=unit|CN=stuff,OU=more", "CN=cert,OU=unit");

        subjectDNConstraintsInterceptor.handleMessage(message);
    }

    @Test
    public void testHandleMessageNoPattern() {
        Message message = getMessage(null, "CN=cert");

        subjectDNConstraintsInterceptor.handleMessage(message);
    }

    @Test
    public void testHandleMessageEmptyPattern() {
        Message message = getMessage("", "CN=cert");

        subjectDNConstraintsInterceptor.handleMessage(message);
    }

    @Test(expected = AccessDeniedException.class)
    public void testHandleMessageNoMatch() {
        Message message = getMessage("CN=cert", "CN=certbad");

        subjectDNConstraintsInterceptor.handleMessage(message);
    }

    @Test(expected = AccessDeniedException.class)
    public void testHandleMessageNoCert() {
        Message message = mock(Message.class);
        when(message.get(WSHandlerConstants.SIG_SUBJECT_CERT_CONSTRAINTS)).thenReturn("CN=certbad");
        Request request = mock(Request.class);
        when(message.get("HTTP.REQUEST")).thenReturn(request);
        when(request.getAttribute("javax.servlet.request.X509Certificate")).thenReturn(null);

        subjectDNConstraintsInterceptor.handleMessage(message);
    }

    @Test
    public void testSetSubjectDNPatterns() {
        Collection<Pattern> patterns = subjectDNConstraintsInterceptor.setSubjectDNPatterns("");
        assertThat(patterns.size(), is(0));
        patterns = subjectDNConstraintsInterceptor.setSubjectDNPatterns(".*");
        assertThat(patterns.size(), is(1));
        patterns = subjectDNConstraintsInterceptor.setSubjectDNPatterns(".*|");
        assertThat(patterns.size(), is(1));
        patterns = subjectDNConstraintsInterceptor.setSubjectDNPatterns(".*|.*local.*|.+");
        assertThat(patterns.size(), is(3));
    }

    private Message getMessage(String constraint, String principal) {
        Message message = mock(Message.class);
        when(message.get(WSHandlerConstants.SIG_SUBJECT_CERT_CONSTRAINTS)).thenReturn(constraint);
        Request request = mock(Request.class);
        when(message.get("HTTP.REQUEST")).thenReturn(request);
        X509Certificate x509Certificate = mock(X509Certificate.class);
        X500Principal x500Principal = new X500Principal(principal);
        when(x509Certificate.getSubjectX500Principal()).thenReturn(x500Principal);
        when(request.getAttribute("javax.servlet.request.X509Certificate")).thenReturn(new X509Certificate[] {
                x509Certificate});
        return message;
    }
}
