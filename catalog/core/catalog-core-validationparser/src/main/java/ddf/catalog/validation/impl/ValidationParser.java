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
package ddf.catalog.validation.impl;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.boon.core.value.LazyValueMap;
import org.boon.core.value.ValueList;
import org.boon.json.JsonFactory;
import org.boon.json.ObjectMapper;
import org.boon.json.annotations.JsonIgnore;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Sets;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.DefaultAttributeValueRegistry;
import ddf.catalog.data.InjectableAttribute;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.InjectableAttributeImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.AttributeValidatorRegistry;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.impl.validator.EnumerationValidator;
import ddf.catalog.validation.impl.validator.FutureDateValidator;
import ddf.catalog.validation.impl.validator.ISO3CountryCodeValidator;
import ddf.catalog.validation.impl.validator.MatchAnyValidator;
import ddf.catalog.validation.impl.validator.PastDateValidator;
import ddf.catalog.validation.impl.validator.PatternValidator;
import ddf.catalog.validation.impl.validator.RangeValidator;
import ddf.catalog.validation.impl.validator.RequiredAttributesMetacardValidator;
import ddf.catalog.validation.impl.validator.SizeValidator;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

@SuppressFBWarnings
public class ValidationParser implements ArtifactInstaller {
    private static final String METACARD_VALIDATORS_PROPERTY = "metacardvalidators";

    private static final String REQUIRED_ATTRIBUTE_VALIDATOR_PROPERTY = "requiredattributes";

    private static final String REQUIRED_ATTRIBUTES_PROPERTY = "requiredattributes";

    private static final String VALIDATOR_PROPERTY = "validator";

    private static final String METACARD_TYPES_PROPERTY = "Metacard Types";

    private static final String VALIDATORS_PROPERTY = "validators";

    private static final String DEFAULTS_PROPERTY = "Defaults";

    private static final String INJECTIONS_PROPERTY = "Injections";

    private static final String MATCH_ANY = "match_any";

    private static final String NAME_PROPERTY = "name";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_INSTANT;

    private final AttributeRegistry attributeRegistry;

    private final AttributeValidatorRegistry attributeValidatorRegistry;

    private final DefaultAttributeValueRegistry defaultAttributeValueRegistry;

    private final Map<String, Changeset> changesetsByFile = new ConcurrentHashMap<>();

    private static final Logger LOGGER = LoggerFactory.getLogger(ValidationParser.class);

    public ValidationParser(AttributeRegistry attributeRegistry,
            AttributeValidatorRegistry attributeValidatorRegistry,
            DefaultAttributeValueRegistry defaultAttributeValueRegistry) {
        this.attributeRegistry = attributeRegistry;
        this.attributeValidatorRegistry = attributeValidatorRegistry;
        this.defaultAttributeValueRegistry = defaultAttributeValueRegistry;
    }

    @Override
    public void install(File file) throws Exception {
        apply(file);
    }

    @Override
    public void update(File file) throws Exception {
        undo(file);
        apply(file);
    }

    @Override
    public void uninstall(File file) throws Exception {
        undo(file);
    }

    @Override
    public boolean canHandle(File file) {
        return file.getName()
                .endsWith(".json");
    }

    private void apply(File file) throws Exception {
        String data;
        ObjectMapper objectMapper = JsonFactory.create();

        try (InputStream input = new FileInputStream(file)) {
            data = IOUtils.toString(input, StandardCharsets.UTF_8.name());
            LOGGER.debug("Installing file [{}]. Contents:\n{}", file.getAbsolutePath(), data);
        }
        if (StringUtils.isEmpty(data)) {
            LOGGER.debug("File is empty [{}]. Nothing to install.", file.getAbsolutePath());
            return; /* nothing to install */
        }

        Outer outer;
        try {
            outer = objectMapper.readValue(data, Outer.class);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Cannot parse json [" + file.getAbsolutePath() + "]",
                    e);
        }

        /* Must manually parse validators */
        Map<String, Object> root = objectMapper.parser()
                .parseMap(data);
        parseValidators(root, outer);
        parseMetacardValidators(root, outer);

