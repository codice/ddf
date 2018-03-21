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
package org.codice.ddf.catalog.async.data

import ddf.catalog.resource.Resource
import org.codice.ddf.catalog.async.data.api.internal.InaccessibleResourceException
import org.codice.ddf.catalog.async.data.impl.LazyProcessResourceImpl
import spock.lang.Shared
import spock.lang.Specification

import java.util.function.Supplier

import static org.codice.ddf.catalog.async.data.impl.ProcessResourceImpl.*

class LazyProcessResourceImplSpec extends Specification {

    @Shared
    String RESOURCE_NAME = "resourceName"

    @Shared
    int RESOURCE_SIZE = 1

    @Shared
    InputStream RESOURCE_INPUTSTREAM = Mock(InputStream)

    @Shared
    String RESOURCE_MIMETYPE = "mimeType"

    @Shared
    String METACARD_ID = "metacardId"

    private Resource resource

    private Supplier<Resource> supplier

    def setup() {
        resource = Mock(Resource) {
            getSize() >> RESOURCE_SIZE
            getInputStream() >> RESOURCE_INPUTSTREAM
            getName() >> RESOURCE_NAME
            getMimeTypeValue() >> RESOURCE_MIMETYPE
        }

        supplier = Mock(Supplier) {
            get() >> resource
        }
    }

    def 'new resource IllegalArgument on bad inputs'() {
        when:
        new LazyProcessResourceImpl(metacardId, resourceSupplier)

        then:
        thrown(IllegalArgumentException)

        where:
        metacardId   | resourceSupplier
        null         | Mock(Supplier)
        ""           | Mock(Supplier)
        " "          | Mock(Supplier)
        METACARD_ID | null

    }

    def 'verify lazy loading not loaded for new resource'() {
        when:
        new LazyProcessResourceImpl(METACARD_ID, supplier)

        then:
        0 * supplier.get()
    }

    def 'verify a new resource is lazy loaded and values populated as expected.'() {
        given:
        Supplier<Resource> supplier = Mock(Supplier) {
            1 * get() >> resource
        }

        long expectedSize = 3914
        String uriString = "something:3839ab393df930303#frag"
        URI expectedUri = new URI(uriString)
        String expectedQualifier = "frag"

        def lazyProcessResource = new LazyProcessResourceImpl(METACARD_ID, supplier)
        lazyProcessResource.setSize(expectedSize)
        lazyProcessResource.setUri(expectedUri)

        expect:
        lazyProcessResource.size == expectedSize
        lazyProcessResource.uri == expectedUri
        lazyProcessResource.qualifier == expectedQualifier
        lazyProcessResource.name == RESOURCE_NAME
        lazyProcessResource.inputStream == RESOURCE_INPUTSTREAM
        lazyProcessResource.mimeType == RESOURCE_MIMETYPE
        !lazyProcessResource.modified
    }

    def 'verify getting resource loading field multiple times only attempts to load the resource once'() {
        given:
        Supplier<Resource> supplier = Mock(Supplier) {
            // this is what's being tested, that the supplier.get is only called once
            1 * get() >> resource
        }

        def lazyProcessResource = new LazyProcessResourceImpl(METACARD_ID, supplier)
        lazyProcessResource.getName()
        lazyProcessResource.getMimeType()
        lazyProcessResource.getInputStream()
    }

    // Size, uri, and qualifier do not load the resource because the metacard generally
    // carries these values. So they are set on the lazyProcessResource and not read
    // from the loaded resource.
    def 'verify a new resource is not lazy loaded when accessing size, uri, or qualifier'() {
        given:
        Supplier<Resource> supplier = Mock(Supplier) {
            0 * get() >> resource
        }

        long expectedSize = 3914
        String uriString = "something:3839ab393df930303#frag"
        URI expectedUri = new URI(uriString)
        String expectedQualifier = "frag"

        def lazyProcessResource = new LazyProcessResourceImpl(METACARD_ID, supplier)
        lazyProcessResource.setSize(expectedSize)
        lazyProcessResource.setUri(expectedUri)

        when:
        lazyProcessResource.getSize()
        lazyProcessResource.getUri()
        lazyProcessResource.getQualifier()
        lazyProcessResource.close()

        then:
        lazyProcessResource.size == expectedSize
        lazyProcessResource.uri == expectedUri
        lazyProcessResource.qualifier == expectedQualifier
        !lazyProcessResource.modified
    }

