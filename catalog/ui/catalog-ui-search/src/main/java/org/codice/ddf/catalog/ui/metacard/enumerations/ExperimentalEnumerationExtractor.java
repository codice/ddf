package org.codice.ddf.catalog.ui.metacard.enumerations;

import static org.apache.commons.lang.StringUtils.isBlank;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import javax.annotation.Nullable;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;

import ddf.catalog.data.AttributeDescriptor;
import ddf.catalog.data.MetacardType;
import ddf.catalog.data.impl.AttributeImpl;
import ddf.catalog.data.impl.BasicTypes;
import ddf.catalog.data.impl.MetacardImpl;
import ddf.catalog.validation.AttributeValidator;
import ddf.catalog.validation.AttributeValidatorRegistry;
import ddf.catalog.validation.violation.ValidationViolation;
import spark.Experimental;

@Experimental // TODO (RCZ) - This is sparks experimental iface, could we use?
public class ExperimentalEnumerationExtractor {
    private final AttributeValidatorRegistry attributeValidatorRegistry;

    private final List<MetacardType> metacardTypes;

    public ExperimentalEnumerationExtractor(AttributeValidatorRegistry attributeValidatorRegistry,
            List<MetacardType> metacardTypes) {
        this.attributeValidatorRegistry = attributeValidatorRegistry;
        this.metacardTypes = metacardTypes;
    }

    public Map<String, Set<String>> getEnumerations(@Nullable String metacardType) {
        if (isBlank(metacardType)) {
            metacardType = BasicTypes.BASIC_METACARD.getName();
        }
        MetacardType type = getTypeFromName(metacardType);

        return type.getAttributeDescriptors()
                .stream()
                .flatMap(ad -> attributeValidatorRegistry.getValidators(ad.getName())
                        .stream()
                        .map(av -> av.validate(new AttributeImpl(ad.getName(),
                                (Serializable) null))))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .filter(avr -> !avr.getSuggestedValues()
                        .isEmpty())
                .map(avr -> avr.getAttributeValidationViolations()
                        .stream()
                        .map(ValidationViolation::getAttributes)
                        .flatMap(Set::stream)
                        .distinct()
                        .collect(Collectors.toMap(o -> o, o -> avr.getSuggestedValues())))
                .reduce((m1, m2) -> {
                    m2.entrySet().forEach(e -> m1.merge(e.getKey(), e.getValue(), Sets::union));
                    return m1;
                }).orElseGet(HashMap::new);

    }

    private MetacardType getTypeFromName(String metacardType) {
        return metacardTypes.stream()
                .filter(mt -> mt.getName()
                        .equals(metacardType))
                .findFirst()
                .orElseThrow(IllegalArgumentException::new);
    }
}
