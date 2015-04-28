/**
 * Copyright (c) Codice Foundation
 *
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 *
 **/
package org.codice.ddf.admin.insecure.defaults.service;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.codice.ddf.admin.insecure.defaults.service.Alert.Level;
import org.junit.Test;
import org.osgi.service.cm.ConfigurationAdmin;

public class InsecureDefaultsServiceBeanTest {

    private static final String BASE_ALERT_TEST_MESSAGE = "This is test alert ";

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

            List<String> messages = new ArrayList<>();
            for (Alert alert : alerts) {
                messages.add(alert.getMessage());
            }
            Collections.sort(messages);

            for (int j = 0; j < alerts.size(); j++) {
                assertThat(alerts.get(j).getLevel(), is(Level.WARN));
                assertThat(alerts.get(j).getMessage(), is(BASE_ALERT_TEST_MESSAGE + j));
            }
        }
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
