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
public class TestXACML extends TestSecurity {

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

    @Override
    public void configurePDP() throws Exception {
        stopFeature(true, "security-pdp-java");
        startFeature(true, "security-pdp-xacml");
    }
}