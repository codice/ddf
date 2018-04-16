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

import ddf.catalog.data.Metacard
import ddf.catalog.data.MetacardCreationException

import org.codice.ddf.platform.util.uuidgenerator.UuidGenerator
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification

import javax.activation.MimeType
import java.nio.file.Path
import java.nio.file.Paths

class MetacardFactorySpec extends Specification {
    @Rule
    TemporaryFolder tempFolder
    @Shared
    File file

    private Metacard metacardPlain
    private Metacard metacardXml
    private Metacard metacardXml2
    private UuidGenerator uuidGenerator
    private Path path

    def setup() {
        file = tempFolder.newFile('testinput.txt')
        path = Paths.get(file.toURI())

        metacardPlain = Mock(Metacard)
        metacardXml = Mock(Metacard)
        metacardXml2 = Mock(Metacard)
        uuidGenerator = Mock(UuidGenerator)

        itBad.transform(_ as InputStream) >> { throw new IOException() }

        transform.transform(_ as MimeType, _ as String, _ as File, _ as String, _ as Map) >> {
            MimeType m ->
                if (m.baseType == 'application/xml') {
                    throw new MetacardCreationException()
                } else if (m.baseType == 'application/xml2') {
                    throw new MetacardCreationException()
                } else if (m.baseType == 'application/xml3') {
                    throw new MetacardCreationException()
                } else if (m.baseType == 'application/xml-bad') {
                    throw new MetacardCreationException()
                } else if (m.baseType == 'text/plain') {
                    [metacardPlain]
                }
        }

    }

    def 'test metacard generation with bad xformer'() {
        when:
        metacardFactory.generateMetacard('application/xml-bad', 'idbad', 'filename', path)

        then:
        thrown(MetacardCreationException)
    }

    def 'test metacard generation with xformer failure'() {
        when:
        def metacard = metacardFactory.generateMetacard('application/xml3', 'test-id', 'filename', path)

        then:
        0 * uuidGenerator.generateUuid()
        1 * metacardXml.setAttribute({ it.name == Metacard.ID && it.values.first() == 'test-id' })
        1 * metacardXml.getTitle() >> { 'this is a title' }
        0 * metacardXml.setAttribute({ it.name == Metacard.TITLE })

        metacard == metacardXml
    }

    def 'test plain text metacard with no id or title'() {
        setup:
        1 * uuidGenerator.generateUuid() >> UUID.randomUUID().toString()

        when:
        def metacard = metacardFactory.generateMetacard('text/plain', null, 'filename', path)

        then:
        1 * metacardPlain.setAttribute({ it.name == Metacard.ID && it.values.first() != null })
        1 * metacardPlain.getTitle() >> { null }
        1 * metacardPlain.setAttribute({
            it.name == Metacard.TITLE && it.values.first() == 'filename'
        })

        metacard == metacardPlain
    }

    def 'test plain text metacard with provided id and xform-generated title'() {
        when:
        def metacard = metacardFactory.generateMetacard('text/plain', 'test-id', 'filename', path)

        then:
        0 * uuidGenerator.generateUuid()
        1 * metacardPlain.setAttribute({ it.name == Metacard.ID && it.values.first() == 'test-id' })
        1 * metacardPlain.getTitle() >> { 'this is a title' }
        0 * metacardPlain.setAttribute({ it.name == Metacard.TITLE })

        metacard == metacardPlain
    }

    def 'test xml metacard with provided id and xform-generated title'() {
        when:
        def metacard = metacardFactory.generateMetacard('application/xml', 'test-id', 'filename', path)

        then:
        0 * uuidGenerator.generateUuid()
        1 * metacardXml.setAttribute({ it.name == Metacard.ID && it.values.first() == 'test-id' })
        1 * metacardXml.getTitle() >> { 'this is a title' }
        0 * metacardXml.setAttribute({ it.name == Metacard.TITLE })

        metacard == metacardXml
    }

    def 'ensure that order of operations matters for transformers'() {
        when:
        def metacard = metacardFactory.generateMetacard('application/xml2', 'test-id', 'filename', path)

        then:
        0 * uuidGenerator.generateUuid()
        1 * metacardXml2.setAttribute({ it.name == Metacard.ID && it.values.first() == 'test-id' })
        1 * metacardXml2.getTitle() >> { 'this is a title' }
        0 * metacardXml2.setAttribute({ it.name == Metacard.TITLE })

        metacard == metacardXml2
    }
}
