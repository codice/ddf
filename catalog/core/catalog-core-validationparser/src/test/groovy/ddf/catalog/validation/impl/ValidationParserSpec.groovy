package ddf.catalog.validation.impl

import ddf.catalog.data.AttributeRegistry
import ddf.catalog.data.DefaultAttributeValueRegistry
import ddf.catalog.data.InjectableAttribute
import ddf.catalog.data.MetacardType
import ddf.catalog.data.defaultvalues.DefaultAttributeValueRegistryImpl
import ddf.catalog.data.impl.*
import ddf.catalog.data.impl.types.CoreAttributes
import ddf.catalog.validation.AttributeValidatorRegistry
import ddf.catalog.validation.MetacardValidator
import ddf.catalog.validation.ReportingMetacardValidator
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceRegistration
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.ZoneOffset

import static org.mockito.Matchers.isNull

class ValidationParserSpec extends Specification {

    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    ValidationParser validationParser

    AttributeRegistry attributeRegistry

    AttributeValidatorRegistry attributeValidatorRegistry

    DefaultAttributeValueRegistry defaultAttributeValueRegistry

    File file

    Bundle mockBundle = Mock(Bundle)

    BundleContext mockBundleContext = Mock(BundleContext)

    void setup() {
        attributeRegistry = new AttributeRegistryImpl()

        attributeValidatorRegistry = new AttributeValidatorRegistryImpl()

        attributeRegistry.registerMetacardType(new MetacardTypeImpl("testMetacard", Arrays.asList(new CoreAttributes())))

        defaultAttributeValueRegistry = new DefaultAttributeValueRegistryImpl()
        validationParser = new ValidationParser(attributeRegistry, attributeValidatorRegistry,
                defaultAttributeValueRegistry, { clazz -> mockBundle })

        mockBundle.getBundleContext() >> mockBundleContext

        file = temporaryFolder.newFile("temp.json")
    }

    def "test blank file"() {
        when: "Blank file installed should be noop"
        validationParser.install(file)

        then:
        notThrown(Exception)
    }

    def "test empty object"() {
        setup:
        file.withPrintWriter { it.write('{}') }

        when:
        validationParser.install(file)

        then:
        notThrown(Exception)
    }

    def "test garbage file"() {
        setup:
        file.withPrintWriter { it.write('lk124!%^(#)zjlksdf@#%!@%spacecats243623ZCBV\\|') }

        when:
        validationParser.install(file)

        then:
        thrown(Exception)
    }

