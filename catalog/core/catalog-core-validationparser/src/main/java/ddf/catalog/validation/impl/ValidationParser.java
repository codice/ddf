/**
 * Copyright (c) Codice Foundation
 *
 * <p>This is free software: you can redistribute it and/or modify it under the terms of the GNU
 * Lesser General Public License as published by the Free Software Foundation, either version 3 of
 * the License, or any later version.
 *
 * <p>This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details. A copy of the GNU Lesser General Public
 * License is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.validation.impl;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.codice.gsonsupport.GsonTypeAdapters.LIST_STRING;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;
import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.AttributeRegistry;
import ddf.catalog.data.DefaultAttributeValueRegistry;
import ddf.catalog.data.InjectableAttribute;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeDescriptorImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.InjectableAttributeImpl;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.data.impl.MetacardTypeImpl;
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.AttributeValidatorRegistry;
import ddf.catalog.validation.MetacardValidator;
import ddf.catalog.validation.ReportingMetacardValidator;
import ddf.catalog.validation.impl.validator.EnumerationValidator;
import ddf.catalog.validation.impl.validator.FutureDateValidator;
import ddf.catalog.validation.impl.validator.ISO3CountryCodeValidator;
import ddf.catalog.validation.impl.validator.MatchAnyValidator;
import ddf.catalog.validation.impl.validator.PastDateValidator;
import ddf.catalog.validation.impl.validator.PatternValidator;
import ddf.catalog.validation.impl.validator.RangeValidator;
import ddf.catalog.validation.impl.validator.RelationshipValidator;
import ddf.catalog.validation.impl.validator.RequiredAttributesMetacardValidator;
import ddf.catalog.validation.impl.validator.SizeValidator;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Type;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.felix.fileinstall.ArtifactInstaller;
import org.codice.ddf.configuration.DictionaryMap;
import org.codice.gsonsupport.GsonTypeAdapters.LongDoubleTypeAdapter;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.ServiceRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ValidationParser implements ArtifactInstaller {

  private static final String METACARD_VALIDATORS_PROPERTY = "metacardvalidators";

  private static final String REQUIRED_ATTRIBUTE_VALIDATOR_PROPERTY = "requiredattributes";

  private static final String SINGLE_VALIDATOR_PROPERTY = "validator";

  private static final String VALIDATORS_PROPERTY = "validators";

  private static final String METACARD_TYPES_PROPERTY = "Metacard Types";

  private static final String DEFAULTS_PROPERTY = "Defaults";

  private static final String INJECTIONS_PROPERTY = "Injections";

  private static final String MATCH_ANY = "match_any";

  private static final String NAME_PROPERTY = "name";

  private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_INSTANT;

  public static final Type OUTER_VALIDATOR_TYPE =
      new TypeToken<List<Outer.Validator>>() {}.getType();

  private final AttributeRegistry attributeRegistry;

  private final AttributeValidatorRegistry attributeValidatorRegistry;

  private final DefaultAttributeValueRegistry defaultAttributeValueRegistry;

  private final Map<String, Changeset> changesetsByFile = new ConcurrentHashMap<>();

  private static final Gson GSON =
      new GsonBuilder()
          .disableHtmlEscaping()
          .registerTypeAdapterFactory(LongDoubleTypeAdapter.FACTORY)
          .registerTypeHierarchyAdapter(Outer.Validator.class, new ValidatorHierarchyAdapter())
          .setLenient()
          .create();

  private static final Logger LOGGER = LoggerFactory.getLogger(ValidationParser.class);

  private final Function<Class, Bundle> bundleLookup;

  public ValidationParser(
      AttributeRegistry attributeRegistry,
      AttributeValidatorRegistry attributeValidatorRegistry,
      DefaultAttributeValueRegistry defaultAttributeValueRegistry) {
    this(
        attributeRegistry,
        attributeValidatorRegistry,
        defaultAttributeValueRegistry,
        FrameworkUtil::getBundle);
  }

  @VisibleForTesting
  ValidationParser(
      AttributeRegistry attributeRegistry,
      AttributeValidatorRegistry attributeValidatorRegistry,
      DefaultAttributeValueRegistry defaultAttributeValueRegistry,
      Function<Class, Bundle> bundleLookup) {
    this.attributeRegistry = attributeRegistry;
    this.attributeValidatorRegistry = attributeValidatorRegistry;
    this.defaultAttributeValueRegistry = defaultAttributeValueRegistry;
    this.bundleLookup = bundleLookup;
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
    return file.getName().endsWith(".json");
  }

  private void apply(File file) throws Exception {
    String data;
    try (InputStream input = new FileInputStream(file)) {
      data = IOUtils.toString(input, StandardCharsets.UTF_8.name());
      LOGGER.debug("Installing file [{}]. Contents:\n{}", file.getAbsolutePath(), data);
    }
    if (StringUtils.isEmpty(data)) {
      LOGGER.debug("File is empty [{}]. Nothing to install.", file.getAbsolutePath());
      return; /* nothing to install */
    }

    Outer outer = GSON.fromJson(data, Outer.class);

    final String filename = file.getName();
    final Changeset changeset = new Changeset();
    changesetsByFile.put(filename, changeset);

    handleSection(changeset, "Attribute Types", outer.attributeTypes, this::parseAttributeTypes);
    handleSection(
        changeset, METACARD_TYPES_PROPERTY, outer.metacardTypes, this::parseMetacardTypes);
    handleSection(changeset, VALIDATORS_PROPERTY, outer.validators, this::parseValidators);
    handleSection(changeset, DEFAULTS_PROPERTY, outer.defaults, this::parseDefaults);
    handleSection(changeset, INJECTIONS_PROPERTY, outer.inject, this::parseInjections);
    handleSection(
        changeset,
        METACARD_VALIDATORS_PROPERTY,
        outer.metacardvalidators,
        this::registerMetacardValidators);
  }

  private <T> void handleSection(
      Changeset changeset,
      String sectionName,
      T sectionData,
      BiFunction<Changeset, T, List<Callable<Boolean>>> parser)
      throws Exception {
    if (sectionData != null) {
      List<Callable<Boolean>> stagedAdds;
      try {
        stagedAdds = parser.apply(changeset, sectionData);
      } catch (Exception e) {
        throw new IllegalArgumentException(
            String.format("Could not parse %s section.", sectionName), e);
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

  private List<Callable<Boolean>> parseAttributeTypes(
      Changeset changeset, Map<String, Outer.AttributeType> attributeTypes) {
    List<Callable<Boolean>> staged = new ArrayList<>();
    for (Map.Entry<String, Outer.AttributeType> entry : attributeTypes.entrySet()) {
      final AttributeDescriptor descriptor =
          new AttributeDescriptorImpl(
              entry.getKey(),
              entry.getValue().indexed,
              entry.getValue().stored,
              entry.getValue().tokenized,
              entry.getValue().multivalued,
              BasicTypes.getAttributeType(entry.getValue().type.replace("_TYPE", "")));

      staged.add(
          () -> {
            attributeRegistry.register(descriptor);
            changeset.attributes.add(descriptor);
            return true;
          });
    }
    return staged;
  }

  @SuppressWarnings("squid:S1149" /* Confined by underlying contract */)
  private List<Callable<Boolean>> parseMetacardTypes(
      Changeset changeset, List<Outer.MetacardType> metacardTypes) {
    List<Callable<Boolean>> staged = new ArrayList<>();
    BundleContext context = getBundleContext();
    for (Outer.MetacardType metacardType : metacardTypes) {
      Set<AttributeDescriptor> attributeDescriptors =
          new HashSet<>(MetacardImpl.BASIC_METACARD.getAttributeDescriptors());
      Set<String> requiredAttributes = new HashSet<>();

      metacardType.attributes.forEach(
          (attributeName, attribute) -> {
            AttributeDescriptor descriptor =
                attributeRegistry
                    .lookup(attributeName)
                    .orElseThrow(
                        () ->
                            new IllegalStateException(
                                String.format(
                                    "Metacard type [%s] includes the attribute [%s], but that attribute is not in the attribute registry.",
                                    metacardType.type, attributeName)));
            attributeDescriptors.add(descriptor);
            if (attribute.required) {
              requiredAttributes.add(attributeName);
            }
          });

      if (!requiredAttributes.isEmpty()) {
        final MetacardValidator validator =
            new RequiredAttributesMetacardValidator(metacardType.type, requiredAttributes);
        staged.add(
            () -> {
              ServiceRegistration<MetacardValidator> registration =
                  context.registerService(MetacardValidator.class, validator, null);
              changeset.metacardValidatorServices.add(registration);
              return registration != null;
            });
      }

      Dictionary<String, Object> properties = new DictionaryMap<>();
      properties.put(NAME_PROPERTY, metacardType.type);
      MetacardType type = new MetacardTypeImpl(metacardType.type, attributeDescriptors);
      staged.add(
          () -> {
            ServiceRegistration<MetacardType> registration =
                context.registerService(MetacardType.class, type, properties);
            changeset.metacardTypeServices.add(registration);
            return registration != null;
          });
    }
    return staged;
  }

  private List<Callable<Boolean>> registerMetacardValidators(
      Changeset changeset,
      List<Map<String, List<MetacardValidatorDefinition>>> metacardValidatorDefinitions) {
    List<Callable<Boolean>> staged = new ArrayList<>();
    BundleContext context = getBundleContext();

    for (Map<String, List<MetacardValidatorDefinition>> mvdMap : metacardValidatorDefinitions) {
      for (Entry<String, List<MetacardValidatorDefinition>> row : mvdMap.entrySet()) {
        try {
          List<MetacardValidator> metacardValidators = getMetacardValidators(row.getValue().get(0));
          metacardValidators.forEach(
              metacardValidator ->
                  staged.add(
                      () -> {
                        ServiceRegistration<MetacardValidator> registration =
                            context.registerService(
                                MetacardValidator.class, metacardValidator, null);
                        changeset.metacardValidatorServices.add(registration);
                        return registration != null;
                      }));

        } catch (IllegalStateException ise) {
          LOGGER.error(
              "Could not register metacard validator for definition: {} {}",
              row.getKey(),
              row.getValue(),
              ise);
        }
      }
    }

    return staged;
  }

  private List<Callable<Boolean>> parseValidators(
      Changeset changeset, Map<String, List<Outer.Validator>> validators) {
    List<Callable<Boolean>> staged = new ArrayList<>();
    for (Map.Entry<String, List<Outer.Validator>> entry : validators.entrySet()) {
      Set<ValidatorWrapper> validatorWrappers = validatorFactory(entry.getKey(), entry.getValue());
      Set<AttributeValidator> attributeValidators =
          validatorWrappers
              .stream()
              .map(ValidatorWrapper::getAttributeValidators)
              .flatMap(Collection::stream)
              .collect(Collectors.toSet());
      String attributeName = entry.getKey();
      staged.add(
          () -> {
            attributeValidatorRegistry.registerValidators(attributeName, attributeValidators);
            changeset.attributeValidators.put(attributeName, attributeValidators);
            return true;
          });
      validatorWrappers
          .stream()
          .map(ValidatorWrapper::getMetacardValidators)
          .flatMap(Collection::stream)
          .collect(Collectors.toSet())
          .forEach(
              validator ->
                  staged.add(
                      () -> {
                        ServiceRegistration<MetacardValidator> registration =
                            getBundleContext()
                                .registerService(MetacardValidator.class, validator, null);
                        changeset.metacardValidatorServices.add(registration);
                        return registration != null;
                      }));
      validatorWrappers
          .stream()
          .map(ValidatorWrapper::getReportingMetacardValidators)
          .flatMap(Collection::stream)
          .collect(Collectors.toSet())
          .forEach(
              validator ->
                  staged.add(
                      () -> {
                        ServiceRegistration<ReportingMetacardValidator> registration =
                            getBundleContext()
                                .registerService(ReportingMetacardValidator.class, validator, null);
                        changeset.reportingMetacardValidatorServices.add(registration);
                        return registration != null;
                      }));
    }
    return staged;
  }

  private Set<ValidatorWrapper> validatorFactory(
      String attribute, List<Outer.Validator> validators) {
    return validators
        .stream()
        .filter(Objects::nonNull)
        .filter(v -> StringUtils.isNotBlank(v.validator))
        .map((Outer.Validator validator) -> getValidator(attribute, validator))
        .collect(toSet());
  }

  private List<MetacardValidator> getMetacardValidators(
      MetacardValidatorDefinition validatorDefinition) {

    if (!REQUIRED_ATTRIBUTE_VALIDATOR_PROPERTY.equals(validatorDefinition.validator)) {
      throw new IllegalStateException(
          String.format("Validator does not exist. (%s)", validatorDefinition.validator));
    }

    if (CollectionUtils.isEmpty(validatorDefinition.requiredattributes)) {
      throw new IllegalStateException(
          "Required Attributes Validator received invalid configuration");
    }

    return ImmutableList.of(
        new RequiredAttributesMetacardValidator(
            validatorDefinition.validator,
            ImmutableSet.copyOf(validatorDefinition.requiredattributes)));
  }

  private ValidatorWrapper getValidator(String key, Outer.Validator validator) {
    ValidatorWrapper wrapper = new ValidatorWrapper();
    List<String> arguments = validator.arguments;
    switch (validator.validator) {
      case "size":
        {
          long lmin = Long.parseLong(arguments.get(0));
          long lmax = Long.parseLong(arguments.get(1));
          wrapper.attributeValidator(new SizeValidator(lmin, lmax));
          break;
        }
      case "pattern":
        {
          String regex = arguments.get(0);
          wrapper.attributeValidator(new PatternValidator(regex));
          break;
        }
      case "pastdate":
        {
          wrapper.attributeValidator(PastDateValidator.getInstance());
          break;
        }
      case "futuredate":
        {
          wrapper.attributeValidator(FutureDateValidator.getInstance());
          break;
        }
      case "enumeration":
        {
          Set<String> values = new HashSet<>(arguments);
          wrapper.attributeValidator(new EnumerationValidator(values, false));
          break;
        }
      case "enumerationignorecase":
        {
          Set<String> values = new HashSet<>(arguments);
          wrapper.attributeValidator(new EnumerationValidator(values, true));
          break;
        }
      case "range":
        {
          BigDecimal min = new BigDecimal(arguments.get(0));
          BigDecimal max = new BigDecimal(arguments.get(1));
          if (arguments.size() > 2) {
            BigDecimal epsilon = new BigDecimal(arguments.get(2));
            wrapper.attributeValidator(new RangeValidator(min, max, epsilon));
          }
          wrapper.attributeValidator(new RangeValidator(min, max));
          break;
        }
      case "iso3_country":
        {
          wrapper.attributeValidator(new ISO3CountryCodeValidator(false));
          break;
        }
      case "iso3_countryignorecase":
        {
          wrapper.attributeValidator(new ISO3CountryCodeValidator(true));
          break;
        }
      case MATCH_ANY:
        {
          List<Outer.Validator> collection = ((Outer.ValidatorCollection) validator).validators;
          List<AttributeValidator> attributeValidators =
              collection
                  .stream()
                  .map((Outer.Validator key1) -> getValidator(key, key1))
                  .map(ValidatorWrapper::getAttributeValidators)
                  .flatMap(Collection::stream)
                  .collect(Collectors.toList());
          MatchAnyValidator matchAnyValidator = new MatchAnyValidator(attributeValidators);
          wrapper.attributeValidator(matchAnyValidator);
          break;
        }
      case "relationship":
        {
          if (arguments.size() < 4) {
            throw new IllegalArgumentException("Not enough parameters for relationship validator");
          }
          RelationshipValidator relationshipValidator =
              new RelationshipValidator(
                  key,
                  arguments.get(0),
                  arguments.get(1),
                  arguments.get(2),
                  arguments.subList(3, arguments.size()).toArray(new String[] {}));
          wrapper.metacardValidator(relationshipValidator);
          wrapper.reportingMetacardValidator(relationshipValidator);
          break;
        }
      default:
        throw new IllegalStateException("Validator does not exist. (" + validator.validator + ")");
    }
    return wrapper;
  }

  /** TODO DDF-3578 once MetacardValidator is eliminated, this pattern can be cleaned up */
  private class ValidatorWrapper {
    private List<MetacardValidator> metacardValidators = new ArrayList<>();
    private List<ReportingMetacardValidator> reportingMetacardValidators = new ArrayList<>();
    private List<AttributeValidator> attributeValidators = new ArrayList<>();

    void attributeValidator(AttributeValidator validator) {
      attributeValidators.add(validator);
    }

    void metacardValidator(MetacardValidator validator) {
      metacardValidators.add(validator);
    }

    void reportingMetacardValidator(ReportingMetacardValidator validator) {
      reportingMetacardValidators.add(validator);
    }

    List<MetacardValidator> getMetacardValidators() {
      return metacardValidators;
    }

    List<ReportingMetacardValidator> getReportingMetacardValidators() {
      return reportingMetacardValidators;
    }

    List<AttributeValidator> getAttributeValidators() {
      return attributeValidators;
    }
  }

  private Serializable parseDefaultValue(AttributeDescriptor descriptor, String defaultValue) {
    switch (descriptor.getType().getAttributeFormat()) {
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

  private List<Callable<Boolean>> parseDefaults(Changeset changeset, List<Outer.Default> defaults) {
    return defaults
        .stream()
        .map(
            defaultObj -> {
              String attribute = defaultObj.attribute;
              AttributeDescriptor descriptor =
                  attributeRegistry
                      .lookup(attribute)
                      .orElseThrow(
                          () ->
                              new IllegalStateException(
                                  String.format(
                                      "The default value for the attribute [%s] cannot be parsed because that attribute has not been registered in the attribute registry",
                                      attribute)));
              Serializable defaultValue = parseDefaultValue(descriptor, defaultObj.value);
              List<String> metacardTypes = defaultObj.metacardTypes;
              if (CollectionUtils.isEmpty(metacardTypes)) {
                return (Callable<Boolean>)
                    () -> {
                      defaultAttributeValueRegistry.setDefaultValue(attribute, defaultValue);
                      changeset.defaults.add(defaultObj);
                      return true;
                    };
              } else {
                return (Callable<Boolean>)
                    () -> {
                      metacardTypes.forEach(
                          metacardType ->
                              defaultAttributeValueRegistry.setDefaultValue(
                                  metacardType, attribute, defaultValue));
                      changeset.defaults.add(defaultObj);
                      return true;
                    };
              }
            })
        .collect(toList());
  }

  private List<Callable<Boolean>> parseInjections(
      Changeset changeset, List<Outer.Injection> injections) {
    BundleContext context = getBundleContext();
    return injections
        .stream()
        .map(
            injection ->
                (Callable<Boolean>)
                    () -> {
                      String attribute = injection.attribute;
                      InjectableAttribute injectableAttribute =
                          new InjectableAttributeImpl(attribute, injection.metacardTypes);
                      ServiceRegistration<InjectableAttribute> injectableAttributeService =
                          context.registerService(
                              InjectableAttribute.class, injectableAttribute, null);
                      changeset.injectableAttributeServices.add(injectableAttributeService);
                      return true;
                    })
        .collect(toList());
  }

  private BundleContext getBundleContext() {
    return Optional.ofNullable(bundleLookup.apply(getClass()))
        .map(Bundle::getBundleContext)
        .orElseThrow(
            () ->
                new IllegalStateException("Could not get the bundle for " + getClass().getName()));
  }

  private void undo(File file) {
    final String filename = file.getName();

    LOGGER.debug("Reversing the changes applied by file [{}].", filename);

    final Changeset changeset = changesetsByFile.get(filename);
    if (changeset != null) {
      undoMetacardTypes(changeset.metacardTypeServices);
      undoMetacardValidators(changeset.metacardValidatorServices);
      undoReportingMetacardValidators(changeset.reportingMetacardValidatorServices);
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

  private void undoReportingMetacardValidators(
      List<ServiceRegistration<ReportingMetacardValidator>> reportingMetacardValidatorServices) {
    reportingMetacardValidatorServices.forEach(ServiceRegistration::unregister);
  }

  private void undoAttributes(Set<AttributeDescriptor> attributes) {
    attributes.forEach(attributeRegistry::deregister);
  }

  private void undoDefaults(List<Outer.Default> defaults) {
    defaults.forEach(
        theDefault -> {
          if (CollectionUtils.isEmpty(theDefault.metacardTypes)) {
            defaultAttributeValueRegistry.removeDefaultValue(theDefault.attribute);
          } else {
            theDefault.metacardTypes.forEach(
                type ->
                    defaultAttributeValueRegistry.removeDefaultValue(type, theDefault.attribute));
          }
        });
  }

  private void undoAttributeValidators(Map<String, Set<AttributeValidator>> attributeValidators) {
    attributeValidators.forEach(
        (attributeName, validatorsToRemove) -> {
          Set<AttributeValidator> currentValidators =
              attributeValidatorRegistry.getValidators(attributeName);
          Set<AttributeValidator> resultingValidators =
              Sets.difference(currentValidators, validatorsToRemove);
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

  private static class Outer {
    List<Outer.MetacardType> metacardTypes;

    Map<String, Outer.AttributeType> attributeTypes;

    Map<String, List<Outer.Validator>> validators;

    List<Outer.Default> defaults;

    List<Outer.Injection> inject;

    // Name casing matches JSON expectations; do not change
    List<Map<String, List<MetacardValidatorDefinition>>> metacardvalidators;

    private static class MetacardType {
      String type;
      Map<String, MetacardAttribute> attributes;
    }

    private static class MetacardAttribute {
      boolean required;
    }

    private static class AttributeType {
      String type;
      boolean tokenized;
      boolean stored;
      boolean indexed;
      boolean multivalued;
    }

    private static class Default {
      String attribute;
      String value;
      List<String> metacardTypes;
    }

    private static class Injection {
      String attribute;
      List<String> metacardTypes;
    }

    private static class Validator {
      @SuppressWarnings("squid:S1700" /* Required to match expected JSON structure */)
      String validator;

      List<String> arguments;

      Validator(String validator) {
        this.validator = validator;
      }

      Validator(String validator, List<String> arguments) {
        this(validator);
        this.arguments = arguments;
      }
    }

    private static class ValidatorCollection extends Validator {
      List<Outer.Validator> validators;

      ValidatorCollection(String validatorName, List<Validator> validators) {
        super(validatorName);
        this.validators = validators;
      }
    }
  }

  private static class MetacardValidatorDefinition {
    String validator;
    List<String> requiredattributes;
  }

  private class Changeset {
    private final List<ServiceRegistration<MetacardType>> metacardTypeServices = new ArrayList<>();

    private final List<ServiceRegistration<MetacardValidator>> metacardValidatorServices =
        new ArrayList<>();

    private final List<ServiceRegistration<ReportingMetacardValidator>>
        reportingMetacardValidatorServices = new ArrayList<>();

    private final Set<AttributeDescriptor> attributes = new HashSet<>();

    private final List<Outer.Default> defaults = new ArrayList<>();

    private final Map<String, Set<AttributeValidator>> attributeValidators = new HashMap<>();

    private final List<ServiceRegistration<InjectableAttribute>> injectableAttributeServices =
        new ArrayList<>();
  }

  private static class ValidatorHierarchyAdapter implements JsonDeserializer<Outer.Validator> {
    @Override
    public Outer.Validator deserialize(
        JsonElement jsonElement, Type type, JsonDeserializationContext context) {
      JsonObject object = jsonElement.getAsJsonObject();
      String validatorName =
          context.deserialize(object.get(SINGLE_VALIDATOR_PROPERTY), String.class);

      Outer.Validator result = null;

      // If the validator has a collection of validators, it must be a collection type
      JsonElement validators = object.get(VALIDATORS_PROPERTY);
      if (validators != null) {
        result =
            new Outer.ValidatorCollection(
                validatorName, context.deserialize(validators, OUTER_VALIDATOR_TYPE));
      }

      if (result == null) {
        result =
            new Outer.Validator(
                validatorName, context.deserialize(object.get("arguments"), LIST_STRING));
      }
      return result;
    }
  }
}
