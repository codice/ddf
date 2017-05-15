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

public class KarafSpringFeatures {
    public enum SpringFeature {
        SPRING("spring"), SPRING_ASPECTS("spring-aspects"), SPRING_INSTRUMENT("spring-instrument"), SPRING_JDBC(
                "spring-jdbc"), SPRING_JMS("spring-jms"), SPRING_TEST("spring-test"), SPRING_ORM(
                "spring-orm"), SPRING_OXM("spring-oxm"), SPRING_TX("spring-tx"), SPRING_WEB(
                "spring-web"), SPRING_WEB_PORTLET("spring-web-portlet"), SPRING_WEBSOCKET(
                "spring-websocket");

        private String featureName;

        SpringFeature(String featureName) {
            this.featureName = featureName;
        }

        @Override
        public String toString() {
            return featureName;
        }
    }

    public static Option karafSpringFeatures(SpringFeature... features) {
        String[] featureStrings = Arrays.stream(features)
                .map(Enum::toString)
                .toArray(String[]::new);

        return features(maven().groupId("org.apache.karaf.features")
                .artifactId("spring")
                .versionAsInProject()
                .classifier("features")
                .type("xml"), featureStrings);
    }
}