    def "test valid file install then uninstall"() {
        setup:
        file.withPrintWriter { it.write(valid) }

        ServiceRegistration<MetacardType> typeService1 = Mock(ServiceRegistration)
        ServiceRegistration<MetacardType> typeService2 = Mock(ServiceRegistration)
        ServiceRegistration<InjectableAttribute> injectService1 = Mock(ServiceRegistration)
        ServiceRegistration<InjectableAttribute> injectService2 = Mock(ServiceRegistration)
        ServiceRegistration<MetacardValidator> validatorService = Mock(ServiceRegistration)

        def type1Name = "type1"
        def type2Name = "type2"

        def attribute1Name = "attribute1"
        def attribute2Name = "attribute2"

        when: "the definition file is installed"
        validationParser.install(file)

        then: "a required attributes metacard validator is registered as a service"
        1 * mockBundleContext.registerService(
                MetacardValidator.class, _ as MetacardValidator, null) >> validatorService

        and: "the metacard types are registered as services"
        1 * mockBundleContext.registerService(MetacardType.class, _ as MetacardType, {
            it.get("name") == type1Name
        }) >> typeService1
        1 * mockBundleContext.registerService(MetacardType.class, _ as MetacardType, {
            it.get("name") == type2Name
        }) >> typeService2

        and: "the two attributes are registered in the attribute registry"
        attributeRegistry.lookup(attribute1Name).isPresent()
        attributeRegistry.lookup(attribute2Name).isPresent()

        and: "the four attribute validators are registered in the attribute validator registry"
        attributeValidatorRegistry.getValidators(attribute1Name).size() == 4

        and: "the default values are registered in the default value registry"
        defaultAttributeValueRegistry.getDefaultValue(type2Name, attribute1Name).isPresent()
        defaultAttributeValueRegistry.getDefaultValue(type2Name, attribute1Name).get() == "value1"

        defaultAttributeValueRegistry.getDefaultValue(type1Name, attribute2Name).isPresent()
        defaultAttributeValueRegistry.getDefaultValue(type1Name, attribute2Name).get() == "value2"
        !defaultAttributeValueRegistry.getDefaultValue(type2Name, attribute2Name).isPresent()

        and: "the injectable attribute services are registered"
        2 * mockBundleContext.registerService(InjectableAttribute.class, _ as InjectableAttribute,
                null) >>> [injectService1, injectService2]

        when: "the definition file is uninstalled"
        validationParser.uninstall(file)

        then: "the required attributes metacard validator service is deregistered"
        1 * validatorService.unregister()

        and: "the metacard type services are deregistered"
        1 * typeService1.unregister()
        1 * typeService2.unregister()

        and: "the two attributes are deregistered"
        !attributeRegistry.lookup(attribute1Name).isPresent()
        !attributeRegistry.lookup(attribute2Name).isPresent()

        and: "the two attribute validators are deregistered"
        attributeValidatorRegistry.getValidators(attribute1Name).size() == 0

        and: "the default values are deregistered"
        !defaultAttributeValueRegistry.getDefaultValue(type2Name, attribute1Name).isPresent()
        !defaultAttributeValueRegistry.getDefaultValue(type1Name, attribute2Name).isPresent()

        and: "the injectable attribute services are deregistered"
        1 * injectService1.unregister()
        1 * injectService2.unregister()
    }

    def "test valid file update"() {
        setup:
        file.withPrintWriter { it.write(valid) }

        def ServiceRegistration<MetacardType> typeService1 = Mock(ServiceRegistration)
        def ServiceRegistration<MetacardType> typeService2 = Mock(ServiceRegistration)
        def ServiceRegistration<InjectableAttribute> injectService1 = Mock(ServiceRegistration)
        def ServiceRegistration<InjectableAttribute> injectService2 = Mock(ServiceRegistration)
        def ServiceRegistration<MetacardValidator> mockValidatorService = Mock(ServiceRegistration)

        def type1Name = "type1"
        def type2Name = "type2"

        def updatedType1Name = "type1-updated"
        def updatedType2Name = "type2-updated"

        def attribute1Name = "attribute1"
        def attribute2Name = "attribute2"

        def updatedAttribute1Name = "attribute1-updated"
        def updatedAttribute2Name = "attribute2-updated"

        when: "the file is installed then updated"
        validationParser.install(file)

        String updatedFileContents = valid.replaceAll(type1Name, updatedType1Name)
                .replaceAll(type2Name, updatedType2Name)
                .replaceAll(attribute1Name, updatedAttribute1Name)
                .replaceAll(attribute2Name, updatedAttribute2Name)

        file.withPrintWriter({ it.write(updatedFileContents) })
        validationParser.update(file)

        then: "a required attributes metacard validator service is registered on the install"
        1 * mockBundleContext.registerService(
                MetacardValidator.class, _ as MetacardValidator, null) >> mockValidatorService

        and: "two metacard type services are registered on the install"
        1 * mockBundleContext.registerService(MetacardType.class, _ as MetacardType, {
            it.get("name") == type1Name
        }) >> typeService1
        1 * mockBundleContext.registerService(MetacardType.class, _ as MetacardType, {
            it.get("name") == type2Name
        }) >> typeService2

        and: "two injectable attribute services are registered on the install"
        1 * mockBundleContext.registerService(InjectableAttribute.class, {
            it.attribute() == attribute1Name
        }, null) >> injectService1
        1 * mockBundleContext.registerService(InjectableAttribute.class, {
            it.attribute() == attribute2Name
        }, null) >> injectService2

        and: "the required attributes metacard validator service is deregistered on the update"
        1 * mockValidatorService.unregister()

        and: "the two metacard type services are deregistered"
        1 * typeService1.unregister()
        1 * typeService2.unregister()

        and: "the two attributes are deregistered"
        !attributeRegistry.lookup(attribute1Name).isPresent()
        !attributeRegistry.lookup(attribute2Name).isPresent()

        and: "the attribute validators are deregistered"
        attributeValidatorRegistry.getValidators(attribute1Name).size() == 0

        and: "the default values are deregistered"
        !defaultAttributeValueRegistry.getDefaultValue(type2Name, attribute1Name).isPresent()

        and: "the two injectable attribute services are deregistered"
        1 * injectService1.unregister()
        1 * injectService2.unregister()

        then: "a new required attributes metacard validator service is registered"
        1 * mockBundleContext.registerService(MetacardValidator.class, _ as MetacardValidator, null)

        and: "two new metacard type services are registered"
        1 * mockBundleContext.registerService(MetacardType.class, _ as MetacardType, {
            it.get("name") == updatedType1Name
        })
        1 * mockBundleContext.registerService(MetacardType.class, _ as MetacardType, {
            it.get("name") == updatedType2Name
        })

        and: "two new attributes are registered"
        attributeRegistry.lookup(updatedAttribute1Name).isPresent()
        attributeRegistry.lookup(updatedAttribute2Name).isPresent()

        and: "four new attribute validators are registered"
        attributeValidatorRegistry.getValidators(updatedAttribute1Name).size() == 4

        and: "two new default values are registered"
        defaultAttributeValueRegistry.getDefaultValue(updatedType2Name, updatedAttribute1Name).isPresent()
        defaultAttributeValueRegistry.getDefaultValue(updatedType2Name, updatedAttribute1Name).get() == "value1"

        defaultAttributeValueRegistry.getDefaultValue(updatedType1Name, updatedAttribute2Name).isPresent()
        defaultAttributeValueRegistry.getDefaultValue(updatedType1Name, updatedAttribute2Name).get() == "value2"
        !defaultAttributeValueRegistry.getDefaultValue(updatedType2Name, updatedAttribute2Name).isPresent()

        and: "two new injectable attribute services are registered"
        1 * mockBundleContext.registerService(InjectableAttribute.class, {
            it.attribute() == updatedAttribute1Name
        }, null)
        1 * mockBundleContext.registerService(InjectableAttribute.class, {
            it.attribute() == updatedAttribute2Name
        }, null)
    }

