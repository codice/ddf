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

public class CamelFeatures {
    public enum CamelFeature {
        XML_SPECS_API("xml-specs-api"), CAMEL("camel"), CAMEL_CORE("camel-core"), CAMEL_CATALOG(
                "camel-catalog"), CAMEL_BLUEPRINT("camel-blueprint"), CAMEL_SPRING("camel-spring"), CAMEL_SCR(
                "camel-scr"), CAMEL_AHC("camel-ahc"), CAMEL_AHC_WS("camel-ahc-ws"), CAMEL_AMQP(
                "camel-amqp"), CAMEL_APNS("camel-apns"), CAMEL_ASTERISK("camel-asterisk"), CAMEL_ATMOSPHERE_WEBSOCKET(
                "camel-atmosphere-websocket"), CAMEL_ATOM("camel-atom"), CAMEL_AVRO("camel-avro"), CAMEL_AWS(
                "camel-aws"), CAMEL_AZURE("camel-azure"), CAMEL_BAM("camel-bam"), CAMEL_BASE64(
                "camel-base64"), CAMEL_BEAN_VALIDATOR("camel-bean-validator"), CAMEL_BEANIO(
                "camel-beanio"), CAMEL_BEANSTALK("camel-beanstalk"), CAMEL_BARCODE("camel-barcode"), CAMEL_BINDY(
                "camel-bindy"), CAMEL_BOON("camel-boon"), CAMEL_BOX("camel-box"), CAMEL_BRAINTREE(
                "camel-braintree"), CAMEL_CACHE("camel-cache"), CAMEL_CASSANDRAQL(
                "camel-cassandraql"), CAMEL_CASTOR("camel-castor"), CAMEL_CDI("camel-cdi"), CAMEL_CHRONICLE(
                "camel-chronicle"), CAMEL_CHUNK("camel-chunk"), CAMEL_CM_SMS("camel-cm-sms"), CAMEL_CMIS(
                "camel-cmis"), CAMEL_COAP("camel-coap"), CAMEL_COMETD("camel-cometd"), CAMEL_CONSUL(
                "camel-consul"), CAMEL_CONTEXT("camel-context"), CAMEL_COUCHDB("camel-couchdb"), CAMEL_COUCHBASE(
                "camel-couchbase"), CAMEL_CRYPTO("camel-crypto"), CAMEL_CSV("camel-csv"), CAMEL_CXF(
                "camel-cxf"), CAMEL_DIGITALOCEAN("camel-digitalocean"), CAMEL_DISRUPTOR(
                "camel-disruptor"), CAMEL_DNS("camel-dns"), CAMEL_DRILL("camel-drill"), CAMEL_DOZER(
                "camel-dozer"), CAMEL_DROPBOX("camel-dropbox"), CAMEL_EHCACHE("camel-ehcache"), CAMEL_ELSQL(
                "camel-elsql"), CAMEL_ELASTICSEARCH("camel-elasticsearch"), CAMEL_ELASTICSEARCH5(
                "camel-elasticsearch5"), CAMEL_ETCD("camel-etcd"), CAMEL_EVENTADMIN(
                "camel-eventadmin"), CAMEL_EXEC("camel-exec"), CAMEL_FACEBOOK("camel-facebook"), CAMEL_FLATPACK(
                "camel-flatpack"), CAMEL_FOP("camel-fop"), CAMEL_FREEMARKER("camel-freemarker"), CAMEL_FTP(
                "camel-ftp"), CAMEL_GANGLIA("camel-ganglia"), CAMEL_GEOCODER("camel-geocoder"), CAMEL_GIT(
                "camel-git"), CAMEL_GITHUB("camel-github"), CAMEL_GOOGLE_CALENDAR(
                "camel-google-calendar"), CAMEL_GOOGLE_DRIVE("camel-google-drive"), CAMEL_GOOGLE_MAIL(
                "camel-google-mail"), CAMEL_GOOGLE_PUBSUB("camel-google-pubsub"), CAMEL_GRAPE(
                "camel-grape"), CAMEL_GROOVY("camel-groovy"), CAMEL_GRPC("camel-grpc"), CAMEL_GSON(
                "camel-gson"), CAMEL_GUAVA_EVENTBUS("camel-guava-eventbus"), CAMEL_GUICE(
                "camel-guice"), CAMEL_HAWTDB("camel-hawtdb"), CAMEL_HAZELCAST("camel-hazelcast"), CAMEL_HDFS(
                "camel-hdfs"), CAMEL_HDFS2("camel-hdfs2"), CAMEL_HIPCHAT("camel-hipchat"), CAMEL_HESSIAN(
                "camel-hessian"), CAMEL_HL7("camel-hl7"), CAMEL_HTTP("camel-http"), CAMEL_HTTP4(
                "camel-http4"), CAMEL_HYSTRIX("camel-hystrix"), CAMEL_IBATIS("camel-ibatis"), CAMEL_ICAL(
                "camel-ical"), CAMEL_IGNITE("camel-ignite"), CAMEL_INFINISPAN("camel-infinispan"), CAMEL_INFLUXDB(
                "camel-influxdb"), CAMEL_IRC("camel-irc"), CAMEL_IRONMQ("camel-ironmq"), CAMEL_JACKSON(
                "camel-jackson"), CAMEL_JACKSONXML("camel-jacksonxml"), CAMEL_JASYPT("camel-jasypt"), CAMEL_JAXB(
                "camel-jaxb"), CAMEL_JBPM("camel-jbpm"), CAMEL_JCACHE("camel-jcache"), CAMEL_JCLOUDS(
                "camel-jclouds"), CAMEL_JCR("camel-jcr"), CAMEL_JDBC("camel-jdbc"), CAMEL_JETTY9(
                "camel-jetty9"), CAMEL_JETTY("camel-jetty"), CAMEL_JGROUPS("camel-jgroups"), CAMEL_JIBX(
                "camel-jibx"), CAMEL_JING("camel-jing"), CAMEL_JMS("camel-jms"), CAMEL_JMX(
                "camel-jmx"), CAMEL_JOLT("camel-jolt"), CAMEL_JOHNZON("camel-johnzon"), CAMEL_JOSQL(
                "camel-josql"), CAMEL_JPA("camel-jpa"), CAMEL_JSCH("camel-jsch"), CAMEL_JSONPATH(
                "camel-jsonpath"), CAMEL_JT400("camel-jt400"), CAMEL_JUEL("camel-juel"), CAMEL_JXPATH(
                "camel-jxpath"), CAMEL_KAFKA("camel-kafka"), CAMEL_KESTREL("camel-kestrel"), CAMEL_KRATI(
                "camel-krati"), CAMEL_KUBERNETES("camel-kubernetes"), CAMEL_LDAP("camel-ldap"), CAMEL_LINKEDIN(
                "camel-linkedin"), CAMEL_LEVELDB("camel-leveldb"), CAMEL_LUCENE("camel-lucene"), CAMEL_LUMBERJACK(
                "camel-lumberjack"), CAMEL_LZF("camel-lzf"), CAMEL_MAIL("camel-mail"), CAMEL_METRICS(
                "camel-metrics"), CAMEL_MINA("camel-mina"), CAMEL_MINA2("camel-mina2"), CAMEL_MLLP(
                "camel-mllp"), CAMEL_MONGODB("camel-mongodb"), CAMEL_MONGODB3("camel-mongodb3"), CAMEL_MONGODB_GRIDFS(
                "camel-mongodb-gridfs"), CAMEL_MQTT("camel-mqtt"), CAMEL_MSV("camel-msv"), CAMEL_MUSTACHE(
                "camel-mustache"), CAMEL_MVEL("camel-mvel"), CAMEL_MYBATIS("camel-mybatis"), CAMEL_NAGIOS(
                "camel-nagios"), CAMEL_NATS("camel-nats"), CAMEL_NETTY("camel-netty"), CAMEL_NETTY_HTTP(
                "camel-netty-http"), CAMEL_NETTY4("camel-netty4"), CAMEL_NETTY4_HTTP(
                "camel-netty4-http"), CAMEL_OGNL("camel-ognl"), CAMEL_OLINGO2("camel-olingo2"), CAMEL_OPENSHIFT(
                "camel-openshift"), CAMEL_OPTAPLANNER("camel-optaplanner"), CAMEL_OPENSTACK(
                "camel-openstack"), CAMEL_PAHO("camel-paho"), CAMEL_PAXLOGGING("camel-paxlogging"), CAMEL_PDF(
                "camel-pdf"), CAMEL_PGEVENT("camel-pgevent"), CAMEL_PRINTER("camel-printer"), CAMEL_PROTOBUF(
                "camel-protobuf"), CAMEL_QUARTZ("camel-quartz"), CAMEL_QUARTZ2("camel-quartz2"), CAMEL_QUICKFIX(
                "camel-quickfix"), CAMEL_RABBITMQ("camel-rabbitmq"), CAMEL_REACTIVE_STREAMS(
                "camel-reactive-streams"), CAMEL_RESTLET("camel-restlet"), CAMEL_RESTLET_JACKSON(
                "camel-restlet-jackson"), CAMEL_RESTLET_GSON("camel-restlet-gson"), CAMEL_RMI(
                "camel-rmi"), CAMEL_ROUTEBOX("camel-routebox"), CAMEL_RSS("camel-rss"), CAMEL_RX(
                "camel-rx"), CAMEL_SAP_NETWEAVER("camel-sap-netweaver"), CAMEL_SALESFORCE(
                "camel-salesforce"), CAMEL_SAXON("camel-saxon"), CAMEL_SCALA("camel-scala"), CAMEL_SCHEMATRON(
                "camel-schematron"), CAMEL_SCRIPT_JRUBY("camel-script-jruby"), CAMEL_SCRIPT_JAVASCRIPT(
                "camel-script-javascript"), CAMEL_SCRIPT_GROOVY("camel-script-groovy"), CAMEL_SCRIPT(
                "camel-script"), CAMEL_SERVICENOW("camel-servicenow"), CAMEL_SERVLET("camel-servlet"), CAMEL_SERVLETLISTENER(
                "camel-servletlistener"), CAMEL_SHIRO("camel-shiro"), CAMEL_SIP("camel-sip"), CAMEL_SJMS(
                "camel-sjms"), CAMEL_SJMS2("camel-sjms2"), CAMEL_SLACK("camel-slack"), CAMEL_SMPP(
                "camel-smpp"), CAMEL_SNAKEYAML("camel-snakeyaml"), CAMEL_SNMP("camel-snmp"), CAMEL_SOAP(
                "camel-soap"), CAMEL_SOLR("camel-solr"), CAMEL_SPARK_REST("camel-spark-rest"), CAMEL_SPLUNK(
                "camel-splunk"), CAMEL_SPRING_BATCH("camel-spring-batch"), CAMEL_SPRING_LDAP(
                "camel-spring-ldap"), CAMEL_SPRING_REDIS("camel-spring-redis"), CAMEL_SPRING_SECURITY(
                "camel-spring-security"), CAMEL_SPRING_WS("camel-spring-ws"), CAMEL_SQL("camel-sql"), CAMEL_SSH(
                "camel-ssh"), CAMEL_STAX("camel-stax"), CAMEL_STREAM("camel-stream"), CAMEL_STOMP(
                "camel-stomp"), CAMEL_STRING_TEMPLATE("camel-string-template"), CAMEL_SWAGGER(
                "camel-swagger"), CAMEL_SWAGGER_JAVA("camel-swagger-java"), CAMEL_SYSLOG(
                "camel-syslog"), CAMEL_TAGSOUP("camel-tagsoup"), CAMEL_TARFILE("camel-tarfile"), CAMEL_TELEGRAM(
                "camel-telegram"), CAMEL_TEST("camel-test"), CAMEL_TEST_KARAF("camel-test-karaf"), CAMEL_TEST_SPRING(
                "camel-test-spring"), CAMEL_TIKA("camel-tika"), CAMEL_TWITTER("camel-twitter"), CAMEL_UNDERTOW(
                "camel-undertow"), CAMEL_UNIVOCITY_PARSERS("camel-univocity-parsers"), CAMEL_URLREWRITE(
                "camel-urlrewrite"), CAMEL_VERTX("camel-vertx"), CAMEL_VELOCITY("camel-velocity"), CAMEL_WEATHER(
                "camel-weather"), CAMEL_WEBSOCKET("camel-websocket"), CAMEL_XMLBEANS(
                "camel-xmlbeans"), CAMEL_XMLJSON("camel-xmljson"), CAMEL_XMLRPC("camel-xmlrpc"), CAMEL_XMLSECURITY(
                "camel-xmlsecurity"), CAMEL_XMPP("camel-xmpp"), CAMEL_XSTREAM("camel-xstream"), CAMEL_YAMMER(
                "camel-yammer"), CAMEL_ZIPFILE("camel-zipfile"), CAMEL_ZIPKIN("camel-zipkin"), CAMEL_ZOOKEEPER(
                "camel-zookeeper"), CAMEL_ZOOKEEPER_MASTER("camel-zookeeper-master");

        private String featureName;

        CamelFeature(String featureName) {
            this.featureName = featureName;
        }

        @Override
        public String toString() {
            return featureName;
        }
    }

    public static Option camelFeatures(CamelFeature... features) {
        String[] featureStrings = Arrays.stream(features)
                .map(Enum::toString)
                .toArray(String[]::new);

        return features(maven().groupId("org.apache.camel.karaf")
                .artifactId("apache-camel")
                .versionAsInProject()
                .classifier("features")
                .type("xml"), featureStrings);
    }
}
