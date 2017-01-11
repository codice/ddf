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

package org.codice.ddf.admin.api.handler.report;

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.FAILURE;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.SUCCESS;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.MessageType.WARNING;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.handler.ConfigurationMessage;

public class TestReport {

    List<ConfigurationMessage> messages;

    public TestReport() {
        this(new ArrayList<>());
    }

    public TestReport(ConfigurationMessage... messages) {
        this(new ArrayList<>());
        Arrays.stream(messages)
                .forEach(msg -> this.messages.add(msg));
    }

    public TestReport(List<ConfigurationMessage> messages) {
        this.messages = messages;
    }

    public List<ConfigurationMessage> getMessages() {
        return messages;
    }

    public TestReport addMessage(ConfigurationMessage result) {
        this.messages.add(result);
        return this;
    }

    public void addMessages(List<ConfigurationMessage> messages) {
        this.messages.addAll(messages);
    }

    public boolean containsUnsuccessfulMessages() {
        return messages.stream()
                .filter(msg -> msg.getType() != SUCCESS)
                .findFirst()
                .isPresent();
    }

    public boolean containsFailureMessages() {
        return messages.stream()
                .filter(msg -> msg.getType() == FAILURE)
                .findFirst()
                .isPresent();
    }

    public static TestReport createGeneralTestReport(Map<String, String> successTypes,
            Map<String, String> failureTypes, Map<String, String> warningTypes,
            List<String> results) {
        TestReport testReport = new TestReport();

        for (String result : results) {
            if (successTypes != null && successTypes.containsKey(result)) {
                testReport.addMessage(new ConfigurationMessage(SUCCESS,
                        result,
                        successTypes.get(result)));
            } else if (warningTypes != null && warningTypes.containsKey(result)) {
                testReport.addMessage(new ConfigurationMessage(WARNING,
                        result,
                        warningTypes.get(result)));
            } else if (failureTypes != null && failureTypes.containsKey(result)) {
                testReport.addMessage(new ConfigurationMessage(FAILURE,
                        result,
                        failureTypes.get(result)));
            }
        }
        return testReport;
    }

    public static TestReport createGeneralTestReport(Map<String, String> successTypes,
            Map<String, String> failureTypes, Map<String, String> warningTypes,
            Map<String, String> resultsToConfigIds) {
        TestReport testReport = new TestReport();

        for (Map.Entry<String, String> result : resultsToConfigIds.entrySet()) {
            String resultName = result.getKey();
            String resultConfigId = result.getValue();
            if (successTypes != null && successTypes.containsKey(resultName)) {
                testReport.addMessage(new ConfigurationMessage(SUCCESS,
                        resultName,
                        successTypes.get(resultName)).configId(resultConfigId));
            } else if (warningTypes != null && warningTypes.containsKey(resultName)) {
                testReport.addMessage(new ConfigurationMessage(WARNING,
                        resultName,
                        warningTypes.get(resultName)).configId(resultConfigId));
            } else if (failureTypes != null && failureTypes.containsKey(resultName)) {
                testReport.addMessage(new ConfigurationMessage(FAILURE,
                        resultName,
                        failureTypes.get(resultName)).configId(resultConfigId));
            }
        }
        return testReport;
    }
}