    def "test default values"() {
        setup:
        file.withPrintWriter { it.write(defaultValues) }

        when:
        validationParser.install(file)

        then:
        verifyDefaultValue("type2", "title", "Default Title")
        def expectedDateTime = OffsetDateTime.of(2020, 2, 2, 2, 2, 2, 0, ZoneOffset.UTC)
        verifyDefaultValue("expiration", Date.from(expectedDateTime.toInstant()))
        verifyDefaultValue("thumbnail", [0x41, 0x42, 0x43] as byte[])
        verifyDefaultValue("short", -123)
        verifyDefaultValue("type1", "integer", 1234567890)
        verifyDefaultValue("long", 1125899906842624)
        verifyDefaultValue("type1", "float", -90.912f)
        verifyDefaultValue("type2", "float", -90.912f)
        verifyDefaultValue("double", 84812938.293818)
        verifyDefaultValue("boolean", true)
    }

    void verifyDefaultValue(attributeName, expected) {
        Optional<Serializable> optional = defaultAttributeValueRegistry.getDefaultValue("", attributeName)
        assert optional.isPresent()
        assert optional.get() == expected
    }

    void verifyDefaultValue(metacardTypeName, attributeName, expected) {
        Optional<Serializable> optional = defaultAttributeValueRegistry.getDefaultValue(metacardTypeName, attributeName)
        assert optional.isPresent()
        assert optional.get() == expected
    }

    def "test invalid validators"() {
        setup:
        file.withPrintWriter { it.write(invalidValidator) }

        when:
        validationParser.install(file)

        then:
        thrown(IllegalArgumentException)
    }

