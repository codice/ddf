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
package org.codice.ddf.admin.insecure.defaults.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.codice.ddf.admin.insecure.defaults.service.Alert.Level;
import org.junit.Test;
import org.mockito.ArgumentMatcher;
import org.osgi.service.cm.ConfigurationAdmin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.spi.LoggingEvent;
import ch.qos.logback.core.Appender;

public class InsecureDefaultsServiceBeanTest {

    private static final String BASE_ALERT_TEST_MESSAGE = "This is test alert ";

    private static final String BASE_INIT_STRING = "Registered Insecure Defaults Service MBean under object name";

    private static final String INIT_ALREADY_EXIST = "Re-registered Insecure Defaults Service MBean";

    private static final String DESTROY_STRING = "Unregistered Insecure Defaults Service MBean";

    private static final String DESTROY_EXCEPT = "Exception unregistering MBean";

    private static final String KEYSTORE_SYSTEM_PROPERTY = "javax.net.ssl.keyStore";

    private static final String KEYSTORE_PASSWORD_SYSTEM_PROPERTY = "javax.net.ssl.keyStorePassword";

    private static final String TRUSTSTORE_SYSTEM_PROPERTY = "javax.net.ssl.trustStore";

    private static final String TRUSTSTORE_PASSWORD_SYSTEM_PROPERTY = "javax.net.ssl.trustStorePassword";

    @Test
    public void validate() {
        // Setup
        final int validatorCount = 1;
        InsecureDefaultsServiceBean bean = createInsecureDefaultsServiceBean(validatorCount);

        // Perform Test
        List<Alert> alerts = bean.validate();

        // Verify
        assertThat(alerts.size(), is(1));
        assertThat(alerts.get(0).getLevel(), is(Level.WARN));
        assertThat(alerts.get(0).getMessage(), is(BASE_ALERT_TEST_MESSAGE + 0));
    }

    // Verify alerts get cleared on each call to validate.
    @Test
    public void validateCalledMultipleTimes() {
        // Setup
        final int validatorCount = 2;
        InsecureDefaultsServiceBean bean = createInsecureDefaultsServiceBean(validatorCount);

        // Call validate twice
        for (int i = 0; i <= 1; i++) {
            // Perform Test
            List<Alert> alerts = bean.validate();

            // Verify
            assertThat(alerts.size(), is(2));
        }
    }

    /**
     * Tests the {@link InsecureDefaultsServiceBean#init()} method
     */
    @Test
    public void testInit() {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        InsecureDefaultsServiceBean serviceBean = createInsecureDefaultsServiceBean(0);
        serviceBean.init();

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage().contains(BASE_INIT_STRING);
            }
        }));
    }

    /**
     * Tests the {@link InsecureDefaultsServiceBean#init()} method for the case where
     * the serviceBean has already been initialized
     */
    @Test
    public void testInitException() {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        InsecureDefaultsServiceBean serviceBean = createInsecureDefaultsServiceBean(0);
        serviceBean.init();
        serviceBean.init();

        verify(mockAppender, atLeastOnce()).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage().contains(INIT_ALREADY_EXIST);
            }
        }));
    }

    /**
     * Tests the {@link InsecureDefaultsServiceBean#destroy()} method
     */
    @Test
    public void testDestroy() {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        InsecureDefaultsServiceBean serviceBean = createInsecureDefaultsServiceBean(0);
        serviceBean.init();
        serviceBean.destroy();

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage().contains(DESTROY_STRING);
            }
        }));
    }

    /**
     * Tests the {@link InsecureDefaultsServiceBean#destroy()} method for the case where
     * the serviceBean has not yet been initialized.
     */
    @Test
    public void testDestroyException() {
        ch.qos.logback.classic.Logger root = (ch.qos.logback.classic.Logger) LoggerFactory
                .getLogger(Logger.ROOT_LOGGER_NAME);
        final Appender mockAppender = mock(Appender.class);
        when(mockAppender.getName()).thenReturn("MOCK");
        root.addAppender(mockAppender);

        InsecureDefaultsServiceBean serviceBean = createInsecureDefaultsServiceBean(0);
        serviceBean.destroy();

        verify(mockAppender).doAppend(argThat(new ArgumentMatcher() {
            @Override
            public boolean matches(final Object argument) {
                return ((LoggingEvent) argument).getFormattedMessage().contains(DESTROY_EXCEPT);
            }
        }));
    }

    /**
     * Tests the {@link InsecureDefaultsServiceBean#addKeystoreValidator()} method
     */
    @Test
    public void testAddKeystoreValidator() {
        ConfigurationAdmin testConfigAdmin = mock(ConfigurationAdmin.class);
        System.setProperty(KEYSTORE_SYSTEM_PROPERTY, "TestKeystorePath");
        System.setProperty(TRUSTSTORE_SYSTEM_PROPERTY, "TestTruststorePath");

        InsecureDefaultsServiceBean serviceBean = new InsecureDefaultsServiceBean(testConfigAdmin);
        List<Validator> result = serviceBean.getValidators();
        assertThat("Should create nine validators.", result.size(), is(9));
    }

    private List<Validator> createMockValidators(int count) {
        List<Validator> validators = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            Validator mockValidator = mock(Validator.class);
            List<Alert> mockAlerts = new ArrayList<>(1);
            mockAlerts.add(new Alert(Level.WARN, BASE_ALERT_TEST_MESSAGE + i));
            when(mockValidator.validate()).thenReturn(mockAlerts);
            validators.add(mockValidator);
        }

        return validators;
    }

    private InsecureDefaultsServiceBean createInsecureDefaultsServiceBean(int validatorCount) {
        InsecureDefaultsServiceBean bean = new InsecureDefaultsServiceBean(null) {
            @Override
            void addValidators(ConfigurationAdmin configurationAdmin) {
                return;
            }
        };
        bean.setValidators(createMockValidators(validatorCount));

        return bean;
    }
}
