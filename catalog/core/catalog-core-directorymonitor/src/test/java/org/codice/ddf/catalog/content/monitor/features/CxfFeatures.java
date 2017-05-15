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
package org.codice.ddf.catalog.content.monitor.features;

import static org.ops4j.pax.exam.CoreOptions.maven;
import static org.ops4j.pax.exam.karaf.options.KarafDistributionOption.features;

import java.util.Arrays;

import org.ops4j.pax.exam.Option;

public class CxfFeatures {
    public enum CxfFeature {
        CXF_SPECS("cxf-specs"), CXF_JAXB("cxf-jaxb"), CXF_ABDERA("cxf-abdera"), WSS4J("wss4j"), CXF_CORE(
                "cxf-core"), CXF_COMMANDS("cxf-commands"), CXF_WSDL("cxf-wsdl"), CXF_WS_POLICY(
                "cxf-ws-policy"), CXF_WS_ADDR("cxf-ws-addr"), CXF_WS_RM("cxf-ws-rm"), CXF_WS_MEX(
                "cxf-ws-mex"), CXF_WS_SECURITY("cxf-ws-security"), CXF_RT_SECURITY("cxf-rt-security"), CXF_RT_SECURITY_SAML(
                "cxf-rt-security-saml"), CXF_HTTP_CLIENT("cxf-http-client"), CXF_HTTP("cxf-http"), CXF_HTTP_PROVIDER(
                "cxf-http-provider"), CXF_HTTP_JETTY("cxf-http-jetty"), CXF_HTTP_ASYNC(
                "cxf-http-async"), CXF_HTTP_NETTY_CLIENT("cxf-http-netty-client"), CXF_HTTP_NETTY_SERVER(
                "cxf-http-netty-server"), CXF_HTTP_UNDERTOW("cxf-http-undertow"), CXF_BINDINGS_SOAP(
                "cxf-bindings-soap"), CXF_JAXWS("cxf-jaxws"), CXF_JAXRS("cxf-jaxrs"), CXF_RS_SECURITY_XML(
                "cxf-rs-security-xml"), CXF_RS_SECURITY_SSO_SAML("cxf-rs-security-sso-saml"), CXF_RS_SECURITY_CORS(
                "cxf-rs-security-cors"), CXF_RS_SECURITY_OAUTH("cxf-rs-security-oauth"), CXF_RS_SECURITY_JOSE(
                "cxf-rs-security-jose"), CXF_RS_SECURITY_OAUTH2("cxf-rs-security-oauth2"), CXF_JACKSON(
                "cxf-jackson"), CXF_JSR_JSON("cxf-jsr-json"), CXF_TRACING_BRAVE("cxf-tracing-brave"), CXF_RS_DESCRIPTION_SWAGGER2(
                "cxf-rs-description-swagger2"), CXF_DATABINDING_AEGIS("cxf-databinding-aegis"), CXF_DATABINDING_JAXB(
                "cxf-databinding-jaxb"), CXF_FEATURES_CLUSTERING("cxf-features-clustering"), CXF_FEATURES_LOGGING(
                "cxf-features-logging"), CXF_FEATURES_THROTTLING("cxf-features-throttling"), CXF_FEATURES_METRICS(
                "cxf-features-metrics"), CXF_BINDINGS_CORBA("cxf-bindings-corba"), CXF_BINDINGS_COLOC(
                "cxf-bindings-coloc"), CXF_TRANSPORTS_LOCAL("cxf-transports-local"), CXF_TRANSPORTS_JMS(
                "cxf-transports-jms"), CXF_TRANSPORTS_UDP("cxf-transports-udp"), CXF_TRANSPORTS_WEBSOCKET_CLIENT(
                "cxf-transports-websocket-client"), CXF_TRANSPORTS_WEBSOCKET_SERVER(
                "cxf-transports-websocket-server"), CXF_JAVASCRIPT("cxf-javascript"), CXF_FRONTEND_JAVASCRIPT(
                "cxf-frontend-javascript"), CXF_XJC_RUNTIME("cxf-xjc-runtime"), CXF_TOOLS(
                "cxf-tools"), CXF("cxf"), CXF_STS("cxf-sts"), CXF_WSN_API("cxf-wsn-api"), CXF_WSN(
                "cxf-wsn"), CXF_WS_DISCOVERY_API("cxf-ws-discovery-api"), CXF_WS_DISCOVERY(
                "cxf-ws-discovery"), CXF_BEAN_VALIDATION_CORE("cxf-bean-validation-core"), CXF_BEAN_VALIDATION(
                "cxf-bean-validation"), CXF_JAXRS_CDI("cxf-jaxrs-cdi");

        private String featureName;

        CxfFeature(String featureName) {
            this.featureName = featureName;
        }

        @Override
        public String toString() {
            return featureName;
        }
    }

    public static Option cxfFeatures(CxfFeature... features) {
        String[] featureStrings = Arrays.stream(features)
                .map(Enum::toString)
                .toArray(String[]::new);

        return features(maven().groupId("org.apache.cxf.karaf")
                .artifactId("apache-cxf")
                .classifier("features")
                .type("xml")
                .versionAsInProject(), featureStrings);
    }
}
