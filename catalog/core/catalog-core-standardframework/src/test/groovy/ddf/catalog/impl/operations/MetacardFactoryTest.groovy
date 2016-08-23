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
import ddf.catalog.transform.InputTransformer
import ddf.mime.MimeTypeToTransformerMapper
import ddf.security.Subject
import org.apache.shiro.subject.PrincipalCollection
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Shared
import spock.lang.Specification

import javax.activation.MimeType
import java.nio.file.Path
import java.nio.file.Paths

class MetacardFactoryTest extends Specification {
    @Rule
    TemporaryFolder tempFolder
    @Shared
    File file

    private Metacard metacardPlain
    private Metacard metacardXml
    private Metacard metacardXml2
    private InputTransformer itPlain
    private InputTransformer itXml
    private InputTransformer itXml2
    private InputTransformer itBad
    private MetacardFactory metacardFactory
    private Path path

    def setup() {
        file = tempFolder.newFile('testinput.txt')
        path = Paths.get(file.toURI())

        metacardPlain = Mock(Metacard)
        metacardXml = Mock(Metacard)
        metacardXml2 = Mock(Metacard)

        itPlain = Mock(InputTransformer)
        itXml = Mock(InputTransformer)
        itXml2 = Mock(InputTransformer)
        itBad = Mock(InputTransformer)

        itBad.transform(_ as InputStream) >> { throw new IOException() }
        itPlain.transform(_ as InputStream) >> { metacardPlain }
        itXml.transform(_ as InputStream) >> { metacardXml }
        itXml2.transform(_ as InputStream) >> { metacardXml2 }

        def mimeTypeToTransformerMapper = Mock(MimeTypeToTransformerMapper)
        mimeTypeToTransformerMapper.findMatches(_ as Class<InputTransformer>, _ as MimeType) >> { x, MimeType m ->
            if (m.baseType == 'application/xml') {
                [itXml, itXml2, itBad]
            } else if (m.baseType == 'application/xml2') {
                [itXml2, itXml, itBad]
            } else if (m.baseType == 'application/xml-bad') {
                [itBad, itXml, itXml2]
            } else if (m.baseType == 'text/plain') {
                [itPlain]
            }
        }

        metacardFactory = new MetacardFactory(mimeTypeToTransformerMapper)
    }

    def 'test metacard generation with bad xformer'() {
        when:
        metacardFactory.generateMetacard('application/xml-bad', 'idbad', 'filename',
                null, path)

        then:
        thrown(MetacardCreationException)
    }

    def 'test plain text metacard with no id or title'() {
        when:
        def metacard = metacardFactory.generateMetacard('text/plain', null, 'filename',
                null, path)

        then:
        1 * metacardPlain.setAttribute({ it.name == Metacard.ID && isUUID(it.values.first()) })
        1 * metacardPlain.getTitle() >> { null }
        1 * metacardPlain.setAttribute({
            it.name == Metacard.TITLE && it.values.first() == 'filename'
        })
        1 * metacardPlain.setAttribute({
            it.name == Metacard.POINT_OF_CONTACT && it.values.first() == ''
        })

        metacard == metacardPlain
    }

    def 'test plain text metacard with provided id and xform-generated title'() {
        when:
        def metacard = metacardFactory.generateMetacard('text/plain', 'test-id', 'filename',
                null, path)

        then:
        1 * metacardPlain.setAttribute({ it.name == Metacard.ID && it.values.first() == 'test-id' })
        1 * metacardPlain.getTitle() >> { 'this is a title' }
        0 * metacardPlain.setAttribute({ it.name == Metacard.TITLE })
        1 * metacardPlain.setAttribute({
            it.name == Metacard.POINT_OF_CONTACT && it.values.first() == ''
        })

        metacard == metacardPlain
    }

    def 'test plain text metacard with provided id and xform-generated title and a subject'() {
        setup:
        def subject = Mock(Subject)
        def principals = Mock(PrincipalCollection)
        principals.oneByType(_) >> { null }
        principals.getPrimaryPrincipal() >> { 'test-subject' }
        subject.getPrincipals() >> { principals }

        when:
        def metacard = metacardFactory.generateMetacard('text/plain', 'test-id', 'filename',
                subject, path)

        then:
        1 * metacardPlain.setAttribute({ it.name == Metacard.ID && it.values.first() == 'test-id' })
        1 * metacardPlain.getTitle() >> { 'this is a title' }
        0 * metacardPlain.setAttribute({ it.name == Metacard.TITLE })
        1 * metacardPlain.setAttribute({
            it.name == Metacard.POINT_OF_CONTACT && it.values.first() == 'test-subject'
        })

        metacard == metacardPlain
    }

    def 'test xml metacard with provided id and xform-generated title'() {
        when:
        def metacard = metacardFactory.generateMetacard('application/xml', 'test-id', 'filename',
                null, path)

        then:
        then:
        1 * metacardXml.setAttribute({ it.name == Metacard.ID && it.values.first() == 'test-id' })
        1 * metacardXml.getTitle() >> { 'this is a title' }
        0 * metacardXml.setAttribute({ it.name == Metacard.TITLE })
        1 * metacardXml.setAttribute({
            it.name == Metacard.POINT_OF_CONTACT && it.values.first() == ''
        })

        metacard == metacardXml
    }

    def 'ensure that order of operations matters for transformers'() {
        when:
        def metacard = metacardFactory.generateMetacard('application/xml2', 'test-id', 'filename',
                null, path)

        then:
        then:
        1 * metacardXml2.setAttribute({ it.name == Metacard.ID && it.values.first() == 'test-id' })
        1 * metacardXml2.getTitle() >> { 'this is a title' }
        0 * metacardXml2.setAttribute({ it.name == Metacard.TITLE })
        1 * metacardXml2.setAttribute({
            it.name == Metacard.POINT_OF_CONTACT && it.values.first() == ''
        })

        metacard == metacardXml2
    }

    private static boolean isUUID(String input) {
        try {
            if (input == null || input.length() != 32) {
                return false
            }
            // Re-insert hyphens to return to 8-4-4-4-12 pattern
            String uuid = input.substring(0, 8) + '-' +
                    input.substring(8, 12) + '-' +
                    input.substring(12, 16) + '-' +
                    input.substring(16, 20) + '-' +
                    input.substring(20)
            UUID.fromString(uuid)
            return true
        } catch (IllegalArgumentException e) {
            return false
        }
    }
}
