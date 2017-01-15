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

package org.codice.ddf.admin.api.config.security.context;

import static org.codice.ddf.admin.api.config.security.context.ContextPolicyUtils.validateContextPath;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.createMissingRequiredFieldMsg;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.codice.ddf.admin.api.config.Configuration;
import org.codice.ddf.admin.api.config.ConfigurationType;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;

import com.google.common.collect.ImmutableMap;

public class ContextPolicyConfiguration extends Configuration {

    public static final String CONFIGURATION_TYPE = "context-policy-manager";

    public static final String CONTEXT_POLICY_BINS = "contextPolicyBins";
    public static final String WHITE_LIST_CONTEXTS  = "whiteListContexts";
    public static final List<String> ALL_FIELDS = Arrays.asList(CONTEXT_POLICY_BINS, WHITE_LIST_CONTEXTS);

    public static final ImmutableMap<String, String> FIELD_DESCS =
            new ImmutableMap.Builder<String, String>()
            .put(CONTEXT_POLICY_BINS, "Objects containing: " +
                    "\n - list of auth types and required attributes for a list of context paths.")
            .put(WHITE_LIST_CONTEXTS, "List of contexts that will not use security.").build();

    private List<ContextPolicyBin> contextPolicyBins;
    private List<String> whiteListContexts;

    public List<ContextPolicyBin> contextPolicyBins() {
        return contextPolicyBins;
    }
    public List<String> whiteListContexts() {
        return whiteListContexts;
    }

    public ContextPolicyConfiguration contextPolicyBins(List<ContextPolicyBin> contextPolicyBins) {
        this.contextPolicyBins = contextPolicyBins;
        return this;
    }
    public ContextPolicyConfiguration whiteListContexts(List<String> whiteListContexts) {
        this.whiteListContexts = whiteListContexts;
        return this;
    }

    public List<ConfigurationMessage> validate(List<String> requiredFields) {

        List<ConfigurationMessage> msgs = new ArrayList<>();

        for(String reqField : requiredFields) {
            switch (reqField) {
            case CONTEXT_POLICY_BINS:
                if(contextPolicyBins() == null || contextPolicyBins().isEmpty()) {
                    msgs.add(createMissingRequiredFieldMsg(CONTEXT_POLICY_BINS));
                } else {
                    msgs.addAll(contextPolicyBins().stream()
                            .map(cpb -> cpb.validate())
                            .flatMap(List::stream)
                            .collect(Collectors.toList()));
                }
                break;
            case  WHITE_LIST_CONTEXTS:
                if(whiteListContexts() == null || whiteListContexts().isEmpty()) {
                    msgs.add(createMissingRequiredFieldMsg(CONTEXT_POLICY_BINS));
                } else {
                    msgs.addAll(whiteListContexts().stream()
                            .map(context -> validateContextPath(context))
                            .flatMap(List::stream)
                            .collect(Collectors.toList()));
                }
            }
        }

        return msgs;
    }

    @Override
    public ConfigurationType getConfigurationType() {
        return new ConfigurationType(CONFIGURATION_TYPE, ContextPolicyConfiguration.class);
    }

    public static Map<String, String> buildFieldMap(String... keys) {
        ImmutableMap.Builder<String, String> map = new ImmutableMap.Builder<>();
        Arrays.stream(keys)
                .forEach(s -> map.put(s, FIELD_DESCS.get(s)));
        return map.build();
    }
}


