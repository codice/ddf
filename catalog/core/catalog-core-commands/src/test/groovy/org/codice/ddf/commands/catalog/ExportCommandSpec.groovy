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
package org.codice.ddf.commands.catalog

import ddf.catalog.CatalogFramework
import ddf.catalog.content.StorageProvider
import ddf.catalog.data.BinaryContent
import ddf.catalog.data.Metacard
import ddf.catalog.data.impl.AttributeImpl
import ddf.catalog.data.impl.MetacardImpl
import ddf.catalog.data.impl.ResultImpl
import ddf.catalog.filter.proxy.builder.GeotoolsFilterBuilder
import ddf.catalog.operation.QueryRequest
import ddf.catalog.operation.ResourceRequest
import ddf.catalog.operation.impl.QueryResponseImpl
import ddf.catalog.operation.impl.ResourceResponseImpl
import ddf.catalog.resource.ResourceNotFoundException
import ddf.catalog.resource.impl.ResourceImpl
import ddf.catalog.source.CatalogProvider
import org.apache.karaf.shell.api.console.Session
import org.osgi.framework.BundleContext
import org.codice.ddf.catalog.transform.Transform;
import spock.lang.Ignore
import spock.lang.Specification
import spock.lang.Unroll

import javax.activation.MimeType
import java.nio.file.Paths
import java.util.zip.ZipFile
import java.util.List

class ExportCommandSpec extends Specification {

    File tmpHomeDir

    BundleContext bundleContext

    CatalogFramework catalogFramework

    ExportCommand exportCommand

    Transform transform

    void setup() {
        tmpHomeDir = File.createTempDir()
        System.setProperty("ddf.home", tmpHomeDir.canonicalPath)

        catalogFramework = Mock(CatalogFramework)

        transform = Mock(Transform) {
            transform(_ as List, _ as String, _ as Map) >> [getMockContent()]
            isMetacardTransformerIdValid(_ as String) >> true
        }

        exportCommand = new ExportCommand(filterBuilder: new GeotoolsFilterBuilder(),
                bundleContext: bundleContext, catalogFramework: catalogFramework,
                transform: transform)
    }

    void cleanup() {
        tmpHomeDir?.deleteOnExit()
    }

    def "Test export no items"() {
        setup:
        catalogFramework.query(_ as QueryRequest) >> { QueryRequest req ->
            new QueryResponseImpl(req, [], 0)
        }
        catalogFramework.getLocalResource(_ as ResourceRequest) >> {
            throw new ResourceNotFoundException('Could not find exception')
        }

        exportCommand.with {
            delete = false
        }

        when:
        exportCommand.executeWithSubject()

        then:
        notThrown(Exception)
        tmpHomeDir.list().size() == 0
    }

    def "Test export no transformer"() {
        setup:
        def transform = Mock(Transform) {
          isMetacardTransformerIdValid(_) >> false
        }
        def exportCommand = new ExportCommand(filterBuilder: new GeotoolsFilterBuilder(),
                bundleContext: bundleContext, catalogFramework: catalogFramework,
                transform: transform)

        when:
        exportCommand.executeWithSubject()

        then:
        thrown(IllegalArgumentException)
        tmpHomeDir.list().size() == 0
    }

    def "Test filename that already exists"() {
        setup:
        def fileData = "This is the file data. There are many files like it, but this one is mine."
        def file = Paths.get(System.getProperty('ddf.home'), 'filealreadyexists').toFile()
        file.createNewFile()
        file.withWriter { it.write(fileData) }

        exportCommand.with {
            delete = false
            output = file.canonicalPath
        }

        when:
        exportCommand.executeWithSubject()

        then:
        thrown(IllegalStateException)
        tmpHomeDir.list().size() == 1
        file.text == fileData
    }

    def "Test blank filename"() {
        setup:
        exportCommand.with {
            delete = false
            output = ""
        }

        when:
        exportCommand.executeWithSubject()

        then:
        thrown(IllegalStateException)
    }

    def "Test bad file name"() {
        setup:
        def file = Paths.get(System.getProperty('ddf.home'), 'badFilename.notazip')

        exportCommand.with {
            delete = false
            output = file
        }

        when:
        exportCommand.executeWithSubject()

        then:
        thrown(IllegalStateException)
    }

    @Unroll
    def "Test abort command with \"#userInputString\" response to warning prompt"(final String userInputString) {
        setup:
        def session = Mock(Session) {
            readLine(_ as String, null) >> userInputString
        }
        exportCommand.with {
            it.delete = true
            it.session = session
        }

        when:
        exportCommand.executeWithSubject()

        then:
        tmpHomeDir.list() == [] // dir is empty
        
        where:
        userInputString << ["n", "N", "no", "NO", "something that isn't no", "n\r"]
    }