    def "ensure transaction component completely fails if one part fails"() {
        setup: "break the validators section, so no validators should get in"
        file.withPrintWriter { it.write(valid.replace("pattern", "spacecats")) }

        when:
        validationParser.install(file)

        then:
        thrown(IllegalArgumentException)
        attributeRegistry.lookup("attribute1").isPresent()
        attributeValidatorRegistry.getValidators("attribute1").size() == 0
    }

    def "test injections"() {
        setup:
        file.withPrintWriter { it.write(valid) }

        when:
        validationParser.install(file)

        then:
        1 * mockBundleContext.registerService(InjectableAttribute.class, {
            it.attribute() == "attribute1"
            it.metacardTypes().isEmpty()
        }, null)

        1 * mockBundleContext.registerService(InjectableAttribute.class, {
            it.attribute() == "attribute2"
            it.metacardTypes() == ["other.type.1", "other.type.2"] as Set
        }, null)
    }

    def "test metacard types"() {
        setup:
        file.withPrintWriter { it.write(valid) }

        def attribute1Name = "attribute1";
        def attribute2Name = "attribute2";
        def expectedAttribute1 = new AttributeDescriptorImpl(
                attribute1Name, true, true, false, false, BasicTypes.STRING_TYPE);
        def expectedAttribute2 = new AttributeDescriptorImpl(
                attribute2Name, true, true, false, true, BasicTypes.XML_TYPE);

        def type1Name = "type1";
        def type2Name = "type2";

        when:
        validationParser.install(file)

        then:
        1 * mockBundleContext.registerService(MetacardType.class, {
            it == new MetacardTypeImpl(type1Name, MetacardImpl.BASIC_METACARD,
                    [expectedAttribute1, expectedAttribute2] as Set)
        }, { it.get("name") == type1Name })
        1 * mockBundleContext.registerService(MetacardType.class, {
            it == new MetacardTypeImpl(type2Name, MetacardImpl.BASIC_METACARD,
                    [expectedAttribute1] as Set)
        }, { it.get("name") == type2Name })
    }

    def "test metacard validators"() {
        setup:
        file.withPrintWriter { it.write(metacardValidator) }

        when:
        validationParser.install(file)

        then:
        1 * mockBundleContext.registerService(MetacardValidator.class, _ as MetacardValidator, isNull())
    }

    def "test invalid required attribute metacard validator"() {
        setup:
        file.withPrintWriter { it.write(invalidRequiredAttrMetacardValidator) }

        when:
        validationParser.install(file)

        then:
        0 * mockBundleContext.registerService(MetacardValidator.class, _ as MetacardValidator, isNull())
    }

    def "test invalid metacard validators"() {
        setup:
        file.withPrintWriter { it.write(invalidMetacardValidator) }

        when:
        validationParser.install(file)

        then:
        0 * mockBundleContext.registerService(MetacardValidator.class, _ as MetacardValidator, isNull())
    }

    def "test match_any validator "() {
        setup:
        file.withPrintWriter { it.write(matchAnyAttributeValidator) }

        when:
        validationParser.install(file)

        then:
        attributeValidatorRegistry.getValidators("title").size() == 2
    }

    def "test relationship validator "() {
        setup:
        file.withPrintWriter { it.write(relationshipAttributeValidator) }

        when:
        validationParser.install(file)

        then:
        1 * mockBundleContext.registerService(_, _ as MetacardValidator, _)
        1 * mockBundleContext.registerService(_, _ as ReportingMetacardValidator, _)
    }


    String valid = '''
{
    "metacardTypes": [
        {
            "type": "type1",
            "attributes": {
                "attribute1": {
                    "required": true
                },
                "attribute2": {
                    "required": false
                }
            }
        },
        {
            "type": "type2",
            "attributes": {
                "attribute1": {
                    "required": false
                }
            }
        }
    ],
    "attributeTypes": {
        "attribute1": {
            "type": "STRING_TYPE",
            "stored": true,
            "indexed": true,
            "tokenized": false,
            "multivalued": false
        },
        "attribute2": {
            "type": "XML_TYPE",
            "stored": true,
            "indexed": true,
            "tokenized": false,
            "multivalued": true
        }
    },
    "validators": {
        "attribute1": [
            {
                "validator": "size",
                "arguments": ["0", "128"]
            },
            {
                "validator": "pattern",
                "arguments": ["(hi)+\\\\d"]
            },
            {
                "validator": "iso3_country"
            },
            {
                "validator": "iso3_countryignorecase"
            }
        ]
    },
    "defaults": [
        {
            "attribute": "attribute1",
            "value": "value1"
        },
        {
            "attribute": "attribute2",
            "value": "value2",
            "metacardTypes": ["type1"]
        }
    ],
    "inject": [
        {
            "attribute": "attribute1"
        },
        {
            "attribute": "attribute2",
            "metacardTypes": ["other.type.1", "other.type.2"]
        }
    ]
}
'''

