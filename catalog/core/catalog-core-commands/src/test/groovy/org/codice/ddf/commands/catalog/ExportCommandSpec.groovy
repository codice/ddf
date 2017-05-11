package org.codice.ddf.commands.catalog

import ddf.catalog.CatalogFramework
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
import ddf.catalog.transform.MetacardTransformer
import groovy.xml.MarkupBuilder
import org.apache.karaf.shell.api.console.Session
import org.osgi.framework.BundleContext
import org.osgi.framework.ServiceReference

import javax.activation.MimeType
import java.nio.file.Paths
import java.util.zip.ZipFile

class ExportCommandSpec extends spock.lang.Specification {

    ExportCommand exportCommand

    CatalogFramework catalogFramework

    MetacardTransformer xmlTransformer

    File tmpHomeDir

    void setup() {
        tmpHomeDir = File.createTempDir()
        System.setProperty("ddf.home", tmpHomeDir.canonicalPath)

        ServiceReference xmlTransformerReference = Mock(ServiceReference)

        xmlTransformer = Mock(MetacardTransformer)

        xmlTransformer.transform(_ as Metacard, _ as Map) >> { metacard, map ->
            metacardToXmlTransformer(metacard, map)
        }

        BundleContext bundleContext = Mock(BundleContext) {
            getServiceReferences(MetacardTransformer, '(id=xml)') >> [xmlTransformerReference]
            getService(xmlTransformerReference) >> xmlTransformer
        }

        catalogFramework = Mock(CatalogFramework)
        catalogFramework.query(_ as QueryRequest) >> { QueryRequest req ->
            new QueryResponseImpl(req, [], 0)
        }
        catalogFramework.getLocalResource(_ as ResourceRequest) >> {
            throw new ResourceNotFoundException('Could not find exception')
        }

        exportCommand = new ExportCommand(filterBuilder: new GeotoolsFilterBuilder(),
                bundleContext: bundleContext, catalogFramework: catalogFramework)

    }

    void cleanup() {
        assert tmpHomeDir?.deleteDir()
    }

    def "Test export no items"() {
        setup:
        exportCommand.with {
            delete = false
        }

        when:
        exportCommand.executeWithSubject()

        then:
        notThrown(Exception)
    }

    def "Test filename that already exists"() {
        setup:
        def file = Paths.get(System.getProperty('ddf.home'), 'filealreadyexists').toFile()
        file.createNewFile()
        exportCommand.with {
            delete = false
            output = file.canonicalPath
        }

        when:
        exportCommand.executeWithSubject()

        then:
        thrown(IllegalStateException)
    }

    def "Test bad parent file"() {
        setup:
        def file = Mock(File) { // This is really dirty and regardless that it works its bad
            getParentFile() >> null // and I should feel bad and delete this.
        }
        exportCommand.with {
            delete = false
            output = file // <- setting a String field to a File Mock object that just happens to
        }                   // get passed to the new File() constructor and so actually passes mock
        // object on to the actual class to be used.
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

    def "Test abort command"() {
        setup:
        InputStream keyboardInput = new ByteArrayInputStream("n\r".getBytes('utf-8'))
        Session session = Mock(Session) {
            getKeyboard() >> keyboardInput
        }
        exportCommand.with {
            it.delete = true
            it.session = session
        }

        when:
        exportCommand.executeWithSubject()

        then:
        notThrown(Exception)
        tmpHomeDir.list() == [] // dir is empty
    }

    def "Test single metacard no content export"() {
        setup:
        exportCommand.with {
            it.delete = false
        }

        def attributes = simpleAttributes()
        attributes.remove(Metacard.RESOURCE_URI) // removed Resource URI simulates no content
        def result = new ResultImpl(simpleMetacard(attributes))
        exportCommand.catalogFramework = Mock(CatalogFramework) {
            query(_ as QueryRequest) >> { QueryRequest req ->
                new QueryResponseImpl(req, [result], 1)
            }
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
        exportCommand.catalogFramework = Mock(CatalogFramework) {
            query(_ as QueryRequest) >> { QueryRequest req ->
                new QueryResponseImpl(req, [result], 1)
            }
            getLocalResource(_ as ResourceRequest) >> { ResourceRequest req ->
                BinaryContent xmlContent = getMetacardToXmlTransformer()(result.metacard, [:])

                return new ResourceResponseImpl(req, [:], new ResourceImpl(xmlContent.inputStream,
                        new MimeType('text/xml'),
                        resourceName))
            }
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
        println(files)
        assert [result.metacard.id].every { id ->
            files.any { it.contains(id) }
        }
        assert [resourceName].every { name ->
            files.any {it.contains(name)}
        }

    }

/**************************************************************************
 *
 * Utility Methods
 *
 *************************************************************************/

    Metacard simpleMetacard(Map kwargs) {
        Metacard metacard = new MetacardImpl()
        kwargs.forEach({ key, val ->
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

    Map namespaces = ['xmlns'         : 'urn:catalog:metacard',
                      'xmlns:gml'     : 'http://www.opengis.net/gml',
                      'xmlns:xlink'   : 'http://www.w3.org/1999/xlink',
                      'xmlns:smil'    : 'http://www.w3.org/2001/SMIL20/',
                      'xmlns:smillang': 'http://www.w3.org/2001/SMIL20/Language']

    Closure metacardToXmlTransformer = {
            //    BiFunction<Metacard, Map, BinaryContent> metacardToXmlTransformer = {
        Metacard metacard, Map properties ->
            def sw = new StringWriter()
            def xml = new MarkupBuilder(sw)
            xml.metacard(namespaces) {
                type("${metacard.metacardType.name}")
                if (metacard.sourceId) {
                    source("${metacard.sourceId}")
                }
                metacard.metacardType.attributeDescriptors.each { ad ->
                    if (metacard.getAttribute(ad.name) != null) {
                        "${ad.type.attributeFormat.toString().toLowerCase()}"('name': "${ad.name}") {
                            metacard.getAttribute(ad.name).values.each {
                                'value'(it)
                            }
                        }
                    }
                }
            }

            final String data = sw.toString()

            return [
                    getInputStream  : { -> new ByteArrayInputStream(data.bytes) },
                    getMimeType     : { -> new MimeType('text/xml') },
                    getMimeTypeValue: { -> 'text/xml' },
                    getSize         : data.&length,
                    getByteArray    : data.&getBytes
            ] as BinaryContent
    }

}







