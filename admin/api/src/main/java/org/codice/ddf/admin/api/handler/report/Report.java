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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.codice.ddf.admin.api.handler.ConfigurationMessage;

/**
 * A {@link Report} is used to relay the results to the caller of an operation performed on a {@link org.codice.ddf.admin.api.config.Configuration}
 * by the {@link org.codice.ddf.admin.api.handler.ConfigurationHandler}.
 */
public class Report {

    List<ConfigurationMessage> messages;

    public Report() {
        this.messages = new ArrayList<>();
    }

    public Report(ConfigurationMessage... messages) {
        this.messages = new ArrayList<>();
        Arrays.stream(messages)
                .forEach(msg -> this.messages.add(msg));
    }

    public Report(List<ConfigurationMessage> messages) {
        this.messages = new ArrayList<>();
        if(messages != null) {
            this.messages.addAll(messages);
        }
    }

    public boolean containsUnsuccessfulMessages() {
        return messages.stream()
                .filter(msg -> msg.type() != ConfigurationMessage.MessageType.SUCCESS)
                .findFirst()
                .isPresent();
    }

    public boolean containsFailureMessages() {
        return messages.stream()
                .filter(msg -> msg.type() == ConfigurationMessage.MessageType.FAILURE)
                .findFirst()
                .isPresent();
    }

    public static Report createReport(Map<String, String> successTypes,
            Map<String, String> failureTypes, Map<String, String> warningTypes,
            String results) {
        return  createReport(successTypes, failureTypes, warningTypes, Arrays.asList(results));
    }
    public static Report createReport(Map<String, String> successTypes,
            Map<String, String> failureTypes, Map<String, String> warningTypes,
            List<String> results) {
        Report testReport = new Report();

        for (String result : results) {
            if (successTypes != null && successTypes.containsKey(result)) {
                testReport.messages(new ConfigurationMessage(ConfigurationMessage.MessageType.SUCCESS,
                        result,
                        successTypes.get(result)));
            } else if (warningTypes != null && warningTypes.containsKey(result)) {
                testReport.messages(new ConfigurationMessage(ConfigurationMessage.MessageType.WARNING,
                        result,
                        warningTypes.get(result)));
            } else if (failureTypes != null && failureTypes.containsKey(result)) {
                testReport.messages(new ConfigurationMessage(ConfigurationMessage.MessageType.FAILURE,
                        result,
                        failureTypes.get(result)));
            }
        }
        return testReport;
    }

    public static Report createReport(Map<String, String> successTypes,
            Map<String, String> failureTypes, Map<String, String> warningTypes,
            Map<String, String> resultsToConfigIds) {
        Report testReport = new Report();

        for (Map.Entry<String, String> result : resultsToConfigIds.entrySet()) {
            String resultName = result.getKey();
            String resultConfigId = result.getValue();
            if (successTypes != null && successTypes.containsKey(resultName)) {
                testReport.messages(new ConfigurationMessage(ConfigurationMessage.MessageType.SUCCESS,
                        resultName,
                        successTypes.get(resultName)).configFieldId(resultConfigId));
            } else if (warningTypes != null && warningTypes.containsKey(resultName)) {
                testReport.messages(new ConfigurationMessage(ConfigurationMessage.MessageType.WARNING,
                        resultName,
                        warningTypes.get(resultName)).configFieldId(resultConfigId));
            } else if (failureTypes != null && failureTypes.containsKey(resultName)) {
                testReport.messages(new ConfigurationMessage(ConfigurationMessage.MessageType.FAILURE,
                        resultName,
                        failureTypes.get(resultName)).configFieldId(resultConfigId));
            }
        }
        return testReport;
    }

    //Getters
    public List<ConfigurationMessage> messages() {
        return messages;
    }

    //Setters
    public Report messages(ConfigurationMessage result) {
        this.messages.add(result);
        return this;
    }

    // TODO: tbatie - 1/20/17 - rename to addMessages
    public Report messages(List<ConfigurationMessage> messages) {
        this.messages.addAll(messages);
        return this;
    }
}
