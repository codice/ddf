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
package groovy.ddf.catalog.validation.impl

import ddf.catalog.data.impl.AttributeImpl
import ddf.catalog.data.impl.MetacardImpl
import ddf.catalog.validation.ValidationException
import ddf.catalog.validation.impl.validator.RelationshipValidator
import ddf.catalog.validation.report.MetacardValidationReport
import spock.lang.Specification
import spock.lang.Unroll

class RelationshipValidatorSpec extends Specification {
    public static final String MUST_HAVE = "mustHave"
    public static final String CANNOT_HAVE = "cannotHave"
    public static final String CAN_ONLY_HAVE = "canOnlyHave"
    public MetacardImpl metacard = new MetacardImpl()

    def "tostring"() {
        when:
        String tostring = new RelationshipValidator("sourceattribute", "sourcevalue", relationship, "targetattribute", "val1", "val2")
        then:
        tostring == "RelationshipValidator{sourceAttribute='sourceattribute', sourceValue='sourcevalue', relationship='" + relationship + "', targetAttribute='targetattribute', targetValues=[val1, val2]'}"
        where:
        relationship  | _
        MUST_HAVE     | _
        CANNOT_HAVE   | _
        CAN_ONLY_HAVE | _
    }

    def "equal"() {
        when:
        RelationshipValidator validator = new RelationshipValidator("sourceattribute", "sourcevalue", MUST_HAVE, "targetattribute", "val1", "val2")
        RelationshipValidator otherValidator = new RelationshipValidator("sourceattribute", "sourcevalue", MUST_HAVE, "targetattribute", "val1", "val2")
        then:
        validator.equals(otherValidator)
        validator.hashCode() == otherValidator.hashCode()
    }

    def "not equal"() {
        when:
        RelationshipValidator validator = new RelationshipValidator(sourceAttribute, sourceValue, relationship, targetAttribute, targetVal1, targetVal2)
        RelationshipValidator otherValidator = new RelationshipValidator("sourceattribute", "sourcevalue", MUST_HAVE, "targetattribute", "val1", "val2")
        then:
        !validator.equals(otherValidator)
        validator.hashCode() != otherValidator.hashCode()
        where:
        sourceAttribute   | sourceValue   | relationship | targetAttribute   | targetVal1 | targetVal2
        "mismatch"        | "sourcevalue" | MUST_HAVE    | "targetattribute" | "val1"     | "val2"
        "sourceattribute" | "mismatch"    | MUST_HAVE    | "targetattribute" | "val1"     | "val2"
        "sourceattribute" | "sourcevalue" | CANNOT_HAVE  | "targetattribute" | "val1"     | "val2"
        "sourceattribute" | "sourcevalue" | MUST_HAVE    | "mismatch"        | "val1"     | "val2"
        "sourceattribute" | "sourcevalue" | MUST_HAVE    | "targetattribute" | "val1"     | "mismatch"
    }

    @Unroll
    def "validation permutations"(
            boolean expectViolation,
            String sourceAttribute,
            String sourceValue,
            String relationship,
            String targetAttribute,
            String... targetValues) {
        setAttribute("source.undef", null)
        setAttribute("source.def", "blah blah")
        setAttribute("target.undef", null)
        setAttribute("target.def", "blah blah")
        setAttribute("source.multivalued", "one", "two", "three")
        setAttribute("target.multivalued", "one", "two", "three")
        RelationshipValidator validator
        Exception e = null
        try {
            validator = new RelationshipValidator(
                    sourceAttribute, sourceValue, relationship, targetAttribute, targetValues)
        } catch (IllegalArgumentException v) {
            e = v
        }
        when:
        Optional<MetacardValidationReport> metacardValidationReport
        if (validator != null) {
            metacardValidationReport = validator.validateMetacard(metacard)
        }
        then:
        expectViolation == (e != null && e instanceof IllegalArgumentException) || (metacardValidationReport != null && metacardValidationReport.isPresent() && !metacardValidationReport.get().getAttributeValidationViolations().isEmpty())
        when:
        try {
            if (validator != null) {
                validator.validate(metacard)
            }
        } catch (ValidationException v) {
            e = v
        }
        then:
        if (validator != null) {
            expectViolation == (e instanceof ValidationException)
        }
        where:
        expectViolation | sourceAttribute | sourceValue | relationship      | targetAttribute      | targetValues
        false           | "source.def"    | null        | MUST_HAVE         | "target.def"         | null
        false           | "source.def"    | null        | MUST_HAVE         | "target.def"         | [null]
        false           | "source.undef"  | null        | MUST_HAVE         | "target.def"         | null
        false           | "source.undef"  | null        | MUST_HAVE         | "target.def"         | [null]
        false           | "source.def"    | null        | MUST_HAVE         | "target.def"         | ["blah blah"]
        false           | "source.def"    | "blah"      | MUST_HAVE         | "target.undef"       | null
        true            | "source.def"    | null        | MUST_HAVE         | "target.undef"       | null
        true            | "source.def"    | null        | MUST_HAVE         | "target.def"         | ["blah"]
        true            | "source.def"    | "blah blah" | MUST_HAVE         | "target.undef"       | null
        false           | "source.def"    | null        | CANNOT_HAVE       | "target.undef"       | null
        false           | "source.def"    | null        | CANNOT_HAVE       | "target.multivalued" | ["five"]
        false           | "source.def"    | null        | CANNOT_HAVE       | "target.undef"       | ["five"]
        false           | "source.def"    | null        | CANNOT_HAVE       | "target.null"        | ["five"]
        true            | "source.def"    | null        | CANNOT_HAVE       | "target.multivalued" | ["two"]
        true            | "source.def"    | null        | CANNOT_HAVE       | "target.def"         | ["blah blah"]
        true            | "source.def"    | null        | CANNOT_HAVE       | "target.def"         | null
        true            | "source.def"    | null        | CANNOT_HAVE       | "target.def"         | [null]
        false           | "source.def"    | null        | CAN_ONLY_HAVE     | "target.multivalued" | ["one", "two", "three", "four"]
        true            | "source.def"    | null        | CAN_ONLY_HAVE     | "target.multivalued" | ["one", "two", "five"]
        true            | "source.def"    | null        | CAN_ONLY_HAVE     | "target.def"         | null
        true            | "source.def"    | null        | "badRelationship" | "target.def"         | null
    }

    void setAttribute(String attribute, String... value) {
        value = value == null ? new String[0] : value
        metacard.setAttribute(new AttributeImpl(attribute, Arrays.asList(value)))
    }
}
