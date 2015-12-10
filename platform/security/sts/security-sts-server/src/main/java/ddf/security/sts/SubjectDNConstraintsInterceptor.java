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
 */
package ddf.security.sts;

import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.cxf.interceptor.Fault;
import org.apache.cxf.interceptor.security.AccessDeniedException;
import org.apache.cxf.message.Message;
import org.apache.cxf.phase.AbstractPhaseInterceptor;
import org.apache.cxf.phase.Phase;
import org.apache.cxf.ws.security.wss4j.AbstractWSS4JInterceptor;
import org.apache.wss4j.dom.handler.WSHandlerConstants;
import org.eclipse.jetty.server.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SubjectDNConstraintsInterceptor extends AbstractPhaseInterceptor<Message> {

    private Logger logger = LoggerFactory.getLogger(SubjectDNConstraintsInterceptor.class);

    public SubjectDNConstraintsInterceptor() {
        super(Phase.PRE_INVOKE);
        addAfter(AbstractWSS4JInterceptor.class.getName());
    }

    /**
     * Return true if the provided certificate matches the regular expression
     * defined in the Subject DN Certificate Constraints.
     *
     * @param message
     * @return true if the certificate matches the constraints
     */
    @Override
    public void handleMessage(Message message) throws Fault {
        if (message != null) {
            String subjectDNConstraints = (String) message
                    .get(WSHandlerConstants.SIG_SUBJECT_CERT_CONSTRAINTS);
            if (subjectDNConstraints == null) {
                logger.warn(
                        "No Subject DN Certificate Constraints were defined. This could be a security issue");
            } else {
                Collection<Pattern> subjectDNPatterns = setSubjectDNPatterns(subjectDNConstraints);
                X509Certificate[] cert;
                cert = ((X509Certificate[]) (((Request) message.get("HTTP.REQUEST")).getAttribute(
                        "javax.servlet.request.X509Certificate")));
                if (cert == null) {
                    throw new AccessDeniedException("No certificate provided.");
                }
                if (!(matches(cert[0], subjectDNPatterns))) {
                    logger.warn("Certificate does not match Subject DN Certificate Constraints");
                    throw new AccessDeniedException(
                            "Certificate DN does not match allowed pattern(s).");
                }
            }
        }
    }

    public Collection<Pattern> setSubjectDNPatterns(String subjectDNConstraints) {
        ArrayList<Pattern> subjectDNPatterns = new ArrayList<>();
        if(subjectDNConstraints != null) {
            String[] patterns = subjectDNConstraints.split(",");
            for (String pattern : patterns) {
                Pattern p = Pattern.compile(pattern);
                subjectDNPatterns.add(p);
            }
        }
        return subjectDNPatterns;
    }

    /**
     * Checks the certificate against the list of regular expressions given.
     * Only matching one of the regular expressions is necessary.
     *
     * @param cert
     * @param subjectDNPatterns
     * @return true if the certificate matches the constraints
     */
    protected boolean matches(final X509Certificate cert,
            final Collection<Pattern> subjectDNPatterns) {
        if (subjectDNPatterns == null || subjectDNPatterns.isEmpty()) {
            logger.warn(
                    "No Subject DN Certificate Constraints were defined. This could be a security issue");
        } else {
            if (cert == null) {
                logger.debug("The certificate is null so no constraints matching was possible");
                return false;
            }
            String subjectName = cert.getSubjectX500Principal().getName();

            for (Pattern subjectDNPattern : subjectDNPatterns) {
                final Matcher matcher = subjectDNPattern.matcher(subjectName);
                if (matcher.matches()) {
                    logger.debug("Subject DN " + subjectName + " matches with pattern "
                            + subjectDNPattern);
                    return true;
                }
            }
            return false;
        }

        return true;
    }

}
