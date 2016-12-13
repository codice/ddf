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

import org.codice.ddf.catalog.async.data.api.internal.ProcessResource
import spock.lang.Specification

class ProcessResourceImplTest extends Specification {

    static final ID = 'id'

    def inputStream = Mock(InputStream)

    static final MIME_TYPE_RAW_DATA = "mimeType"

    static final FILE_NAME = "test"

    static final SIZE = 1

    static final QUALIFIER = "thumbnail"

    static final RESOURCE_URI = ProcessResourceImpl.CONTENT_SCHEME + ":" + ID

    def 'process resource no qualifier'() {
        when:
        def processResource = new ProcessResourceImpl(ID, inputStream, MIME_TYPE_RAW_DATA, FILE_NAME, SIZE)

        then:
        processResource.getQualifier() == ''
        processResource.isModified()
        processResource.getUri() == RESOURCE_URI
    }

    def 'process resource unknown size'() {
        when:
        def processResource = new ProcessResourceImpl(ID, inputStream, MIME_TYPE_RAW_DATA, FILE_NAME)

        then:
        processResource.getSize() == -1
    }

    def 'process resource no qualifier and not modified'() {
        when:
        def processResource = new ProcessResourceImpl(ID, inputStream, MIME_TYPE_RAW_DATA, FILE_NAME, SIZE, false)

        then:
        processResource.getQualifier() == ''
        !processResource.isModified()
        processResource.getUri() == RESOURCE_URI
    }

    def 'test process resource qualified'() {
        when:
        def processResource = new ProcessResourceImpl(ID, inputStream, MIME_TYPE_RAW_DATA, FILE_NAME, SIZE, QUALIFIER)

        then:
        processResource.getQualifier() == QUALIFIER
        processResource.isModified()
        processResource.getUri() == RESOURCE_URI + "#" + QUALIFIER
    }

    def 'test process resource'() {
        when:
        def stream = Mock(InputStream)
        def processResource = new ProcessResourceImpl(ID, stream, mimeType, fileName, SIZE, qualifier, false)

        then:
        if (mimeType == null && fileName == null) {
            assert processResource.getFilename() == ProcessResource.DEFAULT_FILE_NAME
            assert processResource.getMimeType() == ProcessResource.DEFAULT_MIME_TYPE
        } else {
            assert processResource.getQualifier() == qualifier
            assert !processResource.isModified()
            assert processResource.getFilename() == fileName
            assert processResource.getMimeType() == mimeType
            assert processResource.getInputStream() == stream
            assert processResource.getSize() == SIZE
        }

        if (qualifier == null) {
            assert processResource.getUri() == RESOURCE_URI
        } else {
            assert processResource.getUri() == RESOURCE_URI + "#" + QUALIFIER
        }

        where:
        mimeType           | fileName  | qualifier
        MIME_TYPE_RAW_DATA | FILE_NAME | QUALIFIER
        MIME_TYPE_RAW_DATA | FILE_NAME | null
        null               | null      | QUALIFIER
    }

    def 'test process resource IllegalArgumentException'() {
        when:
        new ProcessResourceImpl(id, inputStream, MIME_TYPE_RAW_DATA, FILE_NAME, SIZE, QUALIFIER)

        then:
        thrown IllegalArgumentException

        where:
        id << ["", null]
    }
}