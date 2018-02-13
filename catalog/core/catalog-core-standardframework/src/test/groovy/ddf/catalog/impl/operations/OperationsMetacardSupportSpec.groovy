/**
 * Copyright (c) Codice Foundation
 * <p/>
 * This is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or any later version.
 * <p/>
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details. A copy of the GNU Lesser General Public License
 * is distributed along with this program and can be found at
 * <http://www.gnu.org/licenses/lgpl.html>.
 */
package ddf.catalog.impl.operations

import ddf.catalog.content.data.ContentItem
import ddf.catalog.data.*
import ddf.catalog.impl.FrameworkProperties
import ddf.catalog.source.IngestException
import ddf.mime.MimeTypeMapper
import org.codice.ddf.catalog.transform.Transform
import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator
import spock.lang.Specification
import java.nio.file.Path

class OperationsMetacardSupportSpec extends Specification {
    private OperationsMetacardSupport opsMetacard
    private FrameworkProperties frameworkProperties
    private MimeTypeMapper mimeTypeMapper
    private DefaultAttributeValueRegistry defaultAttributeValueRegistry
    private Metacard generatedMetacard
    private UuidGenerator uuidGenerator
    private Transform transform

    def setup() {
        System.setProperty("bad.files", "")
        System.setProperty("bad.file.extensions", "bad,worse")
        System.setProperty("bad.mime.types", "badmime,worsemime")
        System.setProperty("ignore.files", "")

        mimeTypeMapper = Mock(MimeTypeMapper)

        uuidGenerator = Mock(UuidGenerator)
        defaultAttributeValueRegistry = Mock(DefaultAttributeValueRegistry)

        generatedMetacard = Mock(Metacard)
        generatedMetacard.getId() >> { 'genmeta_id' }

        transform.transform(_, _, _, _, _) >> { [generatedMetacard] }

        frameworkProperties = new FrameworkProperties()
        frameworkProperties.with {
            mimeTypeMapper = this.mimeTypeMapper
            defaultAttributeValueRegistry = this.defaultAttributeValueRegistry
            transform = this.transfor
        }

        opsMetacard = new OperationsMetacardSupport(frameworkProperties)
    }

    def 'test apply injectors to metacard'() {
        setup:
        def metacard = Mock(Metacard)
        def injectors = [Mock(AttributeInjector), Mock(AttributeInjector), Mock(AttributeInjector)]

        when:
        opsMetacard.applyInjectors(metacard, injectors)

        then:
        injectors.each {
            // The response generator here is necessary to force the same object to be returned
            // and reused for each iteration through the loop
            1 * it.injectAttributes(metacard) >> metacard
        }
    }

    def 'test generation of metacard and content items empty input'() {
        setup:
        def metacardMap = [:]
        def contentItems = []
        def contentPaths = [:]

        when:
        opsMetacard.generateMetacardAndContentItems([], metacardMap, contentItems, contentPaths, [:])

        then:
        metacardMap.isEmpty()
        contentItems.isEmpty()
        contentPaths.isEmpty()
    }

    def 'test generation of metacard and content null input stream'() {
        setup:
        def metacardMap = [:]
        def contentItems = []
        def contentPaths = [:]
        def item = Mock(ContentItem)
        item.getInputStream() >> { null }
        def inputs = [item]

        when:
        opsMetacard.generateMetacardAndContentItems(inputs, metacardMap, contentItems, contentPaths, [:])

        then:
        thrown(IngestException)
    }

    def 'test generation of metacard and content broken input stream'() {
        setup:
        def metacardMap = [:]
        def contentItems = []
        def contentPaths = [:]
        def item = Mock(ContentItem)
        item.getInputStream() >> { throw new IOException() }
        def inputs = [item]

        when:
        opsMetacard.generateMetacardAndContentItems(inputs, metacardMap, contentItems, contentPaths, [:])

        then:
        thrown(IngestException)
    }

    def 'test generation of metacard and content unsupported mime type'() {
        setup:
        def metacardMap = [:]
        def contentItems = []
        def contentPaths = [:]
        def item = Mock(ContentItem)
        item.getFilename() >> 'joe.gobbledygook'
        item.getInputStream() >> { new ByteArrayInputStream('hello'.bytes) }
        item.getId() >> 'item.id'
        item.getMimeTypeRawData() >> 'badmime'
        def inputs = [item]

        when:
        opsMetacard.generateMetacardAndContentItems(inputs, metacardMap, contentItems, contentPaths, [:])

        then:
        thrown(IngestException)
    }

