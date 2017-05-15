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

public class KarafStandardFeatures {
    public enum StandardFeature {
        WRAP("wrap"), ARIES_BLUEPRINT("aries-blueprint"), SHELL("shell"), SHELL_COMPAT(
                "shell-compat"), FEATURE("feature"), JAAS("jaas"), SSH("ssh"), MANAGEMENT(
                "management"), BUNDLE("bundle"), CONFIG("config"), DEPLOYER("deployer"), DIAGNOSTIC(
                "diagnostic"), INSTANCE("instance"), KAR("kar"), LOG("log"), PACKAGE("package"), SERVICE(
                "service"), SYSTEM("system"), STANDARD("standard"), MINIMAL("minimal");

        private String featureName;

        StandardFeature(String featureName) {
            this.featureName = featureName;
        }

        @Override
        public String toString() {
            return featureName;
        }
    }

    public static Option karafStandardFeatures(StandardFeature... features) {
        String[] featureStrings = Arrays.stream(features)
                .map(Enum::toString)
                .toArray(String[]::new);

        return features(maven().groupId("org.apache.karaf.features")
                .artifactId("standard")
                .versionAsInProject()
                .classifier("features")
                .type("xml"), featureStrings);
    }
}
