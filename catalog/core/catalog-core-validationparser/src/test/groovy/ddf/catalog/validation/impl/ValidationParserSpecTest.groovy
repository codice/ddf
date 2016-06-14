package ddf.catalog.validation.impl

import static org.mockito.Mockito.when
import static org.powermock.api.mockito.PowerMockito.mockStatic

import ddf.catalog.data.AttributeRegistry
import ddf.catalog.data.DefaultAttributeValueRegistry
import ddf.catalog.data.MetacardType
import ddf.catalog.data.defaultvalues.DefaultAttributeValueRegistryImpl
import ddf.catalog.data.impl.AttributeRegistryImpl
import ddf.catalog.validation.AttributeValidatorRegistry
import ddf.catalog.validation.MetacardValidator
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import org.osgi.framework.Bundle
import org.osgi.framework.BundleContext
import org.osgi.framework.FrameworkUtil
import org.powermock.core.classloader.annotations.PrepareForTest
import org.powermock.modules.junit4.rule.PowerMockRule
import spock.lang.Specification

import java.time.OffsetDateTime
import java.time.ZoneOffset

@PrepareForTest(FrameworkUtil.class)
class ValidationParserSpecTest extends Specification {
    @Rule
    PowerMockRule powerMockRule = new PowerMockRule()

    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    ValidationParser validationParser

    AttributeRegistry attributeRegistry

    AttributeValidatorRegistry attributeValidatorRegistry

    DefaultAttributeValueRegistry defaultAttributeValueRegistry

    File file

    void setup() {
        attributeRegistry = new AttributeRegistryImpl()

        attributeValidatorRegistry = new AttributeValidatorRegistryImpl()

        defaultAttributeValueRegistry = new DefaultAttributeValueRegistryImpl()

        validationParser = new ValidationParser(attributeRegistry, attributeValidatorRegistry, defaultAttributeValueRegistry)

        file = temporaryFolder.newFile("temp.json")
    }

    def "Test Blank File"() {
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
        thrown(IllegalArgumentException)
    }

    def "test valid file"() {
        setup:
        file.withPrintWriter { it.write(valid) }

        mockStatic(FrameworkUtil.class)
        Bundle mockBundle = Mock(Bundle)
        when(FrameworkUtil.getBundle(ValidationParser.class)).thenReturn(mockBundle)

        BundleContext mockBundleContext = Mock(BundleContext)
        mockBundle.getBundleContext() >> mockBundleContext

        when:
        validationParser.install(file)

        then:
        attributeRegistry.getAttributeDescriptor("cool-attribute").isPresent()
        attributeRegistry.getAttributeDescriptor("geospatial-goodness").isPresent()

        def validators = attributeValidatorRegistry.getValidators("cool-attribute")
        validators.size() == 2

        1 * mockBundleContext.registerService(MetacardType.class, _ as MetacardType, {
            it.get("name") == "my-metacard-type"
        })
        1 * mockBundleContext.registerService(MetacardType.class, _ as MetacardType, {
            it.get("name") == "another-useful-type"
        })
        1 * mockBundleContext.registerService(MetacardValidator.class, _ as MetacardValidator, null)
    }

    def "test default values"() {
        setup:
        file.withPrintWriter { it.write(defaultValues) }
        mockStatic(FrameworkUtil.class)
        when(FrameworkUtil.getBundle(ValidationParser.class)).thenReturn(Mock(Bundle))

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
        attributeRegistry.getAttributeDescriptor("cool-attribute").isPresent()
        attributeValidatorRegistry.getValidators("cool-attribute").size() == 0
    }

    String valid = '''
{
    "metacardTypes": [
        {
            "type": "my-metacard-type",
            "attributes": {
                "cool-attribute": {
                    "required": true
                },
                "geospatial-goodness": {
                    "required": false
                }
            }
        },
        {
            "type": "another-useful-type",
            "attributes": {
                "cool-attribute": {
                    "required": false
                },
                "useful-attribute": {
                    "required": false
                }
            }
        }
    ],
    "attributeTypes": {
        "cool-attribute": {
            "type": "STRING_TYPE",
            "stored": true,
            "indexed": true,
            "tokenized": false,
            "multivalued": false
        },
        "geospatial-goodness": {
            "type": "XML_TYPE",
            "stored": true,
            "indexed": true,
            "tokenized": false,
            "multivalued": true
        },
        "useful-attribute": {
            "type": "BOOLEAN_TYPE",
            "stored": true,
            "indexed": false,
            "tokenized": true,
            "multivalued": false
        }
    },
    "validators": {
        "cool-attribute": [
            {
                "validator": "size",
                "arguments": ["0", "128"]
            },
            {
                "validator": "pattern",
                "arguments": ["(hi)+\\d"]
            }
        ]
    }
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
                "arguments": ["(hi)+\\d"]
            }
        ]
    }
}'''

}