    def "Test single metacard no content export"() {
        setup:
        exportCommand.with {
            it.delete = false
        }

        def attributes = simpleAttributes()
        attributes.remove(Metacard.RESOURCE_URI) // removed Resource URI simulates no content

        def result = new ResultImpl(simpleMetacard(attributes))

        catalogFramework.query(_ as QueryRequest) >> { QueryRequest req ->
            new QueryResponseImpl(req, [result], 1)
        } >> { QueryRequest req ->
            new QueryResponseImpl(req, [], 0)
        }

        catalogFramework.getLocalResource(_ as ResourceRequest) >> {
            throw new ResourceNotFoundException('Could not find exception')
        }

        when:
        exportCommand.executeWithSubject()

        then:
        notThrown(Exception)
        tmpHomeDir.list().size() == 1

        String zip = tmpHomeDir.listFiles().find()?.canonicalPath
        assert zip?.trim() as boolean // null or empty
        assert zip.endsWith('.zip')

        def files = new ZipFile(zip).entries()
                .collect { it.isDirectory() ? null : it.name }
                .findAll { it != null }

        assert [result.metacard.id].every { id ->
            files.any { it.contains(id) }
        }
    }

    def "Test single metacard with content export"() {
        setup:
        exportCommand.with {
            it.delete = false
        }

        def result = new ResultImpl(simpleMetacard(simpleAttributes() + [(Metacard.TAGS): [Metacard.DEFAULT_TAG]]))
        def resourceName = "contentfor-${result.metacard.id}.xml" as String

        catalogFramework.query(_ as QueryRequest) >> { QueryRequest req ->
            new QueryResponseImpl(req, [result], 1)
        } >> { QueryRequest req ->
            new QueryResponseImpl(req, [], 0)
        }

        catalogFramework.getLocalResource(_ as ResourceRequest) >> { ResourceRequest req ->
            BinaryContent xmlContent = getMockContent()

            return new ResourceResponseImpl(req, [:], new ResourceImpl(xmlContent.inputStream,
                    new MimeType('text/xml'),
                    resourceName))
        }

        when:
        exportCommand.executeWithSubject()

        then:
        notThrown(Exception)
        tmpHomeDir.list().size() == 1
        tmpHomeDir.list().find()?.endsWith('.zip')

        String zip = tmpHomeDir.listFiles().find()?.canonicalPath
        assert zip?.trim() as boolean // null or empty
        assert zip.endsWith('.zip')

        def files = new ZipFile(zip).entries()
                .collect { it.isDirectory() ? null : it.name }
                .findAll { it != null }
        assert [result.metacard.id].every { id ->
            files.any { it.contains(id) }
        }
        assert [resourceName].every { name ->
            files.any { it.contains(name) }
        }

    }

    def "Test single metacard with content export and delete"() {
        setup:
        def storageProvider = Mock(StorageProvider)
        def catalogProvider = Mock(CatalogProvider)
        exportCommand.with {
            it.delete = true
            it.force = true
            it.storageProvider = storageProvider
            it.catalogProvider = catalogProvider
        }

        def result = new ResultImpl(simpleMetacard(simpleAttributes() + [(Metacard.TAGS): [Metacard.DEFAULT_TAG]]))
        def resourceName = "contentfor-${result.metacard.id}.xml" as String

        catalogFramework.query(_ as QueryRequest) >> { QueryRequest req ->
            new QueryResponseImpl(req, [result], 1)
        } >> { QueryRequest req ->
            new QueryResponseImpl(req, [], 0)
        }

        catalogFramework.getLocalResource(_ as ResourceRequest) >> { ResourceRequest req ->
            BinaryContent xmlContent = getMockContent()

            return new ResourceResponseImpl(req, [:], new ResourceImpl(xmlContent.inputStream,
                    new MimeType('text/xml'),
                    resourceName))
        }

        when:
        exportCommand.executeWithSubject()

        then:
        notThrown(Exception)

        1 * storageProvider.delete(*_)
        1 * storageProvider.commit(*_)
        1 * catalogProvider.delete(*_)

        tmpHomeDir.list().size() == 1
        tmpHomeDir.list().find()?.endsWith('.zip')

        String zip = tmpHomeDir.listFiles().find()?.canonicalPath
        assert zip?.trim() as boolean // null or empty
        assert zip.endsWith('.zip')

        def files = new ZipFile(zip).entries()
                .collect { it.isDirectory() ? null : it.name }
                .findAll { it != null }
        assert [result.metacard.id].every { id ->
            files.any { it.contains(id) }
        }
        assert [resourceName].every { name ->
            files.any { it.contains(name) }
        }
    }

/**************************************************************************
 *
 * Utility Methods
 *
 *************************************************************************/

    Metacard simpleMetacard(Map attributes) {
        Metacard metacard = new MetacardImpl()
        attributes.forEach({ key, val ->
            if (key != null && val != null) {
                metacard.setAttribute(new AttributeImpl(key, val))
            }
        })
        return metacard
    }

    Map simpleAttributes() {
        String id = randomUUID()
        [
                (Metacard.ID)          : id,
                (Metacard.TITLE)       : 'Metacard Title',
                (Metacard.RESOURCE_URI): "content:$id".toString(),
        ]
    }

    String randomUUID() {
        UUID.randomUUID().toString().replaceAll('-', '')
    }

    BinaryContent getMockContent() {
        def data = "<? xml ?><body><data</body>"
        return [
                getInputStream  : { -> new ByteArrayInputStream(data.bytes) },
                getMimeType     : { -> new MimeType('text/xml') },
                getMimeTypeValue: { -> 'text/xml' },
                getSize         : data.&size,
                getByteArray    : data.&getBytes
        ] as BinaryContent
    }
}