        final String filename = file.getName();
        final Changeset changeset = new Changeset();
        changesetsByFile.put(filename, changeset);

        handleSection(changeset,
                "Attribute Types",
                outer.attributeTypes,
                this::parseAttributeTypes);
        handleSection(changeset,
                METACARD_TYPES_PROPERTY,
                outer.metacardTypes,
                this::parseMetacardTypes);
        handleSection(changeset, VALIDATORS_PROPERTY, outer.validators, this::parseValidators);
        handleSection(changeset, DEFAULTS_PROPERTY, outer.defaults, this::parseDefaults);
        handleSection(changeset, INJECTIONS_PROPERTY, outer.inject, this::parseInjections);
        handleSection(changeset,
                METACARD_VALIDATORS_PROPERTY,
                outer.metacardValidators,
                this::registerMetacardValidators);
    }

    private <T> void handleSection(Changeset changeset, String sectionName, T sectionData,
            BiFunction<Changeset, T, List<Callable<Boolean>>> parser) throws Exception {
        if (sectionData != null) {
            List<Callable<Boolean>> stagedAdds;
            try {
                stagedAdds = parser.apply(changeset, sectionData);
            } catch (Exception e) {
                throw new IllegalArgumentException(String.format("Could not parse %s section.",
                        sectionName), e);
            }

            LOGGER.debug("Committing {}", sectionName);
            commitStaged(stagedAdds);
        }
    }

    private void commitStaged(List<Callable<Boolean>> stagedAdds) throws Exception {
        for (Callable<Boolean> staged : stagedAdds) {
            try {
                staged.call();
            } catch (RuntimeException e) {
                LOGGER.debug("Error adding staged item {}", staged, e);
            }
        }
    }

    @SuppressWarnings("unchecked")
    private void parseValidators(Map<String, Object> root, Outer outer) {
        if (root == null || root.get(VALIDATORS_PROPERTY) == null) {
            return;
        }

        ObjectMapper objectMapper = JsonFactory.create();

        Map<String, List<Outer.Validator>> validators = new HashMap<>();
        ((Map<String, Object>) root.get(VALIDATORS_PROPERTY)).forEach((attributeName, value) -> {
            if (value instanceof ValueList) {
                ValueList argumentList = (ValueList) value;
                List<Outer.Validator> validatorList = new ArrayList<>();

                for (Object object : argumentList) {
                    if (object instanceof LazyValueMap) {
                        LazyValueMap lazyValueMap = (LazyValueMap) object;
                        final String comp = (String) lazyValueMap.get(VALIDATOR_PROPERTY);
                        String json = objectMapper.toJson(object);
                        Outer.Validator validator;

                        switch (comp) {
                        /* Switch is used with the intent that additional validators will be added in the future. */
                        case MATCH_ANY:
                            validator = objectMapper.readValue(json,
                                    Outer.ValidatorCollection.class);
                            break;
                        default:
                            validator = objectMapper.readValue(json, Outer.Validator.class);
                            break;
                        }
                        validatorList.add(validator);
                    }
                }
                validators.put(attributeName, validatorList);
            }
        });
        outer.validators = validators;
    }

    @SuppressWarnings("unchecked")
    private void parseMetacardValidators(@Nullable Map<String, Object> root, Outer outer) {
        if (root == null || root.get(METACARD_VALIDATORS_PROPERTY) == null) {
            return;
        }

        List<MetacardValidatorDefinition> metacardValidators = new ArrayList<>();
        Object metacardValidatorsObj = root.get(METACARD_VALIDATORS_PROPERTY);
        if (!(metacardValidatorsObj instanceof List)) {
            return;
        }

        List<Map<String, Object>> metacardValidatorsList =
                (List<Map<String, Object>>) metacardValidatorsObj;
        for (Map<String, Object> metacardTypeList : metacardValidatorsList) {
            for (Map.Entry<String, Object> typeEntry : metacardTypeList.entrySet()) {
                String metacardType = typeEntry.getKey();
                Object validatorDefinitionObj = typeEntry.getValue();

                if (!(validatorDefinitionObj instanceof List)) {
                    continue;
                }

                List<Map<String, Object>> validatorEntryList =
                        (List<Map<String, Object>>) validatorDefinitionObj;
                for (Map<String, Object> metacardDefinitionMap : validatorEntryList) {
                    MetacardValidatorDefinition validatorDefinition =
                            new MetacardValidatorDefinition();
                    validatorDefinition.metacardType = metacardType;
                    validatorDefinition.arguments = new HashMap<>(metacardDefinitionMap);
                    metacardValidators.add(validatorDefinition);
                }
            }
        }
        outer.metacardValidators = metacardValidators;
    }

    private List<Callable<Boolean>> parseAttributeTypes(Changeset changeset,
            Map<String, Outer.AttributeType> attributeTypes) {
        List<Callable<Boolean>> staged = new ArrayList<>();
        for (Map.Entry<String, Outer.AttributeType> entry : attributeTypes.entrySet()) {
            final AttributeDescriptor descriptor = new AttributeDescriptorImpl(entry.getKey(),
                    entry.getValue().indexed,
                    entry.getValue().stored,
                    entry.getValue().tokenized,
                    entry.getValue().multivalued,
                    BasicTypes.getAttributeType(entry.getValue().type));

            staged.add(() -> {
                attributeRegistry.register(descriptor);
                changeset.attributes.add(descriptor);
                return true;
            });
        }
        return staged;
    }

    private List<Callable<Boolean>> parseMetacardTypes(Changeset changeset,
            List<Outer.MetacardType> metacardTypes) {
        List<Callable<Boolean>> staged = new ArrayList<>();
        BundleContext context = getBundleContext();
        for (Outer.MetacardType metacardType : metacardTypes) {
            Set<AttributeDescriptor> attributeDescriptors =
                    new HashSet<>(BasicTypes.BASIC_METACARD.getAttributeDescriptors());
            Set<String> requiredAttributes = new HashSet<>();

            metacardType.attributes.forEach((attributeName, attribute) -> {
                AttributeDescriptor descriptor = attributeRegistry.lookup(attributeName)
                        .orElseThrow(() -> new IllegalStateException(String.format(
                                "Metacard type [%s] includes the attribute [%s], but that attribute is not in the attribute registry.",
                                metacardType.type,
                                attributeName)));
                attributeDescriptors.add(descriptor);
                if (attribute.required) {
                    requiredAttributes.add(attributeName);
                }
            });

            if (!requiredAttributes.isEmpty()) {
                final MetacardValidator validator = new RequiredAttributesMetacardValidator(
                        metacardType.type,
                        requiredAttributes);
                staged.add(() -> {
                    ServiceRegistration<MetacardValidator> registration = context.registerService(
                            MetacardValidator.class,
                            validator,
                            null);
                    changeset.metacardValidatorServices.add(registration);
                    return registration != null;
                });
            }

            Dictionary<String, Object> properties = new Hashtable<>();
            properties.put(NAME_PROPERTY, metacardType.type);
            MetacardType type = new MetacardTypeImpl(metacardType.type, attributeDescriptors);
            staged.add(() -> {
                ServiceRegistration<MetacardType> registration = context.registerService(
                        MetacardType.class,
                        type,
                        properties);
                changeset.metacardTypeServices.add(registration);
                return registration != null;
            });
        }
        return staged;
    }

    private List<Callable<Boolean>> registerMetacardValidators(Changeset changeset,
            List<MetacardValidatorDefinition> metacardValidatorDefinitions) {
        List<Callable<Boolean>> staged = new ArrayList<>();
        BundleContext context = getBundleContext();
        for (MetacardValidatorDefinition metacardValidatorDefinition : metacardValidatorDefinitions) {
            try {
                List<MetacardValidator> metacardValidators = getMetacardValidators(
                        metacardValidatorDefinition);
                metacardValidators.forEach(metacardValidator -> staged.add(() -> {
                    ServiceRegistration<MetacardValidator> registration = context.registerService(
                            MetacardValidator.class,
                            metacardValidator,
                            null);
                    changeset.metacardValidatorServices.add(registration);
                    return registration != null;
                }));

            } catch (IllegalStateException ise) {
                LOGGER.error("Could not register metacard validator for definition: {}",
                        metacardValidatorDefinition,
                        ise);
            }
        }
        return staged;
    }

    private List<Callable<Boolean>> parseValidators(Changeset changeset,
            Map<String, List<Outer.Validator>> validators) {
        List<Callable<Boolean>> staged = new ArrayList<>();
        for (Map.Entry<String, List<Outer.Validator>> entry : validators.entrySet()) {
            Set<AttributeValidator> attributeValidators = validatorFactory(entry.getValue());
            String attributeName = entry.getKey();
            staged.add(() -> {
                attributeValidatorRegistry.registerValidators(attributeName, attributeValidators);
                changeset.attributeValidators.put(attributeName, attributeValidators);
                return true;
            });
        }
        return staged;
    }

    private Set<AttributeValidator> validatorFactory(List<Outer.Validator> validators) {
        return validators.stream()
                .filter(Objects::nonNull)
                .filter(v -> StringUtils.isNotBlank(v.validator))
                .map(this::getValidator)
                .collect(toSet());
    }

    @SuppressWarnings("unchecked")
    private List<MetacardValidator> getMetacardValidators(
            MetacardValidatorDefinition validatorDefinition) {
        List<MetacardValidator> metacardValidators = new ArrayList<>();

        MetacardValidator metacardValidator = null;
        String validatorName = null;
        Object metacardValidatorObj = validatorDefinition.arguments.get(VALIDATOR_PROPERTY);
        if (metacardValidatorObj instanceof String) {
            validatorName = (String) metacardValidatorObj;
        }
        if (validatorName != null) {
            String metacardType = validatorDefinition.metacardType;
            switch (validatorName) {
            case REQUIRED_ATTRIBUTE_VALIDATOR_PROPERTY: {
                if (metacardType == null) {
                    throw new IllegalStateException(
                            "Required Attributes Validator received invalid configuration");
                }

                Set<String> requiredAttributes = new HashSet<>();
                Object requiredAttributesObj = validatorDefinition.arguments.get(
                        REQUIRED_ATTRIBUTES_PROPERTY);
                if (requiredAttributesObj instanceof List) {
                    List<Object> requiredAttrObjList = (List) requiredAttributesObj;
                    requiredAttributes = requiredAttrObjList.stream()
                            .filter(String.class::isInstance)
                            .map(String.class::cast)
                            .collect(Collectors.toSet());
                }

                if (CollectionUtils.isNotEmpty(requiredAttributes)) {
                    metacardValidator = new RequiredAttributesMetacardValidator(metacardType,
                            requiredAttributes);
                } else {
                    throw new IllegalStateException(
                            "Required Attributes Validator received invalid configuration");
                }
                break;
            }
            default:
                throw new IllegalStateException(String.format("Validator does not exist. (%s)",
                        validatorName));
            }
        }

        if (metacardValidator != null) {
            metacardValidators.add(metacardValidator);
        }
        return metacardValidators;
    }

    private AttributeValidator getValidator(Outer.Validator validator) {
        switch (validator.validator) {
        case "size": {
            long lmin = Long.parseLong(validator.arguments.get(0));
            long lmax = Long.parseLong(validator.arguments.get(1));
            return new SizeValidator(lmin, lmax);
        }
        case "pattern": {
            String regex = validator.arguments.get(0);
            return new PatternValidator(regex);
        }
        case "pastdate": {
            return PastDateValidator.getInstance();
        }
        case "futuredate": {
            return FutureDateValidator.getInstance();
        }
        case "enumeration": {
            Set<String> values = new HashSet<>(validator.arguments);
            return new EnumerationValidator(values, false);
        }
        case "enumerationignorecase": {
            Set<String> values = new HashSet<>(validator.arguments);
            return new EnumerationValidator(values, true);
        }
        case "range": {
            BigDecimal min = new BigDecimal(validator.arguments.get(0));
            BigDecimal max = new BigDecimal(validator.arguments.get(1));
            if (validator.arguments.size() > 2) {
                BigDecimal epsilon = new BigDecimal(validator.arguments.get(2));
                return new RangeValidator(min, max, epsilon);
            }
            return new RangeValidator(min, max);
        }
        case "iso3_country": {
            return new ISO3CountryCodeValidator(false);
        }
        case "iso3_countryignorecase": {
            return new ISO3CountryCodeValidator(true);
        }
        case "match_any": {
            List<Outer.Validator> collection = ((Outer.ValidatorCollection) validator).validators;
            return new MatchAnyValidator(collection.stream()
                    .map(this::getValidator)
                    .collect(Collectors.toList()));
        }
        default:
            throw new IllegalStateException(
                    "Validator does not exist. (" + validator.validator + ")");
        }
    }

    private Serializable parseDefaultValue(AttributeDescriptor descriptor, String defaultValue) {
        switch (descriptor.getType()
                .getAttributeFormat()) {
        case BOOLEAN:
            return Boolean.parseBoolean(defaultValue);
        case DATE:
            return Date.from(Instant.from(DATE_FORMATTER.parse(defaultValue)));
        case DOUBLE:
            return Double.parseDouble(defaultValue);
        case FLOAT:
            return Float.parseFloat(defaultValue);
        case SHORT:
            return Short.parseShort(defaultValue);
        case INTEGER:
            return Integer.parseInt(defaultValue);
        case LONG:
            return Long.parseLong(defaultValue);
        case BINARY:
            return defaultValue.getBytes(StandardCharsets.UTF_8);
        default:
            return defaultValue;
        }
    }

    private List<Callable<Boolean>> parseDefaults(Changeset changeset,
            List<Outer.Default> defaults) {
        return defaults.stream()
                .map(defaultObj -> {
                    String attribute = defaultObj.attribute;
                    AttributeDescriptor descriptor = attributeRegistry.lookup(attribute)
                            .orElseThrow(() -> new IllegalStateException(String.format(
                                    "The default value for the attribute [%s] cannot be parsed because that attribute has not been registered in the attribute registry",
                                    attribute)));
                    Serializable defaultValue = parseDefaultValue(descriptor, defaultObj.value);
                    List<String> metacardTypes = defaultObj.metacardTypes;
                    if (CollectionUtils.isEmpty(metacardTypes)) {
                        return (Callable<Boolean>) () -> {
                            defaultAttributeValueRegistry.setDefaultValue(attribute, defaultValue);
                            changeset.defaults.add(defaultObj);
                            return true;
                        };
                    } else {
                        return (Callable<Boolean>) () -> {
                            metacardTypes.forEach(metacardType -> {
                                defaultAttributeValueRegistry.setDefaultValue(metacardType,
                                        attribute,
                                        defaultValue);
                            });
                            changeset.defaults.add(defaultObj);
                            return true;
                        };
                    }
                })
                .collect(toList());
    }

    private List<Callable<Boolean>> parseInjections(Changeset changeset,
            List<Outer.Injection> injections) {
        BundleContext context = getBundleContext();
        return injections.stream()
                .map(injection -> (Callable<Boolean>) () -> {
                    String attribute = injection.attribute;
                    InjectableAttribute injectableAttribute = new InjectableAttributeImpl(attribute,
                            injection.metacardTypes);
                    ServiceRegistration<InjectableAttribute> injectableAttributeService =
                            context.registerService(InjectableAttribute.class,
                                    injectableAttribute,
                                    null);
                    changeset.injectableAttributeServices.add(injectableAttributeService);
                    return true;
                })
                .collect(toList());
    }

    private BundleContext getBundleContext() {
        return Optional.ofNullable(FrameworkUtil.getBundle(getClass()))
                .map(Bundle::getBundleContext)
                .orElseThrow(() -> new IllegalStateException(
                        "Could not get the bundle for " + getClass().getName()));
    }

    private void undo(File file) {
        final String filename = file.getName();

        LOGGER.debug("Reversing the changes applied by file [{}].", filename);

        final Changeset changeset = changesetsByFile.get(filename);
        if (changeset != null) {
            undoMetacardTypes(changeset.metacardTypeServices);
            undoMetacardValidators(changeset.metacardValidatorServices);
            undoAttributes(changeset.attributes);
            undoDefaults(changeset.defaults);
            undoAttributeValidators(changeset.attributeValidators);
            undoInjectableAttributes(changeset.injectableAttributeServices);

            changesetsByFile.remove(filename);
        }
    }

    private void undoMetacardTypes(List<ServiceRegistration<MetacardType>> metacardTypeServices) {
        metacardTypeServices.forEach(ServiceRegistration::unregister);
    }

    private void undoMetacardValidators(
            List<ServiceRegistration<MetacardValidator>> metacardValidatorServices) {
        metacardValidatorServices.forEach(ServiceRegistration::unregister);
    }

    private void undoAttributes(Set<AttributeDescriptor> attributes) {
        attributes.forEach(attributeRegistry::deregister);
    }

    private void undoDefaults(List<Outer.Default> defaults) {
        defaults.forEach(theDefault -> {
            if (CollectionUtils.isEmpty(theDefault.metacardTypes)) {
                defaultAttributeValueRegistry.removeDefaultValue(theDefault.attribute);
            } else {
                theDefault.metacardTypes.forEach(type -> {
                    defaultAttributeValueRegistry.removeDefaultValue(type, theDefault.attribute);
                });
            }
        });
    }

    private void undoAttributeValidators(Map<String, Set<AttributeValidator>> attributeValidators) {
        attributeValidators.forEach((attributeName, validatorsToRemove) -> {
            Set<AttributeValidator> currentValidators = attributeValidatorRegistry.getValidators(
                    attributeName);
            Set<AttributeValidator> resultingValidators = Sets.difference(currentValidators,
                    validatorsToRemove);
            attributeValidatorRegistry.deregisterValidators(attributeName);

            if (!resultingValidators.isEmpty()) {
                attributeValidatorRegistry.registerValidators(attributeName, resultingValidators);
            }
        });
    }

    private void undoInjectableAttributes(
            List<ServiceRegistration<InjectableAttribute>> injectableAttributeServices) {
        injectableAttributeServices.forEach(ServiceRegistration::unregister);
    }

    private class Outer {
        List<Outer.MetacardType> metacardTypes;

        Map<String, AttributeType> attributeTypes;

        @JsonIgnore
        Map<String, List<Validator>> validators;

        List<Default> defaults;

        List<Injection> inject;

        List<MetacardValidatorDefinition> metacardValidators;

        class MetacardType {
            String type;

            Map<String, MetacardAttribute> attributes;
        }

        class MetacardAttribute {
            boolean required;
        }

        class AttributeType {
            String type;

            boolean tokenized;

            boolean stored;

            boolean indexed;

            boolean multivalued;
        }

        class Validator {
            String validator;

            List<String> arguments;
        }

        class ValidatorCollection extends Validator {
            List<Outer.Validator> validators;
        }

        class Default {
            String attribute;

            String value;

            List<String> metacardTypes;
        }

        class Injection {
            String attribute;

            List<String> metacardTypes;
        }
    }

    class MetacardValidatorDefinition {
        String metacardType;

        Map<String, Object> arguments;
    }

    private class Changeset {
        private final List<ServiceRegistration<MetacardType>> metacardTypeServices =
                new ArrayList<>();

        private final List<ServiceRegistration<MetacardValidator>> metacardValidatorServices =
                new ArrayList<>();

        private final Set<AttributeDescriptor> attributes = new HashSet<>();

        private final List<Outer.Default> defaults = new ArrayList<>();

        private final Map<String, Set<AttributeValidator>> attributeValidators = new HashMap<>();

        private final List<ServiceRegistration<InjectableAttribute>> injectableAttributeServices =
                new ArrayList<>();

    }
}