    def 'test generation of metacard and content supported mime type'() {
        setup:
        def metacardMap = [:]
        List<ContentItem> contentItems = []
        Map<String, Map<String, Path>> contentPaths = [:]
        frameworkProperties.mimeTypeMapper.guessMimeType(_, _) >> { 'text/plain' }
        def item = Mock(ContentItem)
        item.getFilename() >> 'joe.txt'
        item.getInputStream() >> { new ByteArrayInputStream('hello'.bytes) }
        item.getId() >> 'item.id'
        item.getMimeTypeRawData() >> 'application/octet-stream'
        def inputs = [item]

        when:
        opsMetacard.generateMetacardAndContentItems(inputs, metacardMap, contentItems, contentPaths, [:])

        then:
        metacardMap.size() == 1
        contentItems.size() == 1
        contentPaths.size() == 1
        metacardMap.get('genmeta_id') == generatedMetacard
        contentItems.first().metacard == generatedMetacard
        contentItems.first().filename == item.getFilename()
        contentItems.first().size == 'hello'.size()
        contentPaths.keySet().first() == item.getId()
    }

    def 'test generation of metacard and content supported ioexception from xformer'() {
        setup:
        def metacardMap = [:]
        List<ContentItem> contentItems = []
        Map<String, Map<String, Path>> contentPaths = [:]
        frameworkProperties.mimeTypeMapper.guessMimeType(_, _) >> { 'text/plain' }
        def item = Mock(ContentItem)
        item.getFilename() >> 'joe.txt'
        item.getInputStream() >> { new ByteArrayInputStream('hello'.bytes) }
        item.getId() >> 'item.id'
        item.getMimeTypeRawData() >> 'application/octet-stream'
        def inputs = [item]

        when:
        opsMetacard.generateMetacardAndContentItems(inputs, metacardMap, contentItems, contentPaths, [:])

        then:
        1 * transformer.transform(_) >> { throw new IOException() }
        thrown(IngestException)
    }

    def 'test set default values'() {
        setup:
        def attDescs = (1..4).collect { num ->
            def mock = Mock(AttributeDescriptor)
            mock.getName() >> { "att${num}" }
            return mock
        }
        def metaType = Mock(MetacardType)
        metaType.getName() >> { 'testtype' }
        metaType.getAttributeDescriptors() >> { attDescs }

        def registry = frameworkProperties.defaultAttributeValueRegistry

        def metacard = Mock(Metacard)
        metacard.getMetacardType() >> { metaType }
        metacard.getAttribute('att1') >> { null }
        metacard.getAttribute('att2') >> {
            def att = Mock(Attribute)
            att.getName() >> 'att2'
            att.getValue() >> 'val2'
            return att
        }
        metacard.getAttribute('att3') >> {
            def att = Mock(Attribute)
            att.getName() >> 'att3'
            att.getValue() >> 'val3'
            return att
        }
        metacard.getAttribute('att4') >> { null }

        when:
        opsMetacard.setDefaultValues(metacard)

        then:
        1 * registry.getDefaultValue('testtype', 'att1') >> { Optional.ofNullable(null) }
        0 * metacard.setAttribute(_)
        0 * registry.getDefaultValue('testtype', 'att2')
        0 * registry.getDefaultValue('testtype', 'att3')

        then:
        1 * registry.getDefaultValue('testtype', 'att4') >> { Optional.ofNullable('default4') }
        1 * metacard.setAttribute(_)
    }

    def 'test multiple detector fall through'() {
        mimeTypeMapper.guessMimeType(_, _) >> null
        def tempFile = Files.createTempFile("test", "bin")
        Files.write(tempFile.toAbsolutePath(), "test file content".getBytes())
        when:
        def mimeType = opsMetacard.guessMimeType(ContentItem.DEFAULT_MIME_TYPE, tempFile.getFileName().toString(), tempFile.toAbsolutePath())
        then:
        !ContentItem.DEFAULT_MIME_TYPE.equals(mimeType)
        "text/plain".equals(mimeType)
    }
}
