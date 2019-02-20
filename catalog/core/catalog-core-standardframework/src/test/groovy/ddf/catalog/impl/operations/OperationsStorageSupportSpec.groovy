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

import ddf.catalog.content.StorageException
import ddf.catalog.content.StorageProvider
import ddf.catalog.content.data.ContentItem
import ddf.catalog.content.operation.StorageRequest
import ddf.catalog.history.Historian
import ddf.catalog.source.IngestException
import ddf.catalog.source.SourceUnavailableException
import spock.lang.Specification

import java.nio.file.Path

class OperationsStorageSupportSpec extends Specification {
    private Historian historian
    private SourceOperations sourceOperations
    private QueryOperations queryOperations
    private OperationsStorageSupport opsStorage

    def setup() {
        historian = Mock(Historian)
        sourceOperations = Mock(SourceOperations)
        queryOperations = Mock(QueryOperations)
        opsStorage = new OperationsStorageSupport(sourceOperations, queryOperations)
        opsStorage.setHistorian(historian)
    }

    def 'test prepare storage with null request'() {
        when:
        opsStorage.prepareStorageRequest(null, null)

        then:
        thrown(IngestException)
    }

    def 'test prepare storage with no content'() {
        when:
        opsStorage.prepareStorageRequest(Mock(StorageRequest), { return null })

        then:
        thrown(IngestException)
    }

    def 'test prepare storage when local catalog not available'() {
        setup:
        def request = Mock(StorageRequest)
        request.hasProperties() >> { false }
        sourceOperations.isSourceAvailable(_) >> { false }

        when:
        opsStorage.prepareStorageRequest(request, { return [Mock(ContentItem)] })

        then:
        1 * queryOperations.setFlagsOnRequest(request)
        thrown(SourceUnavailableException)
    }

    def 'test prepare storage when local storage not available'() {
        setup:
        def request = Mock(StorageRequest)
        request.hasProperties() >> { false }
        sourceOperations.isSourceAvailable(_) >> { true }
        sourceOperations.getStorage() >> { null }

        when:
        opsStorage.prepareStorageRequest(request, { return [Mock(ContentItem)] })

        then:
        1 * queryOperations.setFlagsOnRequest(request)
        thrown(SourceUnavailableException)
    }

    def 'test prepare storage'() {
        setup:
        def request = Mock(StorageRequest)
        request.hasProperties() >> { false }
        sourceOperations.isSourceAvailable(_) >> { true }
        sourceOperations.getStorage() >> { Mock(StorageProvider) }

        when:
        def response = opsStorage.prepareStorageRequest(request, { return [Mock(ContentItem)] })

        then:
        1 * queryOperations.setFlagsOnRequest(request)
        response == request
    }

    def 'commit and cleanup with null request'() {
        setup:
        def path1 = Mock(Path)
        def path2 = Mock(Path)
        def path3 = Mock(Path)
        Map<String, Map<String, Path>> contentPaths = [a: [c: path1], b: [d: path2], c: [e: path3]]

        when:
        opsStorage.commitAndCleanup(null, contentPaths)

        then:
        1 * path1.toFile() >> Mock(File)
        1 * path2.toFile() >> Mock(File)
        1 * path3.toFile() >> Mock(File)
        contentPaths.isEmpty()
    }

    def 'commit and cleanup happy path'() {
        setup:
        def path1 = Mock(Path)

        Map<String, Map<String, Path>> contentPaths = [a: [b: path1]]
        def request = Mock(StorageRequest)

        and:
        def storageProvider = Mock(StorageProvider)
        sourceOperations.getStorage() >> storageProvider

        when:
        opsStorage.commitAndCleanup(request, contentPaths)

        then:
        1 * storageProvider.commit(request)
        1 * path1.toFile() >> Mock(File)
        contentPaths.isEmpty()
    }

    def 'commit and cleanup multiple happy paths per id'() {
        setup:
        def path1 = Mock(Path)
        def path2 = Mock(Path)
        def path3 = Mock(Path)
        def path4 = Mock(Path)

        Map<String, Map<String, Path>> contentPaths = [a: [c: path1, d: path2, e: path3], b: [f: path4]]
        def request = Mock(StorageRequest)

        and:
        def storageProvider = Mock(StorageProvider)
        sourceOperations.getStorage() >> storageProvider

        when:
        opsStorage.commitAndCleanup(request, contentPaths)

        then:
        1 * storageProvider.commit(request)
        1 * path1.toFile() >> Mock(File)
        1 * path2.toFile() >> Mock(File)
        1 * path3.toFile() >> Mock(File)
        1 * path4.toFile() >> Mock(File)
        contentPaths.isEmpty()
    }

    def 'commit and cleanup storage exception'() {
        setup:
        def path1 = Mock(Path)
        Map<String, Map<String, Path>> contentPaths = [a: [b: path1]]
        def request = Mock(StorageRequest)

        and:
        def storageProvider = Mock(StorageProvider)
        sourceOperations.getStorage() >> storageProvider

        when:
        opsStorage.commitAndCleanup(request, contentPaths)

        then:
        1 * storageProvider.commit(request) >> { throw new StorageException('exception') }

        then:
        1 * storageProvider.rollback(request)

        then:
        1 * path1.toFile() >> Mock(File)
        contentPaths.isEmpty()
    }

    def 'commit and cleanup two storage exceptions'() {
        setup:
        def path1 = Mock(Path)
        Map<String, Map<String, Path>> contentPaths = [a: [b: path1]]
        def request = Mock(StorageRequest)

        and:
        def storageProvider = Mock(StorageProvider)
        sourceOperations.getStorage() >> storageProvider

        when:
        opsStorage.commitAndCleanup(request, contentPaths)

        then:
        1 * storageProvider.commit(request) >> { throw new StorageException('exception') }

        then:
        1 * storageProvider.rollback(request) >> { throw new StorageException('exception') }

        then:
        1 * path1.toFile() >> Mock(File)
        contentPaths.isEmpty()
    }
}
