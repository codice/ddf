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
package org.codice.ddf.admin.api.validation;

import static org.codice.ddf.admin.api.handler.ConfigurationMessage.createInvalidFieldMsg;
import static org.codice.ddf.admin.api.handler.ConfigurationMessage.createMissingRequiredFieldMsg;

import java.nio.file.InvalidPathException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.validator.UrlValidator;
import org.codice.ddf.admin.api.config.Configuration;
import org.codice.ddf.admin.api.handler.ConfigurationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationUtils {

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationUtils.class);

    public static final String SERVICE_PID_KEY = "service.pid";

    public static final String FACTORY_PID_KEY = "service.factoryPid";

    private static final Pattern HOST_NAME_PATTERN = Pattern.compile("[0-9a-zA-Z\\.-]+");

    private static final UriPathValidator PATH_VALIDATOR = new UriPathValidator();

    public static final List<ConfigurationMessage> validateString(String strToCheck,
            String configId) {
        List<ConfigurationMessage> errors = new ArrayList<>();
        if (StringUtils.isEmpty(strToCheck)) {
            errors.add(createMissingRequiredFieldMsg(configId));
        }
        return errors;
    }

    public static final List<ConfigurationMessage> validateHostName(String hostName,
            String configId) {
        List<ConfigurationMessage> errors = validateString(hostName, configId);
        if (errors.isEmpty() && !validHostnameFormat(hostName)) {
            errors.add(createInvalidFieldMsg("Hostname format is invalid.", configId));
        }
        return errors;
    }

    public static final List<ConfigurationMessage> validatePort(int port, String configId) {
        List<ConfigurationMessage> errors = new ArrayList<>();
        if (!validPortFormat(port)) {
            errors.add(createInvalidFieldMsg("Port is not in valid range.", configId));
        }
        return errors;
    }

    public static List<ConfigurationMessage> validateContextPath(String contextPath,
            String configId) {
        List<ConfigurationMessage> errors = new ArrayList<>();
        errors.addAll(validateString(contextPath, configId));
        if (errors.isEmpty()) {
            if (!PATH_VALIDATOR.isValidPath(contextPath)) {
                errors.add(createInvalidFieldMsg("Improperly formatted context path.",
                        contextPath));
            }
        }
        return errors;
    }

    public static List<ConfigurationMessage> validateUrl(String url, String configId) {
        List<ConfigurationMessage> errors = new ArrayList<>();
        if (url == null) {
            errors.add(createMissingRequiredFieldMsg(configId));
        }
        if (validUrlFormat(url)) {
            errors.add(createInvalidFieldMsg("Endpoint URL is not in a valid format.", configId));
        }
        return errors;
    }

    public static final List<ConfigurationMessage> validateContextPaths(List<String> contexts,
            String configId) {
        List<ConfigurationMessage> errors = new ArrayList<>();
        if (contexts == null || contexts.isEmpty()) {
            errors.add(createMissingRequiredFieldMsg(configId));
        } else {
            errors.addAll(contexts.stream()
                    .map(context -> validateContextPath(context, configId))
                    .flatMap(List::stream)
                    .collect(Collectors.toList()));
        }

        return errors;
    }

    public static final List<ConfigurationMessage> validateMapping(Map<String, String> mapping,
            String configId) {
        List<ConfigurationMessage> errors = new ArrayList<>();
        if (mapping == null || mapping.isEmpty()) {
            errors.add(createMissingRequiredFieldMsg(configId));
        } else if (mapping.values()
                .stream()
                .anyMatch(StringUtils::isEmpty)) {
            errors.add(createInvalidFieldMsg("Attribute mapping cannot map to an empty values.",
                    configId));
        }

        return errors;
    }

    public static final List<ConfigurationMessage> validateFilePath(String filePath,
            String configId) {
        List<ConfigurationMessage> errors = new ArrayList<>();
        errors.addAll(validateString(filePath, configId));

        if(errors.isEmpty()) {
            try {
                Paths.get(filePath);
            } catch (InvalidPathException e) {
                LOGGER.debug("Received an invalid path {}", filePath, e);
            }
        }

        return errors;
    }

    public static final boolean validUrlFormat(String url) {
        String[] schemes = {"http", "https"};
        UrlValidator validator = new UrlValidator(schemes);
        return validator.isValid(url);
    }

    public static final boolean validHostnameFormat(String hostname) {
        return HOST_NAME_PATTERN.matcher(hostname)
                .matches();
    }

    public static final boolean validPortFormat(int port) {
        return port > 0 && port < 65536;
    }

    public static final <T extends Configuration> List<ConfigurationMessage> validate(
            List<String> fields, T configuration,
            Map<String, Function<T, List<ConfigurationMessage>>> fieldsToValidations) {
        return fields.stream()
                .map(s -> fieldsToValidations.get(s)
                        .apply(configuration))
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    private static class UriPathValidator extends UrlValidator {
        @Override
        protected boolean isValidPath(String path) {
            return super.isValidPath(path);
        }
    }
}