    String defaultValues = '''
{
    "defaults": [
        {
            "attribute": "short",
            "value": "-123"
        },
        {
            "attribute": "integer",
            "value": "1234567890",
            "metacardTypes": ["type1"]
        },
        {
            "attribute": "long",
            "value": "1125899906842624"
        },
        {
            "attribute": "float",
            "value": "-90.912",
            "metacardTypes": ["type1", "type2"]
        },
        {
            "attribute": "double",
            "value": "84812938.293818"
        },
        {
            "attribute": "boolean",
            "value": "true"
        },
        {
            "attribute": "expiration",
            "value": "2020-02-02T02:02:02Z"
        },
        {
            "attribute": "title",
            "value": "Default Title",
            "metacardTypes": ["type2"]
        },
        {
            "attribute": "thumbnail",
            "value": "ABC"
        }
    ],
    "attributeTypes": {
        "short": {
            "type": "SHORT_TYPE",
            "stored": false,
            "indexed": false,
            "tokenized": false,
            "multivalued": false
        },
        "integer": {
            "type": "INTEGER_TYPE",
            "stored": false,
            "indexed": false,
            "tokenized": false,
            "multivalued": false
        },
        "long": {
            "type": "LONG_TYPE",
            "stored": false,
            "indexed": false,
            "tokenized": false,
            "multivalued": false
        },
        "float": {
            "type": "FLOAT_TYPE",
            "stored": false,
            "indexed": false,
            "tokenized": false,
            "multivalued": false
        },
        "double": {
            "type": "DOUBLE_TYPE",
            "stored": false,
            "indexed": false,
            "tokenized": false,
            "multivalued": false
        },
        "boolean": {
            "type": "BOOLEAN_TYPE",
            "stored": false,
            "indexed": false,
            "tokenized": false,
            "multivalued": false
        }
    }
}
'''

    String invalidValidator = '''
{
    "validators": {
        "cool-attribute": [
            {
                "validator": "spacecats",
                "arguments": ["(hi)+\\\\d"]
            }
        ]
    }
}
'''

    String metacardValidator = '''
{
  "metacardvalidators": [{
    "fallback.common": [{
        "validator": "requiredattributes",
        "requiredattributes": [
          "id",
          "title",
          "attr"
        ]
    }]
  }]
}
'''

    String invalidRequiredAttrMetacardValidator = '''
{
  "metacardvalidators": [{
    "fallback.common": [{
        "validator": "requiredattributes",
        "requiredattributes": []
    }]
  }]
}
'''

    String invalidMetacardValidator = '''
{
  "metacardvalidators": [{
    "fallback.common": [{
        "validator": "invalidator",
        "requiredattributes": [
          "id",
          "title",
          "attr"
        ]
    }]
  }]
}
'''

    String matchAnyAttributeValidator = '''
{
    "validators": {
        "title": [
            {
                "validator": "size",
                "arguments": ["0", "128"]
            },
            {
                "validator": "match_any",
                "validators": [
                    {
                        "validator": "enumeration",
                        "arguments": ["abe", "lincoln"]
                    },
                    {
                        "validator": "pattern",
                        "arguments": ["sc-[0-9]\\\\+"]
                    }
                ]
            }
        ]
    }
}
'''

    String relationshipAttributeValidator = '''
{
    "validators": {
        "title": [
            {
                "validator": "relationship",
                "arguments": [null, "mustHave", "description", null]
            }
        ]
    }
}
'''
}
