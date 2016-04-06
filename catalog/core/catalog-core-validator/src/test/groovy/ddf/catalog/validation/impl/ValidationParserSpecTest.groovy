package ddf.catalog.validation.impl

import ddf.catalog.data.AttributeRegistry
import ddf.catalog.data.impl.AttributeRegistryImpl
import ddf.catalog.validation.AttributeValidatorRegistry
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class ValidationParserSpecTest extends Specification {
    @Rule
    TemporaryFolder temporaryFolder = new TemporaryFolder()

    ValidationParser validationParser = new ValidationParser();

    AttributeRegistry attributeRegistry

    AttributeValidatorRegistry attributeValidatorRegistry

    File file

    void setup() {
        attributeRegistry = new AttributeRegistryImpl()
        validationParser.attributeRegistry = attributeRegistry

        attributeValidatorRegistry = new AttributeValidatorRegistryImpl()
        validationParser.attributeValidatorRegistry = attributeValidatorRegistry
        file = temporaryFolder.newFile("temp.json")
    }

    void cleanup() {

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

        when:
        validationParser.install(file)

        then:
        attributeRegistry.getAttributeDescriptor("cool-attribute").isPresent()
        attributeRegistry.getAttributeDescriptor("geospatial-goodness").isPresent()

        def validators = attributeValidatorRegistry.getValidators("cool-attribute")
        validators.size() == 2
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
    def invalidValidator = '''
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