    def 'verify resource not loaded on close'() {
        given:
        def lazyProcessResource = new LazyProcessResourceImpl(METACARD_ID, supplier)

        when:
        lazyProcessResource.close()

        then:
        0 * supplier.get()
        0 * RESOURCE_INPUTSTREAM.close()

    }

    def 'verify resource is loaded on getInputStream'() {
        given:
        Supplier<Resource> supplier = Mock(Supplier) {
            // this is what's being tested, that the supplier.get is only called once
            1 * get() >> resource
        }

        def lazyProcessResource = new LazyProcessResourceImpl(METACARD_ID, supplier)
        lazyProcessResource.getInputStream()
    }

    def 'getInputStream resource failed to load throws InaccessibleResourceException'() {
        given:
        Supplier<Resource> supplier = Mock(Supplier) {
            get() >> null
        }

        def lazyProcessResource = new LazyProcessResourceImpl(METACARD_ID, supplier)

        when:
        lazyProcessResource.getInputStream()

        then:
        thrown InaccessibleResourceException
    }

    def 'verify resource is loaded on getMimeType'() {
        given:
        Supplier<Resource> supplier = Mock(Supplier) {
            // this is what's being tested, that the supplier.get is only called once
            1 * get() >> resource
        }

        def lazyProcessResource = new LazyProcessResourceImpl(METACARD_ID, supplier)
        lazyProcessResource.getMimeType()
    }

    def 'getMimeType resource failed to load throws InaccessibleResourceException'() {
        given:
        Supplier<Resource> supplier = Mock(Supplier) {
            get() >> null
        }

        def lazyProcessResource = new LazyProcessResourceImpl(METACARD_ID, supplier)

        when:
        lazyProcessResource.getMimeType()

        then:
        thrown InaccessibleResourceException
    }

    def 'verify resource is loaded on getName'() {
        given:
        Supplier<Resource> supplier = Mock(Supplier) {
            // this is what's being tested, that the supplier.get is only called once
            1 * get() >> resource
        }

        def lazyProcessResource = new LazyProcessResourceImpl(METACARD_ID, supplier)
        lazyProcessResource.getName()
    }

    def 'getName resource failed to load throws InaccessibleResourceException'() {
        given:
        Supplier<Resource> supplier = Mock(Supplier) {
            get() >> null
        }

        def lazyProcessResource = new LazyProcessResourceImpl(METACARD_ID, supplier)

        when:
        lazyProcessResource.getName()

        then:
        thrown InaccessibleResourceException
    }

    def 'default name used if not set by resource'() {
        given:
        Resource incompleteResource = Mock(Resource) {
            getSize() >> RESOURCE_SIZE
            getInputStream() >> RESOURCE_INPUTSTREAM
            getName() >> null
            getMimeTypeValue() >> null
        }

        Supplier<Resource> supplier = Mock(Supplier) {
            get() >> incompleteResource
        }

        def lazyProcessResource = new LazyProcessResourceImpl(METACARD_ID, supplier)

        when:
        lazyProcessResource.getName()

        then:
        lazyProcessResource.name == DEFAULT_NAME
    }

    def 'default mimetype used if not set by resource'() {
        given:
        Resource incompleteResource = Mock(Resource) {
            getSize() >> RESOURCE_SIZE
            getInputStream() >> RESOURCE_INPUTSTREAM
            getName() >> null
            getMimeTypeValue() >> null
        }

        Supplier<Resource> supplier = Mock(Supplier) {
            get() >> incompleteResource
        }

        def lazyProcessResource = new LazyProcessResourceImpl(METACARD_ID, supplier)

        when:
        lazyProcessResource.getMimeType()

        then:
        lazyProcessResource.mimeType == DEFAULT_MIME_TYPE
    }

    def 'verify new resource is not marked as modified'() {
        given:
        def lazyProcessResource = new LazyProcessResourceImpl(METACARD_ID, supplier)

        expect:
        !lazyProcessResource.modified
    }

    def 'verify marking as modified does that'() {
        given:
        def lazyProcessResource = new LazyProcessResourceImpl(METACARD_ID, supplier)
        !lazyProcessResource.modified

        when:
        lazyProcessResource.markAsModified()

        then:
        lazyProcessResource.modified
    }
}